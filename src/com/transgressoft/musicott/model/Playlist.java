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

import com.transgressoft.musicott.tasks.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.image.*;
import org.fxmisc.easybind.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * Represents a Playlist of tracks. A {@code Playlist} can contain another playlists,
 * if so, an instance is a folder. One cover image of the contained tracks
 * is used when showing a {@link Playlist} on the application.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class Playlist {

    private static final String ADDITION_NOT_SUPPORTED = "Addition not supported on folder playlist";
    private static final String DELETION_NOT_SUPPORTED = "Deletion not supported on folder playlist";

    private final Image DEFAULT_COVER = new Image(getClass().getResourceAsStream(DEFAULT_COVER_PATH));
    private final boolean isFolder;
    private String name;
    private ObservableList<Integer> playlistTrackIds;
    private List<Playlist> containedPlaylists;
    private StringProperty nameProperty;
    private ObjectProperty<Image> playlistCoverProperty;
    private BooleanProperty isFolderProperty;

    private MusicLibrary musicLibrary = MusicLibrary.getInstance();
    private TaskDemon taskDemon = TaskDemon.getInstance();

    public Playlist(String name, boolean isFolder) {
        this.name = name;
        this.isFolder = isFolder;
        playlistTrackIds = FXCollections.observableArrayList();
        containedPlaylists = new ArrayList<>();
        nameProperty = new SimpleStringProperty(this, "name", name);
        EasyBind.subscribe(nameProperty, this::setName);
        playlistCoverProperty = new SimpleObjectProperty<>(this, "cover", DEFAULT_COVER);
        isFolderProperty = new SimpleBooleanProperty(this, "folder", isFolder);
    }

    public void setName(String name) {
        this.name = name;
        nameProperty.setValue(this.name);
    }

    public String getName() {
        return name;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public ObjectProperty<Image> playlistCoverProperty() {
        ObjectProperty<Image> returnedCoverProperty = playlistCoverProperty;
        if (isFolder) {
            Optional<Playlist> childPlaylistNotEmpty = containedPlaylists.stream()
                                                                         .filter(playlist -> ! playlist.getTracks()
                                                                                                       .isEmpty())
                                                                         .findAny();
            if (childPlaylistNotEmpty.isPresent())
                returnedCoverProperty = childPlaylistNotEmpty.get().playlistCoverProperty();
            else
                returnedCoverProperty.set(DEFAULT_COVER);
        }
        else if (playlistCoverProperty.get().equals(DEFAULT_COVER) && ! getTracks().isEmpty())
            changePlaylistCover();
        return returnedCoverProperty;
    }

    public BooleanProperty isFolderProperty() {
        return isFolderProperty;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public boolean isEmpty() {
        return getTracks().isEmpty();
    }

    public boolean addTracks(List<Integer> tracksIds) {
        boolean result = false;
        if (! isFolder) {

            result = playlistTrackIds.addAll(tracksIds);
            if (result) {
                changePlaylistCover();
                taskDemon.saveLibrary(false, false, true);
            }
        }
        return result;
    }

    public boolean removeTracks(List<Integer> tracksIds) {
        if (isFolder)
            throw new UnsupportedOperationException(DELETION_NOT_SUPPORTED);

        boolean result = playlistTrackIds.removeAll(tracksIds);
        if (result) {
            changePlaylistCover();
            taskDemon.saveLibrary(false, false, true);
        }
        return result;
    }

    public void clearTracks() {
        if (isFolder)
            throw new UnsupportedOperationException(DELETION_NOT_SUPPORTED);
        playlistTrackIds.clear();
        playlistCoverProperty.set(DEFAULT_COVER);
        taskDemon.saveLibrary(false, false, true);
    }

    public List<Playlist> getContainedPlaylists() {
        return containedPlaylists;
    }

    public List<Integer> getTracks() {
        List<Integer> allTracksWithin;
        if (isFolder) {
            allTracksWithin = new ArrayList<>();
            containedPlaylists.forEach(playlist -> allTracksWithin.addAll(playlist.getTracks()));
        }
        else
            allTracksWithin = playlistTrackIds;
        return allTracksWithin;
    }

    /**
     * Search in the contained tracks for one that has cover and sets it as the cove of the playlist.
     * If there is no track or any of them has cover, the default cover is used.
     */
    private void changePlaylistCover() {
        List<Integer> tracks = getTracks();
        if (! tracks.isEmpty()) {
            Optional<Integer> trackWithCover = tracks.stream()
                                                     .filter(trackId -> musicLibrary.getTrack(trackId).isPresent())
                                                     .filter(trackId -> musicLibrary.getTrack(trackId).get()
                                                                                    .getCoverImage().isPresent())
                                                     .findAny();

            if (trackWithCover.isPresent()) {
                int trackId = trackWithCover.get();
                musicLibrary.getTrack(trackId).ifPresent(track -> {
                    byte[] coverBytes = track.getCoverImage().get();
                    Image image = new Image(new ByteArrayInputStream(coverBytes));
                    playlistCoverProperty.set(image);
                });
            }
            else
                playlistCoverProperty.set(DEFAULT_COVER);
        }
        else
            playlistCoverProperty.set(DEFAULT_COVER);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if ((o instanceof Playlist) && ((Playlist) o).getName().equals(this.name)) {
            equals = true;
        }
        return equals;
    }

    @Override
    public String toString() {
        return name + "[" + playlistTrackIds.size() + "]";
    }
}
