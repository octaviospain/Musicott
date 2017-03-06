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
import javafx.application.Platform;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.util.*;

import java.io.*;
import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;
import java.util.Optional;
import java.util.stream.*;

import static com.transgressoft.musicott.view.MusicottController.*;
import static com.transgressoft.musicott.view.custom.TrackTableView.*;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;

/**
 * Custom {@link HBox} that represents an set of {@link Track}s,
 * divided by a title, that will be an Album, a Genre or a Label.
 * Includes an {@link ImageView} that shows the common cover of the tracks,
 * a {@link TrackTableView} with the tracks, and some labels with useful information.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class TrackSetAreaRow extends HBox {

    private static final double COVER_SIZE = 130.0;

    private TableView<Entry<Integer, Track>> tracksTableView;
    private TableColumn<Entry<Integer, Track>, String> nameCol;
    private TableColumn<Entry<Integer, Track>, String> artistCol;
    private TableColumn<Entry<Integer, Track>, String> genreCol;
    private TableColumn<Entry<Integer, Track>, Duration> totalTimeCol;
    private TableColumn<Entry<Integer, Track>, Number> trackNumberCol;
    private String artist;
    private String album;
    private Label genresLabel;
    private Label albumLabelLabel;
    private Label yearLabel;
    private Label relatedArtistsLabel;

    private ListProperty<Entry<Integer, Track>> selectedTracksProperty;
    private ObservableList<Entry<Integer, Track>> containedTracks;
    private ListProperty<Entry<Integer, Track>> containedTracksProperty;
    private Comparator<Entry<Integer, Track>> trackEntryComparator;

    public TrackSetAreaRow(String artist, String album, Collection<Entry<Integer, Track>> trackEntries) {
        this.artist = artist;
        this.album = album;
        trackEntryComparator = trackEntryComparator();
        containedTracks = FXCollections.observableArrayList(trackEntries);
        containedTracks.sort(trackEntryComparator);
        containedTracksProperty = new SimpleListProperty<>(this, "contained tracks");
        containedTracksProperty.bind(new SimpleObjectProperty<>(containedTracks));

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
        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(Bindings.createStringBinding(this::getAlbumSizeString, containedTracksProperty));

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
        Label albumLabel = new Label(album);
        albumLabel.setId("albumTitleLabel");
        relatedArtistsLabel = new Label(getRelatedArtistsString());
        relatedArtistsLabel.setId("relatedArtistsLabel");
        relatedArtistsLabel.setAlignment(Pos.BOTTOM_LEFT);
        HBox albumAndRelatedArtistsHBox = new HBox(albumLabel, relatedArtistsLabel);
        HBox.setMargin(albumLabel, new Insets(0, 20, 0, 0));
        HBox.setMargin(relatedArtistsLabel, new Insets(0, 20, 0, 0));
        HBox.setHgrow(albumLabel, Priority.SOMETIMES);
        HBox.setHgrow(relatedArtistsLabel, Priority.SOMETIMES);
        genresLabel = new Label(getGenresString());
        Label separatorLabel = new Label("·");
        yearLabel = new Label(getYearsString());
        albumLabelLabel = new Label(getLabelString());
        albumLabelLabel.setId("albumLabelLabel");
        HBox textLabelsHBox = new HBox(genresLabel, separatorLabel, yearLabel);
        HBox.setMargin(separatorLabel, new Insets(0, 5, 0, 5));
        buildTracksTableView();

        VBox tracksVBox = new VBox(albumAndRelatedArtistsHBox, albumLabelLabel, textLabelsHBox, tracksTableView);
        VBox.setMargin(albumAndRelatedArtistsHBox, new Insets(0, 0, 5, 0));
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

    private String getAlbumSizeString() {
        int numberOfTracks = containedTracksProperty.size();
        String appendix = numberOfTracks == 1 ? " track" : " tracks";
        return String.valueOf(numberOfTracks) + appendix;
    }

    private String getRelatedArtistsString() {
        Set<String> relatedArtists = new HashSet<>();
        containedTracks.forEach(entry -> relatedArtists.addAll(entry.getValue().getArtistsInvolved()));
        relatedArtists.remove(artist);
        String relatedArtistsString = "with " + Joiner.on(", ").join(relatedArtists);
        return relatedArtists.isEmpty() ? "" : relatedArtistsString;
    }

    @SuppressWarnings ("unchecked")
    private void buildTracksTableView() {
        initColumns();
        tracksTableView = new TableView<>();
        tracksTableView.getColumns().addAll(trackNumberCol, nameCol, artistCol, genreCol, totalTimeCol);
        tracksTableView.getSortOrder().add(trackNumberCol);
        tracksTableView.setPrefWidth(USE_COMPUTED_SIZE);
        tracksTableView.setPrefHeight(USE_COMPUTED_SIZE);
        tracksTableView.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        tracksTableView.getStylesheets().add(getClass().getResource(TRACKAREASET_TRACK_TABLE_STYLE).toExternalForm());
        tracksTableView.getStyleClass().add("noheader");
        tracksTableView.setRowFactory(TrackTableRow::new);
        tracksTableView.addEventHandler(KeyEvent.KEY_PRESSED, KEY_PRESSED_ON_TRACK_TABLE_HANDLER);
        tracksTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
        trackNumberCol.setMinWidth(24);
        trackNumberCol.setMaxWidth(24);
        trackNumberCol.setCellValueFactory(cellData -> listenTrackChangesAndSort(cellData.getValue().getValue()));
        trackNumberCol.setStyle("-fx-alignment: CENTER");
        trackNumberCol.setCellFactory(column -> new NumericTableCell());

        nameCol = new TableColumn<>("Name");
        nameCol.setMinWidth(190);
        nameCol.setPrefWidth(USE_COMPUTED_SIZE);
        nameCol.setStyle("-fx-alignment: CENTER-LEFT");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());

        artistCol = new TableColumn<>("Artist");
        artistCol.setMinWidth(60);
        artistCol.setPrefWidth(USE_COMPUTED_SIZE);
        artistCol.setStyle("-fx-alignment: CENTER-LEFT");
        artistCol.setCellValueFactory(cellData -> cellData.getValue().getValue().artistProperty());

        genreCol = new TableColumn<>("Genre");
        genreCol.setMinWidth(60);
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

    private Comparator<Entry<Integer, Track>> trackEntryComparator() {
        return (te1, te2) -> {
            // TODO change this when implementing other tables for other discs in the same album
            int te1TrackNum = te1.getValue().getTrackNumber();
            int te2TrackNum = te2.getValue().getTrackNumber();
            int result = te1TrackNum - te2TrackNum;
            int te1DiscNum = te1.getValue().getDiscNumber();
            int te2DiscNum = te2.getValue().getDiscNumber();
            return result == 0 ? te1DiscNum - te2DiscNum : result;
        };
    }

    /**
     * Extracts the some properties of the track in each row and listen for the changes to update the view
     *
     * @param trackInTheRow The {@link Track} in the row
     *
     * @return The {@link IntegerProperty} of the track number
     */
    private IntegerProperty listenTrackChangesAndSort(Track trackInTheRow) {
        IntegerProperty trackNumProperty = trackInTheRow.trackNumberProperty();
        StringProperty genreProperty = trackInTheRow.genreProperty();
        IntegerProperty yearProperty = trackInTheRow.yearProperty();
        StringProperty labelProperty = trackInTheRow.labelProperty();
        StringProperty albumProperty = trackInTheRow.albumProperty();
        SetProperty<String> artistsInvolvedProperty = trackInTheRow.artistsInvolvedProperty();
        trackNumProperty.addListener((obs, oldTrackNum, newTrackNum) -> containedTracks.sort(trackEntryComparator));
        genreProperty.addListener((obs, oldGenre, newGenre) -> genresLabel.setText(getGenresString()));
        yearProperty.addListener((obs, oldYear, newYear) -> yearLabel.setText(getYearsString()));
        labelProperty.addListener((obs, oldLabel, newLabel) -> albumLabelLabel.setText(getLabelString()));
        albumProperty.addListener((obs, oldAlbum, newAlbum) -> {});
        artistsInvolvedProperty.addListener((obs, oldArtists, newArtists) -> {
            if (! newArtists.contains(artist))
                containedTracksProperty.remove(new SimpleEntry<>(trackInTheRow.getTrackId(), trackInTheRow));
            Platform.runLater(() -> relatedArtistsLabel.setText(getRelatedArtistsString()));
        });
        return trackInTheRow.trackNumberProperty();
    }

    public void removeTrack(Entry<Integer, Track> trackEntry) {
        containedTracksProperty.remove(trackEntry);
    }

    public void selectAllTracks() {
        tracksTableView.getSelectionModel().selectAll();
    }

    public void deselectAllTracks() {
        tracksTableView.getSelectionModel().clearSelection();
    }

    public String getAlbum() {
        return album;
    }

    public ListProperty<Entry<Integer, Track>> selectedTracksProperty() {
        return selectedTracksProperty;
    }

    public ListProperty<Entry<Integer, Track>> containedTracksProperty() {
        return containedTracksProperty;
    }
}