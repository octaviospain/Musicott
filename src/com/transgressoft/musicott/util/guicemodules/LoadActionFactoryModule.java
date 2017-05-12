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
import com.transgressoft.musicott.tasks.load.*;
import com.transgressoft.musicott.util.factories.*;

/**
 * Guice {@link Module} that configures the instantiation of {@link LoadAction}
 * classes using a {@link LoadActionFactory}.
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public class LoadActionFactoryModule extends AbstractModule {

    public static final String TRACKS_ACTION = "TracksAction";
    public static final String PLAYLIST_ACTION = "PlaylistAction";
    public static final String WAVEFORMS_ACTION = "WaveformsAction";

    @Override
    protected void configure() {
        Module loadActionFactoryModule = new FactoryModuleBuilder()
                .implement(LoadAction.class, Names.named(TRACKS_ACTION), TracksLoadAction.class)
                .implement(LoadAction.class, Names.named(PLAYLIST_ACTION), PlaylistsLoadAction.class)
                .implement(LoadAction.class, Names.named(WAVEFORMS_ACTION), WaveformsLoadAction.class)
                .build(LoadActionFactory.class);
        install(loadActionFactoryModule);
    }
}