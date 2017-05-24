package com.transgressoft.musicott.tasks;

import com.google.common.graph.*;
import com.google.common.io.*;
import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tests.*;
import com.transgressoft.musicott.view.*;
import javafx.collections.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;

import java.io.*;

import static com.transgressoft.musicott.model.CommonObject.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith (MockitoExtension.class)
public class SaveMusicLibraryTaskTest {

    ObservableMap<Integer, Track> testTracks = FXCollections.observableHashMap();
    ObservableMap<Integer, float[]> testWaveforms = FXCollections.observableHashMap();
    MutableGraph<Playlist> testPlaylists = GraphBuilder.directed().build();

    String testUserFolder = Files.createTempDir().getAbsolutePath();
    File tracksFile = new File(testUserFolder, TRACKS_FILE.toString());
    File playlistsFile = new File(testUserFolder, PLAYLISTS_FILE.toString());
    File waveformsFile = new File(testUserFolder, WAVEFORMS_FILE.toString());

    @Mock
    TracksLibrary tracksLibraryMock;
    @Mock
    PlaylistsLibrary playlistsLibraryMock;
    @Mock
    WaveformsLibrary waveformsLibraryMock;
    @Mock
    ErrorDialogController errorDialogControllerMock;
    @Mock
    MainPreferences preferencesMock;

    Injector injector;
    SaveMusicLibraryTask task;

    @BeforeEach
    void beforeEach() {
        Track trackMock1 = mock(Track.class);
        trackMock1.setTrackId(1);
        Track trackMock2 = mock(Track.class);
        trackMock2.setTrackId(2);
        testTracks.put(1, trackMock1);
        testTracks.put(2, trackMock2);
        testWaveforms.put(1, new float[]{0.1f, 0.11f});
        testWaveforms.put(2, new float[]{0.2f, 0.22f});
        Playlist playlist1 = mock(Playlist.class);
        playlist1.setName("Playlist1");
        Playlist playlist2 = mock(Playlist.class);
        playlist2.setName("Playlist2");
        testPlaylists.addNode(playlist1);
        testPlaylists.addNode(playlist2);

        when(preferencesMock.getMusicottUserFolder()).thenReturn(testUserFolder);
        when(tracksLibraryMock.getMusicottTracks()).thenReturn(testTracks);
        when(waveformsLibraryMock.getWaveforms()).thenReturn(testWaveforms);
        when(playlistsLibraryMock.getPlaylistsTree()).thenReturn(testPlaylists);
        doNothing().when(errorDialogControllerMock).show(eq("Error saving music library"), any(), any());
        injector = Guice.createInjector(binder -> {
            binder.bind(MainPreferences.class).toInstance(preferencesMock);
            binder.bind(TracksLibrary.class).toInstance(tracksLibraryMock);
            binder.bind(PlaylistsLibrary.class).toInstance(playlistsLibraryMock);
            binder.bind(WaveformsLibrary.class).toInstance(waveformsLibraryMock);
            binder.bind(ErrorDialogController.class).toInstance(errorDialogControllerMock);
        });

        task = injector.getInstance(SaveMusicLibraryTask.class);
        task.setErrorDialog(errorDialogControllerMock);
    }

    @AfterEach
    void afterEach() {
        task.finish();
        if (tracksFile.exists())
            tracksFile.delete();
        if (playlistsFile.exists())
            playlistsFile.delete();
        if (waveformsFile.exists())
            waveformsFile.delete();
        testTracks.clear();
        testWaveforms.clear();
    }

    @Test
    @DisplayName("Tracks serialization")
    void tracksSerialization() throws Exception {
        task.start();

        assertTrue(task.isAlive());

        task.saveMusicLibrary(true, false, false);
        await().untilAsserted(() -> assertTrue(tracksFile.exists()));

        verify(errorDialogControllerMock, times(0)).show(eq("Error saving music library"), any(), any());
    }

    @Test
    @DisplayName ("Waveforms serialization")
    void waveformsSerialization() throws Exception {
        task.start();

        assertTrue(task.isAlive());

        task.saveMusicLibrary(false, true, false);
        await().untilAsserted(() -> assertTrue(waveformsFile.exists()));

        verify(errorDialogControllerMock, times(0)).show(eq("Error saving music library"), any(), any());
    }

    @Test
    @DisplayName ("Playlists serialization")
    void playlistsSerialization() throws Exception {
        task.start();

        assertTrue(task.isAlive());

        task.saveMusicLibrary(false, false, true);
        await().untilAsserted(() -> assertTrue(playlistsFile.exists()));

        verify(errorDialogControllerMock, times(0)).show(eq("Error saving music library"), any(), any());
    }
}