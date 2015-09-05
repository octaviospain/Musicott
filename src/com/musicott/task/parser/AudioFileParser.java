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

package com.musicott.task.parser;

import java.io.File;
import java.io.IOException;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.error.ParseException;
import com.musicott.error.WriteMetadataException;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class AudioFileParser {

	public static Track parseAudioFile(File file, boolean acceptM4a, boolean acceptWav, boolean acceptFlac) {
		int pos= file.getName().lastIndexOf(".");
		String format = file.getName().substring(pos + 1);
		Track track = null;
		try {
			switch(format) {
				case "mp3":
					track = Mp3Parser.parseMp3File(file);
					break;
				case "m4a":
					if(acceptM4a)
						track = M4aParser.parseM4aFile(file);
					break;
				case "wav":
					if(acceptWav)
						track = WavParser.parseWavFile(file);
					break;
				case "flac":
					if(acceptFlac)
						track = FlacParser.parseFlacFile(file);
					 break;
			}
		}
		catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException |
			   WriteMetadataException | UnsupportedTagException | InvalidDataException | ParseException e) {
			ParseException pe = new ParseException("Parsing Error", e, file);
			ErrorHandler.getInstance().addError(pe, ErrorType.PARSE);
		}
		return track;
	}
}