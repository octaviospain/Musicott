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

package com.transgressoft.musicott.model;

/**
 * Enum class that defines the the different track attributes that are shown,
 * (and most of them editable) on the table.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public enum TrackField {
    TRACK_ID,
    FILE_FOLDER,
    FILE_NAME,
    COVER_FILE_NAME,
    NAME,
    ARTIST,
    ALBUM,
    GENRE,
    COMMENTS,
    ALBUM_ARTIST,
    LABEL,
    SIZE,
    TOTAL_TIME,
    TRACK_NUMBER,
    YEAR,
    BIT_RATE,
    PLAY_COUNT,
    DISC_NUMBER,
    BPM,
    HAS_COVER,
    IS_IN_DISK,
	IS_COMPILATION,
    DATE_MODIFIED,
    DATE_ADDED;

    /**
     * Checks if a given <tt>TrackField</tt> is an integer numeric field type
     *
     * @param trackField The <tt>TrackField</tt>
     *
     * @return <tt>true</tt> if is an integer numeric field type, <tt>false</tt> otherwise
     */
    public static boolean isIntegerField(TrackField trackField) {
        return trackField == TrackField.TRACK_NUMBER ||
                trackField == TrackField.DISC_NUMBER ||
                trackField == TrackField.YEAR ||
                trackField == TrackField.BPM;
    }
}
