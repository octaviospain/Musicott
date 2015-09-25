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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cedarsoftware.util.io.JsonReader;
import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.util.ObservableListWrapperCreator;
import com.musicott.view.RootLayoutController;
import com.sun.javafx.collections.ObservableListWrapper;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * @author Octavio Calleya
 *
 */
public class LoadLibraryTask extends Task<Void>{
	
	private List<Track> list;
	private Map<Track,float[]> waveformsMap;
	private File tracksFile, waveformsFile;
	private RootLayoutController rootLayoutController;

	public LoadLibraryTask() {
		tracksFile = new File("./resources/tracks.json");
		waveformsFile = new File("./resources/waveforms.json");
		rootLayoutController = SceneManager.getInstance().getRootController();
	}
	
	@Override
	protected Void call() {
		FileInputStream fis;
		JsonReader jsr;
		if(tracksFile.exists()) {
			try {
				fis = new FileInputStream(tracksFile);
				jsr = new JsonReader(fis);
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
					t.getDateModifiedProperty().setValue(t.getDateModified());
				}
			}
			catch(IOException e) {
				ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
				ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
			}
		}	
		if(waveformsFile.exists()) {
			try {
				fis = new FileInputStream(waveformsFile);
				jsr = new JsonReader(fis);
				waveformsMap = (Map<Track,float[]>) jsr.readObject();
				jsr.close();
				fis.close();
			}
			catch(IOException e) {
				Platform.runLater(() -> {
					ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
					ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
				});
			}
		}
		return null;
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		rootLayoutController.addTracks(list);
		if(waveformsMap == null)
			waveformsMap = new HashMap<Track,float[]>();
		MusicLibrary.getInstance().setWaveforms(waveformsMap);
	}
}