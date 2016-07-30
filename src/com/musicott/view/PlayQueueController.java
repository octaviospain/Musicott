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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.musicott.view;

import com.musicott.view.custom.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import org.slf4j.*;

/**
 * Controller class of the play queue pane.
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class PlayQueueController implements MusicottController {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	@FXML
	private Label titleQueueLabel;
	@FXML
	private ToggleButton historyQueueButton;
	@FXML
	private Button deleteAllButton;
	@FXML
	private ListView<TrackQueueRow> listView;

	private ObservableList<TrackQueueRow> playQueueList;
	private ObservableList<TrackQueueRow> historyQueueList;

	@FXML
	public void initialize() {
		playQueueList = player.getPlayList();
		historyQueueList = player.getHistorylist();
		historyQueueButton.setId("historyQueueButton");
		listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if (oldValue != null)
				oldValue.changeRemoveButtonColor();
			if (newValue != null)
				newValue.changeRemoveButtonColor();
		});
		listView.setItems(playQueueList);
		listView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2)
				if (historyQueueButton.isSelected())
					player.playHistoryIndex(historyQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
				else
					player.playQueueIndex(playQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
		});
		deleteAllButton.setOnAction(event -> {
			if (historyQueueButton.isSelected() && ! historyQueueList.isEmpty())
				clearPlayQueue();
			else
				clearHistoryQueue();
		});
		historyQueueButton.setOnAction(event -> {
			if (historyQueueButton.isSelected())
				showHistoryQueue();
			else
				showPlayQueue();
		});
	}

	/**
	 * Removes a {@link TrackQueueRow} from the play queue pane
	 *
	 * @param trackQueueRow The <tt>TrackQueueRow</tt> the track queue row to remove
	 */
	public void removeTrackQueueRow(TrackQueueRow trackQueueRow) {
		if (historyQueueButton.isSelected())
			historyQueueList.remove(trackQueueRow);
		else
			playQueueList.remove(trackQueueRow);
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
		listView.setItems(historyQueueList);
		titleQueueLabel.setText("Recently played");
		LOG.trace("Showing history queue on the pane");
	}

	private void showPlayQueue() {
		listView.setItems(playQueueList);
		titleQueueLabel.setText("Play Queue");
		LOG.trace("Showing play queue on the pane");
	}
}
