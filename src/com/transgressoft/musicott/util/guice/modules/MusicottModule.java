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
import com.transgressoft.musicott.view.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.property.*;

import java.util.*;

/**
 * Guice {@link Module} that includes some necessary bindings and configurations for
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
        install(new UpdateMusicLibraryFactoryModule());
        install(new ParseModule());
        requestStaticInjection(MetadataParser.class);
        requestStaticInjection(Utils.class);
        requestStaticInjection(TrackTableView.class);
    }

    /*
        Properties provided by the PlaylistTreeView
     */

    @Provides
    @SelectedPlaylistProperty
    ReadOnlyObjectProperty<Optional<Playlist>> providesSelectedPlaylistProperty(PlaylistTreeView playlistTreeView) {
        return playlistTreeView.selectedPlaylistProperty();
    }

    /*
        Properties provided by the NavigationController
     */

    @Provides
    @SelectedMenuProperty
    ReadOnlyObjectProperty<NavigationMode> providesSelectedMenuProperty(NavigationController navigationController) {
        return navigationController.navigationModeProperty();
    }

    /*
        Properties provided by the EditController
     */

    @Provides
    @ShowingEditing
    ReadOnlyBooleanProperty providesShowingEditing(EditController editController) {
        return editController.showingProperty();
    }

    /*
        Properties provided by the RootController
     */

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
}