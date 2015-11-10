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
 */

package com.musicott.view;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;
import com.musicott.view.custom.TrackQueueRow;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

/**
 * @author Octavio Calleya
 *
 */
public class PlayQueueController {
	
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
	private PlayerFacade player;
	
	public PlayQueueController () {
	}
	
	@FXML
	public void initialize() {
		player = PlayerFacade.getInstance();
		playQueueList = FXCollections.observableArrayList();
		historyQueueList = FXCollections.observableArrayList();
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
				if(historyQueueButton.isSelected()) {
					player.playHistoryIndex(historyQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
					historyQueueList.remove(historyQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
				}
				else {
					TrackQueueRow gp = listView.getSelectionModel().getSelectedItem();
					player.playQueueIndex(playQueueList.indexOf(gp));
					playQueueList.remove(playQueueList.indexOf(gp));
					historyQueueList.add(0, gp);
					changeRemoveButton(gp);
				}
			}
		});
	}
	
	public void add(List<Track> list) {
		for(Track track: list) {
			playQueueList.add(new TrackQueueRow(track));
		}
	}
	
	public void removeTopHistoryQueue() {
		if(!historyQueueList.isEmpty())
			historyQueueList.remove(0);
	}
	
	public void removeTracks(List<? extends Track> tracks) {
		Iterator<TrackQueueRow> listIter;
		for(Track t: tracks) {
			listIter = playQueueList.iterator();
			while(listIter.hasNext()) {
				GridPane gp = listIter.next();
				Label l = (Label) gp.lookup(".label");
				if(l.getText().equals(t.getName())) {
					listIter.remove();
					LOG.debug("Track removed from play queue: {}", t);
				}
			}
			listIter = historyQueueList.iterator();
			while(listIter.hasNext()){
				GridPane gp = listIter.next();
				Label l = (Label) gp.lookup(".label");
				if(l.getText().equals(t.getName())) {
					listIter.remove();
					LOG.debug("Track removed from history queue: {}", t);
				}
			}
		}
	}
	
	public void moveTrackToHistory() {
		TrackQueueRow gp = playQueueList.get(0);
		changeRemoveButton(gp);
		historyQueueList.add(0, gp);
		playQueueList.remove(0);
	}
	
	private void changeRemoveButton(GridPane gp) {
		Button b = (Button)gp.lookup(".button");
		b.setOnAction(event -> {player.removeTrackFromHistory(historyQueueList.indexOf(gp)); historyQueueList.remove(gp);});
	}

	public void removeFromList(TrackQueueRow trackQueueRow) {
		if(historyQueueButton.isSelected() && !historyQueueList.isEmpty())
			historyQueueList.remove(trackQueueRow);
		else
			playQueueList.remove(trackQueueRow);
	}
	
	@FXML
	public void doDeleteAll() {
		if(historyQueueButton.isSelected() && !historyQueueList.isEmpty()) {
			historyQueueList.clear();
			player.removeTrackFromHistory(-1);
			LOG.trace("History queue cleared");
		}
		else if(!playQueueList.isEmpty()) {
			playQueueList.clear();
			player.removeTrackFromPlayList(-1);
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