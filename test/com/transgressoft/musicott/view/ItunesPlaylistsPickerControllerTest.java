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

import com.google.common.collect.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import com.worldsworstsoftware.itunes.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;
import org.testfx.util.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class ItunesPlaylistsPickerControllerTest extends JavaFxTestBase<ItunesPlaylistsPickerController> {

    ListView<ItunesPlaylist> sourcePlaylists;
    ListView<ItunesPlaylist> targetPlaylists;
    Button addSelectedButton;
    Button removeSelectedButton;
    Button addAllButton;
    Button removeAllButton;
    Button cancelButton;
    Button importButton;

    ObservableList<ItunesPlaylist> playlists = FXCollections.observableArrayList();
    
    @Override
    @Start
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        doNothing().when(taskDemonMock).cancelItunesImport();
        doNothing().when(taskDemonMock).setItunesPlaylistsToImport(any());

        ItunesPlaylist p1 = mock(ItunesPlaylist.class);
        when(p1.getName()).thenReturn("P1");
        List<Integer> p1Ids = Lists.newArrayList(1, 2, 3);
        when(p1.getTrackIDs()).thenReturn(p1Ids);
        when(p1.getTotalSize()).thenReturn(1024L);
        ItunesPlaylist p2 = mock(ItunesPlaylist.class);
        when(p2.getName()).thenReturn("P2");
        List<Integer> p2Ids = Lists.newArrayList(4, 5, 6);
        when(p2.getTrackIDs()).thenReturn(p2Ids);
        when(p2.getTotalSize()).thenReturn(2048L);
        playlists.addAll(p1, p2);

        loadTestController(Layout.PLAYLISTS_PICKER);
        stage.show();
    }

    @BeforeEach
    void beforeEach(FxRobot fxRobot) {
        sourcePlaylists = find(fxRobot, "#sourcePlaylists");
        targetPlaylists = find(fxRobot, "#targetPlaylists");
        addSelectedButton = find(fxRobot, "#addSelectedButton");
        removeSelectedButton = find(fxRobot, "#removeSelectedButton");
        addAllButton = find(fxRobot, "#addAllButton");
        removeAllButton = find(fxRobot, "#removeAllButton");
        cancelButton = find(fxRobot, "#cancelButton");
        importButton = find(fxRobot, "#importButton");
    }

    @Test
    @DisplayName ("Singleton controller")
    void singletonController() {
        ItunesPlaylistsPickerController anotherController = injector.getInstance(ItunesPlaylistsPickerController.class);

        assertSame(controller, anotherController);
    }

    @Test
    @DisplayName ("Add and remove all")
    void addRemoveAll(FxRobot fxRobot) {
        assertTrue(sourcePlaylists.getItems().isEmpty());
        assertTrue(targetPlaylists.getItems().isEmpty());

        Platform.runLater(() -> controller.pickPlaylists(playlists));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(playlists, sourcePlaylists.getItems());

        fxRobot.clickOn(addAllButton);

        assertTrue(sourcePlaylists.getItems().isEmpty());
        assertEquals(playlists, targetPlaylists.getItems());

        fxRobot.clickOn(removeAllButton);

        assertTrue(targetPlaylists.getItems().isEmpty());
        assertEquals(playlists, sourcePlaylists.getItems());
    }

    @Test
    @DisplayName ("Add and remove selected")
    void addRemoveSelected(FxRobot fxRobot) {
        Platform.runLater(() -> controller.pickPlaylists(playlists));
        WaitForAsyncUtils.waitForFxEvents();

        ListCell<String> firstItem = fxRobot.lookup("#sourcePlaylists").lookup(".list-cell").nth(0).query();
        fxRobot.clickOn(firstItem);
        fxRobot.clickOn(addSelectedButton);

        assertEquals(playlists.get(0), targetPlaylists.getItems().get(0));
        assertEquals(playlists.get(1), sourcePlaylists.getItems().get(0));

        firstItem = fxRobot.lookup("#targetPlaylists").lookup(".list-cell").nth(0).query();
        fxRobot.clickOn(firstItem);
        fxRobot.clickOn(removeSelectedButton);

        assertEquals(playlists, sourcePlaylists.getItems());
        assertTrue(targetPlaylists.getItems().isEmpty());
    }

    @Test
    @DisplayName ("Double click on playlist adds/removes it")
    void doubleClickAddsRemoves(FxRobot fxRobot) {
        Platform.runLater(() -> controller.pickPlaylists(playlists));
        WaitForAsyncUtils.waitForFxEvents();

        ListCell<String> firstItem = fxRobot.lookup("#sourcePlaylists").lookup(".list-cell").nth(0).query();
        fxRobot.doubleClickOn(firstItem);

        assertEquals(playlists.get(0), targetPlaylists.getItems().get(0));
        assertEquals(playlists.get(1), sourcePlaylists.getItems().get(0));

        firstItem = fxRobot.lookup("#targetPlaylists").lookup(".list-cell").nth(0).query();
        fxRobot.doubleClickOn(firstItem);

        assertEquals(playlists, sourcePlaylists.getItems());
        assertTrue(targetPlaylists.getItems().isEmpty());
    }

    @Test
    @DisplayName ("Cancel")
    void cancelButton(FxRobot fxRobot) {
        fxRobot.clickOn(cancelButton);

        verify(taskDemonMock, times(1)).cancelItunesImport();
    }

    @Test
    @DisplayName ("Import")
    void importButton(FxRobot fxRobot) {
        Platform.runLater(() -> controller.pickPlaylists(playlists));
        WaitForAsyncUtils.waitForFxEvents();

        fxRobot.clickOn(addAllButton);
        fxRobot.clickOn(importButton);

        verify(taskDemonMock, times(1)).setItunesPlaylistsToImport(playlists);
    }
}