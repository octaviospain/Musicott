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

import com.google.common.collect.*;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.*;
import org.junit.runner.*;

import java.io.*;
import java.util.*;
import java.util.prefs.*;

import static com.transgressoft.musicott.tasks.ItunesImportTask.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
@RunWith (JUnitPlatform.class)
public class MainPreferencesTest {

	static final String IMPORT_MP3 = "import_mp3_flag";
	static final String IMPORT_M4A = "import_m4a_flag";
	static final String IMPORT_WAV = "import_wav_flag";
	static final String IMPORT_FLAC = "import_flac_flag";

	@Test
	@DisplayName ("Creation with previous extensions already saved")
	void creationWithPreviousConfig() throws Exception {
		Preferences preferences = Preferences.userNodeForPackage(MainPreferences.class);
		preferences.putBoolean(IMPORT_FLAC, true);
		preferences.putBoolean(IMPORT_WAV, true);
		preferences.putBoolean(IMPORT_M4A, true);
		preferences.putBoolean(IMPORT_MP3, true);
		MainPreferences mainPreferences = MainPreferences.getInstance();
		assertEquals(Sets.newHashSet("m4a", "wav", "flac", "mp3"), mainPreferences.getImportFilterExtensions());
	}

	@Test
	@DisplayName ("User folder")
	void userFolderMethodTest() {
		MainPreferences mainPreferences = MainPreferences.getInstance();
		String sep = File.separator;
		String userHome = System.getProperty("user.home");
		String newUserFolder = userHome + sep + "Musicott";
		File newUserFolderFile = new File(newUserFolder);
		mainPreferences.setMusicottUserFolder(newUserFolder);

		assertAll(() -> assertEquals(newUserFolder, mainPreferences.getMusicottUserFolder()),
				  () -> assertEquals(1, mainPreferences.getTrackSequence()),
				  () -> assertTrue(newUserFolderFile.exists()), () -> assertTrue(newUserFolderFile.delete()));
	}

	@Test
	@DisplayName ("Itunes import metadata")
	void itunesImportMetadataTest() {
		MainPreferences mainPreferences = MainPreferences.getInstance();
		mainPreferences.setItunesImportMetadataPolicy(METADATA_POLICY);

		assertEquals(METADATA_POLICY, mainPreferences.getItunesImportMetadataPolicy());
	}

	@Test
	@DisplayName ("Itunes import hold playcount")
	void itunesImportHoldPlaycountTest() {
		MainPreferences mainPreferences = MainPreferences.getInstance();
		mainPreferences.setItunesImportHoldPlaycount(false);

		assertFalse(mainPreferences.getItunesImportHoldPlaycount());
	}

	@Test
	@DisplayName("Itunes import playlists")
	void itunesImportPlaylistsTest() {
		MainPreferences mainPreferences = MainPreferences.getInstance();
		mainPreferences.setItunesImportPlaylists(true);

		assertTrue(mainPreferences.getItunesImportPlaylists());
	}

	@Test
	@DisplayName("Import filter extensions empty")
	void emptyImportFilterExtensionsTest() {
		MainPreferences mainPreferences = MainPreferences.getInstance();
		mainPreferences.setImportFilterExtensions();

		assertEquals(Collections.emptySet(), mainPreferences.getImportFilterExtensions());
	}

	@Test
	@DisplayName("One import filter extension")
	void oneImportFilterExtensionsTest() {
		MainPreferences mainPreferences = MainPreferences.getInstance();
		mainPreferences.setImportFilterExtensions("mp3");

		assertEquals(Sets.newHashSet("mp3"), mainPreferences.getImportFilterExtensions());
	}
}