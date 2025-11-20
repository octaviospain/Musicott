package net.transgressoft.musicott.view;

import net.transgressoft.musicott.view.custom.table.TrackQueueListCell;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/PlayQueueController.fxml")
@Controller
public class PlayQueueController {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final PlayerController playerController;

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
    public PlayQueueController(PlayerController playerController) {
        this.playerController = playerController;
    }

    @FXML
    public void initialize() {
        historyQueueButton.setId("historyQueueButton");
        queuesListView.setCellFactory(listView -> new TrackQueueListCell(this));
        queuesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        queuesListView.setItems(playQueueList);
        queuesListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2)
                if (historyQueueButton.isSelected()) {
                    playerController.playFromHistoryQueue(queuesListView.getSelectionModel().getSelectedItem());
                } else {
                    playerController.playFromQueue(queuesListView.getSelectionModel().getSelectedItem());
                }
        });
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

    private void clearHistoryQueue() {
        historyQueueList.clear();
        LOG.trace("History queue cleared");
    }

    private void clearPlayQueue() {
        playQueueList.clear();
        LOG.trace("Play queue cleared");
    }

    private void showHistoryQueue() {
        queuesListView.setItems(historyQueueList);
        queuesListView.getSelectionModel().clearSelection();
        titleQueueLabel.setText("Recently played");
        LOG.trace("Showing history queue on the pane");
    }

    private void showPlayQueue() {
        queuesListView.setItems(playQueueList);
        queuesListView.getSelectionModel().clearSelection();
        titleQueueLabel.setText("Play Queue");
        LOG.trace("Showing play queue on the pane");
    }

    public void setPlayQueueList(ObservableList<TrackQueueRow> playQueueList) {
        this.playQueueList = playQueueList;

        // Remove the item from the list when the delete button is clicked on each TrackQueueRow
        playQueueList.addListener((ListChangeListener<TrackQueueRow>) change -> {
                while (change.next()) {
                    change.getAddedSubList().forEach(trackQueueRow -> trackQueueRow.setOnDeleteButtonClickedHandler(event -> {
                        playQueueList.remove(trackQueueRow);
                        LOG.debug("Removing track from play queue by clicking the button. Queue size: {}", playQueueList.size());
                    }));
                }
        });
    }

    public void setHistoryQueueList(ObservableList<TrackQueueRow> historyQueueList) {
        this.historyQueueList = historyQueueList;

        // Remove the item from the list when the delete button is clicked on each TrackQueueRow
        historyQueueList.addListener((ListChangeListener<TrackQueueRow>) change ->
                change.getAddedSubList().forEach(trackQueueRow -> trackQueueRow.setOnDeleteButtonClickedHandler(event -> {
                    historyQueueList.remove(trackQueueRow);
                    LOG.debug("Removing track from history queue by clicking the button. History queue size: {}", playQueueList.size());
                })));
    }

    public boolean isShowingHistoryQueue() {
        return queuesListView.getItems().equals(historyQueueList);
    }
}
