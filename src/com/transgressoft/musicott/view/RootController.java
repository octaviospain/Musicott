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
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.factories.*;
import com.transgressoft.musicott.view.custom.*;
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
import javafx.stage.Stage;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.function.*;

import static com.transgressoft.musicott.model.NavigationMode.*;
import static org.fxmisc.easybind.EasyBind.*;

/**
 * Controller class of the root layout of the whole application.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class RootController implements MusicottController, ConfigurableController {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private static final int HOVER_COVER_SIZE = 100;

    private final MusicLibrary musicLibrary;
    private final PlaylistsLibrary playlistsLibrary;
    private final TaskDemon taskDemon;
    private final PlaylistFactory playlistFactory;
    private final TrackTableView trackTable;

    private final Playlist ROOT_PLAYLIST;

    @FXML
    private VBox headerVBox;
    @FXML
    private BorderPane tableBorderPane;
    @FXML
    private BorderPane wrapperBorderPane;
    @FXML
    private BorderPane contentBorderPane;
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
    private Button playRandomButton;
    @FXML
    private StackPane tableStackPane;
    @FXML
    private SplitPane artistsLayout;

    @FXML
    private ArtistsViewController artistsLayoutController;
    @FXML
    private VBox navigationLayout;
    @FXML
    private NavigationController navigationLayoutController;
    @FXML
    private GridPane playerLayout;
    @FXML
    private PlayerController playerLayoutController;
    @FXML
    private MenuBar menuBar;
    @FXML
    private RootMenuBarController menuBarController;

    private TextField playlistTitleTextField;

    private ImageView hoverCoverImageView;
    private ObjectProperty<Image> hoverCoverProperty;

    private ObjectProperty<NavigationMode> navigationModeProperty;
    private ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty;
    private BooleanProperty showingNavigationPaneProperty;
    private BooleanProperty showingTableInfoPaneProperty;
    private ListProperty<Entry<Integer, Track>> showingTracksProperty;

    private EventHandler<KeyEvent> changePlaylistNameTextFieldHandler = changePlaylistNameTextFieldHandler();

    @Inject
    public RootController(MusicLibrary musicLibrary, PlaylistsLibrary playlistsLibrary, TaskDemon taskDemon,
            PlaylistFactory playlistFactory, TrackTableView trackTable) {
        this.musicLibrary = musicLibrary;
        this.playlistsLibrary = playlistsLibrary;
        this.taskDemon = taskDemon;
        this.playlistFactory = playlistFactory;
        this.trackTable = trackTable;
        ROOT_PLAYLIST = playlistFactory.create("ROOT", true);
    }


    @FXML
    public void initialize() {
        showingNavigationPaneProperty = new SimpleBooleanProperty(this, "showing navigation pane", true);
        showingTableInfoPaneProperty = new SimpleBooleanProperty(this, "showing table info pane", true);
        hoverCoverProperty = new SimpleObjectProperty<>(this, "hover cover", DEFAULT_COVER);
        playlistCover.imageProperty().bind(hoverCoverProperty);
        initializeInfoPaneFields();
        hideTableInfoPane();
    }

    @Override
    public void configure() {
        trackTable.setItems(bindedToSearchTextFieldTracks());
        selectedPlaylistProperty = navigationLayoutController.selectedPlaylistProperty();
        subscribe(selectedPlaylistProperty, selected -> selected.ifPresent(this::updateShowingInfoWithPlaylist));

        navigationModeProperty = navigationLayoutController.navigationModeProperty();

        initializeHoverCoverImageView();
//        artistsLayoutController.setPlayerController(playerLayoutController);
//        navigationLayoutController.setRootController(this);
        navigationLayoutController.setNavigationMode(ARTISTS);
    }

    public void setStage(Stage mainStage) {
        String os = System.getProperty("os.name");
        menuBarController.setMainStage(mainStage);
        if (os != null && os.startsWith("Mac")) {
            menuBarController.macMenuBar();
            headerVBox.getChildren().remove(menuBar);
        }
        else
            menuBarController.defaultMenuBar();
    }

    /**
     * Updates the information pane with the selected {@link Playlist}
     *
     * @param playlist The selected {@code Playlist}
     */
    private void updateShowingInfoWithPlaylist(Playlist playlist) {
        playlistTitleTextField.setText(playlist.getName());
        subscribe(playlist.playlistCoverProperty(), hoverCoverProperty::setValue);
        removePlaylistTextField();
    }

    private void initializeInfoPaneFields() {
        initializePlaylistTitleTextField();
        playRandomButton.visibleProperty().bind(showingTracksProperty.emptyProperty().not());
        playRandomButton.setOnAction(e -> {
            if (selectedPlaylistProperty().get().isPresent())
                musicLibrary.playPlaylistRandomly(selectedPlaylistProperty().get().get());
        });
        playlistTracksNumberLabel.textProperty().bind(map(showingTracksProperty.sizeProperty(), s -> s + " songs"));
        playlistSizeLabel.textProperty().bind(map(showingTracksProperty, this::tracksSizeString));
        playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());
        playlistTitleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2)    // double click to edit the playlist name
                placePlaylistTextField();
        });
    }

    private String tracksSizeString(List<Entry<Integer, Track>> entries) {
        long sizeOfAllShowingTracks = entries.stream().mapToLong(trackEntry -> trackEntry.getValue().getSize()).sum();
        String sizeString = Utils.byteSizeString(sizeOfAllShowingTracks, 2);
        if ("0 B".equals(sizeString))
            sizeString = "";
        return sizeString;
    }

    private void initializePlaylistTitleTextField() {
        playlistTitleTextField = new TextField();
        playlistTitleTextField.setMaxWidth(350);
        playlistTitleTextField.setPrefHeight(25);
        playlistTitleTextField.setPadding(new Insets(- 10, 0, - 10, 0));
        playlistTitleTextField.setFont(new Font("Avenir", 19));
        VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
        playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
    }

    /**
     * Binds the text typed on the search text field to a filtered subset of items shown on the table
     */
    private SortedList<Entry<Integer, Track>> bindedToSearchTextFieldTracks() {
        ObservableList<Entry<Integer, Track>> tracks = showingTracksProperty.get();
        FilteredList<Entry<Integer, Track>> filteredTracks = new FilteredList<>(tracks, entry -> true);

        StringProperty searchTextProperty = playerLayoutController.searchTextProperty();
        subscribe(searchTextProperty, query -> filteredTracks.setPredicate(filterTracksByQuery(query)));

        SortedList<Entry<Integer, Track>> sortedTracks = new SortedList<>(filteredTracks);
        sortedTracks.comparatorProperty().bind(trackTable.comparatorProperty());
        return sortedTracks;
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
                combine(navigationModeProperty, musicLibrary.emptyLibraryProperty(),
                             (mode, empty) -> mode.equals(ALL_TRACKS) && ! empty));
        hoverCoverImageView.translateXProperty().bind(
                map(tableStackPane.widthProperty(),
                             width -> (width.doubleValue() / 2) - (HOVER_COVER_SIZE / 2) - 10));
        hoverCoverImageView.translateYProperty().bind(
                map(tableStackPane.heightProperty(),
                             height -> (height.doubleValue() / 2) - (HOVER_COVER_SIZE / 2) - 27));
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
     * @param query The {@code String} to match against the track
     *
     * @return The {@code Predicate}
     */
    private Predicate<Entry<Integer, Track>> filterTracksByQuery(String query) {
        return trackEntry -> {
            boolean result = query == null || query.isEmpty();
            if (! result)
                result = TracksLibrary.trackMatchesString(trackEntry.getValue(), query);
            return result;
        };
    }

    /**
     * Handles the naming of a new playlist placing a {@link TextField} on top
     * of the playlist label asking the user for the name.
     *
     * @param isFolder {@code true} if the new {@link Playlist} is a folder, {@code false} otherwise
     */
    public void enterNewPlaylistName(boolean isFolder) {
        LOG.debug("Editing playlist name");
        showingTracksProperty.clear();
        removeArtistsViewPane();
        placePlaylistTextField();

        Playlist newPlaylist = playlistFactory.create("", isFolder);
        playlistCover.imageProperty().bind(newPlaylist.playlistCoverProperty());
        playlistTitleTextField.clear();
        playlistTitleTextField.setOnKeyPressed(getNameTextFieldHandler(newPlaylist));
    }

    public EventHandler<KeyEvent> getNameTextFieldHandler(Playlist playlist) {
        return event -> {
            String newPlaylistName = playlistTitleTextField.getText();
            if (event.getCode() == KeyCode.ENTER && isValidPlaylistName(newPlaylistName)) {
                playlist.setName(newPlaylistName);
                removePlaylistTextField();
                playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
                addPlaylistToRoot(playlist);
                event.consume();
            }
        };
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
        return ! newName.isEmpty() && ! playlistsLibrary.containsPlaylistName(newName);
    }

    private void addPlaylistToRoot(Playlist playlist) {
        navigationLayoutController.addNewPlaylist(ROOT_PLAYLIST, playlist, true);
        playlistsLibrary.addPlaylistToRoot(playlist);
    }

    public void setArtistTrackSets(Multimap<String, Entry<Integer, Track>> tracksByAlbum) {
        artistsLayoutController.setArtistTrackSets(tracksByAlbum);
    }

    public void removeFromTrackSets(Entry<Integer, Track> trackEntry) {
        artistsLayoutController.removeFromTrackSets(trackEntry);
    }

    public void updateShowingTrackSets() {
        artistsLayoutController.updateShowingTrackSets();
    }

    /**
     * Shows the artists view, containing a left pane with an artist's list and
     * a panel with the tracks divided in their albums
     */
    public void showArtistsView() {
        removeTablePane();
        hideTableInfoPane();
        artistsLayoutController.checkSelectedArtist();
        if (! tableStackPane.getChildren().contains(artistsLayout)) {
            tableStackPane.getChildren().remove(tableBorderPane);
            tableStackPane.getChildren().add(artistsLayout);
            LOG.debug("Showing artists view pane");
        }
    }

    private void removeArtistsViewPane() {
        if (tableStackPane.getChildren().contains(artistsLayout))
            tableStackPane.getChildren().remove(artistsLayout);
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
        if (! wrapperBorderPane.getChildren().equals(navigationLayout)) {
            wrapperBorderPane.setLeft(navigationLayout);
            showingNavigationPaneProperty.set(true);
            LOG.debug("Showing navigation pane");
        }
    }

    /**
     * Hides the left navigation pane
     */
    public void hideNavigationPane() {
        if (wrapperBorderPane.getChildren().contains(navigationLayout)) {
            wrapperBorderPane.getChildren().remove(navigationLayout);
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
        trackHoveredImage = coverBytes.map(bytes -> new Image(new ByteArrayInputStream(bytes))).orElse(DEFAULT_COVER);
        hoverCoverProperty.setValue(trackHoveredImage);
    }

    public void selectTrack(Entry<Integer, Track> entryToSelect) {
        NavigationMode mode = navigationModeProperty.getValue();
        switch (mode) {
            case ALL_TRACKS:
                trackTable.selectFocusAndScroll(entryToSelect);
                break;
            case PLAYLIST:
                if (! navigationLayoutController.selectPlaylistOfTrack(entryToSelect))
                    navigationLayoutController.setNavigationMode(ALL_TRACKS);
                trackTable.selectFocusAndScroll(entryToSelect);
                break;
            case ARTISTS:
                artistsLayoutController.selectTrack(entryToSelect);
                break;
        }
    }

    public void selectAllTracks() {
        NavigationMode mode = navigationModeProperty.getValue();
        switch (mode) {
            case ALL_TRACKS:
            case PLAYLIST:
                trackTable.getSelectionModel().selectAll();
                break;
            case ARTISTS:
                artistsLayoutController.selectAllTracks();
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
                artistsLayoutController.deselectAllTracks();
                break;
        }
    }

    @Inject
    public void setShowingTracksProperty(ListProperty<Entry<Integer, Track>> showingTracksProperty) {
        this.showingTracksProperty = showingTracksProperty;
    }

    public NavigationController getNavigationController() {
        return navigationLayoutController;
    }

    public PlayerController getPlayerController() {
        return playerLayoutController;
    }

    public ObservableList<Entry<Integer, Track>> getSelectedTracks() {
        NavigationMode mode = navigationModeProperty.getValue();
        ObservableList<Entry<Integer, Track>> selectedTracks = null;
        switch (mode) {
            case ALL_TRACKS:
            case PLAYLIST:
                selectedTracks = trackTable.getSelectionModel().getSelectedItems();
                break;
            case ARTISTS:
                selectedTracks = artistsLayoutController.getSelectedTracks();
                break;
        }
        return selectedTracks;
    }

    public ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty() {
        return navigationLayoutController.selectedPlaylistProperty();
    }

    public ReadOnlyBooleanProperty showNavigationPaneProperty() {
        return showingNavigationPaneProperty;
    }

    public ReadOnlyBooleanProperty showTableInfoPaneProperty() {
        return showingTableInfoPaneProperty;
    }
}
