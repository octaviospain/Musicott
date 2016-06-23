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

package com.musicott.tasks;

import com.musicott.*;
import com.musicott.model.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * @author Octavio Calleya
 *
 */
public class UpdateMetadataTask extends Thread {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private List<Track> tracks;
	private File imageFile;
	private CopyOption[] options;
	private List<String> updateErrors;

	public UpdateMetadataTask(List<Track> tracks, File imageFile) {
		this.tracks = tracks;
		this.imageFile = imageFile;
		options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};
		updateErrors = new ArrayList<>();
	}
	
	@Override
	public void run() {
		boolean updated = false, coverChanged = false;
		if(imageFile == null)
			coverChanged = true;
		for(Track track : tracks)
			if(track.getInDisk()) {
				LOG.debug("Updating metadata of {}", track.getFileFolder()+"/"+track.getFileName());
				File backup = makeBackup(track);
				try {
					updated = track.updateMetadata();
				} catch (CannotReadException | IOException | TagException | ReadOnlyFileException
						| InvalidAudioFrameException | CannotWriteException e) {
					LOG.warn("Error writing metadata of "+track, e);
					updateErrors.add(track.getArtist()+" - "+track.getName()+": "+e.getMessage());
				}
				if(imageFile != null)
					coverChanged = track.updateCover(imageFile);
				if((!updated || !coverChanged) && backup != null)
					restoreBackup(track, backup);
			}
		MusicLibrary.getInstance().saveLibrary(true, false, false);
		if(!updateErrors.isEmpty())
			ErrorDemon.getInstance().showExpandableErrorsDialog("Errors writing metadata on some tracks", "", updateErrors);
	}
	
	private File makeBackup(Track track) {
		File original, backup = null;
		original = new File(track.getFileFolder()+"/"+track.getFileName());
		try {
			backup = File.createTempFile(track.getName(), "");
			Files.copy(original.toPath(), backup.toPath(), options);
		} catch (IOException e) {
			LOG.error("Error creating the backup file: "+e.getMessage(), e);
			ErrorDemon.getInstance().showErrorDialog("Error creating the backup file", null, e);
		}	
		return backup;
	}
	
	private void restoreBackup(Track track, File backup) {
		File original = new File(track.getFileFolder()+"/"+track.getFileName());
		try {
			backup = File.createTempFile(track.getName(), "");
			Files.move(backup.toPath(), original.toPath(), options);
		} catch (IOException e) {
			LOG.error("Error restoring the backup file: "+e.getMessage(), e);
			ErrorDemon.getInstance().showErrorDialog("Error restoring the backup file", null, e);
		}
	}
}
