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


import java.io.File;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class TaskPoolManager {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private volatile static TaskPoolManager instance;
	private ParseTask parseTask;
	private Semaphore threadsSemaphore;
	private Queue<Track> tracksToProcessQueue;
	private WaveformTask waveformTask;
	
	private TaskPoolManager() {
		tracksToProcessQueue = new ArrayDeque<>();
		threadsSemaphore = new Semaphore(0);
	}
	
	public static TaskPoolManager getInstance() {
		if(instance == null)
			instance = new TaskPoolManager();
		return instance;
	}

	public void parseFiles(List<File> files, boolean playfinally) {
		if(parseTask == null || parseTask.isDone()) {
			parseTask = new ParseTask(files, playfinally);
			Thread parseThread = new Thread(parseTask, "Parse Files Task");
			parseThread.setDaemon(true);
			parseThread.start();
		}
		else if(parseTask.isRunning())
			parseTask.addFilesToParse(files);
	}
	
	public synchronized void addTrackToProcess(Track track) {
		if(waveformTask == null) {
			waveformTask = new WaveformTask("Waveform task ", threadsSemaphore, this);
			waveformTask.start();
		}
		tracksToProcessQueue.add(track);
		threadsSemaphore.release();
		LOG.debug("Added track {} to waveform process queue", track);
	}
	
	protected synchronized Track getTrackToProcess() {
		return tracksToProcessQueue.poll();
	}
}