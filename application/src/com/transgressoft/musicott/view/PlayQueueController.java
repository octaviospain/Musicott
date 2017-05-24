/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.view;

import com.google.inject.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.*;

/**
 * Controller class of the play queue pane.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
@Singleton
public class PlayQueueController extends InjectableController<AnchorPane> {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

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

    private PlayerFacade player;
    private ObservableList<TrackQueueRow> playQueueList;
    private ObservableList<TrackQueueRow> historyQueueList;

    @FXML
    public void initialize() {
        playQueueList = player.getPlayList();
        historyQueueList = player.getHistoryList();
        historyQueueButton.setId("historyQueueButton");
        queuesListView.setCellFactory(listView -> new TrackQueueListCell(this));
        queuesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        queuesListView.setItems(playQueueList);
        queuesListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2)
                if (historyQueueButton.isSelected())
                    player.playHistoryIndex(
                            historyQueueList.indexOf(queuesListView.getSelectionModel().getSelectedItem()));
                else
                    player.playQueueIndex(playQueueList.indexOf(queuesListView.getSelectionModel().getSelectedItem()));
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
        LOG.debug("PlayQueueController initialized {}", this);
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

    public boolean isShowingHistoryQueue() {
        return queuesListView.getItems().equals(historyQueueList);
    }

    @Inject
    public void setPlayer(PlayerFacade player) {
        this.player = player;
        LOG.debug("playerFacade setted");
    }

    /**
     * Removes a {@link TrackQueueRow} from the play queue pane
     *
     * @param trackQueueRow The {@code TrackQueueRow} the track queue row to remove
     */
    public void removeTrackQueueRow(TrackQueueRow trackQueueRow) {
        if (historyQueueButton.isSelected())
            historyQueueList.remove(trackQueueRow);
        else
            playQueueList.remove(trackQueueRow);
    }
}
