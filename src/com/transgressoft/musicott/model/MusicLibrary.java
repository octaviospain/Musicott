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
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.tasks.load.*;
import com.transgressoft.musicott.view.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import org.slf4j.*;

import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Singleton class that isolates the access to the tracks, waveforms,
 * and playlists of the {@code Musicott} music library.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class MusicLibrary {

    private static final int DEFAULT_RANDOM_QUEUE_SIZE = 8;
    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final ObservableMap<Integer, Track> musicottTracks = FXCollections.observableHashMap();
    private final Map<Integer, float[]> waveforms = new HashMap<>();
    private final List<Playlist> playlists = new ArrayList<>();
    private final ObservableSet<String> artists;

    private final Multimap<String, Entry<Integer, Track>> albumsTracks;
    private final Multimap<String, Integer> artistsTracks;

    private ObservableList<Entry<Integer, Track>> showingTracks;
    private ListProperty<Entry<Integer, Track>> showingTracksProperty;

    private ObservableList<Entry<Integer, Track>> musicottTrackEntriesList;
    private ListProperty<Entry<Integer, Track>> musicottTrackEntriesListProperty;

    private ObservableList<String> artistsList;
    private ListProperty<String> artistsListProperty;

    private TaskDemon taskDemon = TaskDemon.getInstance();
    private StageDemon stageDemon = StageDemon.getInstance();

    private static class InstanceHolder {
        static final MusicLibrary INSTANCE = new MusicLibrary();
        private InstanceHolder() {}
    }

    public static MusicLibrary getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private MusicLibrary() {
        albumsTracks = Multimaps.synchronizedMultimap(HashMultimap.create());
        artistsTracks = Multimaps.synchronizedMultimap(HashMultimap.create());
        musicottTracks.addListener(musicottTracksChangeListener());
        bindTrackEntriesList();
        bindShowingTracks();
        artists = FXCollections.observableSet(artistsTracks.keySet());
        bindArtistsList();
        taskDemon.setMusicCollections(musicottTracks, waveforms, playlists);
    }

    /**
     * Listener that adds or removes the tracks and the related information in the
     * collections if necessary when the Musicott tracks map changes.
     *
     * @return The {@link MapChangeListener} reference
     */
    private MapChangeListener<Integer, Track> musicottTracksChangeListener() {
        return change -> {
            if (change.wasAdded())
                addTrackToCollections(change.getValueAdded());
            else if (change.wasRemoved())
                removeTrackFromCollections(change.getValueRemoved());
            taskDemon.saveLibrary(true, true, true);
        };
    }

    /**
     * Binds the entries of the Musicott tracks to a {@link ListProperty} of all its elements
     */
    private void bindTrackEntriesList() {
        musicottTrackEntriesList = FXCollections.observableArrayList(musicottTracks.entrySet());
        musicottTrackEntriesListProperty = new SimpleListProperty<>(this, "all tracks");
        musicottTrackEntriesListProperty.bind(new SimpleObjectProperty<>(musicottTrackEntriesList));
    }

    /**
     * Binds the entries of the Musicott tracks to a {@link ListProperty} of the elements
     * that are shown in the table
     */
    private void bindShowingTracks() {
        showingTracks = FXCollections.observableArrayList(musicottTracks.entrySet());
        showingTracksProperty = new SimpleListProperty<>(this, "showing tracks");
        showingTracksProperty.bind(new SimpleObjectProperty<>(showingTracks));
    }

    /**
     * Binds the set of artists to a {@link ListProperty} of their elements to be
     * shown on the artists' navigation mode list view.
     */
    private void bindArtistsList() {
        artistsList = FXCollections.observableArrayList(artists);
        artistsList = FXCollections.synchronizedObservableList(artistsList);
        artistsListProperty = new SimpleListProperty<>(this, "artists set");
        artistsListProperty.bind(new SimpleObjectProperty<>(artistsList));
    }

    private boolean addTrackToCollections(Track addedTrack) {
        Integer trackId = addedTrack.getTrackId();
        Entry<Integer, Track> trackEntry = new SimpleEntry<>(trackId, addedTrack);
        boolean[] added = new boolean[]{addTrackEntryToCollections(trackEntry)};
        addedTrack.getArtistsInvolved().forEach(artist -> added[0] |= addArtistTrack(artist, trackId));
        return added[0];
    }

    private boolean addTrackEntryToCollections(Entry<Integer, Track> trackEntry) {
        Track addedTrack = trackEntry.getValue();
        String trackAlbum = addedTrack.getAlbum().isEmpty() ? "Unknown album" : addedTrack.getAlbum();
        boolean[] added = new boolean[]{false};
        added[0] = musicottTrackEntriesList.add(trackEntry);
        added[0] |= albumsTracks.put(trackAlbum, trackEntry);
        addedTrack.getArtistsInvolved().forEach(artist -> added[0] |= addArtistTrack(artist, addedTrack.getTrackId()));
        LOG.debug("Added {}", addedTrack);

        NavigationController navigationController = stageDemon.getNavigationController();
        NavigationMode mode = navigationController == null ? null :
                navigationController.navigationModeProperty().getValue();
        if (mode != null && mode == NavigationMode.ALL_TRACKS)
            showingTracks.add(trackEntry);
        return added[0];
    }

    private boolean removeTrackFromCollections(Track removedTrack) {
        Integer trackId = removedTrack.getTrackId();
        Entry<Integer, Track> trackEntry = new SimpleEntry<>(trackId, removedTrack);
        boolean[] removed = new boolean[]{removeTrackEntryFromCollections(trackEntry)};
        removedTrack.getArtistsInvolved().forEach(artist -> removed[0] |= removeArtistTrack(artist, trackId));

        synchronized (waveforms) {
            waveforms.keySet().remove(trackId);
        }
        synchronized (playlists) {
            playlists.stream().filter(playlist -> ! playlist.isFolder())
                     .forEach(playlist -> playlist.removeTracks(Collections.singletonList(trackId)));
        }
        return removed[0];
    }

    private boolean removeTrackEntryFromCollections(Entry<Integer, Track> trackEntry) {
        stageDemon.getRootController().removeFromTrackSets(trackEntry);
        Track removedTrack = trackEntry.getValue();
        String trackAlbum = removedTrack.getAlbum().isEmpty() ? "Unknown album" : removedTrack.getAlbum();
        boolean[] removed = new boolean[]{false};
        removed[0] = musicottTrackEntriesList.remove(trackEntry);
        removed[0] = showingTracks.remove(trackEntry);
        removed[0] |= albumsTracks.remove(trackAlbum, trackEntry);
        LOG.debug("Removed {}", removedTrack);
        return removed[0];
    }

    /**
     * Link a track to an artist and adds it to the list of
     * artists if it isn't in it yet.
     *
     * @param artist  The artist name
     * @param trackId The id of the track
     *
     * @return {@code True} if the collections were modified {@code False} otherwise
     */
    private boolean addArtistTrack(String artist, int trackId) {
        if (! artistsTracks.containsKey(artist)) {
            Platform.runLater(() -> {
                artistsList.add(artist);
                FXCollections.sort(artistsList);
            });
        }
        return artistsTracks.put(artist, trackId);
    }

    /**
     * Unlink a track of an artist and removes it of the list of
     * artists if it isn't related to any tracks.
     *
     * @param artist  The artist name
     * @param trackId The id of the track
     *
     * @return {@code True} if the collections were modified {@code False} otherwise
     */
    private boolean removeArtistTrack(String artist, int trackId) {
        boolean removed = artistsTracks.remove(artist, trackId);
        if (! artistsTracks.containsKey(artist))
            Platform.runLater(() -> artistsList.remove(artist));
        return removed;
    }

    private boolean addAlbumTracks(String album, List<Entry<Integer, Track>> tracks) {
//        if (! albumsLists.contains(album)) {
//            // TODO update the observable albumLists when implemented album view
//        }
        return albumsTracks.putAll(album, tracks);
    }

    private boolean removeAlbumTracks(String album, List<Entry<Integer, Track>> tracks) {
        boolean removed = albumsTracks.get(album).removeAll(tracks);
        if (! albumsTracks.containsKey(album)) {
            // TODO update observable albumList when implemented album view
        }
        return removed;
    }

    public void updateArtistsInvolvedInTrack(int trackId, Set<String> removedArtists, Set<String> addedArtists) {
        removedArtists.forEach(artist -> removeArtistTrack(artist, trackId));
        addedArtists.forEach(artist -> addArtistTrack(artist, trackId));
    }

    public void updateTrackAlbums(List<Entry<Integer, Track>> modifiedTracks, Set<String> oldAlbums, String newAlbum) {
        oldAlbums.forEach(album -> removeAlbumTracks(album, modifiedTracks));
        addAlbumTracks(newAlbum, modifiedTracks);
    }

    public void addTracks(Map<Integer, Track> tracks) {
        tracks.entrySet().forEach(trackEntry -> {
            synchronized (musicottTracks) {
                musicottTracks.put(trackEntry.getKey(), trackEntry.getValue());
            }
        });
    }

    public void addToShowingTracks(List<Entry<Integer, Track>> trackEntries) {
        showingTracks.addAll(trackEntries);
    }

    public void removeFromShowingTracks(List<Entry<Integer, Track>> trackEntries) {
        showingTracks.removeAll(trackEntries);
    }

    public Optional<Track> getTrack(int trackId) {
        synchronized (musicottTracks) {
            return Optional.ofNullable(musicottTracks.get(trackId));
        }
    }

    public void addWaveform(int trackId, float[] waveform) {
        synchronized (waveforms) {
            waveforms.put(trackId, waveform);
        }
        taskDemon.saveLibrary(false, true, false);
    }

    /**
     * Adds a collection of waveforms to the music library's collection of waveforms.
     * This method is only called from a {@link WaveformsLoadAction} when
     * the user's waveforms are loaded, and that's why {@link TaskDemon#saveLibrary} is not called.
     *
     * @param newWaveforms The {@code Map} of waveforms to be added
     */
    public void addWaveforms(Map<Integer, float[]> newWaveforms) {
        synchronized (waveforms) {
            waveforms.putAll(newWaveforms);
        }
    }

    public void addPlaylist(Playlist playlist) {
        synchronized (playlists) {
            playlists.add(playlist);
        }
        taskDemon.saveLibrary(false, false, true);
    }

    /**
     * Adds {@link Playlist}s to the music library's collection playlists.
     * This method is only called from a {@link PlaylistsLoadAction} when
     * the user's playlists are loaded, and that's why {@link TaskDemon#saveLibrary} is not called.
     *
     * @param newPlaylists The playlists to be added
     */
    public void addPlaylists(List<Playlist> newPlaylists) {
        synchronized (playlists) {
            playlists.addAll(newPlaylists);
        }
    }

    public float[] getWaveform(int trackId) {
        return waveforms.get(trackId);
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    /**
     * Delete tracks from the music library looking also for occurrences in
     * the playlists and the waveforms collections.
     * This method is called on an independent Thread in {@link StageDemon#deleteTracks}
     *
     * @param trackIds A {@code List} of track ids
     */
    public void deleteTracks(List<Integer> trackIds) {
        Platform.runLater(() -> {
            synchronized (musicottTracks) {
                musicottTracks.keySet().removeAll(trackIds);
            }
            taskDemon.saveLibrary(true, true, true);
        });
        String message = "Deleted " + Integer.toString(trackIds.size()) + " tracks";
        Platform.runLater(() -> stageDemon.getNavigationController().setStatusMessage(message));
    }

    public void deletePlaylist(Playlist playlist) {
        synchronized (playlists) {
            boolean removed = playlists.remove(playlist);
            if (! removed) {
                List<Playlist> folders = playlists.stream().filter(Playlist::isFolder).collect(Collectors.toList());
                deletePlaylistFromFolders(playlist, folders);
            }
        }
        taskDemon.saveLibrary(false, false, true);
    }

    private void deletePlaylistFromFolders(Playlist playlist, List<Playlist> folders) {
        for (Playlist folder : folders) {
            ListIterator<Playlist> folderChildsIterator = folder.getContainedPlaylists().listIterator();
            while (folderChildsIterator.hasNext())
                if (folderChildsIterator.next().equals(playlist)) {
                    folderChildsIterator.remove();
                    break;
                }
        }
    }

    public boolean containsTrack(Track track) {
        synchronized (musicottTracks) {
            return musicottTracks.containsValue(track);
        }
    }

    public boolean containsWaveform(int trackId) {
        synchronized (waveforms) {
            return waveforms.containsKey(trackId);
        }
    }

    public boolean containsPlaylist(Playlist playlist) {
        synchronized (playlists) {
            return playlists.contains(playlist);
        }
    }

    public void clearShowingTracks() {
        showingTracks.clear();
    }

    public void showAllTracks() {
        showingTracks.clear();
        showingTracks.addAll(musicottTracks.entrySet());
    }

    public void showPlaylist(Playlist playlist) {
        showingTracks.clear();
        playlist.getTracks().stream().filter(id -> getTrack(id).isPresent())
                .map(id -> new SimpleEntry<>(id, getTrack(id).get()))
                .forEach(trackEntry -> addToShowingTracks(Collections.singletonList(trackEntry)));
    }

    public void showArtist(String artist) {
        new Thread(() -> {
            Multimap<String, Entry<Integer, Track>> newTrackSetsByAlbum = getAlbumTracksOfArtist(artist);
            Platform.runLater(() -> stageDemon.getRootController().setArtistTrackSets(newTrackSetsByAlbum));
        }).start();
    }

    public ImmutableMultimap<String, Entry<Integer, Track>> getAlbumTracksOfArtist(String artist) {
        Set<String> artistAlbums;
        ImmutableMultimap<String, Entry<Integer, Track>> artistAlbumTracks;
        synchronized (artistsTracks) {
            artistAlbums = artistsTracks.get(artist).stream()
                                        .map(musicottTracks::get).map(Track::getAlbum)
                                        .collect(Collectors.toSet());
        }
        synchronized (albumsTracks) {
            artistAlbumTracks = ImmutableMultimap.copyOf(Multimaps.filterKeys(albumsTracks, artistAlbums::contains));
        }
        return artistAlbumTracks;
    }

    /**
     * Makes deletePlaylistFromFolders random playlist of tracks and adds it to the {@link PlayerFacade}
     */
    public void makeRandomPlaylist() {
        Thread randomPlaylistThread = new Thread(() -> {
            List<Integer> randomPlaylist = new ArrayList<>();
            synchronized (musicottTracks) {
                ImmutableList<Integer> trackIDs = ImmutableList.copyOf(musicottTracks.keySet());
                Random randomGenerator = new Random();
                do {
                    int rnd = randomGenerator.nextInt(trackIDs.size());
                    int randomTrackID = trackIDs.get(rnd);
                    Track randomTrack = musicottTracks.get(randomTrackID);
                    if (randomTrack.isPlayable())
                        randomPlaylist.add(randomTrackID);
                } while (randomPlaylist.size() < DEFAULT_RANDOM_QUEUE_SIZE);
            }
            PlayerFacade.getInstance().setRandomList(randomPlaylist);
        }, "Random Playlist Thread");
        randomPlaylistThread.start();
    }

    public ListProperty<String> artistsProperty() {
        return artistsListProperty;
    }

    public ReadOnlyBooleanProperty emptyLibraryProperty() {
        return musicottTrackEntriesListProperty.emptyProperty();
    }

    public ListProperty<Entry<Integer, Track>> showingTracksProperty() {
        return showingTracksProperty;
    }

    @Override
    public int hashCode() {
        int hash;
        synchronized (musicottTracks) {
            synchronized (waveforms) {
                synchronized (playlists) {
                    hash = Objects.hash(musicottTracks, waveforms, playlists);
                }
            }
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        boolean res;
        synchronized (musicottTracks) {
            synchronized (waveforms) {
                synchronized (playlists) {
                    if (o instanceof MusicLibrary) {
                        MusicLibrary object = (MusicLibrary) o;
                        res = object.musicottTracks.equals(this.musicottTracks) && object.waveforms
                                .equals(this.waveforms) && object.playlists.equals(this.playlists);
                    }
                    else {
                        res = false;
                    }
                }
            }
        }
        return res;
    }
}
