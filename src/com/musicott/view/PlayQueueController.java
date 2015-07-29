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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.view;

import java.util.List;

import com.musicott.model.Track;
import com.musicott.player.Mp3Player;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * @author Octavio Calleya
 *
 */
public class PlayQueueController {

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
	private Mp3Player player;
	
	private boolean isPlayQueueShowing = true;
	
	public PlayQueueController () {
	}
	
	@FXML
	public void initialize() {
		playQueueList = FXCollections.observableArrayList();
		historyQueueList = FXCollections.observableArrayList();
		FXCollections.observableHashMap();
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
	
	public void setPlayer(Mp3Player player) {
		this.player = player;
	}
	
	public void add(List<Track> list) {
		for(Track t: list) {
			GridPane gp = new GridPane();
			VBox v = new VBox();
			ImageView iv = new ImageView(new Image("file:"+t.getCoverFileName(),45,45,true,false));
			Label nameLabel = new Label(t.getName());
			Label artistAlbumLabel = new Label(t.getArtist()+" - "+t.getAlbum());
			nameLabel.setStyle("-fx-font-size: 13px");
			artistAlbumLabel.setStyle("-fx-text-fill: #000000be; -fx-font-size: 11px;");
			Button b = new Button();
			b.setId("deleteTrackQueueButton");
			b.setMaxSize(3, 3);
			b.setMinSize(3, 3);
			b.setVisible(false);
			b.setOnAction(event -> {player.removeTrackFromPlayList(playQueueList.indexOf(gp)); playQueueList.remove(gp);});
			v.getChildren().add(nameLabel);
			v.getChildren().add(artistAlbumLabel);
			gp.add(iv, 0, 0);
			gp.add(v, 1, 0);
			gp.add(b, 1, 0);
			ColumnConstraints cc1 = new ColumnConstraints(50);
			ColumnConstraints cc2 = new ColumnConstraints(200);
			gp.getColumnConstraints().addAll(cc1,cc2);
			GridPane.setMargin(b, new Insets(1,1,1,200));
			GridPane.setMargin(iv, new Insets(1,1,1,1));
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
		playQueueList.remove(tracks);
		historyQueueList.remove(tracks);
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
			playQueueList.clear();
			player.removeTrackFromPlayList(-1);
		}
		else {
			historyQueueList.clear();
			player.removeTrackFromHistory(-1);
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
	}
}