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
import com.musicott.error.ErrorHandler;
import com.musicott.error.ParseException;
import com.musicott.model.Track;
import com.musicott.task.parser.Mp3Parser;

/**
 * @author Octavio Calleya
 *
 */
public class ImportTask extends Task<List<Track>>{

	private List<Track> list;
	boolean m4a, wav, flac;
	private int numFiles, currentFiles;
	private File folder;
	
	public ImportTask(File folder, boolean importM4a, boolean importWav, boolean importFlac) {
		list = new ArrayList<Track>();
		this.folder = folder;
		numFiles = 0;
		currentFiles = 0;
		m4a = importM4a;
		wav = importWav;
		flac = importFlac;
	}
	
	@Override
	protected List<Track> call() throws Exception {
		countFiles(folder);
		scanFolder(folder);
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
		Platform.runLater(() -> {SceneManager.getInstance().getRootController().addTracks(list);
								 SceneManager.getInstance().closeImportScene();});
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Canceled");
	}
	
	private void scanFolder(File folder) {
		File[] files = folder.listFiles();
		for(File file:files)
			if(isCancelled())
				break;
			else
				if(file.isDirectory())
					scanFolder(file);
				else {
					try {
						if(file.getName().substring(file.getName().length()-3).equals("mp3")) {
							updateProgress(++currentFiles, numFiles);
							list.add(Mp3Parser.parseMp3File(file));
						}
						else
							if(m4a && file.getName().substring(file.getName().length()-3).equals("m4a")) {
								updateProgress(++currentFiles, numFiles);
								//TODO M4aParser
							}
							else
								if(wav && file.getName().substring(file.getName().length()-3).equals("wav")) {
									updateProgress(++currentFiles, numFiles);
									//TODO WavParser
								}
								else
									if(flac && file.getName().substring(file.getName().length()-4).equals("flac")) {
										updateProgress(++currentFiles, numFiles);
										//TODO FlacParser
									}
					} catch (Exception e) {
						ParseException pe = new ParseException("Parsing Error", e, file);
						ErrorHandler.getInstance().addParseException(pe);
					}
				}
	}
	
	private void countFiles(File folder) {
		File[] files = folder.listFiles();
		for(File file:files)
			if(isCancelled())
				break;
			else
				if(file.isDirectory())
					countFiles(file);
				else
					if(file.getName().substring(file.getName().length()-3).equals("mp3"))
						numFiles++;
					else
						if(m4a && file.getName().substring(file.getName().length()-3).equals("m4a"))
							numFiles++;
						else
							if(wav && file.getName().substring(file.getName().length()-3).equals("wav"))
								numFiles++;
							else
								if(flac && file.getName().substring(file.getName().length()-4).equals("flac"))
									numFiles++;		
	}
}