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

import com.musicott.*;
import javafx.scene.image.*;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

/**
 * Class that does some useful operations with files, directories, strings
 * or other operations utilities to be used for the application
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class Utils {

	/**
	 * Private constructor to hide the implicit public one.
	 */
	private Utils() {}

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
	public static List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) {
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
						remainingFiles -= finalFiles.size();
					}

			if(maxFilesRequired == 0 || remainingFiles > 0) {
				File[] rootSubFolders = rootFolder.listFiles(File::isDirectory);
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
	public static String byteSizeString(long bytes) {
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
			binRemainder = (short) (bytesAmount % 1024);
			decRemainder += Float.valueOf((float) binRemainder / 1024);
		}
		String remainderStr = String.format("%f", decRemainder).substring(2);
		sizeText = bytesAmount + ("0".equals(remainderStr) ? "" : "," + remainderStr) + " " + bytesUnits[u];
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
	public static String byteSizeString(long bytes, int numDecimals) {
		if(numDecimals < 0)
			throw new IllegalArgumentException("Given number of decimals can't be less than zero");

		String byteSizeString = byteSizeString(bytes);
		String decimalSharps = "";
		for(int n = 0; n < numDecimals; n++)
			decimalSharps += "#";
		DecimalFormat decimalFormat = new DecimalFormat("#." + decimalSharps);
		decimalFormat.setRoundingMode(RoundingMode.CEILING);

		int unitPos = byteSizeString.lastIndexOf(' ');
		String stringValue = byteSizeString.substring(0, unitPos);
		stringValue = stringValue.replace(',', '.');
		float floatValue = Float.parseFloat(stringValue);
		byteSizeString = decimalFormat.format(floatValue) + byteSizeString.substring(unitPos);
		return byteSizeString;
	}

	/**
	 * Returns an {@link Image} from an image {@link File}.
	 *
	 * @param imageFile The image.
	 * @return An {@link Optional} with the <tt>image</tt> or not.
	 */
	public static Optional<Image> getImageFromFile(File imageFile) {
		Optional<Image> optionalImage = Optional.empty();
		try {
			byte[] coverBytes = Files.readAllBytes(Paths.get(imageFile.getPath()));
			optionalImage = Optional.of(new Image(new ByteArrayInputStream(coverBytes)));
		} catch (IOException exception) {
			ErrorDemon.getInstance().showErrorDialog("Error getting Image from image file", "", exception);
		}
		return optionalImage;
	}

	/**
	 * This class implements <code>{@link java.io.FileFilter}</code> to
	 * accept a file with some of the given extensions. If no extensions are given
	 * the file is not accepted. The extensions must be given without the dot.
	 *
	 * @author Octavio Calleya
	 */
	public static class ExtensionFileFilter implements FileFilter {

		private String[] extensions;
		private int numExtensions;

		public ExtensionFileFilter(String... extensions) {
			this.extensions = extensions;
			numExtensions = extensions.length;
		}

		public ExtensionFileFilter() {
			extensions = new String[] {};
			numExtensions = 0;
		}

		public void addExtension(String ext) {
			boolean contains = false;
			for(String e: extensions)
				if(e != null && ext.equals(e))
					contains = true;
			if(!contains) {
				ensureArrayLength();
				extensions[numExtensions++] = ext;
			}
		}

		public void removeExtension(String ext) {
			for(int i=0; i<extensions.length; i++)
				if(extensions[i].equals(ext)) {
					extensions[i] = null;
					numExtensions--;
				}
			extensions = Arrays.copyOf(extensions, numExtensions);
		}

		public boolean hasExtension(String ext) {
			for(String e: extensions)
				if(ext.equals(e))
					return true;
			return false;
		}

		public void setExtensions(String... extensions) {
			if(extensions == null)
				this.extensions = new String[] {};
			else
				this.extensions = extensions;
			numExtensions = this.extensions.length;
		}

		public String[] getExtensions() {
			return extensions;
		}

		private void ensureArrayLength() {
			if(numExtensions == extensions.length)
				extensions = Arrays.copyOf(extensions, numExtensions == 0 ? 1 : 2*numExtensions);

		}

		@Override
		public boolean accept(File pathname) {
			boolean res = false;
			if(!pathname.isDirectory() && !pathname.isHidden()) {
				int pos = pathname.getName().lastIndexOf('.');
				if(pos != -1) {
					String extension = pathname.getName().substring(pos+1);
					for(String requiredExtension: extensions)
						if(extension.equals(requiredExtension))
							res = true;
				}
			}
			return res;
		}
	}
}
