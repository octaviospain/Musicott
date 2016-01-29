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

package com.musicott.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that does some useful operations with files, directories, strings
 * or other operations utilities to be used for the application
 * 
 * @author Octavio Calleya
 *
 */
public class Utils {
	
	/**
	 * Retrieves a {@link List} with at most <tt>maxFiles</tt> files that are in a folder or
	 * any of the subfolders in that folder satisfying a condition.
	 * If <tt>maxFilesRequired</tt> is 0 all the files will be retrieved.
	 * 
	 * @param rootFolder The folder from within to find the files
	 * @param filter The {@link FileFilter} condition
	 * @param maxFilesRequired Maximun number of files in the List. 0 indicates no maximum
	 * @return The list containing all the files
	 * @throws IllegalArgumentException Thrown if <tt>maxFilesRequired</tt> argument is less than zero
	 */
	public static List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) throws IllegalArgumentException {
		List<File> finalFiles = new ArrayList<>();
		if(!Thread.currentThread().isInterrupted()) {
			if(maxFilesRequired < 0)
				throw new IllegalArgumentException("maxFilesRequired argument less than zero");
			if(rootFolder == null || filter == null)
				throw new IllegalArgumentException("folder or filter null");
			if(!rootFolder.exists() || !rootFolder.isDirectory())
				throw new IllegalArgumentException("rootFolder argument is not a directory");
			File[] subFiles = rootFolder.listFiles(filter);
			int remainingFiles = maxFilesRequired;
			if(maxFilesRequired == 0)	// No max = add all files
				finalFiles.addAll(Arrays.asList(subFiles));
			else if(maxFilesRequired < subFiles.length) {	// There are more valid files than the required
					finalFiles.addAll(Arrays.asList(Arrays.copyOfRange(subFiles, 0, maxFilesRequired)));
					remainingFiles -= finalFiles.size();		// Zero files remaining
				}
				else if (subFiles.length > 0) {
						finalFiles.addAll(Arrays.asList(subFiles));	// Add all valid files
						remainingFiles -= finalFiles.size();		// If remainingFiles == 0, end;
					}
			
			if(maxFilesRequired == 0 || remainingFiles > 0) {
				File[] rootSubFolders = rootFolder.listFiles(file -> {return file.isDirectory();});
				int sbFldrsCount = 0;
				while((sbFldrsCount < rootSubFolders.length) && !Thread.currentThread().isInterrupted()) {
					File subFolder = rootSubFolders[sbFldrsCount++];
					List<File> subFolderFiles = getAllFilesInFolder(subFolder, filter, remainingFiles);
					finalFiles.addAll(subFolderFiles);
					if(remainingFiles > 0)
						remainingFiles = maxFilesRequired - finalFiles.size();
					if(maxFilesRequired > 0 && remainingFiles == 0)
						break;
				}
			}
		}
		return finalFiles;
	}
	
	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB
	 * 
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @param numDecimals The number of decimals to be shown after the comma
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> or <tt>numDecimals</tt> are negative
	 */
	public static String byteSizeString(long bytes, int numDecimals) throws IllegalArgumentException {
		if(bytes < 0 || numDecimals < 0)
			throw new IllegalArgumentException("Given bytes or number of decimals can't be less than zero");
		String sizeText;
		String[] bytesUnits = {"KB", "MB", "GB", "TB"};
		long bytesAmount = bytes/1024;
		short binRemainder;
		float decRemainder = 0;
		int unit;
		for(unit = 0; bytesAmount > 1024 || unit < bytesUnits.length; unit++) {
			binRemainder = (short) (bytesAmount%1024);
			decRemainder += (((float) binRemainder)/1024)/((unit+1)*1000);
			bytesAmount /= 1024;
		}
		String remainderStr = "" + decRemainder;
		sizeText = bytesAmount + (numDecimals > 0 ? ","+remainderStr.substring(2, 2+numDecimals) : "") + " "+bytesUnits[unit];
		return sizeText;
	}
}