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
import java.io.FileInputStream;
import java.util.List;

import com.cedarsoftware.util.io.JsonReader;
import com.musicott.SceneManager;
import com.musicott.model.ObservableTrack;
import com.musicott.model.Track;
import com.musicott.view.RootLayoutController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * @author Octavio Calleya
 *
 */
public class LoadLibraryTask extends Task<Void>{
	
	private ObservableList<ObservableTrack> list;
	private int numTracks, currentTracks;
	private String dataPath = "./resources/";
	private File tracksFile;
	private RootLayoutController rootLayoutController;

	public LoadLibraryTask() {
		currentTracks = 0;
		tracksFile = new File(dataPath+"tracks.json");
		list = FXCollections.observableArrayList();
		rootLayoutController = SceneManager.getInstance().getRootController();
	}
	
	@Override
	protected Void call() throws Exception {
		if(tracksFile.exists()) {
			FileInputStream fis = new FileInputStream(tracksFile);
			JsonReader jsr = new JsonReader(fis);
			List<Track> trackList = (List<Track>) jsr.readObject();
			numTracks = trackList.size();
			jsr.close();
			fis.close();
			for(Track t: trackList) {
				updateProgress(currentTracks++,numTracks);
				list.add(ObservableTrack.observableTrackFromTrack(t));
			}
		}		
		return null;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		rootLayoutController.addTracks(list);
		SceneManager.getInstance().closeImportScene();
	}
}