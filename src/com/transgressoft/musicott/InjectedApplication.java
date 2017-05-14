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

import java.io.*;
import java.lang.reflect.*;

/**
 * @author Octavio Calleya
 */
public interface InjectedApplication {

    @SuppressWarnings ("unchecked")
    default <T extends InjectableController> ControllerModule<T> createController(Layout layout, Injector injector)
            throws IOException, ReflectiveOperationException {
        T injectableController = initLayout(layout.getPath(), injector);
        Class<? extends ControllerModule> module = layout.getControllerModule();
        Constructor<? extends ControllerModule> constructor = module.getConstructor(injectableController.getClass());
        return constructor.newInstance(injectableController);
    }

    default <T extends InjectableController> T initLayout(String layout, Injector injector) throws IOException {
        FXGuiceInjectionBuilderFactory factory = new FXGuiceInjectionBuilderFactory(injector);
        FXMLLoader fxmlLoader = new FXMLControllerLoader(getClass().getResource(layout), null, factory, injector);
        Parent root = fxmlLoader.load();
        return fxmlLoader.getController();
    }
}