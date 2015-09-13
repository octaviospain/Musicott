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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.concurrent.Task;

import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.task.parser.AudioFileParser;

/**
 * @author Octavio Calleya
 *
 */
public class ImportTask extends Task<List<Track>>{

	private List<Track> list;
	boolean m4a, wav, flac;
	private int currentFiles;
	private File folder;
	private List<File> finalFiles;
	
	public ImportTask(File folder, boolean importM4a, boolean importWav, boolean importFlac) {
		list = new ArrayList<Track>();
		finalFiles = new ArrayList<File>();
		this.folder = folder;
		currentFiles = 0;
		m4a = importM4a;
		wav = importWav;
		flac = importFlac;
	}
	
	@Override
	protected List<Track> call() throws Exception {
		countFiles(folder);
		scanFolder(folder);
		if(!isCancelled()) {
			Platform.runLater(() -> {SceneManager.getInstance().getProgressImportController().setIndeterminate();});
			SceneManager.getInstance().getRootController().addTracks(list);
		}
		return list;
	}
	
	@Override
	protected void running() {
		super.running();
		updateMessage("Importing files to collection");
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Import Succeeded");
		SceneManager.getInstance().closeImportScene();
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Cancelled");
	}
	
	private void scanFolder(File folder) {
		for(File file:finalFiles)
			if(isCancelled())
				break;
			else {
				updateProgress(++currentFiles, finalFiles.size());
				Track currentTrack = AudioFileParser.parseAudioFile(file);
				if(currentTrack != null) {
					list.add(currentTrack);
				}
			}
	}
		
	private void countFiles(File folder) {
		File[] files = folder.listFiles(file -> {
			boolean res = false;
			int pos = file.getName().lastIndexOf(".");
			String format = file.getName().substring(pos + 1);
			if(format.equals("mp3")) {
				res = true;
				finalFiles.add(file);
			}
			else
				if(m4a && format.equals("m4a")) {
					res = true;
					finalFiles.add(file);
				}
				else
					if(wav && format.equals("wav")) {
						res = true;
						finalFiles.add(file);
					}
					else
						if(flac && format.equals("flac")) {
							res = true;
							finalFiles.add(file);
						}
			if(file.isDirectory())
				res = true;
			return res;
		});
		for(File file:files)
			if(isCancelled())
				break;
			else
				if(file.isDirectory())
					countFiles(file);
	}
}