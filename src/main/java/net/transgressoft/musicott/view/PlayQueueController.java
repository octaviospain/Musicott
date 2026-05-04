package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.view.custom.table.AudioItemTableViewBase;
import net.transgressoft.musicott.view.custom.table.TrackQueueListCell;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;

/**
 * Controller for the play queue and history queue pane. Uses {@link PlayerService} directly
 * to access the queue and history observable lists, eliminating the previous coupling to
 * {@link PlayerController}. Supports toggling between queue and history views, clearing
 * each list independently, and accepting drag-and-drop from the library table.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/PlayQueueController.fxml")
@Controller
public class PlayQueueController {

    private static final String DRAG_OVER_STYLE = "-fx-border-color: rgb(99, 255, 109); -fx-border-width: 2px;";
    // TrackQueueRow renders a 42px cover next to two label rows, plus the cell's own padding.
    // Used as the fixed cell size so the ListView's intrinsic prefHeight = items * cellSize and
    // the bottom-aligning VBox can shrink it to fit content with empty space at the top.
    private static final double TRACK_QUEUE_ROW_CELL_HEIGHT = 54.0;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final PlayerService playerService;
    private final ObservableAudioLibrary audioLibrary;

    private ObservableList<TrackQueueRow> playQueueList;
    private ObservableList<TrackQueueRow> historyQueueList;

    @FXML
    private AnchorPane playQueuePane;
    @FXML
    private Label titleQueueLabel;
    @FXML
    private ToggleButton historyQueueButton;
    @FXML
    private Button deleteAllButton;
    @FXML
    private ListView<TrackQueueRow> queuesListView;

    @Autowired
    public PlayQueueController(PlayerService playerService, ObservableAudioLibrary audioLibrary) {
        this.playerService = playerService;
        this.audioLibrary = audioLibrary;
    }

    @FXML
    public void initialize() {
        playQueueList = playerService.getPlayQueueList();
        historyQueueList = playerService.getHistoryQueueList();

        playQueueList.addListener(queueChangeListener(playQueueList, "play queue"));
        historyQueueList.addListener(queueChangeListener(historyQueueList, "history queue"));

        historyQueueButton.setId("historyQueueButton");
        queuesListView.setCellFactory(_ -> new TrackQueueListCell(() -> historyQueueButton.isSelected()));
        queuesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // Fixed cell size + height bound to current items lets the bottom-aligning VBox in the FXML
        // shrink the list to its content so the rows hug the bottom of the popover.
        queuesListView.setFixedCellSize(TRACK_QUEUE_ROW_CELL_HEIGHT);
        queuesListView.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> queuesListView.getItems() == null
                        ? 0.0
                        : queuesListView.getItems().size() * TRACK_QUEUE_ROW_CELL_HEIGHT,
                queuesListView.itemsProperty(),
                Bindings.size(playQueueList),
                Bindings.size(historyQueueList)));
        queuesListView.maxHeightProperty().bind(queuesListView.prefHeightProperty());
        queuesListView.setItems(playQueueList);
        queuesListView.setOnMouseClicked(this::onQueueListClicked);

        queuesListView.setOnDragOver(this::onDragOverQueue);
        queuesListView.setOnDragDropped(this::onDragDroppedQueue);
        queuesListView.setOnDragExited(this::onDragExitedQueue);

        deleteAllButton.setOnAction(event -> {
            if (historyQueueButton.isSelected())
                clearHistoryQueue();
            else
                clearPlayQueue();
        });
        historyQueueButton.setOnAction(event -> {
            if (historyQueueButton.isSelected())
                showHistoryQueue();
            else
                showPlayQueue();
        });
    }

    private ListChangeListener<TrackQueueRow> queueChangeListener(ObservableList<TrackQueueRow> list, String label) {
        return change -> {
            while (change.next()) {
                change.getRemoved().forEach(TrackQueueRow::dispose);
                change.getAddedSubList().forEach(trackQueueRow -> trackQueueRow.setOnDeleteButtonClickedHandler(event -> {
                    list.remove(trackQueueRow);
                    logger.debug("Removing track from {} by clicking the button. Size: {}", label, list.size());
                }));
            }
        };
    }

    private void onQueueListClicked(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() != 2) {
            return;
        }
        TrackQueueRow selected = queuesListView.getSelectionModel().getSelectedItem();
        if (historyQueueButton.isSelected()) {
            playerService.playFromHistoryQueue(selected);
        } else {
            playerService.playFromQueue(selected);
        }
    }

    private void onDragOverQueue(DragEvent event) {
        if (event.getDragboard().hasContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT) && !historyQueueButton.isSelected()) {
            event.acceptTransferModes(TransferMode.COPY);
            queuesListView.setStyle(DRAG_OVER_STYLE);
        }
        event.consume();
    }

    @SuppressWarnings("unchecked")
    private void onDragDroppedQueue(DragEvent event) {
        var dragboard = event.getDragboard();
        if (dragboard.hasContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT)) {
            var audioItemIds = (List<Integer>) dragboard.getContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT);
            var resolvedItems = audioItemIds.stream()
                    .map(id -> audioLibrary.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .map(ObservableAudioItem.class::cast)
                    .toList();
            if (!resolvedItems.isEmpty()) {
                playerService.addToQueue(resolvedItems);
                logger.debug("Added {} tracks to queue from drag-drop", resolvedItems.size());
            }
            event.setDropCompleted(true);
        }
        queuesListView.setStyle("");
        event.consume();
    }

    private void onDragExitedQueue(DragEvent event) {
        queuesListView.setStyle("");
        event.consume();
    }

    private void clearHistoryQueue() {
        historyQueueList.clear();
        logger.trace("History queue cleared");
    }

    private void clearPlayQueue() {
        playQueueList.clear();
        logger.trace("Play queue cleared");
    }

    private void showHistoryQueue() {
        queuesListView.setItems(historyQueueList);
        queuesListView.getSelectionModel().clearSelection();
        titleQueueLabel.setText("Recently played");
        logger.trace("Showing history queue on the pane");
    }

    private void showPlayQueue() {
        queuesListView.setItems(playQueueList);
        queuesListView.getSelectionModel().clearSelection();
        titleQueueLabel.setText("Play Queue");
        logger.trace("Showing play queue on the pane");
    }
}
