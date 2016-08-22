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

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.tasks.ItunesImportTask.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Octavio Calleya
 */
public class MainPreferencesTest {

	static MainPreferences mainPreferences;

	@BeforeAll
	static void beforeAll() {
		mainPreferences = MainPreferences.getInstance();
	}

	@Test
	@DisplayName ("User folder")
	void userFolderMethodTest() {
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
		mainPreferences.setItunesImportMetadataPolicy(METADATA_POLICY);

		assertEquals(METADATA_POLICY, mainPreferences.getItunesImportMetadataPolicy());
	}

	@Test
	@DisplayName ("Itunes import hold playcount")
	void itunesImportHoldPlaycountTest() {
		mainPreferences.setItunesImportHoldPlaycount(false);

		assertFalse(mainPreferences.getItunesImportHoldPlaycount());
	}

	@Test
	@DisplayName("Itunes import playlists")
	void itunesImportPlaylistsTest() {
		mainPreferences.setItunesImportPlaylists(true);

		assertTrue(mainPreferences.getItunesImportPlaylists());
	}

	@Test
	@DisplayName("Import filter extensions empty")
	void emptyImportFilterExtensionsTest() {
		mainPreferences.setImportFilterExtensions();

		assertEquals(Collections.emptySet(), mainPreferences.getImportFilterExtensions());
	}

	@Test
	@DisplayName("One import filter extension")
	void oneImportFilterExtensionsTest() {
		mainPreferences.setImportFilterExtensions("mp3");

		assertEquals(Sets.newHashSet("mp3"), mainPreferences.getImportFilterExtensions());
	}
}