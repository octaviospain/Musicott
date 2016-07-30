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

package com.transgressoft.musicott.tasks;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * Class that extends from {@link Thread} that performs the operation of
 * updating the metadata of the audio files.
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class UpdateMetadataTask extends Thread {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private List<Track> tracks;
	private CopyOption[] options;
	private List<String> updateErrors;

	private ErrorDemon errorDemon = ErrorDemon.getInstance();
	private MusicLibrary musicLibrary = MusicLibrary.getInstance();

	public UpdateMetadataTask(List<Track> tracks) {
		this.tracks = tracks;
		options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};
		updateErrors = new ArrayList<>();
	}

	@Override
	public void run() {
		boolean updated;
		for (Track track : tracks)
			if (track.getInDisk()) {
				File backup = makeBackup(track);
				updated = track.writeMetadata();
				if (! updated && backup != null)
					restoreBackup(track, backup);

				if (updated)
					LOG.debug("Updating metadata of {}", track.getFileFolder() + "/" + track.getFileName());
				else
					LOG.debug("Updating metadata of {}", track.getFileFolder() + "/" + track.getFileName());
			}
		musicLibrary.saveLibrary(true, false, false);
		if (! updateErrors.isEmpty())
			errorDemon.showExpandableErrorsDialog("Errors writing metadata on some tracks", "", updateErrors);
	}

	private File makeBackup(Track track) {
		File original = new File(track.getFileFolder() + "/" + track.getFileName());
		File backup = null;
		try {
			backup = File.createTempFile(track.getName(), "");
			Files.copy(original.toPath(), backup.toPath(), options);
		}
		catch (IOException exception) {
			LOG.error("Error creating the backup file: ", exception.getCause());
			errorDemon.showErrorDialog("Error creating the backup file", "", exception);
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
			errorDemon.showErrorDialog("Error restoring the backup file", "", exception);
		}
	}
}
