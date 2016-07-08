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

package com.musicott.util;

import com.worldsworstsoftware.itunes.parser.logging.*;
import com.worldsworstsoftware.logging.*;
import org.slf4j.*;

/**
 * Class logger needed by iTunesUtilities library
 * to perform the itunes library parse.
 * 
 * @author Octavio Calleya
 * @version 0.9
 * @see <a href="https://github.com/codercowboy/iTunesUtilities">ItunesUtilities</a>
 */
public class ItunesParserLogger implements StatusUpdateLogger, ParserStatusUpdateLogger {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	@Override
	public int getPlaylistParseUpdateFrequency() {
		return UPDATE_FREQUENCY_NEVER;
	}

	@Override
	public int getTrackParseUpdateFrequency() {
		return UPDATE_FREQUENCY_NEVER;
	}

	@Override
	public void debug(String arg0) {
		LOG.debug(arg0);
	}

	@Override
	public void error(String arg0, Exception arg1, boolean arg2) {
		LOG.warn((arg2 ? "" : "NON ") + "RECOVERABLE ERROR: " + arg0 + ": " + arg1.getMessage());
	}

	@Override
	public void fatal(String arg0, Exception arg1, boolean arg2) {
		LOG.error((arg2 ? "" : "NON ") + "RECOVERABLE FATAL ERROR: " + arg0 + ": " + arg1.getMessage());
	}

	@Override
	public void statusUpdate(int arg0, String arg1) {
		LOG.info("#" + arg0 + ": " + arg1);
	}

	@Override
	public void warn(String arg0, Exception arg1, boolean arg2) {
		error(arg0, arg1, arg2);
	}
}