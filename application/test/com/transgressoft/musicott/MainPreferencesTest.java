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
import com.google.common.io.*;
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import javafx.beans.value.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;

import java.io.*;
import java.util.*;
import java.util.prefs.*;

import static com.transgressoft.musicott.MainPreferences.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith (MockitoExtension.class)
public class MainPreferencesTest {

	@Mock
	TracksLibrary tracksLibraryMock;
	@Mock
	ChangeListener<String> userFolderListener;

	Injector injector;

	private void cleanPreferences() {
		Preferences prefs = Preferences.userNodeForPackage(MainPreferences.class);
		prefs.remove("musicott_folder");
		prefs.remove("import_mp3_flag");
		prefs.remove("import_m4a_flag");
		prefs.remove("import_wav_flag");
		prefs.remove("import_flac_flag");
		prefs.remove("itunes_import_hold_playcount");
		prefs.remove("itunes_import_playlists");
		prefs.remove("itunes_import_policy");
	}

	@BeforeEach
	void beforeEach() {
		doNothing().when(userFolderListener).changed(null, null, null);
		injector = Guice.createInjector(binder -> binder.bind(TracksLibrary.class).toInstance(tracksLibraryMock));
		cleanPreferences();
	}

	@AfterEach
	void afterEachTest() {
		cleanPreferences();
	}

	@Test
	@DisplayName ("Default extensions")
	void defaultExtensions() {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener);

		assertEquals(Sets.newHashSet("mp3"), mainPreferences.getImportFilterExtensions());
	}

	@Test
	@DisplayName ("Prevously setted extensions")
	void previouslySettedExtensions() {
		Preferences prefs = Preferences.userNodeForPackage(MainPreferences.class);
		prefs.putBoolean("import_mp3_flag", false);
		prefs.putBoolean("import_m4a_flag", true);
		prefs.putBoolean("import_wav_flag", true);
		prefs.putBoolean("import_flac_flag", true);
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener);

		assertEquals(Sets.newHashSet("m4a", "wav", "flac"), mainPreferences.getImportFilterExtensions());
	}

	@Test
	@DisplayName ("Set user folder")
	void userFolderMethodTest() throws Exception {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		File newUserFolder = Files.createTempDir();
		mainPreferences.setMusicottUserFolder(newUserFolder.getAbsolutePath());

		assertEquals(newUserFolder.getAbsolutePath(), mainPreferences.getMusicottUserFolder());
		assertTrue(newUserFolder.delete());
	}

	@Test
	@DisplayName ("Set user folder to null")
	void userFolderNull() throws Exception {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		assertThrows(NullPointerException.class, () -> mainPreferences.setMusicottUserFolder(null));
	}

	@Test
	@DisplayName ("Track sequence")
	@SuppressWarnings ("unchecked")
	void trackSequence() {
		when(tracksLibraryMock.getTrack(anyInt()))
				.thenReturn(Optional.empty(),
							Optional.of(mock(Track.class)),
							Optional.empty(),
							Optional.of(mock(Track.class)),
							Optional.empty());
		Preferences prefs = Preferences.userNodeForPackage(MainPreferences.class);
		prefs.putInt("track_sequence", 10);

		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		assertEquals(11, mainPreferences.getTrackSequence());
		assertEquals(13, mainPreferences.getTrackSequence());
		assertEquals(15, mainPreferences.getTrackSequence());
		assertEquals(16, mainPreferences.getTrackSequence());
		assertEquals(17, mainPreferences.getTrackSequence());

		mainPreferences.resetTrackSequence();
		assertEquals(1, mainPreferences.getTrackSequence());
	}

	@Test
	@DisplayName ("Itunes import metadata")
	void itunesImportMetadataTest() {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		mainPreferences.setItunesImportMetadataPolicy(ITUNES_DATA_POLICY);

		assertEquals(ITUNES_DATA_POLICY, mainPreferences.getItunesImportMetadataPolicy());
	}

	@Test
	@DisplayName ("Itunes import hold playcount")
	void itunesImportHoldPlaycountTest() {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		mainPreferences.setItunesImportHoldPlaycount(false);

		assertFalse(mainPreferences.getItunesImportHoldPlaycount());
	}

	@Test
	@DisplayName("Import filter extensions empty")
	void emptyImportFilterExtensionsTest() {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		mainPreferences.setImportFilterExtensions();

		assertEquals(Collections.emptySet(), mainPreferences.getImportFilterExtensions());
	}

	@Test
	@DisplayName("Two import filter extension")
	void oneImportFilterExtensionsTest() {
		MainPreferences mainPreferences = new MainPreferences(tracksLibraryMock, userFolderListener) ;
		mainPreferences.setImportFilterExtensions("mp3", "flac");

		assertEquals(Sets.newHashSet("mp3", "flac"), mainPreferences.getImportFilterExtensions());
	}
}
