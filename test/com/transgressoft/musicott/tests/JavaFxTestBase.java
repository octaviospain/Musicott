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
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;

import java.io.*;

/**
 * Base class for testing JavaFX classes
 *
 * @author Octavio Calleya
 */
@ExtendWith ({ApplicationExtension.class, MockitoExtension.class})
public abstract class JavaFxTestBase<T extends InjectableController> implements InjectedApplication {

    @Mock
    protected TaskDemon taskDemonMock;
    @Mock
    protected ServiceDemon serviceDemonMock;
    @Mock
    protected PlayerFacade playerFacadeMock;
    @Mock
    protected MusicLibrary musicLibraryMock;
    @Mock
    protected PlaylistsLibrary playlistsLibraryMock;
    @Mock
    protected TracksLibrary tracksLibraryMock;
    @Mock
    protected AlbumsLibrary albumsLibraryMock;
    @Mock
    protected ArtistsLibrary artistsLibraryMock;
    @Mock
    protected WaveformsLibrary waveformsLibraryMock;
    @Mock
    protected MainPreferences preferencesMock;

    protected T controller;
    protected ControllerModule<T> module;
    protected Module mockedSingletonsTestModule = new MockedSingletonsTestModule();
    protected Injector injector = Guice.createInjector(mockedSingletonsTestModule);

    @Start
    public abstract void start(Stage stage) throws Exception;

    @BeforeAll
    public static void beforeAll() throws Exception {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }
    }

    protected <N extends Node> N find(FxRobot fxRobot, String query) {
        return fxRobot.lookup(query).query();
    }

    protected void loadControllerModule(Layout layout) throws IOException, ReflectiveOperationException {
        module = createController(layout, injector);
        controller = module.providesController();
    }

    private class MockedSingletonsTestModule extends AbstractModule {

        @Override
        protected void configure() {}

        @Provides
        HostServices providesHostServicesSample() {
            return new Application() {
                @Override
                public void start(Stage primaryStage) throws Exception {}}.getHostServices();
        }

        @Provides
        @RootPlaylist
        Playlist providesRootPlaylist(PlaylistFactory factory) {
            return factory.create("ROOT", true);
        }

        @Provides
        TaskDemon providesTaskDemonMock() {
            return taskDemonMock;
        }

        @Provides
        PlayerFacade providesPlayerFacadeMock() {
            return playerFacadeMock;
        }

        @Provides
        ServiceDemon providesServiceDemonMock() {
            return serviceDemonMock;
        }

        @Provides
        MusicLibrary providesMusicLibraryMock() {
            return musicLibraryMock;
        }

        @Provides
        PlaylistsLibrary providesPlaylistsLibraryMock() {
            return playlistsLibraryMock;
        }

        @Provides
        TracksLibrary providesTracksLibraryMock() {
            return tracksLibraryMock;
        }

        @Provides
        ArtistsLibrary providesArtistsLibraryMock() {
            return artistsLibraryMock;
        }

        @Provides
        AlbumsLibrary providesAlbumsLibrary() {
            return albumsLibraryMock;
        }

        @Provides
        WaveformsLibrary waveformsLibraryMock() {
            return waveformsLibraryMock;
        }

        @Provides
        MainPreferences providesPreferencesMock() {
            return preferencesMock;
        }
    }
}