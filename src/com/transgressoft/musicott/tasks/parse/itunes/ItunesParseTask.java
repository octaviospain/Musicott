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

package com.transgressoft.musicott.tasks.parse.itunes;

import com.google.common.collect.*;
import com.google.inject.*;
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.parse.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.view.*;
import com.worldsworstsoftware.itunes.*;
import com.worldsworstsoftware.itunes.parser.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.stage.*;
import org.slf4j.*;

import javax.xml.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static com.transgressoft.musicott.model.CommonObject.*;

/**
 * Extends from {@link BaseParseTask} to perform the operation of import a
 * {@code iTunes} library to the {@link MusicLibrary} of the application.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class ItunesParseTask extends BaseParseTask {

    public static final int METADATA_POLICY = 0;
    public static final int ITUNES_DATA_POLICY = 1;

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final TracksLibrary tracksLibrary;
    private final String itunesLibraryXmlPath;
    private final int metadataPolicy;
    private final boolean holdPlayCount;

    private ForkJoinPool forkJoinPool;
    private Semaphore waitConfirmationSemaphore;

    private List<ItunesTrack> itunesTracks;
    private List<ItunesPlaylist> itunesPlaylists;
    private List<String> notFoundFiles;
    private Map<Integer, Track> idsMap;

    private volatile int currentItunesItemsParsed;
    private int totalItunesItemsToParse;
    private int parsedPlaylistsSize = 0;
    private int parsedTracksSize = 0;

    @Inject
    private ParseActionFactory parseActionFactory;
    @Inject
    @ItunesPlaylistsPicker
    private ItunesPlaylistsPickerController playlistsPickerController;

    @Inject
    public ItunesParseTask(TracksLibrary tracksLibrary, MainPreferences mainPreferences,
            @NavigationCtrl NavigationController navCtrl, @ErrorCtrl ErrorDialogController errorDialog,
            @Assisted String path) {
        super(navCtrl, errorDialog);
        this.tracksLibrary = tracksLibrary;
        itunesLibraryXmlPath = path;
        metadataPolicy = mainPreferences.getItunesImportMetadataPolicy();
        holdPlayCount = mainPreferences.getItunesImportHoldPlaycount();
        forkJoinPool = new ForkJoinPool(4);
        waitConfirmationSemaphore = new Semaphore(0);
        currentItunesItemsParsed = 0;
    }

    @Override
    public int getNumberOfParsedItemsAndIncrement() {
        return ++ currentItunesItemsParsed;
    }

    @Override
    public int getTotalItemsToParse() {
        return totalItunesItemsToParse;
    }

    @Override
    protected Void call() throws Exception {
        if (isValidItunesFile()) {
            parseItunesFile();
            Platform.runLater(() -> {
                playlistsPickerController.pickPlaylists(itunesPlaylists);
                playlistsPickerController.getStage().show();
            });
        }
        else
            cancel();
        waitConfirmationSemaphore.acquire();
        showConfirmationAlert();
        waitConfirmationSemaphore.acquire();
        startMillis = System.currentTimeMillis();
        parseItunesTracks();
        parseItunesPlaylists();
        forkJoinPool.shutdown();
        return null;
    }

    public void setItunesPlaylistsToParse(List<ItunesPlaylist> playlists) {
        itunesPlaylists = playlists;
        waitConfirmationSemaphore.release();
    }

    @SuppressWarnings ("unchecked")
    private void parseItunesFile() {
        Platform.runLater(() -> updateTaskProgressOnView(- 1 ,"Scanning itunes library..."));
        ItunesParserLogger itunesLogger = new ItunesParserLogger();
        ItunesLibrary itunesLibrary = ItunesLibraryParser.parseLibrary(itunesLibraryXmlPath, itunesLogger);
        Map<Integer, ItunesTrack> itunesTrackMap = ImmutableMap.copyOf(itunesLibrary.getTracks());
        itunesTracks = itunesTrackMap.values().stream()
                                     .filter(this::isValidItunesTrack)
                                     .collect(ImmutableList.toImmutableList());
        itunesPlaylists = (List<ItunesPlaylist>) itunesLibrary.getPlaylists();
        itunesPlaylists = itunesPlaylists.stream()
                                         .filter(this::isValidItunesPlaylist)
                                         .collect(ImmutableList.toImmutableList());
        Platform.runLater(() -> updateTaskProgressOnView(0.0 ,""));
    }

    private void parseItunesTracks() {
        currentItunesItemsParsed = 0;
        totalItunesItemsToParse = itunesTracks.size();
        ItunesTracksParseAction parseAction = parseActionFactory.create(itunesTracks, metadataPolicy, holdPlayCount, this);
        ItunesParseResult itunesParseResult = forkJoinPool.invoke(parseAction);

        parsedTracksSize = itunesParseResult.getParsedResults().size();
        parseErrors = itunesParseResult.getParseErrors();
        notFoundFiles = itunesParseResult.getNotFoundFiles();
        idsMap = itunesParseResult.getItunesIdToMusicottTrackMap();
    }

    private void parseItunesPlaylists() throws ExecutionException, InterruptedException {
        if (! itunesPlaylists.isEmpty()) {
            currentItunesItemsParsed = 0;
            totalItunesItemsToParse = itunesPlaylists.size();
            ItunesPlaylistParseAction playlistParseAction = parseActionFactory.create(itunesPlaylists, idsMap, this);
            forkJoinPool.invoke(playlistParseAction);
            parsedPlaylistsSize = playlistParseAction.get().getParsedResults().size();
        }
    }

    private void showConfirmationAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.getDialogPane().getStylesheets().add(getClass().getResource(DIALOG_STYLE.toString()).toExternalForm());
            alert.setTitle("Import");
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setHeaderText(getAlertText());

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK))
                waitConfirmationSemaphore.release();
            else {
                Platform.runLater(() -> updateTaskProgressOnView(0.0, ""));
                cancel();
            }
        });
    }

    private String getAlertText() {
        StringBuilder stringBuilder = new StringBuilder("Import ");
        stringBuilder.append(itunesTracks.size()).append(" songs ");
        if (! itunesPlaylists.isEmpty())
            stringBuilder.append("and " ).append(itunesPlaylists.size()).append(" playlists ");
        stringBuilder.append("from itunes?");
        return stringBuilder.toString();
    }

    private boolean isValidItunesFile() {
        boolean isValid;
        try {
            Source xmlFile = new StreamSource(new File(itunesLibraryXmlPath));
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(getClass().getResource(ITUNES_XSD.toString()));
            Validator validator = schema.newValidator();
            validator.validate(xmlFile);
            isValid = true;
        } catch (Exception exception) {
            LOG.error("Error accessing to the itunes xml: {}", exception.getMessage(), exception);
            errorDialog.show("Error opening the iTunes Library file", "", exception);
            isValid = false;
        }
        return isValid;
    }

    private boolean isValidItunesTrack(ItunesTrack itunesTrack) {
        boolean valid = true;
        if ("URL".equals(itunesTrack.getTrackType()) || "Remote".equals(itunesTrack.getTrackType()))
            valid = false;
        else {
            File itunesFile = Paths.get(URI.create(itunesTrack.getLocation())).toFile();
            int index = itunesFile.toString().lastIndexOf('.');
            String fileExtension = itunesFile.toString().substring(index + 1);
            if (! ("mp3".equals(fileExtension) || "m4a".equals(fileExtension) || "wav".equals(fileExtension)))
                valid = false;
            if (tracksLibrary. containsTrackPath(itunesFile.getParent(), itunesFile.getName()))
                valid = false;
        }
        return valid;
    }

    private boolean isValidItunesPlaylist(ItunesPlaylist itunesPlaylist) {
        boolean notStrangeName = ! "####!####".equals(itunesPlaylist.getName());
        boolean notEmpty = ! itunesPlaylist.getPlaylistItems().isEmpty();
        boolean notHugeSize = itunesPlaylist.getTrackIDs().size() < itunesTracks.size();
        return notStrangeName && notEmpty && notHugeSize;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateMessage("Itunes import succeeded");
        LOG.info("Itunes import task completed");
        computeAndShowElapsedTime(parsedTracksSize +  parsedPlaylistsSize);

        if (! notFoundFiles.isEmpty())
            errorDialog.showExpandable("Some files were not found", "", notFoundFiles);
        if (! parseErrors.isEmpty())
            errorDialog.showExpandable("Errors importing files", "", parseErrors);
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        updateMessage("Itunes import cancelled");
        LOG.info("Itunes import task cancelled");
    }
}
