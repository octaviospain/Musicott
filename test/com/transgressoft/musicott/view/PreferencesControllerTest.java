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
import com.google.inject.util.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class PreferencesControllerTest extends JavaFxTestBase<PreferencesController> {

    BooleanProperty usingLastFmPropertyMock = new SimpleBooleanProperty(false);

    @Mock
    LastFmPreferences lastFmPreferencesMock;
    @Mock
    ErrorDialogController errorDialogMock;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        when(lastFmPreferencesMock.getLastFmUsername()).thenReturn("user");
        when(lastFmPreferencesMock.getLastFmPassword()).thenReturn("pass");

        injector = Guice.createInjector(Modules.override(mockedSingletonsTestModule).with(new TestModule()));

        loadTestController(Layout.PREFERENCES);
        stage.setScene(new Scene(controller.getRoot()));

        injector = injector.createChildInjector(module);

        stage.show();
    }

    @Test
    @DisplayName("Singleton controller")
    void singletonController () {
        PreferencesController anotherController = injector.getInstance(PreferencesController.class);

        assertSame(controller, anotherController);
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {}

        @Provides
        @UsingLastFmProperty
        ReadOnlyBooleanProperty providesUsingLastFmPropertyMock() {
            return usingLastFmPropertyMock;
        }

        @Provides
        ErrorDialogController providesErrorDialogMock() {
            return errorDialogMock;
        }

        @Provides
        LastFmPreferences providesLastFmPreferences() {
            return lastFmPreferencesMock;
        }
    }
}