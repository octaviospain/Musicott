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
import com.google.inject.util.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.*;
import java.util.Map.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith(MockitoExtension.class)
public class RootControllerTest extends JavaFxTestBase<RootController> {

    static BooleanProperty falseProperty = new SimpleBooleanProperty(false);

    @Mock
    EditController editControllerMock;
    @Mock
    NavigationController navControllerMock;
    @Mock
    PlayerController playerControllerMock;
    @Mock
    MainPreferences preferencesMock;
    @Mock
    StageDemon stageDemonMock;
    @Mock
    TaskDemon taskDemonMock;
    @Mock
    PlayerFacade playerMock;
    @Mock
    RootMenuBarController menuBarMock;
    @Mock
    ArtistsViewController artistsControllerMock;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        Map<Class, Object> mocks = ImmutableMap.<Class, Object>builder()
                .put(editControllerMock.getClass(), editControllerMock)
                .put(playerControllerMock.getClass(), playerControllerMock)
                .put(navControllerMock.getClass(), navControllerMock)
                .put(preferencesMock.getClass(), preferencesMock)
                .put(stageDemonMock.getClass(), stageDemonMock)
                .put(taskDemonMock.getClass(), taskDemonMock)
                .put(playerMock.getClass(), playerMock)
                .put(artistsControllerMock.getClass(), artistsControllerMock)
                .put(menuBarMock.getClass(), menuBarMock)
                .build();

        ControllerModule editModule = new EditModule(editControllerMock);
        ControllerModule playerModule = new PlayerModule(playerControllerMock);
        ControllerModule navModule = new NavigationModule(navControllerMock);
        ControllerModule artistsModule = new ArtistsModule(artistsControllerMock);
        ControllerModule menuBarModule = new RootMenuBarModule(menuBarMock);
        RootTestModule rootTestModule = new RootTestModule();

        injector = injectorWithCustomMocks(mocks, editModule, navModule, playerModule, menuBarModule, artistsModule,
                                           new TestModule(), rootTestModule);

        loadControllerModule(Layout.ROOT);
        stage.setScene(new Scene(module.providesController().getRoot()));

        injector = injectorWithCustomMocks(mocks, editModule, navModule, playerModule, menuBarModule, artistsModule,
                                           new TestModule(), Modules.override(rootTestModule).with(module));

        stage.show();
    }

    @Test
    @DisplayName ("Singleton controller")
    void singletonController() {
        RootController controller1 = injector.getInstance(RootController.class);
        RootController controller2 = injector.getInstance(RootController.class);

        assertSame(controller1, controller2);
    }

    private class RootTestModule extends AbstractModule {

        @Override
        protected void configure() {

        }

        @Provides
        @RootCtrl
        RootController providesRootController() {
            return controller;
        }
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            install(new ParseModule());
            install(new TrackFactoryModule());
            install(new UpdateMusicLibraryFactoryModule());
            install(new TrackSetAreaRowFactoryModule());
            install(new WaveformPaneFactoryModule());
        }

        @Provides
        @RootPlaylist
        Playlist providesRootPlaylist(PlaylistFactory factory) {
            return factory.create("ROOT", true);
        }

        @Provides
        @ShowingEditing
        ReadOnlyBooleanProperty providesShowingEditing() {
            return falseProperty;
        }

        @Provides
        @ShowingNavigationPaneProperty
        ReadOnlyBooleanProperty providesShowingNavigationPaneProperty() {
            return falseProperty;
        }

        @Provides
        @ShowingTableInfoPaneProperty
        ReadOnlyBooleanProperty providesShowingTableInfoPaneProperty() {
            return falseProperty;
        }

        @Provides
        @SelectedMenuProperty
        ReadOnlyObjectProperty<NavigationMode> providesSelectedMenuProperty() {
            return new SimpleObjectProperty<>(NavigationMode.ALL_TRACKS);
        }

        @Provides
        @EmptyLibraryProperty
        ReadOnlyBooleanProperty providesEmptyLibraryProperty() {
            return falseProperty;
        }

        @Provides
        @ShowingTracksProperty
        ListProperty<Entry<Integer, Track>> providesShowingTracksProperty() {
            return new SimpleListProperty<>(FXCollections.observableArrayList());
        }

        @Provides
        @SearchingProperty
        ReadOnlyBooleanProperty providesSearchingProperty() {
            return falseProperty;
        }

        @Provides
        @SearchingTextProperty
        StringProperty providesSearchingTextProperty() {
            return new SimpleStringProperty("test");
        }

        @Provides
        @PlayPauseProperty
        BooleanProperty providesPlayPauseProperty() {
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
    }
}