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
import com.musicott.model.Track;
import com.musicott.task.parser.AudioFileParser;

/**
 * @author Octavio Calleya
 *
 */
public class OpenTask extends Task<List<Track>> {

	private List<Track> list;
	private List<File> files;
	private int numFiles;
	
	public OpenTask(List<File> files) {
		this.files = files;
		list = new ArrayList<Track>();
		numFiles = list.size();
	}
	
	@Override
	protected List<Track> call() {
		int i = 0;
		for(File file:files)
			if(isCancelled())
				break;
			else {
				updateProgress(++i, numFiles);
				Track currentTrack = AudioFileParser.parseAudioFile(file);
				if(currentTrack != null) {
					list.add(currentTrack);
				}
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