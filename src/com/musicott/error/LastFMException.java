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

package com.musicott.error;


/**
 * @author Octavio Calleya
 *
 */
public class LastFMException extends Exception {

	private static final long serialVersionUID = 1L;

	public LastFMException() {
		super();
	}
	
	public LastFMException(String msg) {
		super(msg);
	}
	
	public LastFMException(Throwable cause) {
		super(cause);
	}
	
	public LastFMException(String msg, Throwable cause) {
		super(msg,cause);
	}
}