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

package com.transgressoft.musicott.tasks.parse.itunes;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;

import java.util.*;

/**
 * Extends from {@link BaseParseResult} specifying a {@link List}
 * of {@link Playlist}s as result
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class PlaylistsParseResult extends BaseParseResult<List<Playlist>> {

    public PlaylistsParseResult(List<Playlist> parsedItems) {
        super(parsedItems);
    }
}
