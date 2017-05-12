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

package com.transgressoft.musicott.util.factories;

import com.google.inject.name.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.tasks.parse.audiofiles.*;
import com.transgressoft.musicott.tasks.parse.itunes.*;
import com.worldsworstsoftware.itunes.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.util.guicemodules.ParseActionFactoryModule.*;

/**
 * Factory interface to be used by Guice's dependency injection for creating
 * {@link ParseAction} objects
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public interface ParseActionFactory {

    @Named (ITUNES_PLAYLIST_ACTION)
    ItunesPlaylistParseAction create(List<ItunesPlaylist> itunesPlaylistsToParse,
            Map<Integer, Track> itunesIdToMusicottTrackMap, BaseParseTask parentTask);

    @Named (ITUNES_TRACKS_ACTION)
    ItunesTracksParseAction create(List<ItunesTrack> itunesTracks, int metadataPolicy, boolean holdPlayCount,
            BaseParseTask parentTask);

    @Named (AUDIO_FILES_ACTION)
    AudioFilesParseAction create(List<File> filesToParse, BaseParseTask parentTask);
}