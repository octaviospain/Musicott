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
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.view.custom.*;
import com.worldsworstsoftware.itunes.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.stage.Stage;
import org.slf4j.*;

import java.util.*;

/**
 * Controller class of the window that lets the user selects which playlists
 * he/she wants to import.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
@Singleton
public class ItunesPlaylistsPickerController extends InjectableController<BorderPane> {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    @FXML
    private ListView<ItunesPlaylist> sourcePlaylists;
    @FXML
    private ListView<ItunesPlaylist> targetPlaylists;
    @FXML
    private Button addSelectedButton;
    @FXML
    private Button removeSelectedButton;
    @FXML
    private Button addAllButton;
    @FXML
    private Button removeAllButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button importButton;

    private final TaskDemon taskDemon;

    @Inject
    public ItunesPlaylistsPickerController(TaskDemon taskDemon) {
        this.taskDemon = taskDemon;
        LOG.debug("ItunesPlaylistsPickerController created {}", this);
    }

    @FXML
    public void initialize() {
        addSelectedButton.setOnAction(e -> moveSelected(sourcePlaylists, targetPlaylists));
        removeSelectedButton.setOnAction(e -> moveSelected(targetPlaylists, sourcePlaylists));
        addAllButton.setOnAction(e -> moveAll(sourcePlaylists, targetPlaylists));
        removeAllButton.setOnAction(e -> moveAll(targetPlaylists, sourcePlaylists));
        sourcePlaylists.setCellFactory(cell -> new ItunesPlaylistListCell(this));
        sourcePlaylists.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        targetPlaylists.setCellFactory(cell -> new ItunesPlaylistListCell(this));
        targetPlaylists.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cancelButton.setOnAction(e -> {
            taskDemon.cancelItunesImport();
            cancelButton.getScene().getWindow().hide();
        });
        importButton.setOnAction(e -> {
            taskDemon.setItunesPlaylistsToImport(targetPlaylists.getItems());
            cancelButton.getScene().getWindow().hide();
        });
        LOG.debug("ItunesPlaylistsPickerController initialized {}", this);
    }

    private void moveSelected(ListView<ItunesPlaylist> from, ListView<ItunesPlaylist> to) {
        ObservableList<ItunesPlaylist> selectedItems = from.getSelectionModel().getSelectedItems();
        to.getItems().addAll(selectedItems);
        from.getItems().removeAll(selectedItems);
        FXCollections.sort(to.getItems(), Comparator.comparing(ItunesPlaylist::getName));
    }

    private void moveAll(ListView<ItunesPlaylist> from, ListView<ItunesPlaylist> to) {
        to.getItems().addAll(from.getItems());
        from.getItems().clear();
        FXCollections.sort(to.getItems(), Comparator.comparing(ItunesPlaylist::getName));
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Choose iTunes playlists");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        stage.setMinWidth(670);
        stage.setMinHeight(515);
    }

    public void pickPlaylists(List<ItunesPlaylist> playlists) {
        sourcePlaylists.getItems().clear();
        targetPlaylists.getItems().clear();
        sourcePlaylists.getItems().addAll(playlists);
        FXCollections.sort(sourcePlaylists.getItems(), Comparator.comparing(ItunesPlaylist::getName));
    }

    public void movePlaylist(ItunesPlaylist playlist) {
        if (sourcePlaylists.getItems().contains(playlist)) {
            targetPlaylists.getItems().add(playlist);
            sourcePlaylists.getItems().remove(playlist);
            FXCollections.sort(targetPlaylists.getItems(), Comparator.comparing(ItunesPlaylist::getName));
        }
        else if (targetPlaylists.getItems().contains(playlist)) {
            sourcePlaylists.getItems().add(playlist);
            targetPlaylists.getItems().remove(playlist);
            FXCollections.sort(sourcePlaylists.getItems(), Comparator.comparing(ItunesPlaylist::getName));
        }
    }
}
