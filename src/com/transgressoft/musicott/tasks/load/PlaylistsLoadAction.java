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
import com.sun.javafx.collections.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.application.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * This class extends from {@link BaseLoadAction} in order to perform the loading
 * of the {@link List} of playlists of the application's music library stored on a {@code json} file.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class PlaylistsLoadAction extends BaseLoadAction {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private int step = 0;
    private int totalPlaylists;

    public PlaylistsLoadAction(String applicationFolder, MusicLibrary musicLibrary, Application musicottApplication) {
        super(applicationFolder, musicLibrary, musicottApplication);
    }

    @Override
    protected void compute() {
        notifyPreloader(-1, 0, "Loading playlists...");
        String playlistsPath = applicationFolder + File.separator + PLAYLISTS_PERSISTENCE_FILE;
        File playlistsFile = new File(playlistsPath);
        List<Playlist> playlists;
        if (playlistsFile.exists()) {
            playlists = parsePlaylistFromJsonFile(playlistsFile);
            playlists.forEach(playlist -> {
                if (playlist.isFolder())
                    playlist.getContainedPlaylists().forEach(this::setPlaylistProperties);
                setPlaylistProperties(playlist);
            });
        }
        else {
            playlists = new ArrayList<>();
            playlists.add(new Playlist("My Top 10", false));
            playlists.add(new Playlist("Favourites", false));
            playlists.add(new Playlist("Listen later", false));
        }
        musicLibrary.addPlaylists(playlists);
    }

    /**
     * Loads the playlists from a saved file formatted in JSON
     *
     * @param playlistsFile The JSON formatted file of the playlists
     *
     * @return a {@link List} of {@link Playlist} objects
     */
    @SuppressWarnings ("unchecked")
    private List<Playlist> parsePlaylistFromJsonFile(File playlistsFile) {
        List<Playlist> playlists;
        try {
            JsonReader.assignInstantiator(ObservableListWrapper.class, new ObservableListWrapperCreator());
            playlists = (List<Playlist>) parseJsonFile(playlistsFile);
            totalPlaylists = playlists.size();
            LOG.info("Loaded playlists from {}", playlistsFile);
        }
        catch (IOException exception) {
            playlists = new ArrayList<>();
            LOG.error("Error loading playlists: {}", exception.getMessage(), exception);
        }
        return playlists;
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