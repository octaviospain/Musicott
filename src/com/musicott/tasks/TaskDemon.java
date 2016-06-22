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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott.tasks;

import com.musicott.*;
import com.musicott.model.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author Octavio Calleya
 *
 */
public class TaskDemon {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private volatile static TaskDemon instance;
	private ParseTask parseTask;
	private ItunesImportTask itunesImportTask;
	private Semaphore waveformSemaphore;
	private Queue<Track> tracksToProcessQueue;
	private WaveformTask waveformTask;
	
	private TaskDemon() {
		tracksToProcessQueue = new ArrayDeque<>();
		waveformSemaphore = new Semaphore(0);
	}
	
	public static TaskDemon getInstance() {
		if(instance == null)
			instance = new TaskDemon();
		return instance;
	}
	
	public void parseItunesLibrary(String path) {
		if((itunesImportTask != null && itunesImportTask.isRunning()) || (parseTask != null && parseTask.isRunning()))
			ErrorDemon.getInstance().showErrorDialog("There is already an import task running.");
		else {
			itunesImportTask = new ItunesImportTask(path);
			Thread itunesThread = new Thread(itunesImportTask, "Parse Itunes Task");
			itunesThread.setDaemon(true);
			itunesThread.start();
			LOG.debug("Parsing Itunes Library: {}", path);
		}
	}

	public void parseFiles(List<File> filesToParse, boolean playFinally) {
		if((itunesImportTask != null && itunesImportTask.isRunning()) || (parseTask != null && parseTask.isRunning())) 
			ErrorDemon.getInstance().showErrorDialog("There is already an import task running.");
		else {
			parseTask = new ParseTask(filesToParse, playFinally);
			Thread parseThread = new Thread(parseTask, "Parse Files Task");
			parseThread.setDaemon(true);
			parseThread.start();
		}
	}
	
	public synchronized void addTrackToProcess(Track track) {
		if(waveformTask == null) {
			waveformTask = new WaveformTask("Waveform task ", waveformSemaphore, this);
			waveformTask.start();
		}
		tracksToProcessQueue.add(track);
		waveformSemaphore.release();
		LOG.debug("Added track {} to waveform process queue", track);
	}
	
	protected synchronized Track getTrackToProcess() {
		return tracksToProcessQueue.poll();
	}
}