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

package com.transgressoft.musicott.util.guice.modules;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.view.*;
import javafx.beans.property.*;

import java.util.Map.*;

/**
 * Guice {@link Module} that includes the necessary bindings and configurations for
 * constructing the application.
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public class MusicottModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new WaveformPaneFactoryModule());
        install(new UpdateMusicLibraryFactoryModule());
        requestStaticInjection(MetadataParser.class);
        requestStaticInjection(Utils.class);
    }

    @Provides
    @RootPlaylist
    Playlist providesRootPlaylist(PlaylistFactory factory) {
        return factory.create("ROOT", true);
    }

    @Provides
    @ShowingEditing
    ReadOnlyBooleanProperty providesShowingEditing(EditController editController) {
        return editController.showingProperty();
    }

    @Provides
    @ShowingNavigationPaneProperty
    ReadOnlyBooleanProperty providesShowingNavigationPaneProperty(RootController rootController) {
        return rootController.showNavigationPaneProperty();
    }

    @Provides
    @ShowingTableInfoPaneProperty
    ReadOnlyBooleanProperty providesShowingTableInfoPaneProperty(RootController rootController) {
        return rootController.showTableInfoPaneProperty();
    }

    @Provides
    @SelectedMenuProperty
    ReadOnlyObjectProperty<NavigationMode> providesSelectedMenuProperty(NavigationController navigationController) {
        return navigationController.navigationModeProperty();
    }

    @Provides
    @EmptyLibraryProperty
    ReadOnlyBooleanProperty providesEmptyLibraryProperty(TracksLibrary tracksLibrary) {
        return tracksLibrary.tracksProperty().emptyProperty();
    }

    @Provides
    @SearchingTextProperty
    StringProperty providesSearchingTextProperty(PlayerController playerController) {
        return playerController.searchTextProperty();
    }

    @Provides
    @SearchingProperty
    ReadOnlyBooleanProperty providesSearchingProperty(PlayerController playerController) {
        return playerController.searchFieldFocusedProperty();
    }

    @Provides
    @PlayPauseProperty
    BooleanProperty providesPlayPauseProperty(PlayerController playerController) {
        return playerController.playButtonSelectedProperty();
    }

    @Provides
    @PreviousButtonDisabledProperty
    ReadOnlyBooleanProperty providesPrevButtonDisabledProperty(PlayerController playerController) {
        return playerController.previousButtonDisabledProperty();
    }

    @Provides
    @NextButtonDisabledProperty
    ReadOnlyBooleanProperty providesNexButtonDisabledProperty(PlayerController playerController) {
        return playerController.nextButtonDisabledProperty();
    }

    @Provides
    @ShowingTracksProperty
    ListProperty<Entry<Integer, Track>> providesShowingTracksProperty(TracksLibrary tracksLibrary) {
        return tracksLibrary.showingTracksProperty();
    }
}