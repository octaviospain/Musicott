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

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

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
	private Button historyQueueButton;
	@FXML
	private Button deleteAllButton;
	@FXML
	private ListView<GridPane> listView;
	
	private ObservableList<GridPane> playQueueList;
	private ObservableList<GridPane> historyQueueList;
	private PlayerFacade player;
	
	private boolean isPlayQueueShowing = true;
	
	public PlayQueueController () {
	}
	
	@FXML
	public void initialize() {
		player = PlayerFacade.getInstance();
		playQueueList = FXCollections.observableArrayList();
		historyQueueList = FXCollections.observableArrayList();
		historyQueueButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/historyqueue-button-icon.png"))));
		historyQueueButton.setStyle("-fx-background-color: transparent");
		listView.setItems(playQueueList);
		listView.setOnMouseClicked(event -> {
			if(event.getClickCount() == 2) {
				if(isPlayQueueShowing) {
					GridPane gp = listView.getSelectionModel().getSelectedItem();
					player.playQueueIndex(playQueueList.indexOf(gp));
					playQueueList.remove(playQueueList.indexOf(gp));
					historyQueueList.add(0, gp);
					changeRemoveButton(gp);
				}
				else {
					player.playHistoryIndex(historyQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
					historyQueueList.remove(historyQueueList.indexOf(listView.getSelectionModel().getSelectedItem()));
				}
			}
		});
	}
	
	public PlayerFacade getPlayer() {
		return this.player;
	}
	
	public Track getCurrentTrack() {
		if(player != null)
			return player.getCurrentTrack();
		else
			return null;
	}
	
	public void add(List<Track> list) {
		for(Track track: list) {
			GridPane gp = new GridPane();
			VBox v = new VBox();
			ImageView iv;
			if(track.hasCover())
				iv = new ImageView(new Image(new ByteArrayInputStream(track.getCoverBytes())));
			else
				iv = new ImageView(new Image(PlayQueueController.class.getResourceAsStream("/images/default-cover-icon.png")));
			iv.setCacheHint(CacheHint.QUALITY);
			iv.setCache(false);
			iv.setSmooth(true);
			iv.setFitWidth(45.0);
			iv.setFitHeight(45.0);
			Label nameLabel = new Label();
			nameLabel.textProperty().bind(track.getNameProperty());
			Label artistAlbumLabel = new Label();
			artistAlbumLabel.textProperty().bind(Bindings.createStringBinding(
					() -> track.getArtistProperty().get()+" - "+track.getAlbumProperty().get(), track.getArtistProperty(), track.getAlbumProperty())
			);
			nameLabel.setStyle("-fx-font-size: 13px");
			artistAlbumLabel.setStyle("-fx-text-fill: #000000be; -fx-font-size: 11px;");
			Button b = new Button();
			b.setId("deleteTrackQueueButton");
			b.setMaxSize(3, 3);
			b.setMinSize(3, 3);
			b.setVisible(false);
			b.setOnAction(event -> {player.removeTrackFromPlayList(playQueueList.indexOf(gp)); playQueueList.remove(gp);});
			VBox.setMargin(nameLabel, new Insets(0,0,1,0));
			VBox.setMargin(artistAlbumLabel, new Insets(1,0,0,0));
			v.getChildren().add(nameLabel);
			v.getChildren().add(artistAlbumLabel);
			gp.add(iv, 0, 0);
			gp.add(v, 1, 0);
			gp.add(b, 1, 0);
			ColumnConstraints cc1 = new ColumnConstraints(50);
			ColumnConstraints cc2 = new ColumnConstraints(200);
			gp.getColumnConstraints().addAll(cc1,cc2);
			GridPane.setMargin(b, new Insets(1,1,1,200));
			GridPane.setMargin(iv, new Insets(0,0,0,0));
			GridPane.setMargin(v, new Insets(1,1,1,5));
			gp.setOnMouseMoved(event -> b.setVisible(true));
			gp.setOnMouseExited(event -> b.setVisible(false));
			gp.setAlignment(Pos.CENTER_LEFT);
			v.setAlignment(Pos.CENTER_LEFT);
			playQueueList.add(gp);
		}
	}
	
	public void removeTopHistoryQueue() {
		if(!historyQueueList.isEmpty())
			historyQueueList.remove(0);
	}
	
	public void removeTracks(List<? extends Track> tracks) {
		Iterator<GridPane> listIter;
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
		GridPane gp = playQueueList.get(0);
		changeRemoveButton(gp);
		historyQueueList.add(0, gp);
		playQueueList.remove(0);
	}
	
	private void changeRemoveButton(GridPane gp) {
		Button b = (Button)gp.lookup(".button");
		b.setOnAction(event -> {player.removeTrackFromHistory(historyQueueList.indexOf(gp)); historyQueueList.remove(gp);});
	}
	
	@FXML
	public void doDeleteAll() {
		if(isPlayQueueShowing) {
			if(!playQueueList.isEmpty()) {
				playQueueList.clear();
				player.removeTrackFromPlayList(-1);
				LOG.trace("Play queue cleared");
			}
		}
		else if(!historyQueueList.isEmpty()) {
			historyQueueList.clear();
			player.removeTrackFromHistory(-1);
			LOG.trace("History queue cleared");
		}
	}
	
	@FXML
	public void doHistoryQueueButton() {
		if(isPlayQueueShowing) {
			listView.setItems(historyQueueList);
			titleQueueLabel.setText("History");
			isPlayQueueShowing = false;
		}
		else {
			listView.setItems(playQueueList);
			titleQueueLabel.setText("Play Queue");
			isPlayQueueShowing = true;
		}
		LOG.trace("Changed between play and history queue");
	}
}