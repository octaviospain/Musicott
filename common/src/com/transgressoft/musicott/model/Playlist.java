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
import com.google.inject.*;
import com.google.inject.assistedinject.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.image.*;
import org.fxmisc.easybind.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.model.CommonObject.*;

/**
 * Represents a Playlist of tracks. A {@code Playlist} can contain another playlists,
 * if so, an instance is a folder. One cover image of the contained tracks
 * is used when showing a {@link Playlist} on the application.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
public class Playlist {


    private static final String DELETION_NOT_SUPPORTED = "Deletion not supported on folder playlist";
    private static final String ADDITION_NOT_SUPPORTED = "Adding a playlist child is only supported on folder playlist";

    private final Image defaultCoverImage = new Image(getClass().getResourceAsStream(DEFAULT_COVER.toString()));
    private final TracksLibrary tracksLibrary;

    private boolean isFolder;
    private String name;
    private ObservableList<Integer> playlistTrackIds = FXCollections.observableArrayList();
    private Set<Playlist> containedPlaylists = new HashSet<>();
    private StringProperty nameProperty;
    private ObjectProperty<Image> playlistCoverProperty;
    private BooleanProperty isFolderProperty;

    {
        nameProperty = new SimpleStringProperty(this, "name", "");
        EasyBind.subscribe(nameProperty, this::setName);
        playlistCoverProperty = new SimpleObjectProperty<>(this, "cover", defaultCoverImage);
        isFolderProperty = new SimpleBooleanProperty(this, "folder", isFolder);
    }

    public Playlist(TracksLibrary tracksLibrary) {
        this.tracksLibrary = tracksLibrary;
        name = "";
    }

    @Inject
    public Playlist(TracksLibrary tracksLibrary, @Assisted String name, @Assisted boolean isFolder) {
        this.tracksLibrary = tracksLibrary;
        this.isFolder = isFolder;
        setName(name);
    }

    public void setTracksListener(ListChangeListener<Integer> listener) {
        playlistTrackIds.addListener(listener);
    }

    public void removeTracksListener(ListChangeListener<Integer> listener) {
        playlistTrackIds.removeListener(listener);
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
        if (playlistCoverProperty.get().equals(defaultCoverImage) && ! getTracks().isEmpty())
            changePlaylistCover();
        return playlistCoverProperty;
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
            if (result)
                changePlaylistCover();
        }
        return result;
    }

    public boolean removeTracks(List<Integer> tracksIds) {
        if (isFolder)
            throw new UnsupportedOperationException(DELETION_NOT_SUPPORTED);

        boolean result = playlistTrackIds.removeAll(tracksIds);
        if (result)
            changePlaylistCover();
        return result;
    }

    public void clearTracks() {
        if (isFolder)
            throw new UnsupportedOperationException(DELETION_NOT_SUPPORTED);
        playlistTrackIds.clear();
        playlistCoverProperty.set(defaultCoverImage);
    }

    public void addPlaylistChild(Playlist childPlaylist) {
        if (! isFolder)
            throw new UnsupportedOperationException(ADDITION_NOT_SUPPORTED);
        containedPlaylists.add(childPlaylist);
    }

    public void removePlaylistChild(Playlist childPlaylist) {
        if (! isFolder)
            throw new UnsupportedOperationException(DELETION_NOT_SUPPORTED);
        containedPlaylists.remove(childPlaylist);
    }

    public Set<Playlist> getContainedPlaylists() {
        return ImmutableSet.copyOf(containedPlaylists);
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
     * Search in the contained tracks for one that has cover and sets it as the cover of the playlist.
     * If there is no track or any of them has cover, the default cover is used.
     */
    private void changePlaylistCover() {
        List<Integer> tracks = getTracks();
        if (! tracks.isEmpty()) {
            Optional<Integer> trackWithCover = tracks.stream().filter(this::existsTrackWithCover).findAny();
            if (trackWithCover.isPresent()) {
                int trackId = trackWithCover.get();
                tracksLibrary.getTrack(trackId).ifPresent(track -> {
                    byte[] coverBytes = track.getCoverImage().get();
                    Image image = new Image(new ByteArrayInputStream(coverBytes));
                    playlistCoverProperty.set(image);
                });
            }
            else
                playlistCoverProperty.set(defaultCoverImage);
        }
        else
            playlistCoverProperty.set(defaultCoverImage);
    }

    private boolean existsTrackWithCover(int trackId) {
        Optional<Track> track = tracksLibrary.getTrack(trackId);
        return track.isPresent() && track.get().getCoverImage().isPresent();
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
        String folderString = isFolder ? "[FOLDER]" : "";
        int containedItems = isFolder ? containedPlaylists.size() : playlistTrackIds.size();
        return name + folderString + "[" + containedItems + "]";
    }
}
