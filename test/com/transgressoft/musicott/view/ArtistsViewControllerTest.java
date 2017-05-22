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
    StringProperty searchTextProperty = new SimpleStringProperty("");
    ObservableList<String> artists = FXCollections.observableArrayList("John Lennon", "Queen");

    @Override
    @Start
    public void start(Stage stage) throws Exception {
        artistsListProperty.setValue(artists);
        doNothing().when(musicLibraryMock).playRandomArtistPlaylist(anyString());
        doNothing().when(musicLibraryMock).showArtist(anyString());
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

        assertEquals(Optional.of(artists.get(0)), selectedArtistProperty.get());
        assertEquals(artists.get(0), nameLabel.getText());

        // When clicking on the first artist
        ListCell<String> firstItem = fxRobot.lookup("#artistsListView").lookup(".list-cell").nth(1).query();
        fxRobot.clickOn(firstItem);

        // Labels and properties are updated
        assertEquals(artists.get(1), firstItem.getText());
        assertEquals(Optional.of(artists.get(1)), selectedArtistProperty.get());
        assertEquals(artists.get(1), nameLabel.getText());
        assertEquals("0 tracks", totalTracksLabel.getText());
        assertEquals("0 albums", totalAlbumsLabel.getText());
        assertTrue(artistRandomButton.isVisible());
    }

    @Test
    @DisplayName ("Play random from artist")
    void playRandomFromArtist(FxRobot fxRobot) {
        ReadOnlyObjectProperty<Optional<String>> selectedArtistProperty = controller.selectedArtistProperty();

        assertEquals(Optional.of(artists.get(0)), selectedArtistProperty.get());
        assertTrue(artistRandomButton.isVisible());
        fxRobot.clickOn(artistRandomButton);
    }

    @Test
    @DisplayName ("Double click on artist plays random from artist")
    void artistSelectedOnLaunch(FxRobot fxRobot) {
        ListCell<String> firstItem = fxRobot.lookup("#artistsListView").lookup(".list-cell").nth(1).query();
        fxRobot.doubleClickOn(firstItem);

        verify(musicLibraryMock, times(1)).playRandomArtistPlaylist(anyString());
    }

    @Test
    @DisplayName ("Searching text filter the artists")
    void searchingFilterTheArtists() {
        assertEquals(2, artistsListView.getItems().size());

        Platform.runLater(() -> searchTextProperty.setValue("nonexistentartist"));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0, artistsListView.getItems().size());

        Platform.runLater(() -> searchTextProperty.setValue("Que"));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, artistsListView.getItems().size());
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
            return searchTextProperty;
        }
    }
}