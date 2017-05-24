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
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.modules.*;
import javafx.beans.property.*;
import javafx.beans.value.*;

import java.util.Map.*;

/**
 * Guice {@link Module} that configures the instantiation of the necessary classes
 * at the start of the launch of the application
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public class LoaderModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LoadActionFactoryModule());
        install(new WaveformTaskFactoryModule());
        install(new TrackFactoryModule());
    }

    @Provides
    ChangeListener<String> providesUserFolderListener(TaskDemon taskDemon) {
        return taskDemon.getUserFolderListener();
    }

    @Provides
    ChangeListener<Number> providesPlayCountListener(TaskDemon taskDemon) {
        return taskDemon.getIncrementPlayCountChangeListener();
    }

    @Provides
    @RootPlaylist
    Playlist providesRootPlaylist(PlaylistFactory factory) {
        return factory.create("ROOT", true);
    }

    /*
        Property provided by the ServiceDemon
     */

    @Provides
    @UsingLastFmProperty
    ReadOnlyBooleanProperty providesUsingLastFmProperty(ServiceDemon serviceDemon) {
        return serviceDemon.usingLastFmProperty();
    }

    /*
        Properties provided by the ArtistsLibrary
     */
    @Provides
    @ArtistsProperty
    ListProperty<String> providesArtistsProperty(ArtistsLibrary artistsLibrary) {
        return artistsLibrary.artistsListProperty();
    }

    /*
        Properties provided by the TracksLibrary
     */

    @Provides
    @EmptyLibraryProperty
    ReadOnlyBooleanProperty providesEmptyLibraryProperty(TracksLibrary tracksLibrary) {
        return tracksLibrary.emptyTracksLibraryProperty();
    }

    @Provides
    @ShowingTracksProperty
    ListProperty<Entry<Integer, Track>> providesShowingTracksProperty(TracksLibrary tracksLibrary) {
        return tracksLibrary.showingTrackEntriesProperty();
    }
}
