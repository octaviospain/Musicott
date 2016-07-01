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
import javafx.scene.layout.*;
import org.slf4j.*;

/**
 * @author Octavio Calleya
 *
 */
public class PlayQueueController implements MusicottController {
	
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
	private ListView<TrackQueueRow> listView;
	
	private ObservableList<TrackQueueRow> playQueueList;
	private ObservableList<TrackQueueRow> historyQueueList;

	@FXML
	public void initialize() {		
		playQueueList = player.getPlayList();
		historyQueueList = player.getHistorylist();
		historyQueueButton.setId("historyQueueButton");
		listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if(oldValue != null)
				oldValue.changeRemoveButtonColor();
			if(newValue != null)
				newValue.changeRemoveButtonColor();
		});
		listView.setItems(playQueueList);
		listView.setOnMouseClicked(event -> {
			if(event.getClickCount() == 2) {
				if(historyQueueButton.isSelected()) 
					player.playHistoryIndex(historyQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
				else 
					player.playQueueIndex(playQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
			}
		});
		
	}
	
	public void removeTrackQueueRow(TrackQueueRow tqr) {
		if(historyQueueButton.isSelected())
			historyQueueList.remove(tqr);
		else
			playQueueList.remove(tqr);
	}
	
	@FXML
	public void doDeleteAll() {
		if(historyQueueButton.isSelected() && !historyQueueList.isEmpty()) {
			historyQueueList.clear();
			LOG.trace("History queue cleared");
		}
		else if(!playQueueList.isEmpty()) {
			playQueueList.clear();
			LOG.trace("Play queue cleared");
		}
	}
	
	@FXML
	public void doHistoryQueueButton() {
		if(historyQueueButton.isSelected()) {
			listView.setItems(historyQueueList);
			titleQueueLabel.setText("Recently played");
		}
		else {
			listView.setItems(playQueueList);
			titleQueueLabel.setText("Play Queue");
		}
		LOG.trace("Changed between play and history queue");
	}
}
