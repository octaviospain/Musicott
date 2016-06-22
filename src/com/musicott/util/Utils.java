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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott.util;

import java.io.*;
import java.util.*;

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
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> is negative
	 */
	public static String byteSizeString(long bytes) throws IllegalArgumentException {
		if(bytes < 0)
			throw new IllegalArgumentException("Given bytes can't be less than zero");
		String sizeText;
		String[] bytesUnits = {"B", "KB", "MB", "GB", "TB"};
		long bytesAmount = bytes;
		short binRemainder;
		float decRemainder = 0;
		int u;
		for(u = 0; bytesAmount > 1024 && u < bytesUnits.length; u++) {
			bytesAmount /= 1024;
			binRemainder = (short) (bytesAmount%1024);
			decRemainder += (((float) binRemainder)/1024);
		}
		String remainderStr = ("" + decRemainder).substring(2);
		sizeText = bytesAmount + (remainderStr.equals("0") ? "" : ","+remainderStr) + " " + bytesUnits[u];
		return sizeText;
	}
	
	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB, limiting the number
	 * of decimals, if there are any
	 * 
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @param numDecimals The maximum number of decimals to be shown after the comma
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> or <tt>numDecimals</tt> are negative
	 */
	public static String byteSizeString(long bytes, int numDecimals) throws IllegalArgumentException {
		if(numDecimals < 0)
			throw new IllegalArgumentException("Given number of decimals can't be less than zero");
		String byteSizeString = byteSizeString(bytes);
		int pos = byteSizeString.lastIndexOf(",");
		int unitPos = byteSizeString.lastIndexOf(" ");
		if(pos != -1 && numDecimals > 0) {
			String abs = byteSizeString.substring(0, pos+1);
			int rem = Integer.valueOf(byteSizeString.substring(pos+1, unitPos));
			short numDigits = (short) (""+rem).length();
			int remainderIndex = numDecimals < numDigits ? numDecimals : numDigits;
			int magnitudeEsc = (int)Math.pow(10, (numDigits-remainderIndex));
			int boundedRemainder = rem/magnitudeEsc; 
			int remainderRem = rem%magnitudeEsc;
			if(boundedRemainder != 0) {
				int a = 5*magnitudeEsc/10;
				if(remainderRem > a)
					boundedRemainder ++;
				byteSizeString = abs + (boundedRemainder%10 == 0 ? boundedRemainder/10 : boundedRemainder) + byteSizeString.substring(unitPos);
			}
		}
		return byteSizeString;
	}
}