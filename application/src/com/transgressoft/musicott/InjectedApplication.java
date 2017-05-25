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

package com.transgressoft.musicott;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.*;

/**
 * Interface that defines methods for creating JavaFx controller classes that are injected
 * by Dependency Injection using Google's Guice framework.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 *
 * @see <a href="https://github.com/google/guice/">Guice</a>
 */
public interface InjectedApplication {

    Injector getInjector();

    /**
     * Creates a {@link ControllerModule} given a {@link Layout}.
     * A new {@link Stage} is created and used for this view and setted to the controller.
     *
     * @param layout The {@code Layout} of the view
     * @param <T>    The class type of the layout controller
     */
    default <T extends InjectableController> ControllerModule<T> loadControllerModule(Layout layout)
            throws IOException, ReflectiveOperationException {
        Stage independentStage = new Stage();
        ControllerModule<T> module = loadControllerModule(layout, independentStage);
        independentStage.setScene(new Scene(module.providesController().getRoot()));
        return module;
    }

    /**
     * Creates a {@link ControllerModule} given a {@link Layout} and the {@link Stage} in which it belongs
     *
     * @param layout The {@code Layout} of the view
     * @param stage  The {@code Stage} in which the {@code Layout} is supposed to be shown in
     * @param <T>    The class type of the layout controller
     *
     * @return The {@link ControllerModule} containing that defines the singleton DI binding and the controller
     *
     * @throws IOException                  If the {@code FXML} file was not found
     * @throws ReflectiveOperationException If something went bad instantiating the controller class module
     */
    default <T extends InjectableController> ControllerModule<T> loadControllerModule(Layout layout, Stage stage)
            throws IOException, ReflectiveOperationException {
        ControllerModule<T> controllerModule = createController(layout, getInjector());
        InjectableController controller = controllerModule.providesController();
        controller.setStage(stage);
        return controllerModule;
    }

    /**
     * Creates an {@link InjectableController} using Guice's Dependency Injection, returning a
     * {@link ControllerModule} that binds the created object as a singleton instance with a custom
     * binding annotation, that is defined for each {@link Layout}.
     *
     * @param layout   The {@code FXML} view to load
     * @param injector The {@code Guice} {@link Injector}
     * @param <T>      The {@code InjectableController} implementation of the controller
     *
     * @return A {@code ControllerModule} with the controller and the binding
     *
     * @throws IOException                  If the {@code FXML} file was not found
     * @throws ReflectiveOperationException If something went bad instantiating the controller class module
     */
    @SuppressWarnings ("unchecked")
    default <T extends InjectableController> ControllerModule<T> createController(Layout layout,
            Injector injector) throws IOException, ReflectiveOperationException {
        FXGuiceInjectionBuilderFactory factory = new FXGuiceInjectionBuilderFactory(injector);
        FXMLLoader fxmlLoader = new FXMLControllerLoader(getClass().getResource(layout.getPath()), null, factory, injector);

        Parent root = fxmlLoader.load();
        T injectableController = fxmlLoader.getController();
        injectableController.setRoot(root);

        Class<? extends ControllerModule> module = layout.getControllerModule();
        Constructor<? extends ControllerModule> constructor = module.getConstructor(injectableController.getClass());
        return constructor.newInstance(injectableController);
    }
}
