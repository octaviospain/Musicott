package com.transgressoft.musicott.model;

/**
 * Class enum that represents a showing mode of the application
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public enum NavigationMode {

	/**
	 * All tracks in Musicott are shown on the table
	 */
	ALL_TRACKS("All songs"),

	/**
	 * The tracks of a selected {@link Playlist} are shown on the table
	 */
	PLAYLIST("Playlist");

	String name;

	NavigationMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
