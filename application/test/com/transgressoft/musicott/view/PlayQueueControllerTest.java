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

import com.transgressoft.musicott.tests.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.testfx.framework.junit5.*;

import static com.transgressoft.musicott.model.Layout.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class PlayQueueControllerTest extends JavaFxTestBase<PlayQueueController> {

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        loadTestController(PLAY_QUEUE);
        stage.show();
    }

    @Test
    @DisplayName ("Singleton controller")
    void singletonController() {
        PlayQueueController anotherController = injector.getInstance(PlayQueueController.class);

        assertSame(controller, anotherController);
    }
}