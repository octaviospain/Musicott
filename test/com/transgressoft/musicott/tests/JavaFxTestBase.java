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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.tests;

import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.stage.*;
import org.junit.*;
import org.testfx.api.*;
import org.testfx.framework.junit.*;

/**
 * Base class for testing JavaFX classes
 *
 * @author Octavio Calleya
 */
public class JavaFxTestBase extends ApplicationTest {

    protected static String layout;
    protected static Stage testStage;
    protected Application testApplication;
    protected FxRobot robot = new FxRobot();
    protected Object controller;

    @Override
    public void start(Stage stage) throws Exception {
        testApplication = new Application() {

            @Override
            public void start(Stage stage) throws Exception {
                try {
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getResource(layout));
                    Parent nodeLayout = loader.load();
                    controller = loader.getController();
                    testStage.setScene(new Scene(nodeLayout));
                    testStage.setTitle("Test");
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        };

        testApplication.init();
        testApplication.start(testStage);
    }

    @BeforeClass
    public static void setupSpec() throws Exception {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }

//        registerPrimaryStage();
//        setupStage(stage -> {
//            testStage = stage;
//            stage.show();
//            stage.centerOnScreen();
//        });

    }

    @Before
    public void beforeEachTest() throws Exception {
        Platform.runLater(() -> {
//            testStage.show();
//            testStage.centerOnScreen();
        });
    }

    @After
    public void afterEachTest() throws Exception  {
        testApplication.stop();
    }


//    @Before
//    public void beforeEachTest() throws Exception {
//        try {
//            FXMLLoader loader = new FXMLLoader();
//            loader.setLocation(getClass().getResource(layout));
//            Parent nodeLayout = loader.load();
//            controller = loader.getController();
//
//            setupApplication(new ApplicationTest() {
//
//                @Override
//                public void start(Stage stage) throws Exception {
//                    testStage.setScene(new Scene(nodeLayout));
//                    testStage.setTitle("Test");
//                }
//            });
//        }
//        catch (Exception exception) {
//            exception.printStackTrace();
//        }
//    }
}