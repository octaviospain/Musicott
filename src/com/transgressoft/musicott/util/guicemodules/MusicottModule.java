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

package com.transgressoft.musicott.util.guicemodules;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.beans.property.*;

/**
 * Guice {@link Module} that includes the necessary bindings and configurations of the application.
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public class MusicottModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ParseTaskFactoryModule());
        install(new ParseActionFactoryModule());
        install(new LoadActionFactoryModule());
        install(new TrackFactoryModule());
        install(new TrackSetAreaRowFactoryModule());
        install(new WaveformPaneFactoryModule());
        install(new UpdateMusicLibraryFactoryModule());
        requestStaticInjection(MetadataParser.class);
        requestStaticInjection(Utils.class);
    }

    @Provides
    ReadOnlyBooleanProperty providesEmptyLibraryProperty(TracksLibrary tracksLibrary) {
        return tracksLibrary.tracksProperty().emptyProperty();
    }
}