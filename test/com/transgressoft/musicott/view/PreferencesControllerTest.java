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

import com.google.common.collect.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.guice.modules.*;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith(MockitoExtension.class)
public class PreferencesControllerTest extends JavaFxTestBase<PreferencesController> {

    @Mock
    MainPreferences prefsMock;
    @Mock
    StageDemon stageDemonMock;
    @Mock
    ServiceDemon serviceDemonMock;
    @Mock
    TaskDemon taskDemonMock;
    @Mock
    ErrorDemon errorDemonMock;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        testStage = stage;
        when(serviceDemonMock.usingLastFmProperty()).thenReturn(new SimpleBooleanProperty(false));

        Map<Class, Object> mocks = ImmutableMap.<Class, Object>builder()
                .put(prefsMock.getClass(), prefsMock)
                .put(stageDemonMock.getClass(), stageDemonMock)
                .put(serviceDemonMock.getClass(), serviceDemonMock)
                .put(taskDemonMock.getClass(), taskDemonMock)
                .put(errorDemonMock.getClass(), errorDemonMock)
                .build();

        injector = injectorWithCustomMocks(mocks, new ParseModule(), new TrackFactoryModule());

        loadControllerModule(Layout.PREFERENCES);
        stage.setScene(new Scene(module.providesController().getRoot()));

        injector = injector.createChildInjector(module);

        stage.show();
    }

    @Test
    @DisplayName("Singleton controller and stage")
    void singletonController () {
        PreferencesController anotherController = injector.getInstance(PreferencesController.class);

        assertSame(controller, anotherController);
    }
}