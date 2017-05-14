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

package com.transgressoft.musicott.tests;

import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.*;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static org.mockito.Mockito.*;

/**
 * Base class for testing JavaFX classes
 *
 * @author Octavio Calleya
 */
@ExtendWith (ApplicationExtension.class)
public abstract class JavaFxTestBase<T extends InjectableController> implements InjectedApplication {

    protected Stage testStage;
    protected static Injector injector;
    protected T controller;
    protected ControllerModule<T> module;

    @Start
    public abstract void start(Stage stage) throws Exception;

    @BeforeAll
    public static void beforeAll() throws Exception {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }
    }

    protected <N extends Node> N find(FxRobot fxRobot, String query) {
        return fxRobot.lookup(query).query();
    }

    protected void loadControllerModule(Layout layout) throws IOException, ReflectiveOperationException {
        module = createController(layout, injector);
        controller = module.providesController();
    }

    @SuppressWarnings ("unchecked")
    protected Injector injectorWithSimpleMocks(Class... classesToMock) {
        return injectorWithSimpleMocks(Collections.emptyList(), classesToMock);
    }

    @SuppressWarnings ("unchecked")
    protected Injector injectorWithSimpleMocks(Collection<Module> modules, Class... classesToMock) {
        return Guice.createInjector(
                binder -> {
                    Stream.of(classesToMock).forEach(c -> binder.bind(c).toInstance(mock(c)));
                    modules.forEach(binder::install);
                });
    }

    @SuppressWarnings ("unchecked")
    protected Injector injectorWithCustomMocks(Map<Class, Object> mockedObjects, Module... modules) {
        return injectorWithCustomMocks(mockedObjects, Stream.of(modules));
    }

    @SuppressWarnings ("unchecked")
    protected Injector injectorWithCustomMocks(Map<Class, Object> mockedObjects, Collection<Module> modules) {
        return injectorWithCustomMocks(mockedObjects, modules.stream());
    }

    @SuppressWarnings ("unchecked")
    protected Injector injectorWithCustomMocks(Map<Class, Object> mockedObjects, Stream<Module> modules) {
        return Guice.createInjector(
                binder -> {
                    mockedObjects.forEach((key, value) -> binder.bind(key).toInstance(value));
                    modules.forEach(binder::install);
                });
    }
}