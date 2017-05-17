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

import com.google.common.graph.*;
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.Map.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class NavigationControllerTest extends JavaFxTestBase<NavigationController> {

    BooleanProperty emptyLibraryProperty = new SimpleBooleanProperty(true);
    ListProperty<Entry<Integer, Track>> showingTracksProperty = new SimpleListProperty<>();

    @Mock
    PlayerController playerControllerMock;
    @Mock
    RootController rootControllerMock;

    /*
    Tried to mock this object but an Stack Overflow error is thrown when trying to add
    this object to the scene graph. It seems like mockito is not able to mock JavaFx
    at full purposes.
     */
    PlaylistTreeView playlistTreeView;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        Playlist rootPlaylist = new Playlist(tracksLibraryMock, "ROOT", true);
        MutableGraph<Playlist> graph = GraphBuilder.directed().build();
        graph.addNode(rootPlaylist);
        when(playlistsLibraryMock.getPlaylistsTree()).thenReturn(graph);
        playlistTreeView = new PlaylistTreeView(playlistsLibraryMock, rootPlaylist, injector);

        injector = injector.createChildInjector(new TestModule());

        loadTestController(Layout.NAVIGATION);
        stage.setScene(new Scene(controller.getRoot()));

        injector = injector.createChildInjector(module);

        stage.show();
    }

    @Test
    @DisplayName("Singleton controller")
    void singletonController() {
        NavigationController anotherController = injector.getInstance(NavigationController.class);

        assertSame(controller, anotherController);
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {}

        @Provides
        PlaylistTreeView providesPlaylistTreeViewMock() {
            return playlistTreeView;
        }

        @Provides
        @RootCtrl
        RootController providesRootControllerMock() {
            return rootControllerMock;
        }

        @Provides
        @PlayerCtrl
        PlayerController providesPlayerControllerMock() {
            return playerControllerMock;
        }

        @Provides
        @EmptyLibraryProperty
        ReadOnlyBooleanProperty providesEmptyLibraryProperty() {
            return emptyLibraryProperty;
        }

        @Provides
        @ShowingTracksProperty
        ListProperty<Entry<Integer, Track>> providesShowingTracksProperty() {
            return showingTracksProperty;
        }
    }
}