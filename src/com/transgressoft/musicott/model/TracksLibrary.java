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
import com.transgressoft.musicott.util.guice.annotations.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;

import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import static com.transgressoft.musicott.model.AlbumsLibrary.*;

/**
 * Class that isolates the operations over the map of {@link Track}s
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
@Singleton
public class TracksLibrary {

    private static final int DEFAULT_RANDOM_QUEUE_SIZE = 8;

    private final ObservableMap<Integer, Track> musicottTracks = FXCollections.observableHashMap();
    private final ListProperty<Entry<Integer, Track>> trackEntriesListProperty;
    private final ListProperty<Entry<Integer, Track>> showingTracksProperty;
    private final MapChangeListener<Integer, Track> tracksMapChangeListener = tracksMapChangeListener();

    private ArtistsLibrary artistsLibrary;
    private AlbumsLibrary albumsLibrary;
    private ReadOnlyObjectProperty<NavigationMode> navigationModeProperty;

    @Inject
    public TracksLibrary() {
        Set<Entry<Integer, Track>> trackEntries = musicottTracks.entrySet();
        // Binding of the entries of the Musicott tracks to a
        // ListProperty of all its elements
        ObservableList<Entry<Integer, Track>> trackEntriesList = FXCollections.observableArrayList(trackEntries);
        trackEntriesListProperty = new SimpleListProperty<>(this, "all tracks");
        trackEntriesListProperty.bind(new SimpleObjectProperty<>(trackEntriesList));

        // Binding of the entries of the Musicott tracks to a
        // ListProperty of the elements that are shown in the table
        ObservableList<Entry<Integer, Track>> showingTracksList = FXCollections.observableArrayList(trackEntries);
        showingTracksProperty = new SimpleListProperty<>(this, "showing tracks");
        showingTracksProperty.bind(new SimpleObjectProperty<>(showingTracksList));
        addListener(tracksMapChangeListener);
    }

    private MapChangeListener<Integer, Track> tracksMapChangeListener() {
        return change -> {
            if (change.wasAdded())
                addTrackToCollections(change.getValueAdded());
            else if (change.wasRemoved())
                removeTrackFromCollections(change.getValueRemoved());
        };
    }

    private void addTrackToCollections(Track track) {
        Entry<Integer, Track> trackEntry = new SimpleEntry<>(track.getTrackId(), track);
        Platform.runLater(() -> trackEntriesListProperty.add(trackEntry));
        track.getArtistsInvolved().forEach(artist -> artistsLibrary.addArtistTrack(artist, track));
        String trackAlbum = track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum();
        albumsLibrary.addTracks(trackAlbum, Collections.singletonList(trackEntry));
        if (navigationModeProperty != null && navigationModeProperty.get() == NavigationMode.ALL_TRACKS)
            Platform.runLater(() -> showingTracksProperty.add(trackEntry));
    }

    private void removeTrackFromCollections(Track track) {
        Entry<Integer, Track> trackEntry = new SimpleEntry<>(track.getTrackId(), track);
        Platform.runLater(() -> trackEntriesListProperty.remove(trackEntry));
        Platform.runLater(() -> showingTracksProperty.remove(trackEntry));
        track.getArtistsInvolved().forEach(artist -> artistsLibrary.removeArtistTrack(artist, track));
        String trackAlbum = track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum();
        albumsLibrary.removeTracks(trackAlbum, Collections.singletonList(trackEntry));
    }

    public void addListener(MapChangeListener<Integer, Track> listener) {
        musicottTracks.addListener(listener);
    }

    public synchronized void add(Map<Integer, Track> tracksMap) {
        musicottTracks.putAll(tracksMap);
    }

    synchronized void remove(Collection<Track> tracks) {
        musicottTracks.values().removeAll(tracks);
    }

    public synchronized Optional<Track> getTrack(int trackId) {
        return Optional.ofNullable(musicottTracks.get(trackId));
    }

    public synchronized List<Track> getTracks(Collection<Integer> trackIds) {
        return trackIds.stream().map(musicottTracks::get).collect(Collectors.toList());
    }

    public synchronized boolean containsTrackPath(String parent, String fileName) {
        return musicottTracks.values().stream().anyMatch(
                                     track -> track.getFileFolder().equalsIgnoreCase(parent) &&
                                     track.getFileName().equalsIgnoreCase(fileName));
    }

    synchronized int getSize() {
        return musicottTracks.size();
    }

    synchronized void clear() {
        musicottTracks.addListener(tracksMapChangeListener);
        musicottTracks.clear();
        trackEntriesListProperty.clear();
        showingTracksProperty.clear();
        musicottTracks.removeListener(tracksMapChangeListener);
    }

    public synchronized void showAllTracks() {
        showingTracksProperty.clear();
        showingTracksProperty.addAll(musicottTracks.entrySet());
    }

    public List<Track> getRandomTracks() {
        List<Track> randomList = new ArrayList<>();
        ImmutableList<Integer> trackIds;
        synchronized (this) {
            trackIds = ImmutableList.copyOf(musicottTracks.keySet());
        }
        Random randomGenerator = new Random();
        do {
            int rnd = randomGenerator.nextInt(trackIds.size());
            int randomTrackId = trackIds.get(rnd);
            Track randomTrack;
            synchronized (this) {
                randomTrack = musicottTracks.get(randomTrackId);
            }
            if (randomTrack.isInDisk())
                randomList.add(randomTrack);
        } while (randomList.size() < DEFAULT_RANDOM_QUEUE_SIZE);
        return randomList;
    }

    @Inject
    public void setArtistsLibrary(ArtistsLibrary artistsLibrary) {
        this.artistsLibrary = artistsLibrary;
    }

    @Inject
    public void setAlbumsLibrary(AlbumsLibrary albumsLibrary) {
        this.albumsLibrary = albumsLibrary;
    }

    @Inject (optional = true)
    public void setNavigationModeProperty(@SelectedMenuProperty ObjectProperty<NavigationMode> navigationModeProperty) {
        this.navigationModeProperty = navigationModeProperty;
    }

    public ObservableMap<Integer, Track> getMusicottTracks() {
        return musicottTracks;
    }

    public ReadOnlyBooleanProperty emptyTracksLibraryProperty() {
        return trackEntriesListProperty.emptyProperty();
    }

    public ListProperty<Entry<Integer, Track>> showingTrackEntriesProperty() {
        return showingTracksProperty;
    }

    /**
     * Determines if a track matches a given string by its name, artist, label, genre or album.
     *
     * @param track  The {@link Track} to match
     * @param string The string to match against the {@code Track}
     *
     * @return {@code true} if the {@code Track matches}, {@code false} otherwise
     */
    public static boolean trackMatchesString(Track track, String string) {
        boolean matchesName = track.getName().toLowerCase().contains(string.toLowerCase());
        boolean matchesArtist = track.getArtist().toLowerCase().contains(string.toLowerCase());
        boolean matchesLabel = track.getLabel().toLowerCase().contains(string.toLowerCase());
        boolean matchesGenre = track.getGenre().toLowerCase().contains(string.toLowerCase());
        boolean matchesAlbum = track.getAlbum().toLowerCase().contains(string.toLowerCase());
        return matchesName || matchesArtist || matchesLabel || matchesGenre || matchesAlbum;
    }
}