package net.transgressoft.musicott.view;

import net.transgressoft.musicott.events.ErrorEvent;
import net.transgressoft.musicott.events.OpenLogViewerEvent;
import net.transgressoft.musicott.logging.RingBufferHolder;
import net.transgressoft.musicott.logging.RingBufferHolder.LogRecord;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import ch.qos.logback.classic.Level;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Modeless, resizable window that displays captured application log records from the
 * {@link RingBufferHolder} ring buffer. The text area is read-only and scrollable; users can
 * select and copy text. A level-filter combo box limits visible entries to lines at or above
 * the chosen level (default INFO). New records appended while the window is open tail
 * automatically into the text area via a single batched {@code Platform.runLater} per burst.
 *
 * <p>The window is opened by handling an {@link OpenLogViewerEvent}, allowing publishers
 * to remain decoupled from this controller. The window is modeless — it coexists with the
 * application's main window and the error dialog without blocking interaction.
 *
 * <p>Seeding the view and subscribing to new records is done atomically through
 * {@link RingBufferHolder#attachAndSnapshot}, so no record is lost or duplicated across the
 * open/refresh handoff. All level filtering happens on the JavaFX Application Thread, using the
 * level stored with each record rather than re-parsing the encoded text.
 *
 * <p>Export writes the full current buffer snapshot to a user-chosen {@code *.log} file.
 *
 * @author Octavio Calleya
 */
@FxmlView("/fxml/LogViewerController.fxml")
@Controller
public class LogViewerController {

    private static final List<String> LEVELS = Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    static final int DEFAULT_FONT_SIZE = 12;
    static final int MIN_FONT_SIZE = 8;
    static final int MAX_FONT_SIZE = 24;
    static final int FONT_SIZE_STEP = 2;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    @FXML
    AnchorPane root;
    @FXML
    TextArea logTextArea;
    @FXML
    ComboBox<String> levelFilterCombo;
    @FXML
    Button exportButton;
    @FXML
    Button decreaseFontButton;
    @FXML
    Button increaseFontButton;

    private final Supplier<Stage> stageSupplier;
    private final Supplier<FileChooser> fileChooserSupplier;
    private final ApplicationEventPublisher eventPublisher;
    private final KeyCombination.Modifier operativeSystemKeyModifier;

    Stage stage;
    int currentFontSize = DEFAULT_FONT_SIZE;

    /**
     * Currently selected log level filter; retained across opens (singleton controller).
     * Read and written only on the JavaFX Application Thread.
     */
    String currentLevelFilter = "INFO";

    /** Queue of records received from the live-tail listener, drained on the FX thread. */
    private final ConcurrentLinkedQueue<LogRecord> pendingRecords = new ConcurrentLinkedQueue<>();

    /** Guards single-flush scheduling: only one Platform.runLater per burst of records. */
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    /** Stable listener reference so attach and compare-and-clear detach use the same instance. */
    private final Consumer<LogRecord> liveTailListener = this::onLiveTailRecord;

    @Autowired
    public LogViewerController(Supplier<Stage> stageSupplier,
                                Supplier<FileChooser> fileChooserSupplier,
                                ApplicationEventPublisher eventPublisher,
                                KeyCombination.Modifier operativeSystemKeyModifier) {
        this.stageSupplier = stageSupplier;
        this.fileChooserSupplier = fileChooserSupplier;
        this.eventPublisher = eventPublisher;
        this.operativeSystemKeyModifier = operativeSystemKeyModifier;
    }

    @FXML
    public void initialize() {
        logTextArea.setEditable(false);
        logTextArea.setWrapText(false);

        levelFilterCombo.getItems().addAll(LEVELS);
        levelFilterCombo.setValue(currentLevelFilter);
        levelFilterCombo.setOnAction(e -> {
            currentLevelFilter = levelFilterCombo.getValue();
            // Re-seed only while showing; before the first show there is no attached listener.
            if (stage != null && stage.isShowing()) {
                reseed();
            }
        });

        exportButton.setOnAction(e -> exportLogs());
        decreaseFontButton.setOnAction(e -> decreaseFontSize());
        increaseFontButton.setOnAction(e -> increaseFontSize());

        applyFontSize();
    }

    /**
     * Opens the log viewer window. If already showing, brings it to front.
     * On open the view is seeded from the buffer and subscribed to new records atomically, so
     * records emitted before the window was opened (including boot-time and error-dialog records)
     * are visible on first open and no record is lost across the handoff.
     */
    void showWindow() {
        if (stage == null) {
            stage = stageSupplier.get();
            stage.setTitle("Application Logs");
            stage.initModality(Modality.NONE);
            stage.setResizable(true);
            stage.setScene(new Scene(root, 900, 600));
            stage.getScene().getStylesheets().add("/css/logviewer.css");
            stage.getIcons().add(ApplicationImage.APP_ICON.get());
            stage.setOnHidden(e -> detachLiveTail());
            registerFontSizeAccelerators(stage.getScene());
        }
        if (!stage.isShowing()) {
            reseed();
            stage.show();
        }
        stage.toFront();
    }

    /**
     * Registers Ctrl/Cmd +/- keyboard accelerators on the viewer scene so font size can be
     * adjusted regardless of which node has focus. EQUALS covers the unshifted "+" on most
     * keyboard layouts; ADD and SUBTRACT cover the numeric keypad.
     *
     * @param scene the viewer scene to register accelerators on
     */
    private void registerFontSizeAccelerators(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.EQUALS, operativeSystemKeyModifier), this::increaseFontSize);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.PLUS, operativeSystemKeyModifier), this::increaseFontSize);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ADD, operativeSystemKeyModifier), this::increaseFontSize);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.MINUS, operativeSystemKeyModifier), this::decreaseFontSize);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.SUBTRACT, operativeSystemKeyModifier), this::decreaseFontSize);
    }

    /**
     * Increases the log text font size by {@link #FONT_SIZE_STEP} pixels, clamped at
     * {@link #MAX_FONT_SIZE}. The new size is applied as an inline style so it takes
     * precedence over the stylesheet's fixed {@code -fx-font-size} rule.
     */
    void increaseFontSize() {
        currentFontSize = Math.min(currentFontSize + FONT_SIZE_STEP, MAX_FONT_SIZE);
        applyFontSize();
    }

    /**
     * Decreases the log text font size by {@link #FONT_SIZE_STEP} pixels, clamped at
     * {@link #MIN_FONT_SIZE}. The new size is applied as an inline style so it takes
     * precedence over the stylesheet's fixed {@code -fx-font-size} rule.
     */
    void decreaseFontSize() {
        currentFontSize = Math.max(currentFontSize - FONT_SIZE_STEP, MIN_FONT_SIZE);
        applyFontSize();
    }

    /**
     * Applies the current font size as an inline style on the log text area. Inline style
     * has higher specificity than stylesheet rules, so this overrides the 12px baseline
     * declared in logviewer.css for per-session adjustments.
     */
    private void applyFontSize() {
        logTextArea.setStyle("-fx-font-size: " + currentFontSize + "px;");
    }

    /**
     * Detaches any current live-tail listener, re-registers it atomically with a fresh seed
     * snapshot, and re-renders the text area. Used both when the window opens and when the level
     * filter changes. Must be called on the JavaFX Application Thread.
     */
    private void reseed() {
        RingBufferHolder.INSTANCE.removeLiveTailListener(liveTailListener);
        pendingRecords.clear();
        flushScheduled.set(false);
        List<LogRecord> snapshot = RingBufferHolder.INSTANCE.attachAndSnapshot(liveTailListener);
        renderFull(snapshot);
    }

    /**
     * Replaces the text-area content with the filtered text of {@code records}.
     * Runs on the JavaFX Application Thread.
     */
    private void renderFull(List<LogRecord> records) {
        String filtered = records.stream()
                .filter(record -> passesFilter(record.level()))
                .map(LogRecord::text)
                .collect(Collectors.joining());
        logTextArea.setText(filtered);
        logTextArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Live-tail callback invoked on the logback appender thread. Enqueues the record and schedules
     * a single FX-thread flush per burst; filtering is deferred to the flush so the level filter
     * is only ever read on the FX thread.
     */
    private void onLiveTailRecord(LogRecord record) {
        pendingRecords.offer(record);
        if (flushScheduled.compareAndSet(false, true)) {
            Platform.runLater(this::flushPending);
        }
    }

    /** Drains queued records, appends those passing the current filter, and scrolls to the end. */
    private void flushPending() {
        flushScheduled.set(false);
        StringBuilder sb = new StringBuilder();
        LogRecord record;
        while ((record = pendingRecords.poll()) != null) {
            if (passesFilter(record.level())) {
                sb.append(record.text());
            }
        }
        if (!sb.isEmpty()) {
            logTextArea.appendText(sb.toString());
            trimToCapacity();
            logTextArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    /**
     * Trims the oldest lines from the text area so it never displays more than
     * {@link RingBufferHolder#MAX_RECORDS} lines. The ring buffer is bounded and evicts old
     * records, but live tailing only ever appends — without this the text area would grow without
     * limit over a long session with the window left open.
     */
    private void trimToCapacity() {
        int max = RingBufferHolder.MAX_RECORDS;
        int paragraphs = logTextArea.getParagraphs().size();
        if (paragraphs <= max) {
            return;
        }
        String text = logTextArea.getText();
        int excess = paragraphs - max;
        int cut = 0;
        for (int i = 0; i < excess; i++) {
            int newline = text.indexOf('\n', cut);
            if (newline < 0) {
                cut = text.length();
                break;
            }
            cut = newline + 1;
        }
        logTextArea.deleteText(0, cut);
    }

    /**
     * Deregisters the live-tail listener from the ring buffer. Called when the window is hidden.
     */
    void detachLiveTail() {
        RingBufferHolder.INSTANCE.removeLiveTailListener(liveTailListener);
        pendingRecords.clear();
        flushScheduled.set(false);
    }

    /**
     * Opens a save dialog and writes the full current buffer snapshot to the chosen {@code *.log}
     * file. The export writes the complete buffer, not filtered by the current level selection.
     * If the user cancels the dialog or an I/O error occurs, no crash results.
     */
    void exportLogs() {
        FileChooser chooser = fileChooserSupplier.get();
        chooser.setTitle("Export logs");
        chooser.setInitialFileName("musicott.log");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log files", "*.log"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            List<String> snapshot = RingBufferHolder.INSTANCE.snapshot();
            String content = String.join("", snapshot);
            try {
                Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Failed to export logs to {}", file.getAbsolutePath(), e);
                eventPublisher.publishEvent(new ErrorEvent("Export failed",
                        "Could not write log file: " + file.getName(), this));
            }
        }
    }

    /**
     * Listens for {@link OpenLogViewerEvent} and opens the log viewer window on the
     * JavaFX Application Thread.
     *
     * @param event the event signalling that the viewer should be shown
     */
    @EventListener
    public void openLogViewerEventListener(OpenLogViewerEvent event) {
        Platform.runLater(this::showWindow);
    }

    /**
     * Returns {@code true} when {@code level} is at or above the currently selected filter level.
     * Comparison uses the record's stored logback level, so it is independent of the encoder
     * pattern and cannot be defeated by a leading timestamp or thread token.
     *
     * @param level the logback level of a buffered record
     * @return {@code true} if a record at {@code level} should be shown given the current filter
     */
    boolean passesFilter(Level level) {
        if (level == null) return false;
        Level threshold = Level.toLevel(currentLevelFilter, Level.INFO);
        return level.isGreaterOrEqual(threshold);
    }
}
