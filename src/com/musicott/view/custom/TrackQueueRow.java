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

package com.musicott.view.custom;

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.view.*;
import javafx.beans.binding.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

import java.io.*;

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

	public TrackQueueRow(int trackID) {
		super();
		track = MusicLibrary.getInstance().getTrack(trackID).get();
		playQueueController = StageDemon.getInstance().getPlayQueueController();
		placeCover();
		placeLabels();
		placeDeleteHoverButton();
		ColumnConstraints cc1 = new ColumnConstraints(50);
		ColumnConstraints cc2 = new ColumnConstraints(200);
		getColumnConstraints().addAll(cc1,cc2);
		GridPane.setMargin(deleteTrackQueueRowButton, new Insets (1,1,1,200));
		GridPane.setMargin(coverImage, new Insets(0,0,0,0));
		GridPane.setMargin(labelBox, new Insets(1,1,1,5));
		setOnMouseMoved(event -> deleteTrackQueueRowButton.setVisible(true));
		setOnMouseExited(event -> deleteTrackQueueRowButton.setVisible(false));
		setAlignment(Pos.CENTER_LEFT);
	}
	
	public int getRepresentedTrackID() {
		return track.getTrackID();
	}
	
	public void changeRemoveButtonColor() {
		if(deleteTrackQueueRowButton.getId().equals("deleteButton-black"))
			deleteTrackQueueRowButton.setId("deleteButton-white");
		else
			deleteTrackQueueRowButton.setId("deleteButton-black");
	}
	
 	private void placeLabels() {
		nameLabel = new Label();
		nameLabel.setId("nameLabel");
		nameLabel.textProperty().bind(track.nameProperty());
		artistAlbumLabel = new Label();
		artistAlbumLabel.setId("artistAlbumLabel");
		artistAlbumLabel.textProperty().bind(Bindings.createStringBinding(
				() -> track.artistProperty().get()+" - "+track.albumProperty().get(), track.artistProperty(), track.albumProperty())
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
		deleteTrackQueueRowButton.setId("deleteButton-black");
		deleteTrackQueueRowButton.setPrefSize(3, 3);
		deleteTrackQueueRowButton.setVisible(false);
		deleteTrackQueueRowButton.setOnAction(event -> playQueueController.removeTrackQueueRow(this));
		add(deleteTrackQueueRowButton, 1, 0);
	}

	private void placeCover() {
		coverImage = new ImageView();
		coverImage.setFitHeight(40.0);
		coverImage.setFitHeight(40.0);
		if(track.hasCover()) {
			coverImage.setImage(new Image(new ByteArrayInputStream (track.getCoverBytes())));
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
