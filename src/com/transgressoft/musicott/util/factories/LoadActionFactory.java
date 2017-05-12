package com.transgressoft.musicott.util.factories;

import com.google.inject.name.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.load.*;
import javafx.application.*;

import java.util.*;

import static com.transgressoft.musicott.util.guicemodules.LoadActionFactoryModule.*;

/**
 * Factory interface to be used by Guice's dependency injection for creating
 * {@link LoadAction} objects
 *
 * @author Octavio Calleya
 *
 * @version 0.10.1-b
 * @since 0.10.1-b
 */
public interface LoadActionFactory {

    @Named (TRACKS_ACTION)
    TracksLoadAction createTracksAction(List<Track> tracks, int totalTracks, String applicationFolder,
            Application musicottApplication);

    @Named (PLAYLIST_ACTION)
    PlaylistsLoadAction createPlaylistAction(String applicationFolder, Application musicottApplication);

    @Named (WAVEFORMS_ACTION)
    WaveformsLoadAction createWaveformsAction(String applicationFolder, Application musicottApplication);
}