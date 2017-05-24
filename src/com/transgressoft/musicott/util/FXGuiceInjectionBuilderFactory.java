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

/**
 * This class pretends to control the instantiation of FXML controllers
 * creating the objects using Guice's Dependency Injection if the class
 * implements {@link InjectableController} interface, using or the default
 * {@link JavaFXBuilderFactory} otherwise.
 * <p>
 * Based on Arnaud Blouin's
 * <a href="http://torgen-engineering.blogspot.cz/2015/11/dependencies-between-controllers.html">solution</a>
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 *
 * @see FXMLControllerLoader
 * @see InjectableController
 */
public class FXGuiceInjectionBuilderFactory implements BuilderFactory {

    private final Injector injector;
    private final BuilderFactory defaultFactory;


    public FXGuiceInjectionBuilderFactory(final Injector inj) {
        super();
        injector = inj;
        defaultFactory = new JavaFXBuilderFactory();
    }

    @Override
    public Builder<?> getBuilder(final Class<?> type) {
        if (type == RootController.class || type == RootMenuBarController.class || type == PlayQueueController.class
                || type == PlayerController.class || type == NavigationController.class || type ==
                ArtistsViewController.class || type == EditController.class || type == InjectableController.class)
            return () -> injector.getInstance(type);
        return defaultFactory.getBuilder(type);
    }
}
