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

package com.transgressoft.musicott.tasks;

import com.google.common.collect.*;
import com.google.inject.*;
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.collections.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.util.AbstractMap.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * Class that extends from {@link Thread} that performs the operation of
 * updating the metadata of the audio files.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class UpdateMusicLibraryTask extends Thread {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    
    private final AlbumsLibrary albumsLibrary;
    private final ArtistsLibrary artistsLibrary;
    private final StageDemon stageDemon;
    private final TaskDemon taskDemon;
    private final ErrorDemon errorDemon;
    private final List<Track> tracks;
    private final Set<String> changedAlbums;
    private final Optional<String> newAlbum;

    private CopyOption[] options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};
    private List<String> updateErrors = new ArrayList<>();

    @Inject
    public UpdateMusicLibraryTask(AlbumsLibrary albumsLibrary, ArtistsLibrary artistsLibrary, StageDemon stageDemon,
            TaskDemon taskDemon, ErrorDemon errorDemon, @Assisted List<Track> tracks, @Assisted Set<String> changedAlbums,
            @Assisted Optional<String> newAlbum) {
        this.albumsLibrary = albumsLibrary;
        this.artistsLibrary = artistsLibrary;
        this.stageDemon = stageDemon;
        this.taskDemon = taskDemon;
        this.errorDemon = errorDemon;
        this.tracks = tracks;
        this.changedAlbums = changedAlbums;
        this.newAlbum = newAlbum;
    }

    @Override
    public void run() {
        updateMusicLibraryTracks();
        updateMusicLibraryAlbums();
        stageDemon.getRootController().updateShowingTrackSets();
        taskDemon.saveLibrary(true, false, false);
        if (! updateErrors.isEmpty())
            errorDemon.showExpandableErrorsDialog("Errors writing metadata on some tracks", null, updateErrors);
    }

    private void updateMusicLibraryTracks() {
        tracks.forEach(track -> {
            updateArtistsInvolved(track);
            if (track.isInDisk())
                updateFileMetadata(track);
        });
    }

    private void updateMusicLibraryAlbums() {
        newAlbum.ifPresent(album -> {
            List<Entry<Integer, Track>> trackEntries = tracks.stream()
                                                             .map(track -> new SimpleEntry<>(track.getTrackId(), track))
                                                             .collect(Collectors.toList());
            albumsLibrary.updateTrackAlbums(trackEntries, changedAlbums, album);
        });
    }

    private void updateArtistsInvolved(Track track) {
        Set<String> oldArtistsInvolved = track.getArtistsInvolved();
        Set<String> newArtistsInvolved = Utils.getArtistsInvolvedInTrack(track);
        Set<String> removedArtists = Sets.difference(oldArtistsInvolved, newArtistsInvolved).immutableCopy();
        Set<String> addedArtists = Sets.difference(newArtistsInvolved, oldArtistsInvolved).immutableCopy();
        track.setArtistsInvolved(FXCollections.observableSet(newArtistsInvolved));
        artistsLibrary.updateArtistsInvolvedInTrack(track, removedArtists, addedArtists);
    }

    private void updateFileMetadata(Track track) {
        File backup = makeBackup(track);
        try {
            track.writeMetadata();
            deleteBackup(track, backup);
            String filePath = track.getFileFolder() + File.separator + track.getFileName();
            LOG.debug("Updated (or not) metadata of {}", filePath);
        }
        catch (TrackUpdateException exception) {
            if (backup != null)
                restoreBackup(track, backup);
            updateErrors.add(exception.getMessage() + ": " + exception.getCause().getMessage());
        }
    }

    private File makeBackup(Track track) {
        File original = new File(track.getFileFolder(), track.getFileName());
        File backup = null;
        try {
            backup = File.createTempFile(track.getFileName(), "");
            Files.copy(original.toPath(), backup.toPath(), options);
        }
        catch (IOException exception) {
            LOG.error("Error creating the backup file: ", exception.getCause());
            errorDemon.showErrorDialog("Error creating the backup file", null, exception);
        }
        return backup;
    }

    private void deleteBackup(Track track, File backup) {
        if (backup != null && ! backup.delete()) {
            LOG.error("Error deleting backup file of {}", track);
            errorDemon.showErrorDialog("Error deleting the backup file of " + track.getFileName());
        }
    }

    private void restoreBackup(Track track, File backup) {
        File original = new File(track.getFileFolder(), track.getFileName());
        try {
            Files.move(backup.toPath(), original.toPath(), options);
        }
        catch (IOException | UnsupportedOperationException exception) {
            LOG.error("Error restoring the backup file: ", exception.getCause());
            errorDemon.showErrorDialog("Error restoring the backup file", null, exception);
        }
    }
}
