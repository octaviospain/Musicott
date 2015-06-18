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
public class OpenTask extends Task<List<Track>> {

	private List<Track> list;
	private List<File> files;
	
	public OpenTask(List<File> files) {
		this.files = files;
		list = new ArrayList<Track>();
	}
	
	@Override
	protected List<Track> call() {
		int i = 0;
		for(File file:files)
			if(isCancelled())
				break;
			else
				try {
					if(file.getName().substring(file.getName().length()-3).equals("mp3")) {
						updateProgress(++i, files.size());
						list.add(Mp3Parser.parseMp3File(file));
					}
					else
						if(file.getName().substring(file.getName().length()-3).equals("m4a")) {
							updateProgress(++i, files.size());
							//TODO M4aParser
						}
						else
							if(file.getName().substring(file.getName().length()-3).equals("wav")) {
								updateProgress(++i, files.size());
								//TODO WavParser
							}
							else
								if(file.getName().substring(file.getName().length()-4).equals("flac")) {
									updateProgress(++i, files.size());
									//TODO FlacParser
								}
				} catch (Exception e) {
					ParseException pe = new ParseException("Parsing Error", e, file);
					ErrorHandler.getInstance().addParseException(pe);
				}
		if(!isCancelled())
			SceneManager.getInstance().getRootController().addTracks(list);
		return list;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		SceneManager.getInstance().closeImportScene();
	}
}