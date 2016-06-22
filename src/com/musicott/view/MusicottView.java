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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott.view;

/**
 * @author Octavio Calleya
 * @version 0.9
 */
public interface MusicottView {

    String LAYOUTS_PATH = "/view/";

    String ROOT_LAYOUT = LAYOUTS_PATH + "RootLayout.fxml";
    String NAVIGATION_LAYOUT = LAYOUTS_PATH +  "NavigationLayout.fxml";
    String PRELOADER_LAYOUT = LAYOUTS_PATH + "PreloaderPromptLayout.fxml";
    String EDIT_LAYOUT = LAYOUTS_PATH + "EditLayout.fxml";
    String PLAYQUEUE_LAYOUT = LAYOUTS_PATH + "PlayQueueLayout.fxml";
    String PROGRESS_LAYOUT = LAYOUTS_PATH + "ProgressLayout.fxml";
    String PREFERENCES_LAYOUT = LAYOUTS_PATH + "PreferencesLayout.fxml";
    String PLAYER_LAYOUT = LAYOUTS_PATH + "PlayerLayout.fxml";

    String DEFAULT_COVER_IMAGE = "/images/default-cover-image.png";
}