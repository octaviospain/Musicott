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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.error;

/**
 * This enum defines the possible types of collected errors
 * 
 * @author Octavio Calleya
 */
public enum ErrorType {
	
	/**
	 * Error from parsing audio files
	 */
	PARSE,
	
	/**
	 * Error from writing metadata on audio files
	 */
	METADATA,
	
	/**
	 * Common error
	 */
	COMMON,
	
	/**
	 * Fatal errors that force to end the normal execution
	 */
	FATAL
}
