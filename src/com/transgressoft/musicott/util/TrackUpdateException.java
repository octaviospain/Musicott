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

package com.transgressoft.musicott.util;

/**
 * This exception is thrown if an attempt to update an audio file with the
 * information of a {@link com.transgressoft.musicott.model.Track} instance was unsuccessful.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.9
 */
public class TrackUpdateException extends Exception {

    public TrackUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
