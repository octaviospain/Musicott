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

import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.input.KeyCombination.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.Map.*;
import java.util.function.*;

import static org.fxmisc.easybind.EasyBind.*;

/**
 * Controller class of the left pane that contains the playlists, the
 * music library menus, and the status progress and status messages.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class NavigationController extends InjectableController<VBox> {

    private static final String GREEN_STATUS_COLOUR = "-fx-text-fill: rgb(99, 255, 109);";
    private static final String GRAY_STATUS_COLOUR = "-fx-text-fill: rgb(73, 73, 73);";

    private final MusicLibrary musicLibrary;
    private final PlaylistsLibrary playlistsLibrary;
    private final RootController rootController;
    private final StageDemon stageDemon;

    @FXML
    private VBox navigationPaneVBox;
    @FXML
    private VBox navigationVBox;
    @FXML
    private VBox playlistsVBox;
    @FXML
    private Button newPlaylistButton;
    @FXML
    private ProgressBar taskProgressBar;
    @FXML
    private Label statusLabel;

    private NavigationMenuListView navigationMenuListView;
    private PlaylistTreeView playlistTreeView;
    private ObjectProperty<NavigationMode> navigationModeProperty;
    private Optional<Playlist> currentPlayingPlaylist;

    @Inject
    public NavigationController(MusicLibrary musicLibrary, PlaylistsLibrary playlistsLibrary,
            RootController rootController, PlaylistTreeView playlistTreeView, StageDemon stageDemon) {
        this.musicLibrary = musicLibrary;
        this.playlistsLibrary = playlistsLibrary;
        this.rootController = rootController;
        this.playlistTreeView = playlistTreeView;
        this.stageDemon = stageDemon;
    }

    @FXML
    public void initialize() {
        currentPlayingPlaylist = Optional.empty();
        navigationModeProperty = new SimpleObjectProperty<>(this, "showing mode", NavigationMode.ALL_TRACKS);
        navigationMenuListView = new NavigationMenuListView(this);
        subscribe(navigationModeProperty, this::setNavigationMode);
        subscribe(selectedPlaylistProperty(), newPlaylist -> newPlaylist.ifPresent(playlist -> {
            musicLibrary.showPlaylist(playlist);
            setNavigationMode(NavigationMode.PLAYLIST);
        }));
        NavigationMode[] navigationModes = {NavigationMode.ALL_TRACKS, NavigationMode.ARTISTS};
        navigationMenuListView.setItems(FXCollections.observableArrayList(navigationModes));

        ContextMenu newPlaylistButtonContextMenu = newPlaylistButtonContextMenu();

        newPlaylistButton.setContextMenu(newPlaylistButtonContextMenu);
        newPlaylistButton.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            double newPlaylistButtonX = e.getScreenX() + 10.0;
            double newPlaylistButtonY = e.getScreenY() + 10.0;
            newPlaylistButtonContextMenu.show(newPlaylistButton, newPlaylistButtonX, newPlaylistButtonY);
        });

        navigationVBox.getChildren().add(1, navigationMenuListView);
        playlistsVBox.getChildren().add(1, playlistTreeView);
        taskProgressBar.visibleProperty().bind(map(taskProgressBar.progressProperty().isEqualTo(0).not(), Function.identity()));
        taskProgressBar.setProgress(0);

        VBox.setVgrow(playlistTreeView, Priority.ALWAYS);
        VBox.setVgrow(navigationVBox, Priority.ALWAYS);
    }

    @Override
    public VBox getRoot() {
        return navigationPaneVBox;
    }

    /**
     * Changes the view depending of the choose {@link NavigationMode}
     *
     * @param mode The {@code NavigationMode} that the user choose
     */
    public void setNavigationMode(NavigationMode mode) {
        navigationModeProperty.setValue(mode);

        switch (mode) {
            case ALL_TRACKS:
                musicLibrary.showAllTracks();
                navigationMenuListView.getSelectionModel().select(NavigationMode.ALL_TRACKS);
                playlistTreeView.clearAndSelect(- 1);
                Platform.runLater(rootController::showAllTracksView);
                break;
            case ARTISTS:
                navigationMenuListView.getSelectionModel().select(NavigationMode.ARTISTS);
                playlistTreeView.clearAndSelect(- 1);
                Platform.runLater(rootController::showArtistsView);
                break;
            case PLAYLIST:
                navigationMenuListView.getSelectionModel().clearAndSelect(- 1);
                Platform.runLater(rootController::showPlaylistView);
                break;
        }
    }

    private ContextMenu newPlaylistButtonContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem newPlaylistMI;
        MenuItem newFolderPlaylistMI;
        newPlaylistMI = new MenuItem("New Playlist");
        newPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, systemModifier()));
        newPlaylistMI.setOnAction(e -> {
            stageDemon.getRootController().enterNewPlaylistName(false);
            playlistTreeView.clearAndSelect(- 1);
            navigationMenuListView.getSelectionModel().clearAndSelect(- 1);
        });
        newFolderPlaylistMI = new MenuItem("New Playlist Folder");
        newFolderPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, systemModifier(), KeyCombination.SHIFT_DOWN));
        newFolderPlaylistMI.setOnAction(e -> {
            stageDemon.getRootController().enterNewPlaylistName(true);
            playlistTreeView.clearAndSelect(- 1);
            navigationMenuListView.getSelectionModel().clearAndSelect(- 1);
        });
        contextMenu.getItems().addAll(newPlaylistMI, newFolderPlaylistMI);
        return contextMenu;
    }

    /**
     * Returns the key accelerator for the application. Command for os x and control down for windows and linux.
     *
     * @return The {@link Modifier} of the operative system
     */
    private Modifier systemModifier() {
        String os = System.getProperty("os.name");
        Modifier keyModifierOS;
        if (os != null && os.startsWith("Mac"))
            keyModifierOS = KeyCodeCombination.META_DOWN;
        else
            keyModifierOS = KeyCodeCombination.CONTROL_DOWN;
        return keyModifierOS;
    }

    public void addNewPlaylist(Playlist parent, Playlist newPlaylist, boolean selectAfter) {
        playlistTreeView.addPlaylistsToFolder(parent, Collections.singleton(newPlaylist));
        if (selectAfter)
            playlistTreeView.selectPlaylist(newPlaylist);
    }

    public void updateCurrentPlayingPlaylist() {
        currentPlayingPlaylist = selectedPlaylistProperty().get();
    }

    public boolean selectPlaylistOfTrack(Entry<Integer, Track> trackEntry) {
        boolean success = currentPlayingPlaylist.isPresent();
        if (success) {
            Playlist playlist = currentPlayingPlaylist.get();
            success = playlist.getTracks().contains(trackEntry.getKey());
            if (success)
                playlistTreeView.selectPlaylist(playlist);
        }
        return success;
    }

    public void deleteSelectedPlaylist() {
        Playlist selectedPlaylist = selectedPlaylistProperty().get().get();
        playlistTreeView.deletePlaylist(selectedPlaylist);
        playlistsLibrary.deletePlaylist(selectedPlaylist);
        if (playlistsLibrary.isEmpty())
            setNavigationMode(NavigationMode.ALL_TRACKS);
    }

    public void setStatusProgress(double progress) {
        taskProgressBar.setProgress(progress);
    }

    public void setStatusMessage(String message) {
        if (Double.isNaN(taskProgressBar.getProgress()))
            statusLabel.setStyle(GREEN_STATUS_COLOUR);
        else
            statusLabel.setStyle(GRAY_STATUS_COLOUR);
        statusLabel.setText(message);
    }

    public ObjectProperty<NavigationMode> navigationModeProperty() {
        return navigationModeProperty;
    }

    ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty() {
        return playlistTreeView.selectedPlaylistProperty();
    }
}
