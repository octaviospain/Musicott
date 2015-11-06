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

package com.musicott.view.custom;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.view.PlayQueueController;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * @author Octavio Calleya
 *
 */
public class TrackQueueRow extends GridPane {
	
	private final double COVER_SIZE = 42.0;
	
	private PlayQueueController playQueueController;
	private Track track;
	private ImageView coverImage;
	private VBox labelBox;
	private Label nameLabel;
	private Label artistAlbumLabel;
	private Button deleteTrackQueueRowButton;

	public TrackQueueRow(Track track) {
		super();
		this.track = track;
		playQueueController = SceneManager.getInstance().getPlayQueueController();
		placeCover();
		placeLabels();
		placeDeleteHoverButton();
		ColumnConstraints cc1 = new ColumnConstraints(50);
		ColumnConstraints cc2 = new ColumnConstraints(200);
		getColumnConstraints().addAll(cc1,cc2);
		GridPane.setMargin(deleteTrackQueueRowButton, new Insets(1,1,1,200));
		GridPane.setMargin(coverImage, new Insets(0,0,0,0));
		GridPane.setMargin(labelBox, new Insets(1,1,1,5));
		setOnMouseMoved(event -> deleteTrackQueueRowButton.setVisible(true));
		setOnMouseExited(event -> deleteTrackQueueRowButton.setVisible(false));
		setAlignment(Pos.CENTER_LEFT);
	}
	
	public void setTrack(Track track) {
		this.track = track;
	}
	
	public Track getTrack() {
		return this.track;
	}
	
	public static List<TrackQueueRow> makeTrackQueueRows(List<Track> tracks){
		List<TrackQueueRow> tqrList = new ArrayList<>();
		for(Track t: tracks)
			tqrList.add(new TrackQueueRow(t));
		return tqrList;
	}
	
 	private void placeLabels() {
		nameLabel = new Label();
		nameLabel.setId("nameLabel");
		nameLabel.textProperty().bind(track.getNameProperty());
		artistAlbumLabel = new Label();
		artistAlbumLabel.setId("artistAlbumLabel");
		artistAlbumLabel.textProperty().bind(Bindings.createStringBinding(
				() -> track.getArtistProperty().get()+" - "+track.getAlbumProperty().get(), track.getArtistProperty(), track.getAlbumProperty())
		);
		labelBox = new VBox();
		VBox.setMargin(nameLabel, new Insets(0, 0, 1, 0));
		VBox.setMargin(artistAlbumLabel, new Insets(1, 0, 0, 0));
		labelBox.getChildren().add(nameLabel);
		labelBox.getChildren().add(artistAlbumLabel);
		labelBox.setAlignment(Pos.CENTER_LEFT);
		add(labelBox, 1, 0);
	}

	private void placeDeleteHoverButton() {
		deleteTrackQueueRowButton = new Button();
		deleteTrackQueueRowButton.setId("deleteTrackQueueRowButton");
		deleteTrackQueueRowButton.setPrefSize(3, 3);
		deleteTrackQueueRowButton.setVisible(false);
		deleteTrackQueueRowButton.setOnAction(event -> playQueueController.removeFromList(this));
		add(deleteTrackQueueRowButton, 1, 0);
	}

	private void placeCover() {
		coverImage = new ImageView();
		coverImage.setFitHeight(40.0);
		coverImage.setFitHeight(40.0);
		if(track.hasCover()) {
			coverImage.setImage(new Image(new ByteArrayInputStream(track.getCoverBytes())));
		}
		else
			coverImage.setId("coverImage");
		coverImage.setCacheHint(CacheHint.QUALITY);
		coverImage.setCache(false);
		coverImage.setSmooth(true);
		coverImage.setFitWidth(COVER_SIZE);
		coverImage.setFitHeight(COVER_SIZE);
		add(coverImage, 0, 0);
	}
}