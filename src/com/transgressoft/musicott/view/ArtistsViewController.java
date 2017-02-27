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

import javafx.fxml.*;
import javafx.scene.control.*;

/**
 * Controller class of the navigation mode that shows a list with all
 * the artists and an adapted section with all the tracks of a selected
 * artist in the list divided in their albums.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.10-b
 */
public class ArtistsViewController implements MusicottController {

    @FXML
    private SplitPane rootSplitPane;
    @FXML
    private ListView<String> artistsListView;
    @FXML
    private ListView<?> artistMusicListView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label albumsLabel;
    @FXML
    private Label tracksLabel;
    @FXML
    private Button randomButton;

    @FXML
    public void initialize() {

    }
}