package com.musicott.util;

/**
 * This exception is thrown if an attempt to update an audio file with the
 * information of a {@link com.musicott.model.Track} instance was unsuccessful.
 *
 * @author Octavio Calleya
 * @version 0.9
 * @since 0.9
 */
public class TrackUpdateException extends Exception {

	public TrackUpdateException() {
		super();
	}

	public TrackUpdateException(String message) {
		super(message);
	}

	public TrackUpdateException(String message, Throwable cause) {
		super(message, cause);
	}
}