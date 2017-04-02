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

import javafx.scene.image.*;

import java.io.*;

/**
 * Interface that represent a controller of the Musicott application.
 * Stores constants of layout files, logos, css stylesheets and
 * references to the singleton classes.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.9
 */
public interface MusicottController {

    String TRACKS_PERSISTENCE_FILE = "Musicott-tracks.json";
    String WAVEFORMS_PERSISTENCE_FILE = "Musicott-waveforms.json";
    String PLAYLISTS_PERSISTENCE_FILE = "Musicott-playlists.json";

    String LAYOUTS_PATH = File.separator + "view" + File.separator;
    String IMAGES_PATH = File.separator + "images" + File.separator;
    String STYLES_PATH = File.separator + "css" + File.separator;
    String ICONS_PATH = File.separator + "icons" + File.separator;

    String ROOT_LAYOUT = LAYOUTS_PATH + "RootLayout.fxml";
    String PRELOADER_INIT_LAYOUT = LAYOUTS_PATH + "PreloaderLayout.fxml";
    String PRELOADER_FIRST_USE_PROMPT = LAYOUTS_PATH + "PreloaderPromptLayout.fxml";
    String EDIT_LAYOUT = LAYOUTS_PATH + "EditLayout.fxml";
    String PROGRESS_LAYOUT = LAYOUTS_PATH + "ProgressLayout.fxml";
    String PREFERENCES_LAYOUT = LAYOUTS_PATH + "PreferencesLayout.fxml";
    String ERROR_ALERT_LAYOUT = LAYOUTS_PATH + "ErrorDialogLayout.fxml";

    String DEFAULT_COVER_PATH = IMAGES_PATH + "default-cover-image.png";
    String COMMON_ERROR_IMAGE = IMAGES_PATH + "common-error.png";
    String LASTFM_LOGO = IMAGES_PATH + "lastfm-logo.png";
    String MUSICOTT_APP_ICON = IMAGES_PATH + "musicott-app-icon.png";
    String MUSICOTT_ABOUT_LOGO = IMAGES_PATH + "musicott-about-logo.png";
    String DRAGBOARD_ICON_PATH = ICONS_PATH + "dragboard-icon.png";

    String DIALOG_STYLE = STYLES_PATH + "dialog.css";
    String TRACK_TABLE_STYLE = STYLES_PATH + "tracktable.css";
    String BASE_STYLE = STYLES_PATH + "base.css";
    String TRACKAREASET_TRACK_TABLE_STYLE = STYLES_PATH + "tracktable-trackareaset.css";

    Image DEFAULT_COVER = new Image(MusicottController.class.getResourceAsStream(DEFAULT_COVER_PATH));
    Image DRAGBOARD_ICON = new Image(MusicottController.class.getResourceAsStream(DRAGBOARD_ICON_PATH));
}
