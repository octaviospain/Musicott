package net.transgressoft.musicott.view;

import net.transgressoft.musicott.logging.RingBufferHolder;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration tests for {@link LogViewerController}, verifying that the log viewer window
 * opens as a modeless resizable stage, displays captured log records in a read-only text
 * area, filters records by level, exports the held buffer to a file, and shows records
 * that were emitted before the window was opened (including boot-time and error-dialog errors).
 */
@JavaFxSpringTest(classes = LogViewerControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("LogViewerController")
class LogViewerControllerIT extends ApplicationTestBase<AnchorPane> {

    @Autowired
    FxControllerAndView<LogViewerController, AnchorPane> logViewerControllerAndView;

    @Autowired
    LogViewerFileChooserHolder fileChooserHolder;

    @Override
    @BeforeEach
    protected void beforeEach() {
        RingBufferHolder.INSTANCE.resetForTest();
        super.beforeEach();
    }

    @AfterEach
    void closeLogViewerStage() {
        LogViewerController controller = logViewerControllerAndView.getController();
        Platform.runLater(() -> {
            if (controller.stage != null && controller.stage.isShowing()) {
                controller.stage.hide();
            }
            controller.stage = null;
        });
        waitForFxEvents();
    }

    @Override
    protected AnchorPane javaFxComponent() {
        // Return a placeholder pane for the test stage, not the controller's root.
        // The controller's root is placed into its own modeless Stage by showWindow();
        // mounting it in the test stage's scene too would trigger
        // "already set as root of another scene" on the second call to showWindow().
        return new AnchorPane();
    }

    @Test
    @DisplayName("opens a modeless resizable window titled Application Logs")
    void opensAModelessResizableWindowTitledApplicationLogs() throws Exception {
        LogViewerController controller = logViewerControllerAndView.getController();

        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        Stage logStage = waitForDialogStage("Application Logs");
        assertThat(logStage).as("Application Logs stage").isNotNull();
        assertThat(logStage.isResizable()).isTrue();
        assertThat(logStage.getModality()).isEqualTo(Modality.NONE);
    }

    @Test
    @DisplayName("renders a non-editable but selectable text area")
    void rendersANonEditableButSelectableTextArea() {
        LogViewerController controller = logViewerControllerAndView.getController();
        TextArea textArea = controller.logTextArea;

        assertThat(textArea.isEditable()).isFalse();
        assertThat(textArea).isInstanceOf(TextArea.class);
    }

    @Test
    @DisplayName("shows only records at or above the selected level when the filter changes")
    void showsOnlyRecordsAtOrAboveSelectedLevelWhenFilterChanges() throws Exception {
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.test - info message\n", ch.qos.logback.classic.Level.INFO);
        RingBufferHolder.INSTANCE.add("DEBUG net.transgressoft.test - debug message\n", ch.qos.logback.classic.Level.DEBUG);
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.test - warn message\n", ch.qos.logback.classic.Level.WARN);
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.test - error message\n", ch.qos.logback.classic.Level.ERROR);

        LogViewerController controller = logViewerControllerAndView.getController();

        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        // Switch to WARN filter
        Platform.runLater(() -> {
            ComboBox<String> combo = controller.levelFilterCombo;
            combo.setValue("WARN");
            // Trigger the action listener
            combo.getOnAction().handle(null);
        });
        waitForFxEvents();

        String text = controller.logTextArea.getText();
        assertThat(text).contains("warn message");
        assertThat(text).contains("error message");
        assertThat(text).doesNotContain("info message");
        assertThat(text).doesNotContain("debug message");
    }

    @Test
    @DisplayName("exports the currently held buffer to the chosen file")
    void exportsTheCurrentlyHeldBufferToTheChosenFile(@TempDir Path tempDir) throws Exception {
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.test - export line one\n", ch.qos.logback.classic.Level.INFO);
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.test - export line two\n", ch.qos.logback.classic.Level.WARN);

        LogViewerController controller = logViewerControllerAndView.getController();

        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        // Filter the VIEW to ERROR so neither seeded line (INFO, WARN) is visible. Export must
        // still write the FULL held buffer, independent of the active level filter.
        Platform.runLater(() -> {
            controller.levelFilterCombo.setValue("ERROR");
            controller.levelFilterCombo.getOnAction().handle(null);
        });
        waitForFxEvents();
        assertThat(controller.logTextArea.getText())
                .as("ERROR filter hides the INFO/WARN lines from the displayed view")
                .doesNotContain("export line one")
                .doesNotContain("export line two");

        File exportFile = tempDir.resolve("test-export.log").toFile();
        when(fileChooserHolder.mock.showSaveDialog(any())).thenReturn(exportFile);

        Platform.runLater(controller::exportLogs);
        waitForFxEvents();

        assertThat(exportFile).exists();
        String exportContent = Files.readString(exportFile.toPath());
        assertThat(exportContent)
                .as("export writes the full buffer regardless of the active filter")
                .contains("export line one")
                .contains("export line two");
    }

    @Test
    @DisplayName("captures records emitted before the window is opened")
    void capturesRecordsEmittedBeforeTheWindowIsOpened() throws Exception {
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.test - pre-open boot message\n", ch.qos.logback.classic.Level.INFO);

        LogViewerController controller = logViewerControllerAndView.getController();

        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        String text = controller.logTextArea.getText();
        assertThat(text).contains("pre-open boot message");
    }

    @Test
    @DisplayName("live-tails records appended while the window is open")
    void liveTailsRecordsAppendedWhileTheWindowIsOpen() throws Exception {
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.test - seeded before open\n", ch.qos.logback.classic.Level.INFO);

        LogViewerController controller = logViewerControllerAndView.getController();

        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        assertThat(controller.logTextArea.getText()).contains("seeded before open");

        // A record added after the window opened must tail into the text area.
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.test - tailed after open\n", ch.qos.logback.classic.Level.WARN);
        waitForFxEvents();

        String text = controller.logTextArea.getText();
        assertThat(text).contains("seeded before open");
        assertThat(text).contains("tailed after open");
    }

    @Test
    @DisplayName("shows error-dialog errors that were added to the buffer")
    void showsErrorDialogErrorsThatWereAddedToTheBuffer() throws Exception {
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.test - simulated error-dialog error\n", ch.qos.logback.classic.Level.ERROR);
        // Set filter to include ERROR (INFO default already passes ERROR, but be explicit)
        LogViewerController controller = logViewerControllerAndView.getController();
        controller.currentLevelFilter = "INFO";

        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        String text = controller.logTextArea.getText();
        assertThat(text).contains("simulated error-dialog error");
    }

    @Test
    @DisplayName("applies a default font size on initialize")
    void appliesADefaultFontSizeOnInitialize() {
        LogViewerController controller = logViewerControllerAndView.getController();

        assertThat(controller.logTextArea.getStyle())
                .as("default 12px inline style applied at initialize")
                .contains("-fx-font-size: " + LogViewerController.DEFAULT_FONT_SIZE + "px");
    }

    @Test
    @DisplayName("increases the log font size up to the maximum and clamps")
    void increasesTheLogFontSizeUpToTheMaximumAndClamps() {
        LogViewerController controller = logViewerControllerAndView.getController();

        // Reset to default first so the test is order-independent
        Platform.runLater(() -> {
            controller.currentFontSize = LogViewerController.DEFAULT_FONT_SIZE;
            controller.logTextArea.setStyle("-fx-font-size: " + LogViewerController.DEFAULT_FONT_SIZE + "px;");
        });
        waitForFxEvents();

        // Call increase enough times to reach and exceed MAX
        int callsNeeded = (LogViewerController.MAX_FONT_SIZE - LogViewerController.DEFAULT_FONT_SIZE)
                / LogViewerController.FONT_SIZE_STEP + 2;
        for (int i = 0; i < callsNeeded; i++) {
            Platform.runLater(controller::increaseFontSize);
            waitForFxEvents();
        }

        assertThat(controller.currentFontSize)
                .as("font size must not exceed MAX after extra increase calls")
                .isEqualTo(LogViewerController.MAX_FONT_SIZE);
        assertThat(controller.logTextArea.getStyle())
                .contains("-fx-font-size: " + LogViewerController.MAX_FONT_SIZE + "px");
    }

    @Test
    @DisplayName("decreases the log font size down to the minimum and clamps")
    void decreasesTheLogFontSizeDownToTheMinimumAndClamps() {
        LogViewerController controller = logViewerControllerAndView.getController();

        // Reset to default first so the test is order-independent
        Platform.runLater(() -> {
            controller.currentFontSize = LogViewerController.DEFAULT_FONT_SIZE;
            controller.logTextArea.setStyle("-fx-font-size: " + LogViewerController.DEFAULT_FONT_SIZE + "px;");
        });
        waitForFxEvents();

        // Call decrease enough times to reach and exceed MIN
        int callsNeeded = (LogViewerController.DEFAULT_FONT_SIZE - LogViewerController.MIN_FONT_SIZE)
                / LogViewerController.FONT_SIZE_STEP + 2;
        for (int i = 0; i < callsNeeded; i++) {
            Platform.runLater(controller::decreaseFontSize);
            waitForFxEvents();
        }

        assertThat(controller.currentFontSize)
                .as("font size must not go below MIN after extra decrease calls")
                .isEqualTo(LogViewerController.MIN_FONT_SIZE);
        assertThat(controller.logTextArea.getStyle())
                .contains("-fx-font-size: " + LogViewerController.MIN_FONT_SIZE + "px");
    }
}

/**
 * Holder for the shared mutable {@link FileChooser} mock, allowing the test body
 * to configure return values after the Spring context is fully built.
 * {@code getExtensionFilters()} is pre-stubbed to return a real observable list because
 * {@code exportLogs()} calls {@code .add()} on the returned list before invoking
 * {@code showSaveDialog}.
 */
class LogViewerFileChooserHolder {
    final FileChooser mock = mock(FileChooser.class);

    LogViewerFileChooserHolder() {
        ObservableList<FileChooser.ExtensionFilter> filters = FXCollections.observableArrayList();
        when(mock.getExtensionFilters()).thenReturn(filters);
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                LogViewerController.class
        })
})
class LogViewerControllerITConfiguration {

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public Supplier<Stage> stageSupplier() {
        return Stage::new;
    }

    @Bean
    public LogViewerFileChooserHolder fileChooserHolder() {
        return new LogViewerFileChooserHolder();
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier(LogViewerFileChooserHolder holder) {
        return () -> holder.mock;
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

    // destroyMethod = "" prevents Spring from auto-inferring shutdown(), which would call
    // Platform.exit() and kill the JavaFX Application Thread between test classes
    @Bean(destroyMethod = "")
    public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext) {
        return new SpringFxWeaver(applicationContext);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public <C, V extends Node> FxControllerAndView<C, V> controllerAndView(FxWeaver fxWeaver, InjectionPoint injectionPoint) {
        return new InjectionPointLazyFxControllerAndViewResolver(fxWeaver).resolve(injectionPoint);
    }
}
