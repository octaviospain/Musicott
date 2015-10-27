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

import java.io.File;

import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class ParseException extends Exception{

	public ParseException() {
		super();
	}
	
	public ParseException(String msg) {
		super(msg);
	}
	
	public ParseException(String msg, File f) {
		super(msg+" in "+f.getName());
	}
	
	public ParseException(Throwable cause) {
		super(cause);
	}
	
	public ParseException(String msg, Throwable cause, File f) {
		super(msg+" in "+f.getName(), cause);
	}
	
	public ParseException(String msg, Throwable cause, Track t) {
		super(msg+" in "+t, cause);
	}
}