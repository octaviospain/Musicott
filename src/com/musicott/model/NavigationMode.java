package com.musicott.model;

/**
 * @author Octavio Calleya
 *
 */
public enum NavigationMode {

	ALL_SONGS_MODE("All songs");

	String name;

	NavigationMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
