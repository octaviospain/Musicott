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

import com.transgressoft.musicott.util.*;
import javafx.scene.*;
import javafx.stage.*;

/**
 * JavaFx controller classes that has fields or methods injected by Google's Guice Dependency
 * Injection Framework should extend from this class.
 * <p>
 * When an instance of a {@code InjectableController} is requested,
 * is created using an {@code Injector}, and the {@link #configure} method
 * is called before returning the object.
 * <p>
 * Based on Arnaud Blouin's
 * <a href="http://torgen-engineering.blogspot.cz/2015/11/dependencies-between-controllers.html">solution</a>
 *
 * @param <R> The class type of the root node of the controller layout
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.10.1-b
 *
 * @see MetadataParser.FXGuiceInjectionBuilderFactory
 * @see MetadataParser.FXMLControllerLoader
 * @see <a href="https://github.com/google/guice">Guice</a>
 */
public abstract class InjectableController<R extends Parent> {

    private R rootNode;
    protected Stage stage;

    /**
     * This method is intended to be called when an instance of this class
     * is created in the {@link MetadataParser.FXMLControllerLoader#load} method.
     * <p>
     * When using nested {@code FXML} views and {@code Guice}, fields injected
     * by both frameworks are messed up.
     * In order to properly inject the fields, the access to inherited controller classes
     * should be accessed inside this method, and not inside {@code initialize()},
     * because they will be injected but not their attributes, due to the order of
     * subsequent creation of the nested views.
     */
    public void configure() {}

    public void setRoot(R rootNode) {
        this.rootNode = rootNode;
    }

    public R getRoot() {
        return rootNode;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}