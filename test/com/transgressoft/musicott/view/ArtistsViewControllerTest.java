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
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
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
public class ArtistsViewControllerTest extends JavaFxTestBase<ArtistsViewController> {

    TrackTableViewContextMenu trackTableViewContextMenuMock;

    Button artistRandomButton;
    ListView<String> artistsListView;
    ListView<TrackSetAreaRow> trackSetsListView;
    Label nameLabel;
    Label totalAlbumsLabel;
    Label totalTracksLabel;

    ListProperty<String> artistsListProperty = new SimpleListProperty<>();
    StringProperty searchingTextProperty = new SimpleStringProperty("");
    ObservableList<String> artists = FXCollections.observableArrayList("John Lennon", "Queen");

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        trackTableViewContextMenuMock = mock(TrackTableViewContextMenu.class);

        injector = injector.createChildInjector(new TestModule());

        loadTestController(Layout.ARTISTS);
        stage.setScene(new Scene(controller.getRoot()));

        injector = injector.createChildInjector(module);

        stage.show();
    }

    @BeforeEach
    void beforeEach(FxRobot fxRobot) {
        artistRandomButton = find(fxRobot, "#artistRandomButton");
        artistsListView = find(fxRobot, "#artistsListView");
        trackSetsListView = find(fxRobot, "#trackSetsListView");
        nameLabel = find(fxRobot, "#nameLabel");
        totalAlbumsLabel = find(fxRobot, "#totalAlbumsLabel");
        totalTracksLabel = find(fxRobot, "#totalTracksLabel");
    }

    @Test
    @DisplayName ("Singleton controller")
    void singletonController() {
        ArtistsViewController anotherController = injector.getInstance(ArtistsViewController.class);

        assertSame(controller, anotherController);
    }

    @Test
    @DisplayName ("Artist name change on click")
    void artistNameChange(FxRobot fxRobot) {
        ReadOnlyObjectProperty<Optional<String>> selectedArtistProperty = controller.selectedArtistProperty();

        assertEquals(Optional.empty(), selectedArtistProperty.get());
        assertEquals("", nameLabel.getText());
        assertFalse(artistRandomButton.isVisible());

        Platform.runLater(() -> artistsListProperty.setValue(artists));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(artistsListView.getItems().size(), 2);
        assertEquals(artistsListView.getItems().get(0), artists.get(0));
        assertEquals(artistsListView.getItems().get(1), artists.get(1));

        // When clicking on the first artist
        ListCell<String> firstItem = fxRobot.lookup("#artistsListView").lookup(".list-cell").nth(0).query();
        fxRobot.clickOn(firstItem);

        // Label and property are updated
        assertEquals(artists.get(0), firstItem.getText());
        assertEquals(Optional.of(artists.get(0)), selectedArtistProperty.get());
        assertEquals(artists.get(0), nameLabel.getText());
        assertTrue(artistRandomButton.isVisible());
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            install(new TrackSetAreaRowFactoryModule());
        }

        @Provides
        TrackTableViewContextMenu providesContextMenuMock() {
            return trackTableViewContextMenuMock;
        }

        @Provides
        @ArtistsProperty
        ListProperty<String> providesArtistsPropertyMock() {
            return artistsListProperty;
        }

        @Provides
        @SearchTextProperty
        StringProperty providesSearchingTextProperty() {
            return searchingTextProperty;
        }
    }
}