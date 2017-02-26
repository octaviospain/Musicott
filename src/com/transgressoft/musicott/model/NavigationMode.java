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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

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
