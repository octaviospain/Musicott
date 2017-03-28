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
import com.transgressoft.musicott.view.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import org.slf4j.*;

import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;

import static com.transgressoft.musicott.model.AlbumsLibrary.*;

/**
 * Singleton class that manages the operations over the
 * collection of tracks, waveforms, playlists, artists and albums.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class MusicLibrary {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    public final TracksLibrary tracks = new TracksLibrary();
    public final WaveformsLibrary waveforms = new WaveformsLibrary();
    public final PlaylistsLibrary playlists = new PlaylistsLibrary(this);
    private final ArtistsLibrary artists = new ArtistsLibrary();
    private final AlbumsLibrary albums = new AlbumsLibrary();

    private final MapChangeListener<Integer, Track> musicottTracksListener = musicottTracksChangeListener();

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
        tracks.addListener(musicottTracksListener);
        taskDemon.setMusicCollections(tracks.musicottTracks, waveforms.waveforms, playlists.playlistsTree);
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
            taskDemon.saveLibrary(true, true, false);
        };
    }

    private boolean addTrackToCollections(Track track) {
        String trackAlbum = track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum();
        boolean[] added = new boolean[]{false};
        Entry<Integer, Track> trackEntry = new SimpleEntry<>(track.getTrackId(), track);

        Platform.runLater(() -> added[0] = tracks.tracksProperty().add(trackEntry));
        Platform.runLater(() -> added[0] |= albums.addTracks(trackAlbum, Collections.singletonList(trackEntry)));
        track.getArtistsInvolved().forEach(artist -> added[0] |= artists.addArtistTrack(artist, track));
        LOG.debug("Added {}", track);

        NavigationController navigationController = stageDemon.getNavigationController();
        NavigationMode mode = navigationController == null ? null :
                navigationController.navigationModeProperty().getValue();
        if (mode != null && mode == NavigationMode.ALL_TRACKS)
            showingTracksProperty().add(trackEntry);
        return added[0];
    }

    private boolean removeTrackFromCollections(Track track) {
        String trackAlbum = track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum();
        boolean[] removed = new boolean[]{false};
        Entry<Integer, Track> trackEntry = new SimpleEntry<>(track.getTrackId(), track);
        stageDemon.getRootController().removeFromTrackSets(trackEntry);

        Platform.runLater(() -> removed [0] = tracks.tracksProperty().remove(trackEntry));
        Platform.runLater(() -> removed[0] |= showingTracksProperty().remove(trackEntry));
        removed[0] |= albums.removeTracks(trackAlbum, Collections.singletonList(trackEntry));
        track.getArtistsInvolved().forEach(artist -> removed[0] |= artists.removeArtistTrack(artist, track));
        waveforms.removeWaveform(track.getTrackId());
        playlists.removeFromPlaylists(track);
        LOG.debug("Removed {}", track);
        return removed[0];
    }

    public boolean artistContainsMatchedTrack(String artist, String query) {
        return artists.artistContainsMatchedTrack(artist, query);
    }

    public void showAllTracks() {
        tracks.resetShowingTracks();
    }

    public void showPlaylist(Playlist playlist) {
        showingTracksProperty().clear();
        List<Entry<Integer, Track>> tracksUnderPlaylist = playlists.getTrackEntriesUnderPlaylist(playlist);
        showingTracksProperty().addAll(tracksUnderPlaylist);
    }

    public void showArtist(String artist) {
        showArtistAndSelectTrack(artist, null);
    }

    public void showArtistAndSelectTrack(String artist, Entry<Integer, Track> trackToSelect) {
        new Thread(() -> {
            Multimap<String, Entry<Integer, Track>> newTrackSetsByAlbum = getAlbumTracksOfArtist(artist);
            Platform.runLater(() -> {
                stageDemon.getRootController().setArtistTrackSets(newTrackSetsByAlbum);
                if (trackToSelect != null)
                    stageDemon.getRootController().selectTrack(trackToSelect);
            });
        }).start();
    }

    public void updateArtistsInvolvedInTrack(Track track, Set<String> removedArtists, Set<String> addedArtists) {
        removedArtists.forEach(artist -> artists.removeArtistTrack(artist, track));
        addedArtists.forEach(artist -> artists.addArtistTrack(artist, track));
    }

    public void updateTrackAlbums(List<Entry<Integer, Track>> modifiedTracks, Set<String> oldAlbums, String newAlbum) {
        oldAlbums.forEach(album -> albums.removeTracks(album, modifiedTracks));
        albums.addTracks(newAlbum, modifiedTracks);
    }

    /**
     * Delete tracks from the music library looking also for occurrences in
     * the playlists and the waveforms collections.
     * This method is called on an independent Thread in {@link StageDemon#deleteTracks}
     *
     * @param tracksToDelete A {@code List} of track ids
     */
    public void deleteTracks(Collection<Track> tracksToDelete) {
        if (tracksToDelete.size() == tracks.getSize())
            Platform.runLater(this::clearCollections);
        else
            tracks.remove(tracksToDelete);
    }

    private void clearCollections() {
        tracks.removeListener(musicottTracksListener);
        tracks.clear();
        artists.clear();
        albums.clear();
        tracks.addListener(musicottTracksListener);
        waveforms.clearWaveforms();
        playlists.clearPlaylists();
        taskDemon.saveLibrary(true, true, true);
    }

    /**
     * Searches and returns all the tracks, mapped by album, in which an artist is involved.
     *
     * @param artist The given artist to find their related tracks
     * @return A {@link ImmutableMultimap} with the albums as keys, and {@code Entries} as values
     */
    public ImmutableMultimap<String, Entry<Integer, Track>> getAlbumTracksOfArtist(String artist) {
        ImmutableMultimap<String, Entry<Integer, Track>> artistAlbumTracks;
        if (artists.contains(artist)) {
            Set<String> artistAlbums = artists.getArtistAlbums(artist);
            artistAlbumTracks = albums.getTracksByAlbum(artist, artistAlbums);
        }
        else
            artistAlbumTracks = ImmutableMultimap.of();
        return artistAlbumTracks;
    }

    /**
     * Makes a random playlist of tracks of the given artist and adds it to the {@link PlayerFacade}
     *
     * @param artist The given artist
     */
    public void playRandomArtistPlaylist(String artist) {
        Thread randomArtistPlaylistThread = new Thread(() -> {
            List<Track> randomList = artists.getRandomListOfArtistTracks(artist);
            PlayerFacade.getInstance().setRandomListAndPlay(randomList);
        }, "Random Artist Playlist Thread");
        randomArtistPlaylistThread.start();
    }

    /**
     * Makes a random playlist of tracks and adds it to the {@link PlayerFacade}
     */
    public void playRandomPlaylist() {
        Thread randomPlaylistThread = new Thread(() -> {
            List<Track> randomPlaylist = tracks.getRandomList();
            PlayerFacade.getInstance().setRandomListAndPlay(randomPlaylist);
        }, "Random Playlist Thread");
        randomPlaylistThread.start();
    }

    public void playPlaylistRandomly(Playlist playlist) {
        Thread randomPlaylistPlayThread = new Thread(() -> {
            List<Track> randomList = playlists.getRandomSortedTrackList(playlist);
            PlayerFacade.getInstance().setRandomListAndPlay(randomList);
        }, "Shuffle Playlist Thread");
        randomPlaylistPlayThread.start();
    }

    public ListProperty<String> artistsProperty() {
        return artists.artistsListProperty();
    }

    public ReadOnlyBooleanProperty emptyLibraryProperty() {
        return tracks.tracksProperty().emptyProperty();
    }

    public ListProperty<Entry<Integer, Track>> showingTracksProperty() {
        return tracks.showingTracksProperty();
    }
}