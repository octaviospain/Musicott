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

package com.transgressoft.musicott.view;

import com.google.common.collect.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Controller class of the root layout of the whole application.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class RootController implements MusicottController {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private static final int HOVER_COVER_SIZE = 100;

    @FXML
    private BorderPane tableBorderPane;
    @FXML
    private BorderPane wrapperBorderPane;
    @FXML
    private BorderPane contentBorderPane;
    @FXML
    private SplitPane artistsViewSplitPane;
    @FXML
    private ImageView playlistCover;
    @FXML
    private Label playlistTracksNumberLabel;
    @FXML
    private Label playlistSizeLabel;
    @FXML
    private Label playlistTitleLabel;
    @FXML
    private HBox tableInfoHBox;
    @FXML
    private GridPane playlistInfoGridPane;
    @FXML
    private StackPane tableStackPane;
    @FXML
    private ListView<String> artistsListView;
    @FXML
    private VBox trackSetsAreaVBox;
    @FXML
    private Label nameLabel;
    @FXML
    private Label totalAlbumsLabel;
    @FXML
    private Label totalTracksLabel;
    @FXML
    private Button randomButton;
    private VBox navigationPaneVBox;
    private TextField playlistTitleTextField;

    private Image defaultCoverImage;
    private ImageView hoverCoverImageView;
    private ObjectProperty<Image> hoverCoverProperty;
    private TrackTableView trackTable = new TrackTableView();

    private ObservableList<TrackSetAreaRow> trackSetAreaRows = FXCollections.observableArrayList();

    private ListProperty<String> artistsProperty;
    private ListProperty<TrackSetAreaRow> trackSetsProperty;
    private ObjectProperty<NavigationMode> navigationModeProperty;
    private ReadOnlyObjectProperty<String> selectedArtistProperty;
    private ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty;
    private ListProperty<Entry<Integer, Track>> selectedTracksProperty;
    private BooleanProperty showingNavigationPaneProperty;
    private BooleanProperty showingTableInfoPaneProperty;

    private EventHandler<KeyEvent> changePlaylistNameTextFieldHandler = changePlaylistNameTextFieldHandler();

    private StageDemon stageDemon = StageDemon.getInstance();
    private MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private TaskDemon taskDemon = TaskDemon.getInstance();

    @FXML
    public void initialize() {
        selectedPlaylistProperty = stageDemon.getNavigationController().selectedPlaylistProperty();
        selectedPlaylistProperty.addListener(
                (obs, oldSelected, newSelected) -> newSelected.ifPresent(this::updateShowingInfoWithPlaylist));
        trackSetsProperty = new SimpleListProperty<>(trackSetAreaRows);
        totalAlbumsLabel.textProperty().bind(
                Bindings.createStringBinding(this::getAlbumString, trackSetsProperty));
        navigationModeProperty = stageDemon.getNavigationController().navigationModeProperty();
        selectedTracksProperty = new SimpleListProperty<>(this, "selection");
        selectedTracksProperty.bind(
                Bindings.createObjectBinding(this::chooseSelectedTracks, trackSetsProperty, navigationModeProperty));
        showingNavigationPaneProperty = new SimpleBooleanProperty(this, "showing navigation pane", true);
        showingTableInfoPaneProperty = new SimpleBooleanProperty(this, "showing table info pane", true);

        selectedArtistProperty = artistsListView.getSelectionModel().selectedItemProperty();
        selectedArtistProperty.addListener(((observable, oldArtist, newArtist) -> musicLibrary.showArtist(newArtist)));
        artistsProperty = musicLibrary.artistsProperty();
        artistsListView.setItems(artistsProperty);
        artistsProperty.addListener((obs, oldArtists, newArtists) -> {
            if (newArtists != null && ! newArtists.isEmpty())
                artistsListView.getSelectionModel().clearAndSelect(0);
        });

        totalTracksLabel.setText(String.valueOf(0) + " artists");
        nameLabel.textProperty().bind(Bindings.createStringBinding(this::getNameString, selectedArtistProperty));
        defaultCoverImage = new Image(DEFAULT_COVER_IMAGE);
        hoverCoverProperty = new SimpleObjectProperty<>(this, "hover cover", defaultCoverImage);
        playlistCover.imageProperty().bind(hoverCoverProperty);
        initializeInfoPaneFields();
        initializeHoverCoverImageView();
        bindSearchTextField();
        hideTableInfoPane();
    }

    /**
     * Updates the information pane with the selected {@link Playlist}
     *
     * @param playlist The selected {@code Playlist}
     */
    private void updateShowingInfoWithPlaylist(Playlist playlist) {
        playlistTitleTextField.setText(playlist.getName());
        if (playlist.isEmpty())
            hoverCoverProperty.setValue(defaultCoverImage);
        else
            hoverCoverProperty.setValue(playlist.playlistCoverProperty().getValue());
        playlist.playlistCoverProperty().addListener((obs, oldCover, newCover) -> hoverCoverProperty.setValue(newCover));
        removePlaylistTextField();
    }

    private void initializeInfoPaneFields() {
        initializePlaylistTitleTextField();
        ListProperty<Entry<Integer, Track>> showingTracksProperty = musicLibrary.showingTracksProperty();

        playlistTracksNumberLabel.textProperty().bind(Bindings.createStringBinding(
                () -> showingTracksProperty.sizeProperty().get() + " songs", showingTracksProperty));

        playlistSizeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            long sizeOfAllShowingTracks = showingTracksProperty.stream().mapToLong(
                    trackEntry -> (long) trackEntry.getValue().getSize()).sum();

            String sizeString = Utils.byteSizeString(sizeOfAllShowingTracks, 2);
            if ("0 B".equals(sizeString))
                sizeString = "";
            return sizeString;
        }, showingTracksProperty));

        playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());

        playlistTitleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2)    // double click to edit the playlist name
                placePlaylistTextField();
        });
    }

    private void initializePlaylistTitleTextField() {
        playlistTitleTextField = new TextField();
        playlistTitleTextField.setMaxWidth(150);
        playlistTitleTextField.setPrefHeight(25);
        playlistTitleTextField.setPadding(new Insets(- 10, 0, - 10, 0));
        playlistTitleTextField.setFont(new Font("Avenir", 19));
        VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
        playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
    }

    /**
     * Binds the text typed on the search text field to a filtered subset of items shown on the table
     */
    private void bindSearchTextField() {
        ObservableList<Entry<Integer, Track>> tracks = musicLibrary.showingTracksProperty().get();
        FilteredList<Entry<Integer, Track>> filteredTracks = new FilteredList<>(tracks, predicate -> true);

        StringProperty searchTextProperty = stageDemon.getPlayerController().searchTextProperty();
        searchTextProperty.addListener((observable, oldText, newText) -> filteredTracks
                .setPredicate(findTracksContainingTextPredicate(newText)));

        SortedList<Map.Entry<Integer, Track>> sortedTracks = new SortedList<>(filteredTracks);
        sortedTracks.comparatorProperty().bind(trackTable.comparatorProperty());
        trackTable.setItems(sortedTracks);
    }

    private String getNameString() {
        String selectedArtist = selectedArtistProperty.getValue();
        if (selectedArtist == null)
            selectedArtist = "";
        return selectedArtist;
    }

    private String getAlbumString() {
        int numberOfTrackSets = trackSetsProperty.size();
        String appendix = numberOfTrackSets == 1 ? " album" : " albums";
        return String.valueOf(numberOfTrackSets) + appendix;
    }

    private String getTotalArtistTracksString() {
        int totalArtistTracks =
                trackSetsProperty.stream().mapToInt(trackEntry -> trackEntry.containedTracksProperty().size()).sum();
        String appendix = totalArtistTracks == 1 ? " track" : " tracks";
        return String.valueOf(totalArtistTracks) + appendix;
    }

    private EventHandler<KeyEvent> changePlaylistNameTextFieldHandler() {
        return event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Playlist playlist = selectedPlaylistProperty.getValue().get();
                String newName = playlistTitleTextField.getText();
                if (isValidPlaylistName(newName) || playlist.getName().equals(newName)) {
                    playlist.setName(newName);
                    removePlaylistTextField();
                    taskDemon.saveLibrary(false, false, true);
                }
                event.consume();
            }
        };
    }

    private void initializeHoverCoverImageView() {
        hoverCoverImageView = new ImageView();
        hoverCoverImageView.setFitWidth(HOVER_COVER_SIZE);
        hoverCoverImageView.setFitHeight(HOVER_COVER_SIZE);
        hoverCoverImageView.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> navigationModeProperty.getValue().equals(NavigationMode.ALL_TRACKS) &&
                                ! musicLibrary.emptyLibraryProperty().get(),
                        navigationModeProperty, musicLibrary.emptyLibraryProperty()));

        hoverCoverImageView.translateXProperty().bind(
                Bindings.createDoubleBinding(
                        () -> (tableStackPane.widthProperty().doubleValue() / 2) - (HOVER_COVER_SIZE / 2) - 10,
                        tableStackPane.widthProperty()));
        hoverCoverImageView.translateYProperty().bind(
                Bindings.createDoubleBinding(
                        () -> (tableStackPane.heightProperty().doubleValue() / 2) - (HOVER_COVER_SIZE / 2) - 27,
                        tableStackPane.heightProperty()));
    }

    /**
     * Puts a text field to edit the name of the playlist
     */
    private void placePlaylistTextField() {
        showTableInfoPane();
        if (! playlistInfoGridPane.getChildren().contains(playlistTitleTextField)) {
            playlistInfoGridPane.getChildren().remove(playlistTitleLabel);
            playlistInfoGridPane.add(playlistTitleTextField, 0, 0);
            playlistTitleTextField.requestFocus();
        }
    }

    /**
     * Removes the text field and shows the label with the title of the selected or entered playlist
     */
    private void removePlaylistTextField() {
        showTableInfoPane();
        if (! playlistInfoGridPane.getChildren().contains(playlistTitleLabel)) {
            playlistInfoGridPane.getChildren().remove(playlistTitleTextField);
            playlistInfoGridPane.add(playlistTitleLabel, 0, 0);
        }
    }

    /**
     * Returns a {@link Predicate} that evaluates the match of a given {@code String} to a given track {@link Entry}
     *
     * @param string The {@code String} to match against the track
     *
     * @return The {@code Predicate}
     */
    private Predicate<Entry<Integer, Track>> findTracksContainingTextPredicate(String string) {
        return trackEntry -> {
            boolean result = string == null || string.isEmpty();
            if (! result)
                result = trackMatchesString(trackEntry.getValue(), string);
            return result;
        };
    }

    /**
     * Determines if a track matches a given string by its name, artist, label, genre or album.
     *
     * @param track  The {@link Track} to match
     * @param string The string to match against the {@code Track}
     *
     * @return {@code true} if the {@code Track matches}, {@code false} otherwise
     */
    private boolean trackMatchesString(Track track, String string) {
        boolean matchesName = track.getName().toLowerCase().contains(string.toLowerCase());
        boolean matchesArtist = track.getArtist().toLowerCase().contains(string.toLowerCase());
        boolean matchesLabel = track.getLabel().toLowerCase().contains(string.toLowerCase());
        boolean matchesGenre = track.getGenre().toLowerCase().contains(string.toLowerCase());
        boolean matchesAlbum = track.getAlbum().toLowerCase().contains(string.toLowerCase());
        return matchesName || matchesArtist || matchesLabel || matchesGenre || matchesAlbum;
    }

    /**
     * Handles the naming of a new playlist placing a {@link TextField} on top
     * of the playlist label asking the user for the name.
     *
     * @param isFolder {@code true} if the new {@link Playlist} is a folder, {@code false} otherwise
     */
    public void enterNewPlaylistName(boolean isFolder) {
        LOG.debug("Editing playlist name");
        musicLibrary.clearShowingTracks();
        placePlaylistTextField();

        Playlist newPlaylist = new Playlist("", isFolder);
        playlistCover.imageProperty().bind(newPlaylist.playlistCoverProperty());

        EventHandler<KeyEvent> newPlaylistNameTextFieldHandler = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String newPlaylistName = playlistTitleTextField.getText();

                if (isValidPlaylistName(newPlaylistName)) {
                    newPlaylist.setName(newPlaylistName);
                    removePlaylistTextField();
                    stageDemon.getNavigationController().addNewPlaylist(newPlaylist);
                    playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
                }
                event.consume();
            }
        };
        playlistTitleTextField.clear();
        playlistTitleTextField.setOnKeyPressed(newPlaylistNameTextFieldHandler);
    }

    /**
     * Ensures that a string for a playlist is valid, checking if
     * it is empty, or another playlist has the same name.
     *
     * @param newName The name of the playlist to check
     *
     * @return {@code true} if its a valid name, {@code false} otherwise
     */
    private boolean isValidPlaylistName(String newName) {
        Playlist blankPlaylist = new Playlist(newName, false);
        return ! newName.isEmpty() && ! musicLibrary.containsPlaylist(blankPlaylist);
    }

    public void addAlbumTrackSets(Multimap<String, Entry<Integer, Track>> tracksByAlbum) {
        List<TrackSetAreaRow> newTrackSets = tracksByAlbum.asMap().entrySet().stream().map(multimapEntry ->
            new TrackSetAreaRow(nameLabel.getText(), multimapEntry.getKey(), multimapEntry.getValue()))
                                                          .collect(Collectors.toList());
        trackSetAreaRows.clear();
        trackSetAreaRows.addAll(newTrackSets);
        trackSetAreaRows.sort((trackSet1, trackSet2) -> trackSet1.getAlbum().compareTo(trackSet2.getAlbum()));
        trackSetsAreaVBox.getChildren().clear();
        trackSetsAreaVBox.getChildren().addAll(this.trackSetAreaRows);
        totalTracksLabel.setText(getTotalArtistTracksString());
    }

    public void removeFromTrackSets(Entry<Integer, Track> trackEntry) {
        Iterator<TrackSetAreaRow> trackSetAreaRowIterator = trackSetAreaRows.iterator();
        while (trackSetAreaRowIterator.hasNext()) {
            TrackSetAreaRow trackSetAreaRow = trackSetAreaRowIterator.next();
            trackSetAreaRow.removeTrack(trackEntry);
            totalTracksLabel.setText(getTotalArtistTracksString());
            if (trackSetAreaRow.containedTracksProperty().isEmpty()) {
                trackSetAreaRowIterator.remove();
                trackSetsAreaVBox.getChildren().remove(trackSetAreaRow);
            }
        }
        if (artistsListView.getSelectionModel().getSelectedItem() == null) {
            artistsListView.getSelectionModel().select(0);
            artistsListView.getFocusModel().focus(0);
            artistsListView.scrollTo(0);
        }
    }

    /**
     * Shows the artists view, containing a left pane with an artist's list and
     * a panel with the tracks divided in their albums
     */
    public void showArtistsView() {
        removeTablePane();
        hideTableInfoPane();
        if (artistsListView.getSelectionModel().getSelectedItem() == null)
            artistsListView.getSelectionModel().select(0);
        if (! tableStackPane.getChildren().contains(artistsViewSplitPane)) {
            tableStackPane.getChildren().remove(tableBorderPane);
            tableStackPane.getChildren().add(artistsViewSplitPane);
            LOG.debug("Showing artists view pane");
        }
    }

    private void removeArtistsViewPane() {
        if (tableStackPane.getChildren().contains(artistsViewSplitPane))
            tableStackPane.getChildren().remove(artistsViewSplitPane);
    }

    /**
     * Shows the view with only the table containing all the tracks in the music library
     */
    public void showAllTracksView() {
        removeArtistsViewPane();
        hideTableInfoPane();
        showTablePane();
    }

    public void showTablePane() {
        if (! tableStackPane.getChildren().contains(trackTable))
            tableStackPane.getChildren().add(trackTable);
        if (! tableStackPane.getChildren().contains(hoverCoverImageView))
            tableStackPane.getChildren().add(hoverCoverImageView);
    }

    public void removeTablePane() {
        if (tableStackPane.getChildren().contains(trackTable))
            tableStackPane.getChildren().remove(trackTable);
        if (tableStackPane.getChildren().contains(hoverCoverImageView))
            tableStackPane.getChildren().remove(hoverCoverImageView);
    }

    /**
     * Shows the view with a table showing the tracks of the selected playlist and
     * a info pane in the top with information about it
     */
    public void showPlaylistView() {
        removeArtistsViewPane();
        showTableInfoPane();
        showTablePane();
    }

    /**
     * Shows the upper table info pane
     */
    public void showTableInfoPane() {
        if (! contentBorderPane.getChildren().contains(tableInfoHBox)) {
            contentBorderPane.setTop(tableInfoHBox);
            showingTableInfoPaneProperty.set(true);
            LOG.debug("Showing info pane");
        }
    }

    /**
     * Hides the upper table info pane
     */
    public void hideTableInfoPane() {
        if (contentBorderPane.getChildren().contains(tableInfoHBox)) {
            contentBorderPane.getChildren().remove(tableInfoHBox);
            showingTableInfoPaneProperty.set(false);
            LOG.debug("Hiding info pane");
        }
    }

    /**
     * Shows the left navigation pane
     */
    public void showNavigationPane() {
        if (! wrapperBorderPane.getChildren().equals(navigationPaneVBox)) {
            wrapperBorderPane.setLeft(navigationPaneVBox);
            showingNavigationPaneProperty.set(true);
            LOG.debug("Showing navigation pane");
        }
    }

    /**
     * Hides the left navigation pane
     */
    public void hideNavigationPane() {
        if (wrapperBorderPane.getChildren().contains(navigationPaneVBox)) {
            wrapperBorderPane.getChildren().remove(navigationPaneVBox);
            showingNavigationPaneProperty.set(false);
            LOG.debug("Showing navigation pane");
        }
    }

    /**
     * Shows the cover image of the track the is hovered on the table,
     * or the default cover image, on the playlist info pane, or in an floating
     * {@link ImageView} on the right bottom of the table.
     */
    public void updateTrackHoveredCover(Optional<byte[]> coverBytes) {
        hoverCoverImageView.imageProperty().bind(hoverCoverProperty);
        Image trackHoveredImage;
        if (coverBytes.isPresent())
            trackHoveredImage = new Image(new ByteArrayInputStream(coverBytes.get()));
        else
            trackHoveredImage = defaultCoverImage;
        hoverCoverProperty.setValue(trackHoveredImage);
    }

    public void setNavigationPane(VBox navigationPaneVBox) {
        this.navigationPaneVBox = navigationPaneVBox;
    }

    public void selectAllTracks() {
        NavigationMode mode = navigationModeProperty.getValue();
        switch (mode) {
            case ALL_TRACKS:
            case PLAYLIST:
                trackTable.getSelectionModel().selectAll();
                break;
            case ARTISTS:
                trackSetsProperty.forEach(TrackSetAreaRow::selectAllTracks);
                break;
        }
    }

    public void deselectAllTracks() {
        NavigationMode mode = navigationModeProperty.getValue();
        switch (mode) {
            case ALL_TRACKS:
            case PLAYLIST:
                trackTable.getSelectionModel().clearSelection();
                break;
            case ARTISTS:
                trackSetsProperty.forEach(TrackSetAreaRow::deselectAllTracks);
                break;
        }
    }

    private ObservableList<Entry<Integer, Track>> chooseSelectedTracks() {
        ObservableList<Entry<Integer, Track>> selectedTracks = null;
        NavigationMode mode = navigationModeProperty.getValue();
        switch (mode) {
            case ALL_TRACKS:
            case PLAYLIST:
                selectedTracks = trackTable.getSelectionModel().getSelectedItems();
                break;
            case ARTISTS:
                selectedTracks = trackSetsProperty.stream()
                                                  .flatMap(entry -> entry.selectedTracksProperty().stream())
                                                  .collect(Collectors.toCollection(FXCollections::observableArrayList));
                break;
        }
        return selectedTracks;
    }

    public ListProperty<Entry<Integer, Track>> selectedTracksProperty () {
        return selectedTracksProperty;
    }

    public ReadOnlyBooleanProperty showNavigationPaneProperty() {
        return showingNavigationPaneProperty;
    }

    public ReadOnlyBooleanProperty showTableInfoPaneProperty() {
        return showingTableInfoPaneProperty;
    }
}
