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
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.Map.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class RootControllerTest extends JavaFxTestBase<RootController> {

    ListProperty<Entry<Integer, Track>> showingTracksProperty =
            new SimpleListProperty<>(FXCollections.emptyObservableList());
    BooleanProperty falseProperty = new SimpleBooleanProperty(false);
    BooleanProperty emptyLibraryProperty = new SimpleBooleanProperty(false);
    ObjectProperty<NavigationMode> navigationModeProperty = new SimpleObjectProperty<>(NavigationMode.ARTISTS);
    ObjectProperty<Optional<Playlist>> selectedPlaylistProperty = new SimpleObjectProperty<>(Optional.empty());
    ListProperty<String> artistsPropertyMock = new SimpleListProperty<>();

    @Mock
    RootController rootControllerMock;
    @Mock
    ArtistsViewController artistsControllerMock;
    @Mock
    NavigationController navigationControllerMock;
    @Mock
    PlayerController playerControllerMock;
    @Mock
    RootMenuBarController menuBarMock;
    @Mock
    PreferencesController preferencesControllerMock;
    @Mock
    PlayQueueController playQueueControllerMock;
    @Mock
    TrackSetAreaRowFactory trackSetAreaRowFactoryMock;

    PlaylistTreeView playlistTreeView;
    TrackTableView trackTableView;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        trackTableView = new TrackTableView(injector);

        ListProperty<String> artistsListProperty = new SimpleListProperty<>();
        when(artistsLibraryMock.artistsListProperty()).thenReturn(artistsListProperty);

        Playlist rootPlaylist = new Playlist(tracksLibraryMock, "ROOT", true);
        MutableGraph<Playlist> graph = GraphBuilder.directed().build();
        graph.addNode(rootPlaylist);
        when(playlistsLibraryMock.getPlaylistsTree()).thenReturn(graph);
        playlistTreeView = new PlaylistTreeView(playlistsLibraryMock, tracksLibraryMock, rootPlaylist, injector);

        injector = injector.createChildInjector(new TestModule());
        loadTestController(Layout.ROOT);
        stage.show();
    }

    @Test
    @DisplayName ("Singleton controller")
    void singletonController() {
        RootController anotherController = injector.getInstance(RootController.class);

        assertSame(controller, anotherController);
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            install(new WaveformPaneFactoryModule());
        }

        @Provides
        TrackSetAreaRowFactory providesTrackSetAreaRowFactoryMock() {
            return trackSetAreaRowFactoryMock;
        }

        @Provides
        TrackTableView providesTrackTableViewMock() {
            return trackTableView;
        }

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
        @ArtistsCtrl
        ArtistsViewController providesArtistsControllerMock() {
            return artistsControllerMock;
        }

        @Provides
        @NavigationCtrl
        NavigationController providesNavigationControllerMock() {
            return navigationControllerMock;
        }

        @Provides
        @PlayerCtrl
        PlayerController providesPlayerControllerMock() {
            return playerControllerMock;
        }

        @Provides
        @PrefCtrl
        PreferencesController providesPreferencesControllerMock() {
            return preferencesControllerMock;
        }

        @Provides
        @MenuBarCtrl
        RootMenuBarController providesMenuBarControllerMock() {
            return menuBarMock;
        }

        @Provides
        @PlayQueueCtrl
        PlayQueueController providesPlayQueueControllerMock() {
            return playQueueControllerMock;
        }

        @Provides
        @ArtistsProperty
        ListProperty<String> providesArtistsProperty() {
            return artistsPropertyMock;
        }

        @Provides
        @SearchTextProperty
        StringProperty providesSearchingTextProperty() {
            return new SimpleStringProperty("");
        }

        @Provides
        @SelectedMenuProperty
        ReadOnlyObjectProperty<NavigationMode> providesSelectedMenuProperty() {
            return navigationModeProperty;
        }

        @Provides
        @SelectedPlaylistProperty
        ReadOnlyObjectProperty<Optional<Playlist>> providesSelectedPlaylistProperty() {
            return selectedPlaylistProperty;
        }

        @Provides
        @ShowingTableInfoPaneProperty
        ReadOnlyBooleanProperty providesShowingTableInfoPaneProperty() {
            return falseProperty;
        }

        @Provides
        @ShowingNavigationPaneProperty
        ReadOnlyBooleanProperty providesShowingNavigationPaneProperty() {
            return falseProperty;
        }

        @Provides
        @ShowingEditing
        ReadOnlyBooleanProperty providesShowingEditing() {
            return falseProperty;
        }

        @Provides
        @SearchingProperty
        ReadOnlyBooleanProperty providesSearchingProperty() {
            return falseProperty;
        }

        @Provides
        @PreviousButtonDisabledProperty
        ReadOnlyBooleanProperty providesPrevButtonDisabledProperty() {
            return falseProperty;
        }

        @Provides
        @NextButtonDisabledProperty
        ReadOnlyBooleanProperty providesNextButtonDisabledProperty() {
            return falseProperty;
        }

        @Provides
        @PlayPauseProperty
        BooleanProperty providesPlayPauseProperty() {
            return falseProperty;
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