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

package com.musicott.task;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import com.musicott.model.Track;
import com.musicott.util.Utils;

/**
 * @author Octavio Calleya
 *
 */
public class ParseFolderTask extends ParseTask {
	
	private FileFilter fileFilter;
	private File rootFolder;
	
	public ParseFolderTask(File folder, FileFilter filter) {
		super();
		rootFolder = folder;
		fileFilter = filter;
	}

	@Override
	protected List<Track> call() throws Exception {
		LOG.info("Finding valid files in {}", rootFolder);
		files = Utils.getAllFilesInFolder(rootFolder, fileFilter, 0);
		parseFiles();
		return tracks;
	}
}