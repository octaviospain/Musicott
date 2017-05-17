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

package com.transgressoft.musicott.util.guice.modules;

import com.google.inject.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.*;
import javafx.beans.property.*;

/**
 * @author Octavio Calleya
 */
public class PlayerModule extends AbstractModule implements ControllerModule<PlayerController> {

    private PlayerController controller;

    public PlayerModule(PlayerController controller) {
        this.controller = controller;
    }

    @Override
    protected void configure() {
        bind(InjectableController.class).annotatedWith(PlayerCtrl.class).toInstance(controller);
    }

    @Provides
    @PlayerCtrl
    public PlayerController providesController() {
        return controller;
    }

    @Provides
    @SearchTextProperty
    StringProperty providesSearchingTextProperty(PlayerController playerController) {
        return playerController.searchTextProperty();
    }

    @Provides
    @SearchingProperty
    ReadOnlyBooleanProperty providesSearchingProperty(PlayerController playerController) {
        return playerController.searchFieldFocusedProperty();
    }

    @Provides
    @PlayPauseProperty
    BooleanProperty providesPlayPauseProperty(PlayerController playerController) {
        return playerController.playButtonSelectedProperty();
    }

    @Provides
    @PreviousButtonDisabledProperty
    ReadOnlyBooleanProperty providesPrevButtonDisabledProperty(PlayerController playerController) {
        return playerController.previousButtonDisabledProperty();
    }

    @Provides
    @NextButtonDisabledProperty
    ReadOnlyBooleanProperty providesNexButtonDisabledProperty(PlayerController playerController) {
        return playerController.nextButtonDisabledProperty();
    }
}