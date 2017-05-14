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
import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.*;
import java.util.Map.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith(MockitoExtension.class)
public class NavigationControllerTest extends JavaFxTestBase<NavigationController> {

    static BooleanProperty falseProperty = new SimpleBooleanProperty(false);

    @Mock
    StageDemon stageDemonMock;
    @Mock
    TaskDemon taskDemonMock;
    @Mock
    PlayerFacade playerFacadeMock;
    @Mock
    PlayerController playerControllerMock;
    @Mock
    RootController rootControllerMock;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        injector = Guice.createInjector(new TestModule());

        PlaylistTreeView playlistTreeViewMock = mock(PlaylistTreeView.class);
        doNothing().when(playlistTreeViewMock).clearAndSelect(anyInt());

        Map<Class, Object> mocks = ImmutableMap.<Class, Object>builder()
                                               .put(stageDemonMock.getClass(), stageDemonMock)
                                               .put(taskDemonMock.getClass(), taskDemonMock)
                                               .put(playerFacadeMock.getClass(), playerFacadeMock)
                                               .put(playerControllerMock.getClass(), playerControllerMock)
                                               .put(rootControllerMock.getClass(), rootControllerMock)
                                               .put(playlistTreeViewMock.getClass(), playlistTreeViewMock)
                                               .build();
        injector = injectorWithCustomMocks(mocks, new TestModule());

        loadControllerModule(Layout.NAVIGATION);
        stage.setScene(new Scene(module.providesController().getRoot()));

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
        protected void configure() {
            install(new ParseModule());
            install(new TrackFactoryModule());
        }

        @Provides
        @RootPlaylist
        Playlist providesRootPlaylist(PlaylistFactory factory) {
            return factory.create("ROOT", true);
        }

        @Provides
        @EmptyLibraryProperty
        ReadOnlyBooleanProperty providesEmptyLibraryProperty() {
            return falseProperty;
        }

        @Provides
        @ShowingTracksProperty
        ListProperty<Entry<Integer, Track>> providesShowingTracksProperty() {
            return new SimpleListProperty<>();
        }
    }
}