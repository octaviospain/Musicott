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

package com.transgressoft.musicott.view;

import com.google.common.collect.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.Platform;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.stream.*;

import static com.transgressoft.musicott.model.AlbumsLibrary.*;
import static org.fxmisc.easybind.EasyBind.*;

/**
 * Controller class that isolates the behaviour of the artists view.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class ArtistsViewController {

    @FXML
    private ListView<String> artistsListView;
    @FXML
    private ListView<TrackSetAreaRow> trackSetsListView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label totalAlbumsLabel;
    @FXML
    private Label totalTracksLabel;
    @FXML
    private Button artistRandomButton;

    private ObservableMap<String, TrackSetAreaRow> albumTrackSets;
    private ObjectProperty<Optional<String>> selectedArtistProperty;

    private PlayerController playerLayoutController;
    private MusicLibrary musicLibrary = MusicLibrary.getInstance();

    @FXML
    public void initialize() {
        albumTrackSets = FXCollections.observableMap(new TreeMap<String, TrackSetAreaRow>());
        albumTrackSets.addListener(albumTrackSetsListener());
        artistsListView.getSelectionModel().selectedItemProperty().addListener(selectedArtistListener());
        artistsListView.setOnMouseClicked(this::doubleClickOnArtistHandler);

        totalAlbumsLabel.setText(String.valueOf(0) + " albums");
        totalTracksLabel.setText(String.valueOf(0) + " tracks");
        artistRandomButton.visibleProperty().bind(map(nameLabel.textProperty().isEmpty().not(), Function.identity()));
        artistRandomButton.setOnAction(e -> musicLibrary.playRandomArtistPlaylist(selectedArtistProperty.get().get()));
        selectedArtistProperty = new SimpleObjectProperty<>(this, "selected artist", Optional.empty());
        nameLabel.textProperty().bind(Bindings.createStringBinding(() -> {
                    if (selectedArtistProperty.get().isPresent())
                        return selectedArtistProperty.get().get();
                    else
                        return "";
                }, selectedArtistProperty));
        if (! artistsListView.getItems().isEmpty())
            checkSelectedArtist();
    }

    private MapChangeListener<String, TrackSetAreaRow> albumTrackSetsListener() {
        return change -> {
            if (change.wasAdded())
                trackSetsListView.getItems().add(change.getValueAdded());
            else if (change.wasRemoved())
                trackSetsListView.getItems().remove(change.getValueRemoved());
            totalTracksLabel.setText(getTotalArtistTracksString());
            totalAlbumsLabel.setText(getAlbumString());
            checkSelectedArtist();
        };
    }

    private FilteredList<String> bindedToSearchFieldArtists() {
        ObservableList<String> tracks = musicLibrary.artistsProperty();
        FilteredList<String> filteredArtists = new FilteredList<>(tracks, artist -> true);

        StringProperty searchTextProperty = playerLayoutController.searchTextProperty();
        subscribe(searchTextProperty, query -> filteredArtists.setPredicate(filterArtistsByQuery(query)));
        return filteredArtists;
    }

    private Predicate<String> filterArtistsByQuery(String query) {
        return artist -> {
            boolean result = query == null || query.isEmpty();
            if(! result)
                result = artistMatchesQuery(artist, query);
            return result;
        };
    }

    private boolean artistMatchesQuery(String artist, String query) {
        boolean matchesName = artist.toLowerCase().contains(query.toLowerCase());
        boolean containsMatchedTrack = musicLibrary.artistContainsMatchedTrack(artist, query);
        return matchesName || containsMatchedTrack;
    }

    private ChangeListener<String> selectedArtistListener() {
        return (observable, oldArtist, newArtist) -> {
            if (newArtist != null) {
                if (! nameLabel.getText().equals(newArtist)) {
                    selectedArtistProperty.set(Optional.of(newArtist));
                    musicLibrary.showArtist(newArtist);
                }
            }
            else {
                if (artistsListView.getItems().isEmpty()) {
                    selectedArtistProperty.set(Optional.empty());
                    albumTrackSets.clear();
                }
                else
                    checkSelectedArtist();
            }
        };
    }

    private void doubleClickOnArtistHandler(MouseEvent event) {
        if (event.getClickCount() == 2 && selectedArtistProperty.get().isPresent())
            musicLibrary.playRandomArtistPlaylist(selectedArtistProperty.get().get());
    }

    private String getTotalArtistTracksString() {
        int totalArtistTracks = albumTrackSets.values().stream()
                                              .mapToInt(trackSet -> trackSet.containedTracksProperty().size())
                                              .sum();
        String appendix = totalArtistTracks == 1 ? " track" : " tracks";
        return String.valueOf(totalArtistTracks) + appendix;
    }

    private String getAlbumString() {
        int numberOfTrackSets = albumTrackSets.size();
        String appendix = numberOfTrackSets == 1 ? " album" : " albums";
        return String.valueOf(numberOfTrackSets) + appendix;
    }

    void checkSelectedArtist() {
        if (artistsListView.getSelectionModel().getSelectedItem() == null) {
            Platform.runLater(() -> {
                artistsListView.getSelectionModel().select(0);
                artistsListView.getFocusModel().focus(0);
                artistsListView.scrollTo(0);
            });
        }
    }

    void updateShowingTrackSets() {
        if (selectedArtistProperty.getValue().isPresent()) {
            String showingArtist = selectedArtistProperty.getValue().get();
            Multimap<String, Entry<Integer, Track>> updatedAlbumTrackSets = musicLibrary.getAlbumTracksOfArtist(showingArtist);

            Set<String> oldAlbums = albumTrackSets.keySet();
            Set<String> newAlbums = updatedAlbumTrackSets.keySet();
            Set<String> addedAlbums = Sets.difference(newAlbums, oldAlbums).immutableCopy();
            Set<String> removedAlbums = Sets.difference(oldAlbums, newAlbums).immutableCopy();
            Set<String> holdedAlbums = Sets.intersection(oldAlbums, newAlbums).immutableCopy();

            updateHoldedAlbums(updatedAlbumTrackSets, holdedAlbums);
            addedAlbums.forEach(album -> Platform.runLater(() -> addTrackSet(album, updatedAlbumTrackSets.get(album))));
            removedAlbums.forEach(album -> Platform.runLater(() -> albumTrackSets.remove(album)));

            if (albumTrackSets.isEmpty() && ! artistsListView.getItems().isEmpty())
                checkSelectedArtist();
        }
    }

    private void updateHoldedAlbums(Multimap<String, Entry<Integer, Track>> updatedAlbumTrackSets, Set<String> holdedAlbums) {
        holdedAlbums.forEach(album -> {
            TrackSetAreaRow albumTrackSet = albumTrackSets.get(album);
            Set<Entry<Integer, Track>> oldTracks = ImmutableSet.copyOf(albumTrackSet.containedTracksProperty());
            Set<Entry<Integer, Track>> newTracks = ImmutableSet.copyOf(updatedAlbumTrackSets.get(album));
            Set<Entry<Integer, Track>> addedTracks = Sets.difference(newTracks, oldTracks).immutableCopy();
            Set<Entry<Integer, Track>> removedTracks = Sets.difference(oldTracks, newTracks).immutableCopy();

            Platform.runLater(() -> {
                albumTrackSets.get(album).containedTracksProperty().addAll(addedTracks);
                albumTrackSets.get(album).containedTracksProperty().removeAll(removedTracks);
            });
        });
    }

    private void addTrackSet(String album, Collection<Entry<Integer, Track>> tracks) {
        selectedArtistProperty.getValue().ifPresent(showingArtist -> {
            TrackSetAreaRow trackSetAreaRow = new TrackSetAreaRow(showingArtist, album, tracks);
            subscribe(trackSetAreaRow.selectedTracksProperty(), selection -> checkSelectedArtist());
            albumTrackSets.put(album, trackSetAreaRow);
        });
    }

    void setPlayerController(PlayerController playerLayoutController) {
        this.playerLayoutController = playerLayoutController;
        artistsListView.setItems(bindedToSearchFieldArtists());
    }

    /**
     * Puts several {@link TrackSetAreaRow}s in the view given the tracks, in form of
     * {@link Entry}, mapped by album
     *
     * @param tracksByAlbum The @{@link Multimap} containing tracks mapped by album
     */
    public void setArtistTrackSets(Multimap<String, Entry<Integer, Track>> tracksByAlbum) {
        albumTrackSets.clear();
        trackSetsListView.getItems().clear();
        tracksByAlbum.asMap().entrySet().forEach(multimapEntry -> {
            String album = multimapEntry.getKey();
            Collection<Entry<Integer, Track>> tracks = multimapEntry.getValue();
            addTrackSet(album, tracks);
        });
    }

    /**
     * Removes a given track {@link Entry} from the {@link TrackSetAreaRow}s that
     * are shown on the view
     *
     * @param trackEntry An {@link Entry} with a {@link Track} id and itself
     */
    public void removeFromTrackSets(Entry<Integer, Track> trackEntry) {
        String trackAlbum = trackEntry.getValue().getAlbum();
        trackAlbum = trackAlbum.isEmpty() ? UNK_ALBUM : trackAlbum;
        if (albumTrackSets.containsKey(trackAlbum)) {
            TrackSetAreaRow albumAreaRow = albumTrackSets.get(trackAlbum);
            boolean[] removed = new boolean[]{false};
            Platform.runLater(() -> removed[0] = albumAreaRow.containedTracksProperty().remove(trackEntry));
            if (removed[0] && albumAreaRow.containedTracksProperty().isEmpty())
                albumTrackSets.remove(trackAlbum);
            if (albumTrackSets.isEmpty() && ! artistsListView.getItems().isEmpty())
                checkSelectedArtist();
        }
    }

    public ObservableList<Entry<Integer, Track>> getSelectedTracks() {
        return albumTrackSets.values().stream().flatMap(entry -> entry.selectedTracksProperty().stream())
                             .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    public void selectTrack(Entry<Integer, Track> trackEntry) {
        Set<String> artistsInvolved = trackEntry.getValue().getArtistsInvolved();
        Optional<String> selectedArtist = selectedArtistProperty.getValue();
        if (selectedArtist.isPresent() && ! artistsInvolved.contains(selectedArtist.get())) {
            String newArtist = artistsInvolved.stream().findFirst().get();
            selectedArtistProperty.setValue(Optional.of(newArtist));
            musicLibrary.showArtistAndSelectTrack(newArtist, trackEntry);
            artistsListView.getSelectionModel().select(newArtist);
            artistsListView.scrollTo(newArtist);
        }
        else {
            Track track = trackEntry.getValue();
            String trackAlbum = track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum();
            albumTrackSets.values().forEach(trackSet -> {
                if (trackSet.getAlbum().equals(trackAlbum)) {
                    trackSet.selectTrack(trackEntry);
                    trackSetsListView.scrollTo(trackSet);
                }
                else
                    trackSet.deselectAllTracks();
            });
        }
    }

    public void selectAllTracks() {
        albumTrackSets.values().forEach(TrackSetAreaRow::selectAllTracks);
    }

    public void deselectAllTracks() {
        albumTrackSets.values().forEach(TrackSetAreaRow::deselectAllTracks);
    }
}