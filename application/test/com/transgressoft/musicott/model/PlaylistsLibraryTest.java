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
import com.google.common.graph.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.*;
import javafx.beans.value.*;
import javafx.collections.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;

import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith (MockitoExtension.class)
public class PlaylistsLibraryTest {

    @Mock
    TracksLibrary tracksLibraryMock;
    @Mock
    TaskDemon taskDemonMock;

    Playlist rootPlaylist;
    PlaylistsLibrary playlistsLibrary;

    @BeforeEach
    @SuppressWarnings ("unchecked")
    void beforeEach() {
        rootPlaylist = new Playlist(tracksLibraryMock, "ROOT", true);
        doNothing().when(taskDemonMock).saveLibrary(anyBoolean(), anyBoolean(), anyBoolean());
        ListChangeListener<Integer> tracksListenerMock = mock(ListChangeListener.class);
        ChangeListener<String> nameListenerMock = mock(ChangeListener.class);
        doNothing().when(nameListenerMock).changed(null, null, null);
        playlistsLibrary = new PlaylistsLibrary(rootPlaylist);
        playlistsLibrary.setPlaylistTracksListener(tracksListenerMock);
        playlistsLibrary.setPlaylistNameListener(nameListenerMock);
    }

    @Test
    @DisplayName ("Remove playlists then is empty")
    void clearPlaylistsThenIsEmpty() {
        Playlist p1 = new Playlist(tracksLibraryMock, "2", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "3", false);
        playlistsLibrary.addPlaylist(rootPlaylist, p1);
        playlistsLibrary.addPlaylist(rootPlaylist, p2);

        assertFalse(playlistsLibrary.isEmpty());

        playlistsLibrary.deletePlaylist(p1);
        playlistsLibrary.deletePlaylist(p2);

        assertTrue(playlistsLibrary.isEmpty());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
    }

    @Test
    @DisplayName ("Clear playlists")
    void clearPlaylists() {
        Playlist folder = new Playlist(tracksLibraryMock, "folder", true);
        Playlist p1 = new Playlist(tracksLibraryMock, "1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "2", false);
        folder.addPlaylistChild(p1);
        folder.addPlaylistChild(p2);
        p1.addTracks(Lists.newArrayList(1));
        p2.addTracks(Lists.newArrayList(2));

        playlistsLibrary.addPlaylistToRoot(folder);
        playlistsLibrary.clearPlaylists();

        assertEquals(1, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
        assertEquals(2, playlistsLibrary.getPlaylistsTree().successors(folder).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p1).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p2).size());
        assertContainsPlaylists(folder, p1, p2);

        assertTrue(p1.getTracks().isEmpty());
        assertTrue(p2.getTracks().isEmpty());
    }

    @Test
    @DisplayName ("Remove tracks from playlists")
    void removeTracksFromPlaylists() {
        Playlist folder = new Playlist(tracksLibraryMock, "folder", true);
        Playlist p1 = new Playlist(tracksLibraryMock, "1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "2", false);
        folder.addPlaylistChild(p1);
        folder.addPlaylistChild(p2);
        p1.addTracks(Lists.newArrayList(1));
        p2.addTracks(Lists.newArrayList(2));

        Track track1 = mock(Track.class);
        Track track2 = mock(Track.class);
        when(track1.getTrackId()).thenReturn(1);
        when(track2.getTrackId()).thenReturn(2);

        playlistsLibrary.addPlaylistToRoot(folder);
        playlistsLibrary.removeFromPlaylists(Lists.newArrayList(track1, track2));

        assertTrue(folder.getTracks().isEmpty());
        assertTrue(p1.getTracks().isEmpty());
        assertTrue(p2.getTracks().isEmpty());

        assertEquals(1, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
        assertEquals(2, playlistsLibrary.getPlaylistsTree().successors(folder).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p1).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p2).size());
        assertContainsPlaylists(folder, p1, p2);
    }

    @Test
    @DisplayName ("Add playlists to root")
    void addPlaylistsToRoot() {
        Playlist p1 = new Playlist(tracksLibraryMock, "1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "2", false);
        Playlist p3 = new Playlist(tracksLibraryMock, "3", false);
        playlistsLibrary.addPlaylistToRoot(p1);
        playlistsLibrary.addPlaylistToRoot(p2);
        playlistsLibrary.addPlaylistToRoot(p3);

        assertEquals(rootPlaylist, playlistsLibrary.getParentPlaylist(p1));
        assertTrue(rootPlaylist.getContainedPlaylists().contains(p1));
        assertEquals(rootPlaylist, playlistsLibrary.getParentPlaylist(p2));
        assertTrue(rootPlaylist.getContainedPlaylists().contains(p2));
        assertEquals(rootPlaylist, playlistsLibrary.getParentPlaylist(p3));
        assertTrue(rootPlaylist.getContainedPlaylists().contains(p3));

        assertEquals(3, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p1).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p2).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p3).size());
        assertContainsPlaylists(p1, p2, p3);
    }

    @Test
    @DisplayName ("Add playlists to playlist folder")
    void addPlaylistsToPlaylistFolder() {
        Playlist folder = new Playlist(tracksLibraryMock, "folder", true);
        Playlist p1 = new Playlist(tracksLibraryMock, "1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "2", false);
        folder.addPlaylistChild(p1);
        folder.addPlaylistChild(p2);
        playlistsLibrary.addPlaylistToRoot(folder);

        assertEquals(folder, playlistsLibrary.getParentPlaylist(p1));
        assertTrue(folder.getContainedPlaylists().contains(p1));
        assertEquals(folder, playlistsLibrary.getParentPlaylist(p2));
        assertTrue(folder.getContainedPlaylists().contains(p2));
        assertEquals(rootPlaylist, playlistsLibrary.getParentPlaylist(folder));
        assertTrue(rootPlaylist.getContainedPlaylists().contains(folder));

        assertEquals(1, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
        assertEquals(2, playlistsLibrary.getPlaylistsTree().successors(folder).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p1).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p2).size());
        assertContainsPlaylists(folder, p1, p2);
    }

    @Test
    @DisplayName ("Add playlists recursively")
    void addPlaylistsRecursively() {
        Playlist folder = new Playlist(tracksLibraryMock, "folder", true);
        Playlist p1 = new Playlist(tracksLibraryMock, "1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "2", false);
        Playlist folder2 = new Playlist(tracksLibraryMock, "folder2", true);
        Playlist p3 = new Playlist(tracksLibraryMock, "3", true);
        Playlist p4 = new Playlist(tracksLibraryMock, "4", true);
        folder.addPlaylistChild(p1);
        folder.addPlaylistChild(p2);
        folder2.addPlaylistChild(p3);
        folder2.addPlaylistChild(p4);
        folder.addPlaylistChild(folder2);

        MutableGraph<Playlist> testGraph = GraphBuilder.directed().build();
        testGraph.addNode(rootPlaylist);
        testGraph.putEdge(rootPlaylist, folder);
        testGraph.putEdge(folder, p1);
        testGraph.putEdge(folder, p2);
        testGraph.putEdge(folder, folder2);
        testGraph.putEdge(folder2, p3);
        testGraph.putEdge(folder2, p4);

        playlistsLibrary.addPlaylistsRecursively(rootPlaylist, testGraph.successors(rootPlaylist));

        assertEquals(folder, playlistsLibrary.getParentPlaylist(p1));
        assertEquals(folder, playlistsLibrary.getParentPlaylist(p2));
        assertEquals(rootPlaylist, playlistsLibrary.getParentPlaylist(folder));
        assertEquals(folder, playlistsLibrary.getParentPlaylist(folder2));
        assertEquals(folder2, playlistsLibrary.getParentPlaylist(p3));
        assertEquals(folder2, playlistsLibrary.getParentPlaylist(p4));

        assertEquals(1, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
        assertEquals(3, playlistsLibrary.getPlaylistsTree().successors(folder).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p1).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p2).size());
        assertEquals(2, playlistsLibrary.getPlaylistsTree().successors(folder2).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p3).size());
        assertEquals(0, playlistsLibrary.getPlaylistsTree().successors(p4).size());
        assertContainsPlaylists(folder, folder2, p1, p2, p3, p4);
    }

    @Test
    @DisplayName ("Move playlist")
    void movePlaylist() {
        Playlist folder = new Playlist(tracksLibraryMock, "folder", true);
        Playlist p1 = new Playlist(tracksLibraryMock, "1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "2", false);
        folder.addPlaylistChild(p1);
        folder.addPlaylistChild(p2);

        Playlist folder2 = new Playlist(tracksLibraryMock, "folder2", true);
        Playlist p3 = new Playlist(tracksLibraryMock, "3", false);
        Playlist p4 = new Playlist(tracksLibraryMock, "4", false);
        folder2.addPlaylistChild(p3);
        folder2.addPlaylistChild(p4);
        folder.addPlaylistChild(folder2);

        Playlist folder3 = new Playlist(tracksLibraryMock, "folder3", true);
        Playlist p5 = new Playlist(tracksLibraryMock, "5", false);
        Playlist p6 = new Playlist(tracksLibraryMock, "6", false);
        folder3.addPlaylistChild(p5);
        folder3.addPlaylistChild(p6);

        playlistsLibrary.addPlaylistToRoot(folder);
        playlistsLibrary.addPlaylist(folder2, folder3);

        assertEquals(rootPlaylist, playlistsLibrary.getParentPlaylist(folder));
        assertTrue(rootPlaylist.getContainedPlaylists().contains(folder));
        assertTrue(folder.getContainedPlaylists().contains(p1));
        assertTrue(folder.getContainedPlaylists().contains(p2));

        assertEquals(folder2, playlistsLibrary.getParentPlaylist(folder3));
        assertTrue(folder2.getContainedPlaylists().contains(p3));
        assertTrue(folder2.getContainedPlaylists().contains(p4));

        playlistsLibrary.movePlaylist(folder3, folder);

        assertTrue(folder.getContainedPlaylists().contains(folder3));
        assertTrue(folder3.getContainedPlaylists().contains(p5));
        assertTrue(folder3.getContainedPlaylists().contains(p6));

        assertEquals(1, playlistsLibrary.getPlaylistsTree().successors(rootPlaylist).size());
        assertEquals(4, playlistsLibrary.getPlaylistsTree().successors(folder).size());
        assertEquals(2, playlistsLibrary.getPlaylistsTree().successors(folder2).size());
        assertEquals(2, playlistsLibrary.getPlaylistsTree().successors(folder3).size());

        assertContainsPlaylists(folder, folder2, folder3, p1, p2, p3, p4, p5, p6);
    }

    private void assertContainsPlaylists(Playlist... playlists) {
        Stream.of(playlists).forEach(playlist -> {
            assertTrue(playlistsLibrary.getPlaylistsTree().nodes().contains(playlist));
            assertTrue(playlistsLibrary.containsPlaylistName(playlist.getName()));
            assertEquals(1, playlistsLibrary.getPlaylistsTree().predecessors(playlist).size());
        });
    }
}