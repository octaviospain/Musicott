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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.Task;

import com.cedarsoftware.util.io.JsonWriter;
import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.ObservableTrack;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class SaveLibraryTask extends Task<Void> {

	private List<ObservableTrack> list;
	private int numTracks, currentTracks;
	private File tracksFile;

	public SaveLibraryTask() {
		tracksFile = new File("./resources/tracks.json");
		list = MusicLibrary.getInstance().getTracks();
		numTracks = list.size();
		currentTracks = 0;
	}
	
	@Override
	protected Void call() throws Exception {	
		List<Track> trackList = new ArrayList<Track>();
		for(ObservableTrack ot: list) {
			updateProgress(++currentTracks,numTracks);
			trackList.add(Track.trackFromObservableTrack(ot));
		}
		try {
			FileOutputStream fos = new FileOutputStream(tracksFile);				
			JsonWriter jsw = new JsonWriter(fos);
			jsw.write(trackList);
			jsw.close();
			fos.close();
		}
		catch (IOException e) {
			//TODO show error dialog and continue without save library
		}
		return null;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		SceneManager.getInstance().closeImportScene();
	}
}