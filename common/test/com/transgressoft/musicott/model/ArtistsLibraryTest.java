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

import com.google.common.collect.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.testfx.framework.junit5.*;

import java.util.*;

import static com.transgressoft.musicott.model.AlbumsLibrary.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith (ApplicationExtension.class)
public class ArtistsLibraryTest {

    ArtistsLibrary artistsLibrary;

    @Start
    public void start(Stage stage) {
        artistsLibrary = new ArtistsLibrary();
    }

    @Test
    @DisplayName ("Add and remove artist track")
    void addRemoveArtistTrack() {
        Track track1 = mock(Track.class);

        assertTrue(artistsLibrary.addArtistTrack("artist1", track1));
        assertTrue(artistsLibrary.contains("artist1"));
        assertTrue(artistsLibrary.removeArtistTrack("artist1", track1));
        assertFalse(artistsLibrary.contains("artist1"));
    }

    @Test
    @DisplayName ("Artist contains matched track")
    void artistContainsMatchedTrack() {
        Track track1 = mock(Track.class);
        when(track1.getName()).thenReturn("my song name");
        when(track1.getArtist()).thenReturn("pepe");
        when(track1.getLabel()).thenReturn("sony");
        when(track1.getGenre()).thenReturn("techno");
        when(track1.getAlbum()).thenReturn("My favourites");

        artistsLibrary.addArtistTrack("artist1", track1);

        assertTrue(artistsLibrary.artistContainsMatchedTrack("artist1", "son"));
        assertTrue(artistsLibrary.artistContainsMatchedTrack("artist1", "pe"));
        assertTrue(artistsLibrary.artistContainsMatchedTrack("artist1", "ony"));
        assertTrue(artistsLibrary.artistContainsMatchedTrack("artist1", "fav"));
        assertTrue(artistsLibrary.artistContainsMatchedTrack("artist1", "tec"));
    }

    @Test
    @DisplayName ("Clear")
    void clear() {
        Track track1 = mock(Track.class);
        Track track2 = mock(Track.class);

        assertTrue(artistsLibrary.addArtistTrack("artist1", track1));
        assertTrue(artistsLibrary.addArtistTrack("artist2", track2));
        artistsLibrary.clear();
        assertFalse(artistsLibrary.contains("artist1"));
        assertFalse(artistsLibrary.contains("artist2"));
    }

    @Test
    @DisplayName ("Get artist albums")
    void getArtistAlbums() {
        Track track1 = mock(Track.class);
        Track track2 = mock(Track.class);
        when(track1.getAlbum()).thenReturn("");
        when(track2.getAlbum()).thenReturn("album1");

        assertTrue(artistsLibrary.addArtistTrack("artist1", track1));
        assertTrue(artistsLibrary.addArtistTrack("artist1", track2));

        ImmutableSet<String> artistAlbums = artistsLibrary.getArtistAlbums("artist1");
        assertEquals(2, artistAlbums.size());
        assertTrue(artistAlbums.contains(UNK_ALBUM));
        assertTrue(artistAlbums.contains("album1"));
    }

    @Test
    @DisplayName ("Get random list of artist tracks")
    void getRandomListOfArtistTracks() {
        Track track1 = mock(Track.class);
        Track track2 = mock(Track.class);
        Track track3 = mock(Track.class);
        Track track4 = mock(Track.class);

        assertTrue(artistsLibrary.addArtistTrack("artist1", track1));
        assertTrue(artistsLibrary.addArtistTrack("artist2", track2));
        assertTrue(artistsLibrary.addArtistTrack("artist1", track3));
        assertTrue(artistsLibrary.addArtistTrack("artist2", track4));

        List<Track> randomListOfArtistTracks1 = artistsLibrary.getRandomListOfArtistTracks("artist1");
        List<Track> randomListOfArtistTracks2 = artistsLibrary.getRandomListOfArtistTracks("artist2");

        assertEquals(2, randomListOfArtistTracks1.size());
        assertTrue(randomListOfArtistTracks1.contains(track1));
        assertTrue(randomListOfArtistTracks1.contains(track3));
        assertEquals(2, randomListOfArtistTracks2.size());
        assertTrue(randomListOfArtistTracks2.contains(track2));
        assertTrue(randomListOfArtistTracks2.contains(track4));
    }
}