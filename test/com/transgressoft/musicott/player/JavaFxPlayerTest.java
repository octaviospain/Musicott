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

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import javafx.application.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.stage.*;
import org.junit.*;
import org.junit.runner.*;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.*;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author Octavio Calleya
 */
@RunWith (PowerMockRunner.class)
@PrepareForTest (PlayerFacade.class)
public class JavaFxPlayerTest extends JavaFxTestBase {

    private Path testFilesPath = Paths.get("test-resources", "testfiles");
    private TrackPlayer player;

    @BeforeClass
    public static void setupSpec() throws Exception {
    }

    @Override
    public void start(Stage stage) throws Exception {
        testStage = stage;
        layout = null;
        super.start(stage);
    }

    @Override
    @Before
    public void beforeEachTest() throws Exception {
        PowerMockito.mockStatic(PlayerFacade.class);
        PlayerFacade playerMock = mock(PlayerFacade.class);
        when(PlayerFacade.getInstance()).thenReturn(playerMock);

        Track track = new Track(0, testFilesPath.toString(), "testeable.mp3");
        player = new JavaFxPlayer();
        player.setTrack(track);
    }

    @Override
    @After
    public void afterEachTest() throws Exception {
        super.afterEachTest();
        verifyStatic();
    }

    @Test
    public void playerStateReadyTest() {
        sleep(500);
        assertEquals(Status.READY, player.getStatus());
    }

    @Test
    public void playerVolumeTest() {
        Platform.runLater(() -> player.setVolume(- 1.0));
        sleep(1000);
        assertEquals(0.0, player.volumeProperty().get());
    }

    @Test
    public void playerPlayTest() {
        Platform.runLater(() -> player.play());
        sleep(500);
        assertEquals(Status.PLAYING, player.getStatus());
    }

    @Test
    public void playerPauseTest() {
        Platform.runLater(() -> {
            player.play();
            player.pause();
        });
        sleep(500);
        assertEquals(Status.PAUSED, player.getStatus());
    }

    @Test
    public void playerStopTest() {
        Platform.runLater(() -> {
            player.play();
            player.stop();
        });
        sleep(500);
        assertEquals(Status.READY, player.getStatus());
    }
}
