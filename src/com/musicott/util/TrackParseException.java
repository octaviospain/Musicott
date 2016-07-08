package com.musicott.util;

/**
 * This exception is thrown if an attempt to parse a file to create
 * a {@link com.musicott.model.Track} instance was unsuccessful.
 *
 * @author Octavio Calleya
 * @version 0.9
 * @since 0.9
 */
public class TrackParseException extends Exception {

	public TrackParseException() {
		super();
	}

	public TrackParseException(String message) {
		super(message);
	}

	public TrackParseException(String message, Throwable cause) {
		super(message, cause);
	}
}