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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.musicott.model;

import com.musicott.*;
import com.musicott.player.*;
import com.musicott.tasks.*;
import com.musicott.view.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import org.slf4j.*;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Singleton class that isolates the access to the tracks, waveforms, and playlists of the <tt>Musicott</tt> music
 * library.
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class MusicLibrary {

	private static final int DEFAULT_RANDOM_QUEUE_SIZE = 8;
	private static MusicLibrary instance;
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private ObservableMap<Integer, Track> musicottTracks;
	private Map<Integer, float[]> waveforms;
	private List<Playlist> playlists;

	private ObservableList<Map.Entry<Integer, Track>> showingTracks;
	private ListProperty<Map.Entry<Integer, Track>> showingTracksProperty;

	private ObservableList<Map.Entry<Integer, Track>> musicottTrackEntriesList;
	private ListProperty<Map.Entry<Integer, Track>> musicottTrackEntriesListProperty;
	private SaveMusicLibraryTask saveMusicLibraryTask;
	private MapChangeListener<Integer, Track> musicottTracksChangeListener = musicottTracksChangeListener();

	private MusicLibrary() {
		musicottTracks = FXCollections.observableHashMap();
		musicottTracks.addListener(musicottTracksChangeListener);
		bindTrackEntriesList();
		bindShowingTracks();
		waveforms = new HashMap<>();
		playlists = new ArrayList<>();
	}

	/**
	 * Binds a list of entries of the Musicott tracks map, and it to a list property
	 */
	private void bindTrackEntriesList() {
		musicottTrackEntriesList = FXCollections.observableArrayList(musicottTracks.entrySet());
		musicottTrackEntriesListProperty = new SimpleListProperty<>(this, "all tracks");
		musicottTrackEntriesListProperty.bind(new SimpleObjectProperty<>(musicottTrackEntriesList));
	}

	/**
	 * Binds the tracks that must be shown on the table to a list property
	 */
	private void bindShowingTracks() {
		showingTracks = FXCollections.observableArrayList(musicottTracks.entrySet());
		;
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
	 * @return The {@link WeakMapChangeListener} reference
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
			saveLibrary(true, false, false);
		};
	}

	public void saveLibrary(boolean saveTracks, boolean saveWaveforms, boolean savePlaylists) {
		if (saveMusicLibraryTask == null) {
			saveMusicLibraryTask = new SaveMusicLibraryTask(musicottTracks, waveforms, playlists);
			saveMusicLibraryTask.setDaemon(true);
			saveMusicLibraryTask.start();
		}
		saveMusicLibraryTask.saveMusicLibrary(saveTracks, saveWaveforms, savePlaylists);
	}

	public void addTracks(Map<Integer, Track> tracks) {
		tracks.entrySet().parallelStream().forEach(trackEntry -> {
			synchronized (musicottTracks) {
				synchronized (musicottTrackEntriesList) {
					musicottTracks.put(trackEntry.getKey(), trackEntry.getValue());
					musicottTrackEntriesList.addAll(trackEntry);

					NavigationController navigationController = StageDemon.getInstance().getNavigationController();
					NavigationMode mode = navigationController == null ? null : navigationController.getNavigationMode();

					if (mode != null && mode == NavigationMode.ALL_TRACKS)
						addToShowingTracksStream(Stream.of(trackEntry));
				}
			}
		});
		saveLibrary(true, false, false);
	}

	private void addToShowingTracksStream(Stream<Entry<Integer, Track>> tracksEntriesStream) {
		tracksEntriesStream.parallel()
						   .filter(trackEntry -> ! showingTracks.contains(trackEntry))
						   .forEach(trackEntry -> Platform.runLater(() -> showingTracks.add(trackEntry)));
	}

	void addToShowingTracks(List<? extends Integer> tracksIds) {
		Stream<Entry<Integer, Track>> trackStream = tracksIds.parallelStream().filter(id -> getTrack(id).isPresent())
															 .map(id -> new AbstractMap.SimpleEntry<>(id, getTrack(id)
																	 .get()));
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
	}

	public void addWaveforms(Map<Integer, float[]> newWaveforms) {
		synchronized (waveforms) {
			waveforms.putAll(newWaveforms);
		}
	}

	public void addPlaylist(Playlist playlist) {
		synchronized (playlists) {
			playlists.add(playlist);
		}
		saveLibrary(false, false, true);
	}

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

	public void deleteTracks(List<Integer> trackIds) {
		synchronized (musicottTracks) {
			waveforms.keySet().removeAll(trackIds);
			Platform.runLater(() -> {
				removeFromShowingTracks(trackIds);
				playlists.stream().filter(playlist -> ! playlist.isFolder())
						 .forEach(playlist -> playlist.removeTracks(trackIds));
				musicottTracks.keySet().removeAll(trackIds);
			});
		}
		saveLibrary(true, true, true);
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
				List<Playlist> folders = playlists.stream().filter(p -> p.isFolder()).collect(Collectors.toList());

			search:
				for (Playlist folder : folders) {
					ListIterator<Playlist> folderChildsIterator = folder.getContainedPlaylists().listIterator();
					while (folderChildsIterator.hasNext())
						if (folderChildsIterator.next().equals(playlist)) {
						folderChildsIterator.remove();
						break search;
					}
				}
			}
		}
		saveLibrary(false, false, true);
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
	 * Makes a random playlist of tracks and adds it to the {@link PlayerFacade}
	 */
	public void makeRandomPlaylist() {
		Thread randomPlaylistThread = new Thread(() -> {
			List<Integer> randomPlaylist = new ArrayList<>();
			synchronized (musicottTracks) {
				List<Integer> trackIDs = new ArrayList<>(musicottTracks.keySet());
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
		int hash = 71;
		synchronized (musicottTracks) {
			synchronized (waveforms) {
				synchronized (playlists) {
					hash = 73 * hash + musicottTracks.hashCode();
					hash = 73 * hash + waveforms.hashCode();
					hash = 73 * hash + playlists.hashCode();
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
						res = object.musicottTracks.equals(this.musicottTracks) &&
								object.musicottTracks.equals(this.waveforms) &&
								object.musicottTracks.equals(this.playlists);
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
