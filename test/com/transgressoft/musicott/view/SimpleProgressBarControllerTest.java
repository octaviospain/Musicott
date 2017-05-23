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

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class SimpleProgressBarControllerTest extends JavaFxTestBase<SimpleProgressBarController> {

    ProgressBar progressBar;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        loadTestController(Layout.PROGRESS_BAR);
        stage.show();
    }

    @BeforeEach
    void beforeEach(FxRobot fxRobot) {
        progressBar = find(fxRobot, "#progressBar");
    }

    @Test
    @DisplayName ("Singleton controller")
    void singletonController() {
        SimpleProgressBarController anotherController = injector.getInstance(SimpleProgressBarController.class);

        assertEquals(controller, anotherController);
    }

    @Test
    @DisplayName ("Is indeterminate progress")
    void indeterminateProgress() {
        assertTrue(progressBar.isIndeterminate());
    }
}