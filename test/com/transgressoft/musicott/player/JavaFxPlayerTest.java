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

package com.transgressoft.musicott.player;

import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.scene.media.MediaPlayer.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.testfx.framework.junit5.*;
import org.testfx.util.*;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith (ApplicationExtension.class)
public class JavaFxPlayerTest {

    static Injector injector;
    static TrackFactory trackFactory;

    Path testFilesPath = Paths.get("test-resources", "testfiles");
    TrackPlayer player;

    @BeforeAll
    public static void beforeAll() throws Exception {
        injector = Guice.createInjector(new TestModule());
        trackFactory = injector.getInstance(TrackFactory.class);
    }

    @BeforeEach
    void beforeEachTest() throws Exception {
        Track track = trackFactory.create(testFilesPath.toString(), "testeable.mp3");
        player = new JavaFxPlayer();
        player.setTrack(track);
    }

    @Test
    @DisplayName("Negative volume sets to zero value")
    void playerVolumeTest() {
        Platform.runLater(() -> player.setVolume(- 1.0));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0.0, player.volumeProperty().get());
    }

    @Test
    @DisplayName("Ready Status")
    void playerStateReadyTest() {
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(Status.READY, player.getStatus());
    }

    @Test
    @DisplayName("Playing status")
    void playerPlayTest() {
        Platform.runLater(() -> player.play());
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(Status.PLAYING, player.getStatus());
    }

    @Test
    @DisplayName("Pause status")
    void playerPauseTest() {
        Platform.runLater(() -> player.play());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(Status.PLAYING, player.getStatus());

        Platform.runLater(() -> player.pause());
        WaitForAsyncUtils.waitForFxEvents(10);
        assertEquals(Status.PAUSED, player.getStatus());
    }

    @Test
    @DisplayName("Stop status")
    void playerStopTest() {
        Platform.runLater(() -> {
            player.play();
            player.stop();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(Status.READY, player.getStatus());
    }

    private static class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            install(new TrackFactoryModule());
            bind(ErrorDialogController.class).toInstance(mock(ErrorDialogController.class));
            MainPreferences preferences = mock(MainPreferences.class);
            when(preferences.getTrackSequence()).thenReturn(0);
            bind(MainPreferences.class).toInstance(preferences);
        }

        @Provides
        ChangeListener<Number> providesPlayCountListener() {
            return (a, b, c) -> {};
        }
    }
}
