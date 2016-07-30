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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.services;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.lastfm.*;
import javafx.application.*;
import org.slf4j.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Extends from {@link Thread} to perform the connection to the LastFM API
 * service in order to scrobble and updates the user profile.
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class LastFmTask extends Thread {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private static final String FAILED = "failed";

	private Track trackToScrobble;
	private LastFmService lastFmService;
	private Semaphore updateAndScrobbleSemaphore;
	private List<Map<Integer, Track>> tracksToScrobbleLater;

	private ErrorDemon errorDemon = ErrorDemon.getInstance();
	private ServiceDemon serviceDemon = ServiceDemon.getInstance();

	public LastFmTask() {
		super("LastFM Thread");
		lastFmService = new LastFmService();
		updateAndScrobbleSemaphore = new Semaphore(0);
		tracksToScrobbleLater = new ArrayList<>();
	}

	public void updateAndScrobble(Track track) {
		trackToScrobble = track;
		updateAndScrobbleSemaphore.release();
	}

	@Override
	public void run() {
		if (loginToLastFmApi())
			updateAndScrobbleLoop();
	}

	private boolean loginToLastFmApi() {
		boolean loginResult = true;
		if (! lastFmService.isApiConfigurationPresent()) {
			errorDemon.showLastFmErrorDialog("LastFM error", "LastFM API Key or Secret are invalid");
			loginResult = false;
		}

		if (loginResult) {
			LastFmResponse lastFmResponse = lastFmService.getSession();
			if (lastFmResponse.getStatus().equals(FAILED)) {
				handleLastFMError(lastFmResponse.getError());
				loginResult = false;
			}
			else
				serviceDemon.setUsingLastFm(true);
		}
		Platform.runLater(StageDemon.getInstance()::closeIndeterminateProgress);
		return loginResult;
	}

	private void updateAndScrobbleLoop() {
		while (! Thread.currentThread().isInterrupted()) {
			try {
				scrobbleTracksSavedForLater();
				updateAndScrobbleSemaphore.acquire();
				updateNowPlaying();
				scrobble();
			}
			catch (InterruptedException exception) {
				LOG.warn("LastFM thread error: {}", exception);
				errorDemon.showErrorDialog("Error using LastFM", "", exception);
				serviceDemon.setUsingLastFm(false);
				break;
			}
		}
	}

	private void updateNowPlaying() {
		LastFmResponse lastFmResponse = lastFmService.updateNowPlaying(trackToScrobble);
		if (lastFmResponse.getStatus().equals(FAILED))
			handleLastFMError(lastFmResponse.getError());
	}

	private void scrobble() {
		LastFmResponse lastFmResponse = lastFmService.scrobbleTrack(trackToScrobble);
		if (lastFmResponse.getStatus().equals(FAILED))
			handleLastFMError(lastFmResponse.getError());
			addTrackToScrobbleLater(trackToScrobble);
	}

	private void scrobbleTracksSavedForLater() {
		tracksToScrobbleLater.stream().filter(mapBatch -> ! mapBatch.isEmpty())
							 .forEach(lastFmService::scrobbleTrackBatch);
	}

	/**
	 * Puts a track that hasn't been scrobbled successfully into a {@link Map} in order
	 * to scrobble then in a batch. The batch can have a size up to 50 items to be scrobbled
	 * on the LastFM service, so if a batch is fulled, other one is created an added to the list.
	 *
	 * @param track The {@link Track} instance to add
	 *
	 * @see <a href="http://www.last.fm/api/show/track.scrobble">LastFM API documentation</a>
	 */
	private void addTrackToScrobbleLater(Track track) {
		ListIterator<Map<Integer, Track>> trackToScrobbleLaterIterator = tracksToScrobbleLater.listIterator();
		boolean done = false;
		while (trackToScrobbleLaterIterator.hasNext() && ! done) {
			Map<Integer, Track> trackBatch = trackToScrobbleLaterIterator.next();
			if (trackBatch.size() < 50) {
				trackBatch.put((int) System.currentTimeMillis() / 1000, track);
				done = true;
			}
			else if (! trackToScrobbleLaterIterator.hasNext()) {
				Map<Integer, Track> newMapBatch = new HashMap<>();
				newMapBatch.put((int) System.currentTimeMillis() / 1000, track);
				trackToScrobbleLaterIterator.add(newMapBatch);
				done = true;
			}
		}
	}

	private void handleLastFMError(LastFmError error) {
		//TODO Error handling for Last FM error statuses
		String errorTitle;
		String errorMessage;
		switch (error.getCode()) {
			case "4":
				errorTitle = "Authentication Failed";
				errorMessage = "Username or password invalid.";
				break;
			case "8":
				errorTitle = "Operation failed";
				errorMessage = " Something else went wrong.";
				break;
			case "11":
				errorTitle = "Service Offline";
				errorMessage = "This service is temporarily offline. Try again later.";
				break;
			case "26":
				errorTitle = "Suspended API key";
				errorMessage = "Access for your account has been suspended, please contact Last.fm";
				break;
			case "16":
			case "29":
				errorTitle = "Rate limit exceeded";
				errorMessage = "Your IP has made too many requests in a short period.";
				break;
			default:
				errorTitle = "LastFM error " + error.getCode();
				errorMessage = error.getMessage();
		}
		LOG.info("LastFM error: {}", error.getMessage());
		errorDemon.showLastFmErrorDialog(errorTitle, errorMessage);
	}
}
