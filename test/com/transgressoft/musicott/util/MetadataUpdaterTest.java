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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.util;

import com.transgressoft.musicott.model.*;
import org.junit.jupiter.api.*;

/**
 * @author Octavio Calleya
 */
public class MetadataUpdaterTest {

    private Track testedTrack = prepareTestedTrack();

    private Track prepareTestedTrack() {
        Track track = new Track();

        return testedTrack;
    }

    @Test
    @DisplayName ("Invalid file write audio metadata")
    void invalidFileTest() {

    }

    @Test
    @DisplayName ("Write audio metadata")
    void writeAudioMetadataTest() {

    }

    @Test
    @DisplayName ("Write wav audio metadata")
    void writeWavAudioMetadataTest() {

    }

    @Test
    @DisplayName("Update cover")
    void updateCoverTest() {

    }

    @Test
    @DisplayName("Set cover found in folder")
    void setCoverFoundInFolderTest() {

    }
}