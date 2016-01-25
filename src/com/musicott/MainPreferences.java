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

package com.musicott;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.prefs.Preferences;

import static com.musicott.task.ItunesImportTask.HOLD_ITUNES_DATA_POLICY;

/**
 * @author Octavio Calleya
 *
 */
public class MainPreferences {

	private final String MUSICOTT_FOLDER = "musicott_folder";
	private final String TRACK_SEQUENCE = "track_sequence";
	private final String PLAYLIST_SEQUENCE = "playlist_sequence";
	private final String IMPORT_MP3 = "import_mp3_flag";
	private final String IMPORT_M4A = "import_m4a_flag";
	private final String IMPORT_WAV = "import_wav_flag";
	private final String IMPORT_FLAC = "import_flac_flag";
	private final String ITUNES_IMPORT_HOLD_PLAYCOUNT = "itunes_import_hold_playcount";
	private final String ITUNES_IMPORT_PLAYLISTS = "itunes_import_playlists";
	private final String ITUNES_IMPORT_METADATA_POLICY = "itunes_import_policy";
	
	private static MainPreferences instance;
	private Preferences preferences;
	private ExtensionFileFilter extensionsFilter;
	
	private MainPreferences() {
		preferences = Preferences.userNodeForPackage(getClass());
		extensionsFilter = new ExtensionFileFilter();
		if(preferences.getBoolean(IMPORT_MP3, true))
			extensionsFilter.addExtension("mp3");
		if(preferences.getBoolean(IMPORT_M4A, false))
			extensionsFilter.addExtension("m4a");
		if(preferences.getBoolean(IMPORT_WAV, false))
			extensionsFilter.addExtension("wav");
		if(preferences.getBoolean(IMPORT_FLAC, false))
			extensionsFilter.addExtension("flac");	
	}
	
	public static MainPreferences getInstance() {
		if(instance == null)
			instance = new MainPreferences();
		return instance;			
	}
	
	public int getTrackSequence() {
		int sequence = preferences.getInt(TRACK_SEQUENCE, 0);
		preferences.putInt(TRACK_SEQUENCE, ++sequence);
		return sequence;
	}
	
	public int getPlaylistSequence() {
		int sequence = preferences.getInt(PLAYLIST_SEQUENCE, 0);
		preferences.putInt(PLAYLIST_SEQUENCE, ++sequence);
		return sequence;
	}
	
	public boolean setMusicottUserFolder(String path) {
		preferences.put(MUSICOTT_FOLDER, path);
		preferences.putInt(TRACK_SEQUENCE, 0);	// reset the sequences
		preferences.putInt(PLAYLIST_SEQUENCE, 0);
		return new File(path).mkdirs();
	}
	
	public String getMusicottUserFolder() {
		return preferences.get(MUSICOTT_FOLDER, null);
	}
	
	public void setItunesImportMetadataPolicy(int policy) {
		preferences.putInt(ITUNES_IMPORT_METADATA_POLICY, policy);
	}
	
	public int getItunesImportMetadataPolicy() {
		return preferences.getInt(ITUNES_IMPORT_METADATA_POLICY, HOLD_ITUNES_DATA_POLICY);
	}
	
	public void setItunesImportHoldPlaycount(boolean holdPlayCount) {
		preferences.putBoolean(ITUNES_IMPORT_HOLD_PLAYCOUNT, holdPlayCount);
	}
	
	public boolean getItunesImportHoldPlaycount() {
		return preferences.getBoolean(ITUNES_IMPORT_HOLD_PLAYCOUNT, true);
	}
	
	public void setItunesImportPlaylists(boolean importPlaylists) {
		preferences.putBoolean(ITUNES_IMPORT_PLAYLISTS, importPlaylists);
	}
	
	public boolean getItunesImportPlaylists() {
		return preferences.getBoolean(ITUNES_IMPORT_PLAYLISTS, false);
	}	
	
	public void setImportFilterExtensions(String... newImportFilterExtensions) {
		extensionsFilter.setExtensions(newImportFilterExtensions);
		preferences.putBoolean(IMPORT_MP3, extensionsFilter.hasExtension("mp3"));
		preferences.putBoolean(IMPORT_M4A, extensionsFilter.hasExtension("m4a"));
		preferences.putBoolean(IMPORT_WAV, extensionsFilter.hasExtension("wav"));
		preferences.putBoolean(IMPORT_FLAC, extensionsFilter.hasExtension("flac"));
	}
	
	public String[] getImportFilterExtensions() {
		return extensionsFilter.getExtensions();
	}
	
	public ExtensionFileFilter getExtensionsFileFilter() {
		return this.extensionsFilter;
	}
	
	/**
	 * This class implements <code>{@link java.io.FileFilter}</code> to
	 * accept a file with some of the given extensions. If no extensions are given
	 * the file is not accepted. The extensions mus be given without the dot.
	 * 
	 * @author Octavio Calleya
	 *
	 */
	public class ExtensionFileFilter implements FileFilter {
		
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
		
		private void setExtensions(String... extensions) {
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
				int pos = pathname.getName().lastIndexOf(".");
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