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

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class TrackFieldTest {

    @Test
    @DisplayName ("Is track number integer field")
    public void isTrackNumberIntegerField() {
        assertTrue(TrackField.isIntegerField(TrackField.TRACK_NUMBER));
    }

    @Test
    @DisplayName ("Is disc number integer field")
    public void isDiscNumberIntegerField() {
        assertTrue(TrackField.isIntegerField(TrackField.DISC_NUMBER));
    }

    @Test
    @DisplayName ("Is year number integer field")
    public void isYearNumberIntegerField() {
        assertTrue(TrackField.isIntegerField(TrackField.YEAR));
    }

    @Test
    @DisplayName ("Is bpm number integer field")
    public void isBpmNumberIntegerField() {
        assertTrue(TrackField.isIntegerField(TrackField.BPM));
    }

    @Test
    @DisplayName ("Is genre number integer field")
    public void isGenreIntegerField() {
        assertFalse(TrackField.isIntegerField(TrackField.GENRE));
    }
}