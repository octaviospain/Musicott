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
 */

package com.musicott.task;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.Track;

import javafx.application.Platform;

/**
 * @author Octavio Calleya
 *
 */
public class UpdateMetadataTask extends Thread {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private List<Track> tracks;
	private File imageFile;
	private CopyOption[] options;

	public UpdateMetadataTask(List<Track> tracks, File imageFile) {
		this.tracks = tracks;
		this.imageFile = imageFile;
		options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};
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
				updated = track.updateMetadata();
				if(imageFile != null)
					coverChanged = track.updateCover(imageFile);
				if((!updated || !coverChanged) && backup != null)
					restoreBackup(track, backup);
			}
		SceneManager.getInstance().saveLibrary(true, false);
		if(ErrorHandler.getInstance().hasErrors(ErrorType.METADATA))
			Platform.runLater(() -> ErrorHandler.getInstance().showErrorDialog(ErrorType.METADATA));
	}
	
	private File makeBackup(Track track) {
		File original, backup = null;
		original = new File(track.getFileFolder()+"/"+track.getFileName());
		try {
			backup = File.createTempFile(track.getName(), "");
			Files.copy(original.toPath(), backup.toPath(), options);
		} catch (IOException e) {
			LOG.error("It wasn't able to copy the backup file: "+e.getMessage(), e);
			ErrorHandler.getInstance().addError(e, ErrorType.METADATA);
		}	
		return backup;
	}
	
	private void restoreBackup(Track track, File backup) {
		File original = new File(track.getFileFolder()+"/"+track.getFileName());
		try {
			backup = File.createTempFile(track.getName(), "");
			Files.move(backup.toPath(), original.toPath(), options);
		} catch (IOException e) {
			LOG.error("It wasn't able to restore the backup file: "+e.getMessage(), e);
			ErrorHandler.getInstance().addError(e, ErrorType.METADATA);
		}
	}
}