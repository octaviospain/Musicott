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
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.tasks.parse.audiofiles.*;
import com.transgressoft.musicott.tasks.parse.itunes.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;

/**
 * Guice {@link Module} that configures the instantiation of {@link ParseTask}
 * and {@link ParseAction} classes using a {@link ParseTaskFactory} and a
 * {@link ParseActionFactory} respectively.
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public class ParseModule extends AbstractModule {

    @Override
    protected void configure() {
        Module parseTaskFactoryModule = new FactoryModuleBuilder()
                .implement(ParseTask.class, ItunesParse.class, ItunesParseTask.class)
                .implement(ParseTask.class, AudioParse.class, AudioFilesParseTask.class)
                .build(ParseTaskFactory.class);
        install(parseTaskFactoryModule);

        Module parseActionFactoryModule = new FactoryModuleBuilder()
                .implement(ParseAction.class, ItunesPlaylistAction.class, ItunesPlaylistParseAction.class)
                .implement(ParseAction.class, ItunesTracksAction.class, ItunesTracksParseAction.class)
                .implement(ParseAction.class, AudioFilesAction.class, AudioFilesParseAction.class)
                .build(ParseActionFactory.class);
        install(parseActionFactoryModule);
    }
}