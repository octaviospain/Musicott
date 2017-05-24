package com.transgressoft.musicott.util.guice.factories;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.load.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import javafx.application.*;

import java.util.*;

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

    @TracksAction
    TracksLoadAction createTracksAction(List<Track> tracks, int totalTracks, String applicationFolder,
            Application musicottApplication);

    @PlaylistAction
    PlaylistsLoadAction createPlaylistAction(String applicationFolder, Application musicottApplication);

    @WaveformsAction
    WaveformsLoadAction createWaveformsAction(String applicationFolder, Application musicottApplication);
}
