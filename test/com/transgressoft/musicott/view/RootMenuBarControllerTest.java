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
import com.transgressoft.musicott.util.guice.modules.*;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.*;
import java.util.Map.*;

import static com.transgressoft.musicott.model.Layout.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith(MockitoExtension.class)
public class RootMenuBarControllerTest extends JavaFxTestBase<RootMenuBarController> {

    static final BooleanProperty falseProperty = new SimpleBooleanProperty(false);

    @Mock
    EditController editControllerMock;
    @Mock
    RootController rootControllerMock;
    @Mock
    NavigationController navControllerMock;
    @Mock
    PlayerController playerControllerMock;
    @Mock
    ErrorDialogController errorDialogMock;
    @Mock
    PlayerFacade playerMock;
    @Mock
    TaskDemon taskDemonMock;
    @Mock
    StageDemon stageDemonMock;
    @Mock
    MainPreferences preferencesMock;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        when(editControllerMock.showingProperty()).thenReturn(falseProperty);

        Map<Class, Object> mocks = ImmutableMap.<Class, Object>builder()
                .put(editControllerMock.getClass(), editControllerMock)
                .put(errorDialogMock.getClass(), errorDialogMock)
                .put(playerMock.getClass(), playerMock)
                .put(taskDemonMock.getClass(), taskDemonMock)
                .put(stageDemonMock.getClass(), stageDemonMock)
                .put(preferencesMock.getClass(), preferencesMock)
                .put(playerControllerMock.getClass(), playerControllerMock)
                .put(rootControllerMock.getClass(), rootControllerMock)
                .build();

        ControllerModule rootModule = new RootModule(rootControllerMock);
        ControllerModule editModule = new EditModule(editControllerMock);
        ControllerModule navModule = new NavigationModule(navControllerMock);
        ControllerModule playerModule = new PlayerModule(playerControllerMock);

        ArrayList<Module> modules = Lists.newArrayList(rootModule, editModule, navModule, playerModule,
                                                       new TestModule(), new ParseModule(),
                                                       new WaveformPaneFactoryModule(), new TrackFactoryModule(),
                                                       new UpdateMusicLibraryFactoryModule());

        injector = injectorWithCustomMocks(mocks, modules);

        loadControllerModule(MENU_BAR);
        stage.setScene(new Scene(module.providesController().getRoot()));

        injector = injector.createChildInjector(module);

        stage.show();
    }

    @Test
    @DisplayName("Singleton controller and stage")
    void singletonController() throws Exception {
        RootMenuBarController anotherController = injector.getInstance(RootMenuBarController.class);

        assertSame(controller, anotherController);
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {

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
            return new SimpleListProperty<>();
        }

        @Provides
        @SearchingProperty
        ReadOnlyBooleanProperty providesSearchingProperty() {
            return falseProperty;
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