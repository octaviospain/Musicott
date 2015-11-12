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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.worldsworstsoftware.itunes.ItunesLibrary;
import com.worldsworstsoftware.itunes.ItunesTrack;

import javafx.concurrent.Task;

/**
 * @author Octavio Calleya
 *
 */
public class ItunesImportTask extends Task<Void> {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	public static final int HOLD_METADATA_POLICY = 0;
	public static final int HOLD_ITUNES_DATA_POLICY = 1;
	
	private SceneManager sc;
	private MusicLibrary ml;
	private List<Track> tracks;
	private ItunesLibrary itunesLibrary;
	private List<ItunesTrack> itunesItems;
	private final String itunesLibraryXMLPath;
	private final int metadataPolicy;
	private boolean importPlaylists, keepPlayCount;

	public ItunesImportTask(String path, int metadataPolicy, boolean importPlaylists, boolean keepPlaycount) {
		itunesLibraryXMLPath = path;
		this.metadataPolicy = metadataPolicy;
		this.importPlaylists = importPlaylists;
		this.keepPlayCount = keepPlaycount;
		tracks = new ArrayList<>();
		itunesItems = new ArrayList<>();
	}

	@Override
	protected Void call() throws Exception {
		parseItunesLibrary();
		if(importPlaylists)
			parsePlaylists();
		return null;
	}
	
	private void parseItunesLibrary() {
		
	}
	
	private void parsePlaylists() {
		// TODO
	}
	
	@Override
	protected void succeeded() {
		super.succeeded();
		updateMessage("Itunes import succeeded");
		ml.getTracks().addAll(tracks);
		sc.getRootController().setStatusProgress(0.0);
		sc.getRootController().setStatusMessage("Itunes import completed");
		LOG.info("Itunes import task completed");
	}
	
	@Override
	protected void cancelled() {
		super.cancelled();
		updateMessage("Itunes import cancelled");
		LOG.info("Itunes import task cancelled");
	}
}