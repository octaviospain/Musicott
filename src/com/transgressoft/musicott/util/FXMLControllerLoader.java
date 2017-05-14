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

package com.transgressoft.musicott.util;

import com.google.inject.*;
import com.transgressoft.musicott.view.*;
import javafx.fxml.*;
import javafx.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class inherits from {@link FXMLLoader} in order to provide proper
 * creation of FXML controller classes when using Guice's Dependency Injection
 * and nested views.
 * <p>
 * Based on Arnaud Blouin's
 * <a href="http://torgen-engineering.blogspot.cz/2015/11/dependencies-between-controllers.html">solution</a>
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 *
 * @see FXGuiceInjectionBuilderFactory
 * @see InjectableController
 */
public class FXMLControllerLoader extends FXMLLoader {

    private final Set<Object> controllers;

    public FXMLControllerLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory, Injector injector) {
        super(location, resources, builderFactory);

        controllers = new HashSet<>();

        setControllerFactory(classToInstantiate -> {
            final Object instance = injector.getInstance(classToInstantiate);
            if (instance != null)
                controllers.add(instance);
            return instance;
        });
    }

    @Override
    public <T> T load() throws IOException {
        final T loaded = super.load();
        controllers.stream().filter(c -> c instanceof InjectableController)
                   .forEach(c -> ((InjectableController) c).configure());
        return loaded;
    }
}