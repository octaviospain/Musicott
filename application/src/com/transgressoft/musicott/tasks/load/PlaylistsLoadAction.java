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

package com.transgressoft.musicott.tasks.load;

import com.cedarsoftware.util.io.*;
import com.google.common.graph.*;
import com.google.inject.*;
import com.google.inject.assistedinject.*;
import com.sun.javafx.collections.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.jsoniocreators.*;
import javafx.application.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.model.CommonObject.*;

/**
 * This class extends from {@link BaseLoadAction} in order to perform the loading
 * of the {@link Graph} of playlists of the application's music library stored on a {@code json} file.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.9.2-b
 */
public class PlaylistsLoadAction extends BaseLoadAction {

    private final transient Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final transient PlaylistsLibrary playlistsLibrary;
    private final transient Playlist rootPlaylist;

    private transient MutableGraph<Playlist> playlists;
    private int step = 0;
    private int totalPlaylists;

    private transient PlaylistFactory playlistFactory;

    @Inject
    public PlaylistsLoadAction(PlaylistsLibrary playlistsLibrary, PlaylistFactory playlistFactory,
            @RootPlaylist Playlist rootPlaylist, @Assisted String applicationFolder,
            @Assisted Application application) {
        super(applicationFolder, application);
        this.playlistsLibrary = playlistsLibrary;
        this.playlistFactory = playlistFactory;
        this.rootPlaylist = rootPlaylist;
    }

    @Override
    protected void compute() {
        notifyPreloader(- 1, 0, "Loading playlists...");
        File playlistsFile = new File(applicationFolder, PLAYLISTS_FILE.toString());
        if (playlistsFile.exists()) {
            parsePlaylistFromJsonFile(playlistsFile);
            playlists.nodes().forEach(this::setPlaylistProperties);
        }
        else
            createDefaultPlaylists();
        playlistsLibrary.addPlaylistsRecursively(rootPlaylist, playlists.successors(rootPlaylist));
    }

    /**
     * Loads the playlists from a saved file formatted in JSON
     *
     * @param playlistsFile The JSON formatted file of the playlists
     *
     * @return a {@link List} of {@link Playlist} objects
     */
    @SuppressWarnings ("unchecked")
    private void parsePlaylistFromJsonFile(File playlistsFile) {
        try {
            JsonReader.assignInstantiator(Playlist.class, new PlaylistCreator());
            JsonReader.assignInstantiator(ElementOrder.class, new ElementOrderCreator());
            JsonReader.assignInstantiator(ObservableListWrapper.class, new ObservableListWrapperCreator());
            JsonReader.assignInstantiator(ConfigurableMutableGraph.class, new ConfigurableMutableGraphCreator());
            JsonReader.assignInstantiator(ConfigurableMutableValueGraph.class, new ConfigurableMutableValueGraphCreator());
            playlists = (MutableGraph<Playlist>) parseJsonFile(playlistsFile);
            totalPlaylists = playlists.nodes().size();
            LOG.info("Loaded playlists from {}", playlistsFile);
        }
        catch (IOException exception) {
            createDefaultPlaylists();
            LOG.error("Error loading playlists: {}", exception.getMessage(), exception);
        }
    }

    private void createDefaultPlaylists() {
        Playlist top10 = playlistFactory.create("My Top 10", false);
        Playlist favs = playlistFactory.create("Favourites", false);
        Playlist listenLater = playlistFactory.create("Listen later", false);
        rootPlaylist.addPlaylistChild(top10);
        rootPlaylist.addPlaylistChild(favs);
        rootPlaylist.addPlaylistChild(listenLater);

        playlists = GraphBuilder.directed().build();
        playlists.putEdge(rootPlaylist, top10);
        playlists.putEdge(rootPlaylist, favs);
        playlists.putEdge(rootPlaylist, listenLater);
    }

    /**
     * Sets the values of the properties of a {@link Playlist} object,
     * because those are not restored on the {@code json} file when deserialized
     *
     * @param playlist The track to set its properties values
     */
    private void setPlaylistProperties(Playlist playlist) {
        playlist.nameProperty().setValue(playlist.getName());
        playlist.isFolderProperty().setValue(playlist.isFolder());
        notifyPreloader(++ step, totalPlaylists, "Loading playlists...");
    }
}
