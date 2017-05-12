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
 * collection of tracksLibrary, waveformsLibrary, playlistsLibrary, artistsLibrary and albumsLibrar.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class MusicLibrary {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final StageDemon stageDemon;
    private final TaskDemon taskDemon;
    private final PlayerFacade playerFacade;
    private final MapChangeListener<Integer, Track> musicottTracksListener = musicottTracksChangeListener();

    private TracksLibrary tracksLibrary;
    private ArtistsLibrary artistsLibrary;
    private AlbumsLibrary albumsLibrary;
    private WaveformsLibrary waveformsLibrary;
    private PlaylistsLibrary playlistsLibrary;

    @Inject
    public MusicLibrary(TracksLibrary tracksLibrary, ArtistsLibrary artistsLibrary, AlbumsLibrary albumsLibrary,
            WaveformsLibrary waveformsLibrary, PlaylistsLibrary playlistsLibrary, StageDemon stageDemon,
            TaskDemon taskDemon, Provider<PlayerFacade> playerFacade) {
        this.tracksLibrary = tracksLibrary;
        this.artistsLibrary = artistsLibrary;
        this.albumsLibrary = albumsLibrary;
        this.waveformsLibrary = waveformsLibrary;
        this.playlistsLibrary = playlistsLibrary;
        this.stageDemon = stageDemon;
        this.taskDemon = taskDemon;
        this.playerFacade = playerFacade.get();
        this.tracksLibrary.addListener(musicottTracksListener);
    }

    /**
     * Listener that adds or removes the tracksLibrary and the related information in the
     * collections if necessary when the Musicott tracksLibrary map changes.
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

        Platform.runLater(() -> added[0] = tracksLibrary.tracksProperty().add(trackEntry));
        Platform.runLater(() -> added[0] |= albumsLibrary.addTracks(trackAlbum, Collections.singletonList(trackEntry)));
        track.getArtistsInvolved().forEach(artist -> added[0] |= artistsLibrary.addArtistTrack(artist, track));
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

        Platform.runLater(() -> removed [0] = tracksLibrary.tracksProperty().remove(trackEntry));
        Platform.runLater(() -> removed[0] |= showingTracksProperty().remove(trackEntry));
        removed[0] |= albumsLibrary.removeTracks(trackAlbum, Collections.singletonList(trackEntry));
        track.getArtistsInvolved().forEach(artist -> removed[0] |= artistsLibrary.removeArtistTrack(artist, track));
        waveformsLibrary.removeWaveform(track.getTrackId());
        playlistsLibrary.removeFromPlaylists(track);
        LOG.debug("Removed {}", track);
        return removed[0];
    }

    public boolean artistContainsMatchedTrack(String artist, String query) {
        return artistsLibrary.artistContainsMatchedTrack(artist, query);
    }

    public void showAllTracks() {
        tracksLibrary.resetShowingTracks();
    }

    public void showPlaylist(Playlist playlist) {
        showingTracksProperty().clear();
        List<Entry<Integer, Track>> tracksUnderPlaylist = playlistsLibrary.getTrackEntriesUnderPlaylist(playlist);
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
        removedArtists.forEach(artist -> artistsLibrary.removeArtistTrack(artist, track));
        addedArtists.forEach(artist -> artistsLibrary.addArtistTrack(artist, track));
    }

    public void updateTrackAlbums(List<Entry<Integer, Track>> modifiedTracks, Set<String> oldAlbums, String newAlbum) {
        oldAlbums.forEach(album -> albumsLibrary.removeTracks(album, modifiedTracks));
        albumsLibrary.addTracks(newAlbum, modifiedTracks);
    }

    /**
     * Delete tracksLibrary from the music library looking also for occurrences in
     * the playlistsLibrary and the waveformsLibrary collections.
     * This method is called on an independent Thread in {@link StageDemon#deleteTracks}
     *
     * @param tracksToDelete A {@code List} of track ids
     */
    public void deleteTracks(Collection<Track> tracksToDelete) {
        if (tracksToDelete.size() == tracksLibrary.getSize())
            Platform.runLater(this::clearCollections);
        else
            tracksLibrary.remove(tracksToDelete);
    }

    private void clearCollections() {
        tracksLibrary.removeListener(musicottTracksListener);
        tracksLibrary.clear();
        artistsLibrary.clear();
        albumsLibrary.clear();
        tracksLibrary.addListener(musicottTracksListener);
        waveformsLibrary.clearWaveforms();
        playlistsLibrary.clearPlaylists();
        taskDemon.saveLibrary(true, true, true);
    }

    /**
     * Searches and returns all the tracksLibrary, mapped by album, in which an artist is involved.
     *
     * @param artist The given artist to find their related tracksLibrary
     * @return A {@link ImmutableMultimap} with the albumsLibrary as keys, and {@code Entries} as values
     */
    public ImmutableMultimap<String, Entry<Integer, Track>> getAlbumTracksOfArtist(String artist) {
        ImmutableMultimap<String, Entry<Integer, Track>> artistAlbumTracks;
        if (artistsLibrary.contains(artist)) {
            Set<String> artistAlbums = artistsLibrary.getArtistAlbums(artist);
            artistAlbumTracks = albumsLibrary.getTracksByAlbum(artist, artistAlbums);
        }
        else
            artistAlbumTracks = ImmutableMultimap.of();
        return artistAlbumTracks;
    }

    /**
     * Makes a random playlist of tracksLibrary of the given artist and adds it to the {@link PlayerFacade}
     *
     * @param artist The given artist
     */
    public void playRandomArtistPlaylist(String artist) {
        Thread randomArtistPlaylistThread = new Thread(() -> {
            List<Track> randomList = artistsLibrary.getRandomListOfArtistTracks(artist);
            playerFacade.setRandomListAndPlay(randomList);
        }, "Random Artist Playlist Thread");
        randomArtistPlaylistThread.start();
    }

    /**
     * Makes a random playlist of tracksLibrary and adds it to the {@link PlayerFacade}
     */
    public void playRandomPlaylist() {
        Thread randomPlaylistThread = new Thread(() -> {
            List<Track> randomPlaylist = tracksLibrary.getRandomList();
            playerFacade.setRandomListAndPlay(randomPlaylist);
        }, "Random Playlist Thread");
        randomPlaylistThread.start();
    }

    public void playPlaylistRandomly(Playlist playlist) {
        Thread randomPlaylistPlayThread = new Thread(() -> {
            List<Track> randomList = playlistsLibrary.getRandomSortedTrackList(playlist);
            playerFacade.setRandomListAndPlay(randomList);
        }, "Shuffle Playlist Thread");
        randomPlaylistPlayThread.start();
    }

    public TracksLibrary getTracksLibrary() {
        return tracksLibrary;
    }

//    @Inject
//    public void setTracksLibrary(TracksLibrary tracksLibrary) {
//        this.tracksLibrary = tracksLibrary;
//        this.tracksLibrary.addListener(musicottTracksListener);
//    }

//    @Inject
//    public void setArtistsLibrary(ArtistsLibrary artistsLibrary) {
//        this.artistsLibrary = artistsLibrary;
//    }

//    @Inject
//    public void setAlbumsLibrary(AlbumsLibrary albumsLibrary) {
//        this.albumsLibrary = albumsLibrary;
//    }

    public WaveformsLibrary getWaveformsLibrary() {
        return waveformsLibrary;
    }

//    @Inject
//    public void setWaveformsLibrary(WaveformsLibrary waveformsLibrary) {
//        this.waveformsLibrary = waveformsLibrary;
//    }

    public PlaylistsLibrary getPlaylistsLibrary() {
        return playlistsLibrary;
    }

//    @Inject
//    public void setPlaylistsLibrary(PlaylistsLibrary playlistsLibrary) {
//        this.playlistsLibrary = playlistsLibrary;
//    }

    public ListProperty<String> artistsLibraryProperty() {
        return artistsLibrary.artistsListProperty();
    }

    public ReadOnlyBooleanProperty emptyLibraryProperty() {
        return tracksLibrary.tracksProperty().emptyProperty();
    }

    public ListProperty<Entry<Integer, Track>> showingTracksProperty() {
        return tracksLibrary.showingTracksProperty();
    }
}
