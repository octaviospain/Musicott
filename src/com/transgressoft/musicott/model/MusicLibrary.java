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
    private static MusicLibrary instance;
    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private final ObservableMap<Integer, Track> musicottTracks;
    private final Map<Integer, float[]> waveforms;
    private final List<Playlist> playlists;

    private ObservableList<Map.Entry<Integer, Track>> showingTracks;
    private ListProperty<Map.Entry<Integer, Track>> showingTracksProperty;

    private ObservableList<Map.Entry<Integer, Track>> musicottTrackEntriesList;
    private ListProperty<Map.Entry<Integer, Track>> musicottTrackEntriesListProperty;

    private Multimap<Integer, String> artistsMultiMap;

    private TaskDemon taskDemon = TaskDemon.getInstance();

    private MusicLibrary() {
        musicottTracks = FXCollections.observableHashMap();
        musicottTracks.addListener(musicottTracksChangeListener());
        bindTrackEntriesList();
        bindShowingTracks();
        waveforms = new HashMap<>();
        playlists = new ArrayList<>();
        artistsMultiMap = TreeMultimap.create();
        taskDemon.setMusicCollections(musicottTracks, waveforms, playlists);
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

    public static MusicLibrary getInstance() {
        if (instance == null)
            instance = new MusicLibrary();
        return instance;
    }

    /**
     * Listener that adds or removes the tracks changed in the Musicott tracks map to the track entries list.
     *
     * @return The {@link MapChangeListener} reference
     */
    private MapChangeListener<Integer, Track> musicottTracksChangeListener() {
        return (MapChangeListener.Change<? extends Integer, ? extends Track> change) -> {
            if (change.wasAdded()) {
                Track addedTrack = change.getValueAdded();
                Integer trackId = addedTrack.getTrackId();
                musicottTrackEntriesList.add(new AbstractMap.SimpleEntry<>(trackId, addedTrack));
            }
            else if (change.wasRemoved()) {
                Track removedTrack = change.getValueRemoved();
                Integer trackId = removedTrack.getTrackId();
                musicottTrackEntriesList.remove(new AbstractMap.SimpleEntry<>(trackId, removedTrack));
            }
            taskDemon.saveLibrary(true, false, false);
        };
    }

    public void addTracks(Map<Integer, Track> tracks) {
        NavigationController navigationController = StageDemon.getInstance().getNavigationController();
        NavigationMode mode = navigationController == null ? null : navigationController.getNavigationMode();

        tracks.entrySet().parallelStream().forEach(trackEntry -> {
            synchronized (musicottTracks) {
                musicottTracks.put(trackEntry.getKey(), trackEntry.getValue());
            }
            if (mode != null && mode == NavigationMode.ALL_TRACKS)
                addToShowingTracksStream(Stream.of(trackEntry));

        });
    }

    public void addArtistsMultiMap(Multimap<Integer, String> tracksToArtistsMultimap) {
        artistsMultiMap.putAll(tracksToArtistsMultimap);
    }

    public void updateArtistsInvolvedInTrack(int trackId, Set<String> artistsInvolved) {
        artistsMultiMap.putAll(trackId, artistsInvolved);
    }

    private void addToShowingTracksStream(Stream<Entry<Integer, Track>> tracksEntriesStream) {
        tracksEntriesStream.parallel().filter(trackEntry -> ! showingTracks.contains(trackEntry))
                           .forEach(trackEntry -> Platform.runLater(() -> showingTracks.add(trackEntry)));
    }

    void addToShowingTracks(List<? extends Integer> tracksIds) {
        Stream<Entry<Integer, Track>> trackStream = tracksIds.parallelStream().filter(id -> getTrack(id).isPresent())
                                                        .map(id -> new AbstractMap.SimpleEntry<>(id, getTrack(id).get()));
        addToShowingTracksStream(trackStream);
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
            removeFromShowingTracks(trackIds);
            synchronized (musicottTracks) {
                musicottTracks.keySet().removeAll(trackIds);
            }
        });

        trackIds.forEach(artistsMultiMap::removeAll);

        boolean[] playlistsChanged = new boolean[]{false};
        synchronized (playlists) {
            playlists.stream().filter(playlist -> ! playlist.isFolder())
                     .forEach(playlist -> playlistsChanged[0] = playlist.removeTracks(trackIds));
        }

        boolean waveformsChanged = waveforms.keySet().removeAll(trackIds);

        taskDemon.saveLibrary(true, waveformsChanged, playlistsChanged[0]);
        LOG.info("Deleted {} tracks", trackIds.size());
        String message = "Deleted " + Integer.toString(trackIds.size()) + " tracks";
        Platform.runLater(() -> StageDemon.getInstance().getNavigationController().setStatusMessage(message));
    }

    void removeFromShowingTracks(List<? extends Integer> trackIds) {
        trackIds.stream().map(id -> new AbstractMap.SimpleEntry<>(id, getTrack(id).get()))
                .forEach(showingTracks::remove);
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

    public ReadOnlyBooleanProperty emptyLibraryProperty() {
        return musicottTrackEntriesListProperty.emptyProperty();
    }

    public ListProperty<Map.Entry<Integer, Track>> showingTracksProperty() {
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
