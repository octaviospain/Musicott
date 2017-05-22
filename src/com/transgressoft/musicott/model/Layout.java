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

package com.transgressoft.musicott.model;

import com.google.common.collect.*;
import com.transgressoft.musicott.util.guice.modules.*;

import java.util.*;

import static com.transgressoft.musicott.model.CommonObject.*;

/**
 * Defines the different views with the respective paths and {@link ControllerModule} classes.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public enum Layout {

    ROOT (LAYOUTS_PATH + "RootLayout.fxml", RootModule.class),
    PRELOADER (LAYOUTS_PATH + "PreloaderLayout.fxml", null),
    PRELOADER_PROMPT (LAYOUTS_PATH + "PreloaderPromptLayout.fxml", null),
    EDITION (LAYOUTS_PATH + "EditLayout.fxml", EditModule.class),
    ARTISTS (LAYOUTS_PATH + "ArtistsLayout.fxml", ArtistsModule.class),
    PROGRESS_BAR (LAYOUTS_PATH + "ProgressLayout.fxml", SimpleProgressBarModule.class),
    PREFERENCES (LAYOUTS_PATH + "PreferencesLayout.fxml", PreferencesModule.class),
    NAVIGATION (LAYOUTS_PATH + "NavigationLayout.fxml", NavigationModule.class),
    PLAYER (LAYOUTS_PATH + "PlayerLayout.fxml", PlayerModule.class),
    PLAY_QUEUE (LAYOUTS_PATH + "PlayQueueLayout.fxml", PlayQueueModule.class),
    MENU_BAR (LAYOUTS_PATH + "RootMenuBarLayout.fxml", RootMenuBarModule.class),
    ERROR_DIALOG (LAYOUTS_PATH + "ErrorDialogLayout.fxml", ErrorDialogModule.class);

    private String path;
    private Class<? extends ControllerModule> controllerModule;

    Layout(String path, Class<? extends ControllerModule> controllerModule) {
        this.path = path;
        this.controllerModule = controllerModule;
    }

    public String getPath() {
        return path;
    }

    public Class<? extends ControllerModule> getControllerModule() {
        return controllerModule;
    }

    public static boolean isIndependentLayout(Layout layout) {
        return layout == ERROR_DIALOG || layout == PREFERENCES || layout == PROGRESS_BAR || layout == EDITION;
    }

    public static List<Layout> getLayoutsInLoadingOrder() {
        return ImmutableList.<Layout>builder()
                           .add(ERROR_DIALOG)
                           .add(PREFERENCES)
                           .add(PROGRESS_BAR)
                           .add(PLAY_QUEUE)
                           .add(PLAYER)
                           .add(ARTISTS)
                           .add(NAVIGATION)
                           .add(MENU_BAR)
                           .add(EDITION)
                           .add(ROOT)
                           .build();
    }
}