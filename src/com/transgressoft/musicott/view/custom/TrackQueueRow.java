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

package com.transgressoft.musicott.view.custom;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.view.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

import static org.fxmisc.easybind.EasyBind.*;

/**
 * Class that represents a single {@link Track} in the play queue or the history queue.
 * It contains labels showing the artist and track names, the cover image, and a button
 * to remove the {@code TrackQueueRow} from his list.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class TrackQueueRow extends GridPane {

    private static final double COVER_SIZE = 42.0;
    private final PlayQueueController playQueueController;
    private final Track track;

    private ImageView coverImage;
    private VBox labelBox;
    private Button deleteTrackQueueRowButton;

    public TrackQueueRow(Track track, PlayQueueController playQueueController) {
        super();
        this.track = track;
        this.playQueueController = playQueueController;
        placeCover();
        placeLabels();
        placeDeleteHoverButton();
        ColumnConstraints cc1 = new ColumnConstraints(50);
        ColumnConstraints cc2 = new ColumnConstraints(200);
        getColumnConstraints().addAll(cc1, cc2);
        GridPane.setMargin(deleteTrackQueueRowButton, new Insets(1, 1, 1, 200));
        GridPane.setMargin(coverImage, new Insets(0, 0, 0, 0));
        GridPane.setMargin(labelBox, new Insets(1, 1, 1, 5));
        setOnMouseMoved(event -> deleteTrackQueueRowButton.setVisible(true));
        setOnMouseExited(event -> deleteTrackQueueRowButton.setVisible(false));
        setAlignment(Pos.CENTER_LEFT);
    }

    public Track getRepresentedTrack() {
        return track;
    }

    private void placeCover() {
        coverImage = new ImageView();
        coverImage.setId("coverImage");
        subscribe(track.hasCoverProperty(), c -> Utils.updateCoverImage(track, coverImage));
        coverImage.setCacheHint(CacheHint.QUALITY);
        coverImage.setCache(false);
        coverImage.setSmooth(true);
        coverImage.setFitWidth(COVER_SIZE);
        coverImage.setFitHeight(COVER_SIZE);
        add(coverImage, 0, 0);
    }

    private void placeLabels() {
        Label nameLabel = new Label();
        nameLabel.setId("nameLabel");
        nameLabel.textProperty().bind(track.nameProperty());
        Label artistAlbumLabel = new Label();
        artistAlbumLabel.setId("artistAlbumLabel");
        artistAlbumLabel.textProperty().bind(
                combine(track.artistProperty(), track.albumProperty(), (art, alb) -> art + " - " + alb));
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
        deleteTrackQueueRowButton.setId("deleteButton-white");
        deleteTrackQueueRowButton.setVisible(false);
        deleteTrackQueueRowButton.setOnAction(event -> playQueueController.removeTrackQueueRow(this));
        add(deleteTrackQueueRowButton, 1, 0);
    }
}
