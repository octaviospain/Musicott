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
import com.google.inject.assistedinject.*;
import com.google.inject.name.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.tasks.parse.audiofiles.*;
import com.transgressoft.musicott.tasks.parse.itunes.*;
import com.transgressoft.musicott.util.factories.*;

/**
 * Guice {@link Module} that configures the instantiation of {@link ParseAction}
 * classes using a {@link ParseActionFactoryModule}.
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public class ParseActionFactoryModule extends AbstractModule {

    public static final String ITUNES_PLAYLIST_ACTION = "ItunesPlaylistsAction";
    public static final String ITUNES_TRACKS_ACTION = "ItunesTracksAction";
    public static final String AUDIO_FILES_ACTION = "AudioFilesAction";

    @Override
    protected void configure() {
        Module parseActionFactoryModule = new FactoryModuleBuilder()
                .implement(ParseAction.class, Names.named(ITUNES_PLAYLIST_ACTION), ItunesPlaylistParseAction.class)
                .implement(ParseAction.class, Names.named(ITUNES_TRACKS_ACTION), ItunesTracksParseAction.class)
                .implement(ParseAction.class, Names.named(AUDIO_FILES_ACTION), AudioFilesParseAction.class)
                .build(ParseActionFactory.class);
        install(parseActionFactoryModule);
    }
}