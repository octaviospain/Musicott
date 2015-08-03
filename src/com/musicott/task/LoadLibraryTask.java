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
import java.io.IOException;
import java.util.List;

import com.cedarsoftware.util.io.JsonReader;
import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.util.ObservableListWrapperCreator;
import com.musicott.view.RootLayoutController;
import com.sun.javafx.collections.ObservableListWrapper;

import javafx.concurrent.Task;

/**
 * @author Octavio Calleya
 *
 */
public class LoadLibraryTask extends Task<Void>{
	
	private List<Track> list;
	private File tracksFile;
	private RootLayoutController rootLayoutController;

	public LoadLibraryTask() {
		tracksFile = new File("./resources/tracks.json");
		rootLayoutController = SceneManager.getInstance().getRootController();
	}
	
	@Override
	protected Void call() throws IOException {
		if(tracksFile.exists()) {
			FileInputStream fis = new FileInputStream(tracksFile);
			JsonReader jsr = new JsonReader(fis);
			JsonReader.assignInstantiator(ObservableListWrapper.class, new ObservableListWrapperCreator());
			list = (List<Track>) jsr.readObject();
			jsr.close();
			fis.close();
			
			for(Track t: list) {
				t.getNameProperty().setValue(t.getName());
				t.getArtistProperty().setValue(t.getArtist());
				t.getAlbumProperty().setValue(t.getAlbum());
				t.getGenreProperty().setValue(t.getGenre());
				t.getCommentsProperty().setValue(t.getComments());
				t.getAlbumArtistProperty().setValue(t.getAlbumArtist());
				t.getLabelProperty().setValue(t.getLabel());
				t.getTrackNumberProperty().setValue(t.getTrackNumber());
				t.getYearProperty().setValue(t.getYear());
				t.getDiscNumberProperty().setValue(t.getDiscNumber());
				t.getBpmProperty().setValue(t.getBpm());
				t.getHasCoverProperty().setValue(t.getHasCover());
			}
		}		
		return null;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		if(list != null)
			rootLayoutController.addTracks(list);
	}
}