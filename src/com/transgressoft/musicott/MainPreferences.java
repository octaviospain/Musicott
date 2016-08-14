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

package com.transgressoft.musicott;

import java.io.*;
import java.util.*;
import java.util.prefs.*;

import static com.transgressoft.musicott.tasks.ItunesImportTask.*;

/**
 * Singleton class that isolates some user preferences, such as the application folder
 * or the the iTunes import options, using the java predefined class {@link Preferences}.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class MainPreferences {

	/**
	 * The path where the application files will be stored
	 */
	private static final String MUSICOTT_FOLDER = "musicott_folder";

	/**
	 * The sequence number of the keys of the {@link com.transgressoft.musicott.model.Track} map
	 */
	private static final String TRACK_SEQUENCE = "track_sequence";

	/**
	 * The sequence number of the keys of the {@link com.transgressoft.musicott.model.Playlist} map
	 */
	private static final String PLAYLIST_SEQUENCE = "playlist_sequence";

	private static final String IMPORT_MP3 = "import_mp3_flag";
	private static final String IMPORT_M4A = "import_m4a_flag";
	private static final String IMPORT_WAV = "import_wav_flag";
	private static final String IMPORT_FLAC = "import_flac_flag";

	/**
	 * Flag that indicates if the play count must be kept when importing
	 * from iTunes, instead of reset them to 0 plays.
	 */
	private static final String ITUNES_IMPORT_HOLD_PLAYCOUNT = "itunes_import_hold_playcount";

	/**
	 * Flag that indicates of the iTunes playlists must be imported
	 * too when importing from a iTunes library.
	 */
	private static final String ITUNES_IMPORT_PLAYLISTS = "itunes_import_playlists";

	/**
	 * The flag to choose between parse the metadata of the imported files,
	 * or the iTunes data saved in the library, when importing from a iTunes library.
	 */
	private static final String ITUNES_IMPORT_METADATA_POLICY = "itunes_import_policy";
	private static final String[] IMPORT_EXTENSIONS = {"mp3", "m4a", "wav", "flac"};

	private static MainPreferences instance;
	private Preferences preferences;
	private Set<String> importExtensions;

	/**
	 * Private constructor of the class to be called from {@link #getInstance()}.
	 * By default, if the application is used in the first time, the only valid
	 * extension when importing files is <tt>*.mp3</tt>.
	 */
	private MainPreferences() {
		preferences = Preferences.userNodeForPackage(getClass());
		importExtensions = new HashSet<>();
		importExtensions.addAll(Arrays.asList(IMPORT_EXTENSIONS));
		if (preferences.getBoolean(IMPORT_MP3, true))
			importExtensions.add("mp3");
		else
			importExtensions.remove("mp3");
		if (preferences.getBoolean(IMPORT_M4A, false))
			importExtensions.add("m4a");
		else
			importExtensions.remove("m4a");
		if (preferences.getBoolean(IMPORT_WAV, false))
			importExtensions.add("wav");
		else
			importExtensions.remove("wav");
		if (preferences.getBoolean(IMPORT_FLAC, false))
			importExtensions.add("flac");
		else
			importExtensions.remove("flac");
	}

	public static MainPreferences getInstance() {
		if (instance == null)
			instance = new MainPreferences();
		return instance;
	}

	/**
	 * Returns 0 if the application is used in the first time, that is,
	 * if there is no record for the track sequence in the class {@link Preferences};
	 * or the next integer to use for the {@link com.transgressoft.musicott.model.Track} map
	 *
	 * @return The next integer to use for the track map
	 */
	public int getTrackSequence() {
		int sequence = preferences.getInt(TRACK_SEQUENCE, 0);
		preferences.putInt(TRACK_SEQUENCE, ++ sequence);
		return sequence;
	}

	/**
	 * Sets the application folder path and resets the track and play list sequences
	 *
	 * @param path The path to the application folder
	 *
	 * @return <tt>true</tt> if the creation of the directory was successfull, <tt>false</tt> otherwise
	 */
	public boolean setMusicottUserFolder(String path) {
		preferences.put(MUSICOTT_FOLDER, path);
		preferences.putInt(TRACK_SEQUENCE, 0);
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
		return preferences.getInt(ITUNES_IMPORT_METADATA_POLICY, ITUNES_DATA_POLICY);
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
		importExtensions.clear();
		importExtensions.addAll(Arrays.asList(newImportFilterExtensions));
		preferences.putBoolean(IMPORT_MP3, importExtensions.contains("mp3"));
		preferences.putBoolean(IMPORT_M4A, importExtensions.contains("m4a"));
		preferences.putBoolean(IMPORT_WAV, importExtensions.contains("wav"));
		preferences.putBoolean(IMPORT_FLAC, importExtensions.contains("flac"));
	}

	public Set<String> getImportFilterExtensions() {
		return importExtensions;
	}
}
