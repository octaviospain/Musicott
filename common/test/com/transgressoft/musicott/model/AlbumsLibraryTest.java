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
import javafx.collections.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.testfx.framework.junit5.*;
import org.testfx.util.*;

import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith (ApplicationExtension.class)
public class AlbumsLibraryTest {

    AlbumsLibrary albumsLibrary;

    @Start
    public void start(Stage stage) throws Exception {
        albumsLibrary = new AlbumsLibrary();
    }

    @Test
    @DisplayName ("Add tracks remove tracks")
    void addTracksRemoveTracks() {
        Entry<Integer, Track> track1 = new SimpleEntry<>(1, mock(Track.class));
        Entry<Integer, Track> track2 = new SimpleEntry<>(2, mock(Track.class));
        Entry<Integer, Track> track3 = new SimpleEntry<>(3, mock(Track.class));

        ArrayList<Entry<Integer, Track>> tracks1 = Lists.newArrayList(track1, track2);
        ArrayList<Entry<Integer, Track>> tracks2 = Lists.newArrayList(track3);
        albumsLibrary.addTracks("album1", tracks1);
        WaitForAsyncUtils.waitForFxEvents();
        albumsLibrary.addTracks("album2", tracks2);

        assertEquals(2, albumsLibrary.albumsListProperty().size());
        assertEquals("album1", albumsLibrary.albumsListProperty().get(0));
        assertEquals("album2", albumsLibrary.albumsListProperty().get(1));

        albumsLibrary.removeTracks("album3", tracks1);

        assertTrue(albumsLibrary.albumsListProperty().size() == 2);
        assertEquals("album1", albumsLibrary.albumsListProperty().get(0));
        assertEquals("album2", albumsLibrary.albumsListProperty().get(1));

        assertTrue(albumsLibrary.removeTracks("album1", tracks1));
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(albumsLibrary.removeTracks("album2", tracks2));
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(albumsLibrary.albumsListProperty().isEmpty());
    }

    @Test
    @DisplayName ("Clear tracks")
    void clearTracks() {
        Entry<Integer, Track> track1 = new SimpleEntry<>(1, mock(Track.class));
        Entry<Integer, Track> track2 = new SimpleEntry<>(2, mock(Track.class));
        Entry<Integer, Track> track3 = new SimpleEntry<>(3, mock(Track.class));

        ArrayList<Entry<Integer, Track>> tracks1 = Lists.newArrayList(track1, track2);
        ArrayList<Entry<Integer, Track>> tracks2 = Lists.newArrayList(track3);
        albumsLibrary.addTracks("album1", tracks1);
        WaitForAsyncUtils.waitForFxEvents();
        albumsLibrary.addTracks("album2", tracks2);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(2, albumsLibrary.albumsListProperty().size());
        assertEquals("album1", albumsLibrary.albumsListProperty().get(0));
        assertEquals("album2", albumsLibrary.albumsListProperty().get(1));

        albumsLibrary.clear();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(albumsLibrary.albumsListProperty().isEmpty());
    }

    @Test
    @DisplayName ("Update track albums")
    void updateTrackAlbums() {
        Entry<Integer, Track> track1 = new SimpleEntry<>(1, mock(Track.class));
        Entry<Integer, Track> track2 = new SimpleEntry<>(2, mock(Track.class));
        Entry<Integer, Track> track3 = new SimpleEntry<>(3, mock(Track.class));

        ArrayList<Entry<Integer, Track>> tracks1 = Lists.newArrayList(track1, track2);
        ArrayList<Entry<Integer, Track>> tracks2 = Lists.newArrayList(track3);
        albumsLibrary.addTracks("album1", tracks1);
        albumsLibrary.addTracks("album2", tracks2);
        WaitForAsyncUtils.waitForFxEvents();

        albumsLibrary.updateTrackAlbums(tracks1, Collections.singleton("album1"), "album3");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(2, albumsLibrary.albumsListProperty().size());
        assertFalse(albumsLibrary.albumsListProperty().contains("album1"));
        assertEquals("album2", albumsLibrary.albumsListProperty().get(0));
        assertEquals("album3", albumsLibrary.albumsListProperty().get(1));
    }

    @Test
    @DisplayName ("Tracks by album")
    void tracksByAlbum() {
        Track t1 = mock(Track.class);
        when(t1.getArtistsInvolved()).thenReturn(FXCollections.observableSet("artist1", "artist2"));
        Entry<Integer, Track> track1 = new SimpleEntry<>(1, t1);
        Track t2 = mock(Track.class);
        when(t2.getArtistsInvolved()).thenReturn(FXCollections.observableSet("artist1"));
        Entry<Integer, Track> track2 = new SimpleEntry<>(2, t2);
        Track t3 = mock(Track.class);
        when(t3.getArtistsInvolved()).thenReturn(FXCollections.observableSet("artist2", "artist3"));
        Entry<Integer, Track> track3 = new SimpleEntry<>(3, t3);

        ArrayList<Entry<Integer, Track>> tracks1 = Lists.newArrayList(track1, track2);
        ArrayList<Entry<Integer, Track>> tracks2 = Lists.newArrayList(track3);
        albumsLibrary.addTracks("album1", tracks1);
        albumsLibrary.addTracks("album2", tracks2);
        WaitForAsyncUtils.waitForFxEvents();

        Multimap<String, Entry<Integer, Track>> tracksByAlbum = albumsLibrary.getTracksByAlbum("artist2", Collections.singleton("album1"));
        assertTrue(tracksByAlbum.containsKey("album1"));
        assertTrue(tracksByAlbum.get("album1").contains(track1));

        Set<String> albumSet = new HashSet<>();
        albumSet.add("album1");
        albumSet.add("album2");
        tracksByAlbum = albumsLibrary.getTracksByAlbum("artist2", albumSet);
        assertTrue(tracksByAlbum.containsKey("album1"));
        assertTrue(tracksByAlbum.get("album1").contains(track1));
        assertTrue(tracksByAlbum.containsKey("album2"));
        assertTrue(tracksByAlbum.get("album2").contains(track3));
    }
}