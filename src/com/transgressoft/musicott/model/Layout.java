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

import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.*;

import java.lang.annotation.*;

/**
 * @author Octavio Calleya
 */
public enum Layout implements MusicottLayout {

    ROOT(LAYOUTS_PATH + "RootLayout.fxml", RootCtrl.class, RootModule.class),
    PRELOADER(LAYOUTS_PATH + "PreloaderLayout.fxml", null, null),
    PRELOADER_PROMPT(LAYOUTS_PATH + "PreloaderPromptLayout.fxml", null, null),
    EDITION(LAYOUTS_PATH + "EditLayout.fxml", EditCtrl.class, EditModule.class),
    PROGRESS_BAR(LAYOUTS_PATH + "ProgressLayout.fxml", null, null),
    PREFERENCES(LAYOUTS_PATH + "PreferencesLayout.fxml", PrefCtrl.class, PreferencesModule.class),
    NAVIGATION(LAYOUTS_PATH + "NavigationLayout.fxml", NavigationCtrl.class, NavigationModule.class),
    PLAY_QUEUE(LAYOUTS_PATH + "PlayQueueLayout.fxml", PlayQueueCtrl.class, PlayQueueModule.class),
    MENU_BAR(LAYOUTS_PATH + "RootMenuBarLayout.fxml", RootMenuBar.class, RootMenuBarModule.class),
    ERROR_DIALOG(LAYOUTS_PATH + "ErrorDialogLayout.fxml", ErrorCtrl.class, ErrorDialogModule.class);

    private String path;
    private Class<? extends Annotation> controllerAnnotation;
    private Class<? extends ControllerModule> controllerModule;

    Layout(String path, Class<? extends Annotation> controllerAnnotation,
            Class<? extends ControllerModule> controllerModule) {
        this.path = path;
        this.controllerAnnotation = controllerAnnotation;
        this.controllerModule = controllerModule;
    }

    public String getPath() {
        return path;
    }

    public Class<? extends Annotation> getControllerAnnotation() {
        return controllerAnnotation;
    }

    public Class<? extends ControllerModule> getControllerModule() {
        return controllerModule;
    }
}