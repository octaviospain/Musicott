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

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import javafx.beans.property.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import static com.transgressoft.musicott.model.Layout.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
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
    PreferencesController preferencesControllerMock;

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        when(editControllerMock.showingProperty()).thenReturn(falseProperty);

        injector = injector.createChildInjector(new TestModule());

        loadControllerModule(MENU_BAR);
        stage.setScene(new Scene(controller.getRoot()));

        injector = injector.createChildInjector(module);

        stage.show();
    }

    @Test
    @DisplayName("Singleton controller")
    void singletonController() throws Exception {
        RootMenuBarController anotherController = injector.getInstance(RootMenuBarController.class);

        assertSame(controller, anotherController);
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {}

        @Provides
        @RootCtrl
        RootController providesRootControllerMock() {
            return rootControllerMock;
        }

        @Provides
        @EditCtrl
        EditController providesEditControllerMock() {
            return editControllerMock;
        }

        @Provides
        @NavigationCtrl
        NavigationController providesNavigationControllerMock() {
            return navControllerMock;
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