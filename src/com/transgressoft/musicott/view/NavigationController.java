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
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.input.KeyCombination.*;
import javafx.scene.layout.*;
import org.slf4j.*;

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

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final TracksLibrary tracksLibrary;
    private final PlaylistsLibrary playlistsLibrary;

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

    private RootController rootController;
    private NavigationMenuListView navigationMenuListView;
    private PlaylistTreeView playlistTreeView;
    private Optional<Playlist> currentPlayingPlaylist;
    private ObjectProperty<NavigationMode> navigationModeProperty;
    private ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty;

    @Inject
    public NavigationController(TracksLibrary tracksLibrary, PlaylistsLibrary playlistsLibrary) {
        this.tracksLibrary = tracksLibrary;
        this.playlistsLibrary = playlistsLibrary;
        LOG.debug("NavigationController created {}", this);
    }

    @FXML
    public void initialize() {
        currentPlayingPlaylist = Optional.empty();
        navigationModeProperty = new SimpleObjectProperty<>(this, "showing mode", NavigationMode.ARTISTS);
        navigationMenuListView = new NavigationMenuListView(this);
        ContextMenu newPlaylistButtonContextMenu = newPlaylistButtonContextMenu();

        newPlaylistButton.setContextMenu(newPlaylistButtonContextMenu);
        newPlaylistButton.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            double newPlaylistButtonX = e.getScreenX() + 10.0;
            double newPlaylistButtonY = e.getScreenY() + 10.0;
            newPlaylistButtonContextMenu.show(newPlaylistButton, newPlaylistButtonX, newPlaylistButtonY);
        });

        navigationVBox.getChildren().add(1, navigationMenuListView);
        taskProgressBar.visibleProperty().bind(map(taskProgressBar.progressProperty().isEqualTo(0).not(), Function.identity()));
        taskProgressBar.setProgress(0);

        VBox.setVgrow(navigationVBox, Priority.ALWAYS);
        LOG.debug("NavigationController initialized {}", this);
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
                tracksLibrary.showAllTracks();
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
            rootController.enterNewPlaylistName(false);
            playlistTreeView.clearAndSelect(- 1);
            navigationMenuListView.getSelectionModel().clearAndSelect(- 1);
        });
        newFolderPlaylistMI = new MenuItem("New Playlist Folder");
        newFolderPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, systemModifier(), KeyCombination.SHIFT_DOWN));
        newFolderPlaylistMI.setOnAction(e -> {
            rootController.enterNewPlaylistName(true);
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
        currentPlayingPlaylist = selectedPlaylistProperty.get();
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
        Playlist selectedPlaylist = selectedPlaylistProperty.get().get();
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

    @Inject (optional = true)
    public void setPlaylistTreeView(PlaylistTreeView playlistTreeView) {
        this.playlistTreeView = playlistTreeView;
        playlistsVBox.getChildren().add(1, playlistTreeView);
        VBox.setVgrow(playlistTreeView, Priority.ALWAYS);
        LOG.debug("playlistTreeView setted ");

        selectedPlaylistProperty = playlistTreeView.selectedPlaylistProperty();
        subscribe(selectedPlaylistProperty,
                  playlist -> playlist.ifPresent(
                          p -> setNavigationMode(NavigationMode.PLAYLIST)));
    }

    @Inject (optional = true)
    public void setRootController(@RootCtrl RootController c) {
        rootController = c;
        LOG.debug("rootController setted ");
    }

    public ReadOnlyObjectProperty<NavigationMode> navigationModeProperty() {
        return navigationModeProperty;
    }
}
