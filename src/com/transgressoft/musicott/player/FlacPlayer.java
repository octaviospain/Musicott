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

package com.transgressoft.musicott.player;

import com.transgressoft.musicott.model.*;

/**
 * @author Octavio Calleya
 */
public class FlacPlayer implements TrackPlayer {

	@Override
	public String getStatus() {
		return "";
	}

	@Override
	public void setTrack(Track track) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void setVolume(double value) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void seek(double seekValue) {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void play() {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void pause() {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void stop() {
		throw new UnsupportedOperationException("Unimplemented");
	}

	@Override
	public void dispose() {
		throw new UnsupportedOperationException("Unimplemented");
	}
}