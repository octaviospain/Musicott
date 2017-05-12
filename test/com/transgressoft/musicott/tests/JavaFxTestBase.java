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
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guicemodules.*;
import javafx.scene.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;

/**
 * Base class for testing JavaFX classes
 *
 * @author Octavio Calleya
 */
@ExtendWith (ApplicationExtension.class)
public abstract class JavaFxTestBase {

    protected static Injector injector;
    protected FXMLControllerLoader loader;

    @Start
    public abstract void start(javafx.stage.Stage stage) throws Exception;

    @BeforeAll
    public static void beforeAll() throws Exception {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }
        injector = Guice.createInjector(new MusicottModule());
    }

    public <T extends Node> T find(FxRobot fxRobot, String query) {
        return fxRobot.lookup(query).query();
    }
}
