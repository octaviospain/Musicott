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

import com.google.common.base.*;
import com.transgressoft.musicott.model.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.util.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.Optional;
import java.util.stream.*;

import static com.transgressoft.musicott.view.MusicottController.*;
import static javafx.scene.control.TableView.*;

/**
 * @author Octavio Calleya
 */
public class TrackSetAreaRow extends HBox {

    private static final double COVER_SIZE = 130.0;

    private TableView<Entry<Integer, Track>> tracksTableView;
    private TableColumn<Entry<Integer, Track>, String> nameCol;
    private TableColumn<Entry<Integer, Track>, String> artistCol;
    private TableColumn<Entry<Integer, Track>, String> genreCol;
    private TableColumn<Entry<Integer, Track>, Duration> totalTimeCol;
    private TableColumn<Entry<Integer, Track>, Number> trackNumberCol;
    private Label titleLabel;
    private Label sizeLabel;
    private Label genresLabel;
    private Label albumLabelLabel;
    private Label yearLabel;
    private ObservableList<Entry<Integer, Track>> containedTracks;

    private ListProperty<Entry<Integer, Track>> selectedTracksProperty;
    private ListProperty<Entry<Integer, Track>> containedTracksProperty;

    public TrackSetAreaRow(String title, Collection<Entry<Integer, Track>> trackEntries) {
        containedTracks = FXCollections.observableArrayList(trackEntries);
        containedTracksProperty = new SimpleListProperty<>(this, "contained tracks");
        containedTracksProperty.bind(new SimpleObjectProperty<>(containedTracks));

        titleLabel = new Label(title);
        titleLabel.setId("areaTitleLabel");
        placeLeftVBox();
        placeRightVBox();
        setPrefWidth(USE_COMPUTED_SIZE);
        setPrefHeight(USE_COMPUTED_SIZE);
        getStylesheets().add(getClass().getResource(BASE_STYLE).toExternalForm());
        tracksTableView.setItems(containedTracks);
        tracksTableView.sort();
        selectedTracksProperty = new SimpleListProperty<>(this, "selected artist tracks");
        selectedTracksProperty.bind(new SimpleObjectProperty<>(tracksTableView.getSelectionModel().getSelectedItems()));
    }

    private void placeLeftVBox() {
        ImageView coverImageView = new ImageView(getTrackSetImage());
        coverImageView.setFitWidth(COVER_SIZE);
        coverImageView.setFitHeight(COVER_SIZE);

        int trackSize = containedTracks.size();
        String tracksSizeString = trackSize == 1 ? " track" : " tracks";
        sizeLabel = new Label(trackSize + tracksSizeString);

        BorderPane coverBorderPane = new BorderPane();
        coverBorderPane.setTop(coverImageView);
        coverBorderPane.setCenter(sizeLabel);
        BorderPane.setMargin(sizeLabel, new Insets(10, 0, 0, 0));
        BorderPane.setAlignment(sizeLabel, Pos.TOP_LEFT);
        VBox.setMargin(coverImageView, new Insets(0, 0, 10, 0));
        HBox.setHgrow(coverBorderPane, Priority.NEVER);
        HBox.setMargin(coverBorderPane, new Insets(20, 20, 20, 20));
        getChildren().add(coverBorderPane);
    }

    private void placeRightVBox() {
        genresLabel = new Label(getGenresString());
        Label separatorLabel = new Label("·");
        yearLabel = new Label(getYearsString());
        albumLabelLabel = new Label(getLabelString());
        albumLabelLabel.setId("albumLabelLabel");
        if (albumLabelLabel.getText().isEmpty())
            albumLabelLabel.setVisible(false);
        else
            albumLabelLabel.setVisible(true);
        HBox textLabelsHBox = new HBox(genresLabel, separatorLabel, yearLabel);
        HBox.setMargin(separatorLabel, new Insets(0, 5, 0, 5));
        buildTracksTableView();

        VBox tracksVBox = new VBox(titleLabel, albumLabelLabel, textLabelsHBox, tracksTableView);
        VBox.setMargin(titleLabel, new Insets(0, 0, 5, 0));
        VBox.setMargin(albumLabelLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(textLabelsHBox, new Insets(0, 0, 10, 0));
        VBox.setVgrow(tracksTableView, Priority.ALWAYS);
        HBox.setHgrow(tracksVBox, Priority.SOMETIMES);
        HBox.setMargin(tracksVBox, new Insets(20, 20, 20, 0));
        getChildren().add(tracksVBox);
    }

    private Image getTrackSetImage() {
        Image trackSetImage = new Image(DEFAULT_COVER_IMAGE);
        for (Entry<Integer, Track> trackEntry : containedTracks) {
            Optional<byte[]> trackCover = trackEntry.getValue().getCoverImage();
            if (trackCover.isPresent()) {
                trackSetImage = new Image(new ByteArrayInputStream(trackCover.get()));
                break;
            }
        }
        return trackSetImage;
    }

    private String getGenresString() {
        Set<String> genres = containedTracks.stream().map(entry -> entry.getValue().getGenre())
                                            .collect(Collectors.toSet());
        return Joiner.on(", ").join(genres);
    }

    private String getYearsString() {
        Set<String> differentYears = containedTracks.stream().map(entry -> String.valueOf(entry.getValue().getYear()))
                                                    .collect(Collectors.toSet());
        return Joiner.on(", ").join(differentYears);
    }

    private String getLabelString() {
        Set<String> differentLabels = containedTracks.stream().map(entry -> entry.getValue().getLabel())
                                                .collect(Collectors.toSet());
        differentLabels.remove("");
        return Joiner.on(", ").join(differentLabels);
    }

    @SuppressWarnings ("unchecked")
    private void buildTracksTableView() {
        initColumns();
        tracksTableView = new TableView<>();
        tracksTableView.getColumns().addAll(trackNumberCol, nameCol, artistCol, genreCol, totalTimeCol);
        tracksTableView.setPrefWidth(USE_COMPUTED_SIZE);
        tracksTableView.setPrefHeight(USE_COMPUTED_SIZE);
        tracksTableView.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        tracksTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tracksTableView.getSortOrder().add(trackNumberCol);
        tracksTableView.setRowFactory(TrackTableRow::new);
        tracksTableView.addEventHandler(KeyEvent.KEY_PRESSED, getKeyPressedEventHandler());
        tracksTableView.getStylesheets().add(getClass().getResource(TRACKAREASET_TRACK_TABLE_STYLE).toExternalForm());
        tracksTableView.getStyleClass().add("noheader");
        tracksTableView.setFixedCellSize(25);
        tracksTableView.prefHeightProperty().bind(tracksTableView.fixedCellSizeProperty().multiply(
                Bindings.createDoubleBinding(() -> tracksTableView.itemsProperty().getValue().size() * 1.07,
                                              tracksTableView.itemsProperty())));
        tracksTableView.minHeightProperty().bind(tracksTableView.prefHeightProperty());
        tracksTableView.maxHeightProperty().bind(tracksTableView.prefHeightProperty());

        TrackTableViewContextMenu trackTableContextMenu = new TrackTableViewContextMenu();
        tracksTableView.setContextMenu(trackTableContextMenu);
        tracksTableView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY)
                trackTableContextMenu.show(this, event.getScreenX(), event.getScreenY());
            else if (event.getButton() == MouseButton.PRIMARY && trackTableContextMenu.isShowing())
                trackTableContextMenu.hide();
        });
    }

    private void initColumns() {
        trackNumberCol = new TableColumn<>();
        trackNumberCol.setMinWidth(25);
        trackNumberCol.setPrefWidth(25);
        trackNumberCol.setMinWidth(25);
        trackNumberCol.setCellValueFactory(cellData -> cellData.getValue().getValue().trackNumberProperty());
        trackNumberCol.setStyle("-fx-alignment: CENTER-CENTER");
        trackNumberCol.setCellFactory(column -> new NumericTableCell());

        nameCol = new TableColumn<>("Name");
        nameCol.setMinWidth(290);
        nameCol.setPrefWidth(USE_COMPUTED_SIZE);
        nameCol.setStyle("-fx-alignment: CENTER-LEFT");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());

        artistCol = new TableColumn<>("Artist");
        artistCol.setMinWidth(80);
        artistCol.setPrefWidth(USE_COMPUTED_SIZE);
        artistCol.setStyle("-fx-alignment: CENTER-LEFT");
        artistCol.setCellValueFactory(cellData -> cellData.getValue().getValue().artistProperty());

        genreCol = new TableColumn<>("Genre");
        genreCol.setMinWidth(100);
        genreCol.setPrefWidth(USE_COMPUTED_SIZE);
        genreCol.setStyle("-fx-alignment: CENTER-RIGHT");
        genreCol.setCellValueFactory(cellData -> cellData.getValue().getValue().genreProperty());

        totalTimeCol = new TableColumn<>("Duration");
        totalTimeCol.setMinWidth(60);
        totalTimeCol.setPrefWidth(60);
        totalTimeCol.setMinWidth(60);
        totalTimeCol.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(cellData.getValue().getValue().getTotalTime()));
        totalTimeCol.setStyle("-fx-alignment: CENTER-RIGHT");
        totalTimeCol.setCellFactory(column -> new DurationTableCell());
    }

    /**
     * Returns a {@link EventHandler} that fires the play of a {@link Track} when
     * the user presses the {@code Enter} key, and pauses/resumes the player when the user
     * presses the {@code Space} key.
     *
     * @return The {@code EventHandler}
     */
    private EventHandler<KeyEvent> getKeyPressedEventHandler() {
        return event -> {
            //            if (event.getCode() == KeyCode.ENTER) {
            //                List<Integer> selectionIDs = selection.stream().map(Entry::getKey).collect(Collectors
            // .toList());
            //                player.addTracksToPlayQueue(selectionIDs, true);
            //            }
            //            else if (event.getCode() == KeyCode.SPACE) {
            //                String playerStatus = player.getPlayerStatus();
            //                if ("PLAYING".equals(playerStatus))
            //                    player.pause();
            //                else if ("PAUSED".equals(playerStatus))
            //                    player.resume();
            //                else if ("STOPPED".equals(playerStatus))
            //                    player.play(true);
            //            }
        };
    }

    public ListProperty<Entry<Integer, Track>> selectedTracksProperty() {
        return selectedTracksProperty;
    }

    public ListProperty<Entry<Integer, Track>> containedTracksProperty() {
        return containedTracksProperty;
    }
}