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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott;

import com.google.common.collect.*;
import org.junit.jupiter.api.*;
import org.junit.platform.runner.*;
import org.junit.runner.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.tasks.parse.ItunesParseTask.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
@RunWith (JUnitPlatform.class)
public class MainPreferencesTest {

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