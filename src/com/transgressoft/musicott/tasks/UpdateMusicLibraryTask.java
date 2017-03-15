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

package com.transgressoft.musicott.tasks;

import com.google.common.collect.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.collections.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * Class that extends from {@link Thread} that performs the operation of
 * updating the metadata of the audio files.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 */
public class UpdateMusicLibraryTask extends Thread {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private List<Track> tracks;
	private Set<String> changedAlbums;
	private Optional<String> newAlbum;
	private CopyOption[] options;
	private List<String> updateErrors;

	private ErrorDemon errorDemon = ErrorDemon.getInstance();
	private TaskDemon taskDemon = TaskDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();
	private StageDemon stageDemon = StageDemon.getInstance();

	public UpdateMusicLibraryTask(List<Track> tracks, Set<String> changedAlbums, Optional<String> newAlbum) {
		this.tracks = tracks;
		this.changedAlbums = changedAlbums;
		this.newAlbum = newAlbum;
		options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};
		updateErrors = new ArrayList<>();
	}

	@Override
	public void run() {
		updateMusicLibraryAlbums();
		updateMusicLibraryTracks();
		stageDemon.getRootController().updateShowingTrackSets();
        taskDemon.saveLibrary(true, false, false);
		if (! updateErrors.isEmpty())
			errorDemon.showExpandableErrorsDialog("Errors writing metadata on some tracks", null, updateErrors);
	}

	private void updateMusicLibraryAlbums() {
		newAlbum.ifPresent(album -> {
			List<Entry<Integer, Track>> trackEntries = tracks.stream()
															 .map(track -> new SimpleEntry<>(track.getTrackId(), track))
															 .collect(Collectors.toList());
			musicLibrary.updateTrackAlbums(trackEntries, changedAlbums, album);
		});
	}

	private void updateMusicLibraryTracks() {
		tracks.forEach(track -> {
			updateArtistsInvolved(track);
			if (track.isInDisk())
				updateFileMetadata(track);
		});
	}

	private void updateArtistsInvolved(Track track) {
		Set<String> oldArtistsInvolved = track.getArtistsInvolved();
		Set<String> newArtistsInvolved = Utils.getArtistsInvolvedInTrack(track);
		Set<String> removedArtists = Sets.difference(oldArtistsInvolved, newArtistsInvolved).immutableCopy();
		Set<String> addedArtists = Sets.difference(newArtistsInvolved, oldArtistsInvolved).immutableCopy();
		track.setArtistsInvolved(FXCollections.observableSet(newArtistsInvolved));
		musicLibrary.updateArtistsInvolvedInTrack(track.getTrackId(), removedArtists, addedArtists);
	}

	private void updateFileMetadata(Track track) {
		File backup = makeBackup(track);
		try {
			track.writeMetadata();
			deleteBackup(track, backup);
			LOG.debug("Updated (or not) metadata of {}", track.getFileFolder() + "/" + track.getFileName());
		}
		catch (TrackUpdateException exception) {
			if (backup != null)
				restoreBackup(track, backup);
			updateErrors.add(exception.getMessage() + ": " + exception.getCause().getMessage());
		}
	}

	private File makeBackup(Track track) {
		File original = new File(track.getFileFolder() + "/" + track.getFileName());
		File backup = null;
		try {
			backup = File.createTempFile(track.getFileName(), "");
			Files.copy(original.toPath(), backup.toPath(), options);
		}
		catch (IOException exception) {
			LOG.error("Error creating the backup file: ", exception.getCause());
			errorDemon.showErrorDialog("Error creating the backup file", null, exception);
		}
		return backup;
	}

	private void restoreBackup(Track track, File backup) {
		File original = new File(track.getFileFolder() + "/" + track.getFileName());
		try {
			Files.move(backup.toPath(), original.toPath(), options);
		}
		catch (IOException | UnsupportedOperationException exception) {
			LOG.error("Error restoring the backup file: ", exception.getCause());
			errorDemon.showErrorDialog("Error restoring the backup file", null, exception);
		}
	}

	private void deleteBackup(Track track, File backup) {
		if (! backup.delete()) {
			LOG.error("Error deleting backup file of {}", track);
			errorDemon.showErrorDialog("Error deleting the backup file of " + track.getFileName());
		}
	}
}
