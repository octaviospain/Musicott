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

package com.transgressoft.musicott.view.custom;

import com.google.common.graph.*;
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import javafx.beans.property.*;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.framework.junit5.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith ({MockitoExtension.class, ApplicationExtension.class})
public class PlaylistTreeViewTest {

    @Mock
    PlaylistsLibrary playlistsLibraryMock;
    @Mock
    TracksLibrary tracksLibraryMock;

    PlaylistTreeView treeView;
    MutableGraph<Playlist> testGraph;
    Injector injector = Guice.createInjector();
    Playlist rootPlaylist = new Playlist(tracksLibraryMock, "ROOT", true);
    Playlist folder;
    Playlist p1;
    Playlist p2;
    Playlist folder2;
    Playlist p3;
    Playlist p4;

    @BeforeEach
    void beforeEach() {
        testGraph = GraphBuilder.directed().build();
        testGraph.addNode(rootPlaylist);
        doNothing().when(tracksLibraryMock).showPlaylist(any());
        doNothing().when(playlistsLibraryMock).movePlaylist(any(), any());
        when(playlistsLibraryMock.getPlaylistsTree()).thenReturn(testGraph);
        treeView = new PlaylistTreeView(playlistsLibraryMock, tracksLibraryMock, rootPlaylist, injector);
    }

    @Test
    @DisplayName ("Constructor create playlists from library")
    void constructorCreatePlaylistItems() {
        createTestPlaylistGraph();
        treeView = new PlaylistTreeView(playlistsLibraryMock, tracksLibraryMock, rootPlaylist, injector);

        assertEquals(rootPlaylist, treeView.getRoot().getValue());
        assertEquals(1, treeView.getRoot().getChildren().size());
        assertTrue(treeView.getRoot().getChildren().stream().anyMatch(i -> i.getValue().equals(folder)));

        TreeItem<Playlist> folderItemReal = treeView.getRoot().getChildren().get(0);
        assertEquals(3, folderItemReal.getChildren().size());
        assertTrue(folderItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(p1)));
        assertTrue(folderItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(p2)));
        assertTrue(folderItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(folder2)));

        Optional<TreeItem<Playlist>> folder2Opt = folderItemReal.getChildren().stream()
                                                             .filter(ti -> ti.getValue().equals(folder2))
                                                             .findAny();
        assertTrue(folder2Opt.isPresent());
        TreeItem<Playlist> folder2Real = folder2Opt.get();
        assertEquals(2, folder2Real.getChildren().size());
        assertTrue(folder2Real.getChildren().stream().anyMatch(i -> i.getValue().equals(p3)));
        assertTrue(folder2Real.getChildren().stream().anyMatch(i -> i.getValue().equals(p4)));
    }

    @Test
    @DisplayName ("Add playlists, clearAnSelect index, and select")
    void addClearAndSelect() {
        ReadOnlyObjectProperty<Optional<Playlist>> selectedPlaylistProperty = treeView.selectedPlaylistProperty();
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());

        Playlist p1 = new Playlist(tracksLibraryMock, "p1", false);
        Playlist p2 = new Playlist(tracksLibraryMock, "p2", false);

        treeView.addPlaylistsToFolder(rootPlaylist, Collections.singleton(p1));
        assertEquals(-1, treeView.getSelectionModel().getSelectedIndex());
        assertEquals(Optional.empty(), selectedPlaylistProperty.get());

        treeView.selectPlaylist(p1);
        assertEquals(p1, treeView.getSelectionModel().getSelectedItem().getValue());
        assertEquals(Optional.of(p1), selectedPlaylistProperty.get());

        treeView.addPlaylistsToFolder(rootPlaylist, Collections.singleton(p2));
        assertEquals(0, treeView.getSelectionModel().getSelectedIndex());

        treeView.clearAndSelect(1);
        assertEquals(p2, treeView.getSelectionModel().getSelectedItem().getValue());
        assertEquals(Optional.of(p2), selectedPlaylistProperty.get());
    }

    @Test
    @DisplayName ("Delete playlist")
    void deletePlaylist() {
        createTestPlaylistGraph();
        when(playlistsLibraryMock.getParentPlaylist(folder2)).thenReturn(folder);
        treeView = new PlaylistTreeView(playlistsLibraryMock, tracksLibraryMock, rootPlaylist, injector);

        treeView.deletePlaylist(folder2);

        TreeItem<Playlist> folderItemReal = treeView.getRoot().getChildren().get(0);
        assertEquals(2, folderItemReal.getChildren().size());
        assertTrue(folderItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(p1)));
        assertTrue(folderItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(p2)));
        assertTrue(folderItemReal.getChildren().stream().noneMatch(i -> i.getValue().equals(folder2)));
    }

    @Test
    @DisplayName ("Move playlist")
    void movePlaylist() {
        createTestPlaylistGraph();

        Playlist folder3 = new Playlist(tracksLibraryMock, "folder3", true);
        Playlist p5 = new Playlist(tracksLibraryMock, "5", true);
        Playlist p6 = new Playlist(tracksLibraryMock, "6", true);
        folder3.addPlaylistChild(p5);
        folder3.addPlaylistChild(p6);
        folder2.addPlaylistChild(folder3);
        testGraph.putEdge(folder3, p5);
        testGraph.putEdge(folder3, p6);
        testGraph.putEdge(folder2, folder3);

        when(playlistsLibraryMock.getParentPlaylist(folder3)).thenReturn(folder2);
        treeView = new PlaylistTreeView(playlistsLibraryMock, tracksLibraryMock, rootPlaylist, injector);

        treeView.movePlaylist("folder3", rootPlaylist);

        assertEquals(rootPlaylist, treeView.getRoot().getValue());
        assertEquals(2, treeView.getRoot().getChildren().size());
        assertTrue(treeView.getRoot().getChildren().stream().anyMatch(i -> i.getValue().equals(folder)));
        assertTrue(treeView.getRoot().getChildren().stream().anyMatch(i -> i.getValue().equals(folder3)));

        Optional<TreeItem<Playlist>> folder3ItemOpt = treeView.getRoot().getChildren()
                                                     .stream()
                                                     .filter(i -> i.getValue().equals(folder3))
                                                     .findAny();
        assertTrue(folder3ItemOpt.isPresent());
        TreeItem<Playlist> folder3ItemReal = folder3ItemOpt.get();
        assertEquals(2, folder3ItemReal.getChildren().size());
        assertTrue(folder3ItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(p5)));
        assertTrue(folder3ItemReal.getChildren().stream().anyMatch(i -> i.getValue().equals(p6)));
    }


    @Test
    @DisplayName ("Set context menu")
    void setContextMenu() {
        PlaylistTreeViewContextMenu contextMenu = new PlaylistTreeViewContextMenu(null, null);
        treeView.setPlaylistTreeViewContextMenu(contextMenu);

        assertEquals(contextMenu, treeView.getContextMenu());
    }

    private void createTestPlaylistGraph() {
        folder = new Playlist(tracksLibraryMock, "folder", true);
        p1 = new Playlist(tracksLibraryMock, "1", false);
        p2 = new Playlist(tracksLibraryMock, "2", false);
        folder2 = new Playlist(tracksLibraryMock, "folder2", true);
        p3 = new Playlist(tracksLibraryMock, "3", true);
        p4 = new Playlist(tracksLibraryMock, "4", true);

        folder.addPlaylistChild(p1);
        folder.addPlaylistChild(p2);
        folder2.addPlaylistChild(p3);
        folder2.addPlaylistChild(p4);
        folder.addPlaylistChild(folder2);

        testGraph.putEdge(rootPlaylist, folder);
        testGraph.putEdge(folder, p1);
        testGraph.putEdge(folder, p2);
        testGraph.putEdge(folder, folder2);
        testGraph.putEdge(folder2, p3);
        testGraph.putEdge(folder2, p4);
    }
}