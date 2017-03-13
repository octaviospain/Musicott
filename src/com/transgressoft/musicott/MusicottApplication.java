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
import com.sun.javafx.application.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.services.lastfm.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.tasks.load.*;
import com.transgressoft.musicott.util.*;
import javafx.application.*;
import javafx.stage.*;
import org.slf4j.Logger;
import org.slf4j.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Creates and launches Musicott. The creation of the application follows this steps:
 * <ol>
 * <li>Initialization of the {@code Logger}</li>
 * <li>The {@link MainPreloader} is created and shown</li>
 * <li>It is checked whether it is the first use, and if so, the user enters the application folder</li>
 * <li>Configuration properties, tracks, playlists and waveforms are loaded</li>
 * <li>The main window is shown</li>
 * </ol>
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @see <a href="https://octaviospain.github.io/Musicott">Musicott</a>
 */
public class MusicottApplication extends Application {

    static final String FIRST_USE_EVENT = "first_use";
    private static final String CONFIG_FILE = "resources/config/config.properties";
    private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
    private static final String LOG_FILE = "Musicott-main-log.txt";
    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private MusicLibrary musicLibrary;
    private MainPreferences preferences;
    private StageDemon stageDemon;

    public MusicottApplication() {
        musicLibrary = MusicLibrary.getInstance();
        preferences = MainPreferences.getInstance();
        stageDemon = StageDemon.getInstance();
    }

    public static void main(String[] args) {
        initializeLogger();
        LauncherImpl.launchApplication(MusicottApplication.class, MainPreloader.class, args);
    }

    /**
     * Initializes a {@link Logger} that stores the log entries on a file
     *
     * @see <a href=http://www.slf4j.org/>slf4j</href>
     */
    private static void initializeLogger() {
        Handler baseFileHandler;
        LogManager logManager = LogManager.getLogManager();
        java.util.logging.Logger logger = logManager.getLogger("");
        Handler rootHandler = logger.getHandlers()[0];

        try {
            logManager.readConfiguration(new FileInputStream(LOGGING_PROPERTIES));
            baseFileHandler = new FileHandler(LOG_FILE);
            baseFileHandler.setFormatter(new SimpleFormatter() {

                @Override
                public String format(LogRecord record) {
                    return logTextString(record);
                }
            });
            logManager.getLogger("").removeHandler(rootHandler);
            logManager.getLogger("").addHandler(baseFileHandler);
        }
        catch (SecurityException | IOException exception) {
            System.err.println("Error initiating logger: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * Constructs a log message given a {@link LogRecord}
     *
     * @param record The {@code LogRecord} instance
     *
     * @return The formatted string of a log entries
     */
    private static String logTextString(LogRecord record) {
        StringBuilder stringBuilder = new StringBuilder();
        String dateTimePattern = "dd/MM/yy HH:mm:ss :nnnnnnnnn";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateTimePattern);

        String logDate = LocalDateTime.now().format(dateFormatter);
        String loggerName = record.getLoggerName();
        String sourceMethod = record.getSourceMethodName();
        String firstLine = loggerName + " " + sourceMethod + " " + logDate + "\n";

        String sequenceNumber = String.valueOf(record.getSequenceNumber());
        String loggerLevel = record.getLevel().toString();
        String message = record.getMessage();
        String secondLine = sequenceNumber + "\t" + loggerLevel + ":" + message + "\n\n";

        stringBuilder.append(firstLine);
        stringBuilder.append(secondLine);

        if (record.getThrown() != null) {
            stringBuilder.append(record.getThrown() + "\n");
            for (StackTraceElement stackTraceElement : record.getThrown().getStackTrace())
                stringBuilder.append(stackTraceElement + "\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public void init() throws Exception {
        if (preferences.getMusicottUserFolder() == null || ! new File(preferences.getMusicottUserFolder()).exists()) {
            LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));
        }
        stageDemon.setApplicationHostServices(getHostServices());
        String applicationFolder = preferences.getMusicottUserFolder();
        loadConfigProperties();

        ImmutableList<Callable<Void>> loadTasksList = ImmutableList
                .of(new WaveformsLoadAction(applicationFolder, musicLibrary, this),
                    new PlaylistsLoadAction(applicationFolder, musicLibrary, this),
                    new TracksLoadAction(null, applicationFolder, musicLibrary, this));

        ForkJoinPool loadForkJoinPool = new ForkJoinPool(4);
        loadForkJoinPool.invokeAll(loadTasksList);
        loadForkJoinPool.shutdown();
    }

    /**
     * Loads required configuration parameters from a {@code .properties} file
     */
    private void loadConfigProperties() {
        Properties properties = new Properties();
        try {
            BaseLoadAction.notifyPreloader(0, 4, "Loading configuration...", this);
            properties.load(new FileInputStream(CONFIG_FILE));
            String apiKey = properties.getProperty("lastfm_api_key");
            String apiSecret = properties.getProperty("lastfm_api_secret");
            LastFmPreferences servicePreferences = ServiceDemon.getInstance().getLastFmPreferences();
            servicePreferences.setApiSecret(apiSecret);
            servicePreferences.setApiKey(apiKey);
        }
        catch (IOException exception) {
            LOG.warn("Error loading configuration properties", exception);
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setOnCloseRequest(event -> {
            LOG.info("Exiting Musicott");
            TaskDemon.getInstance().shutDownTasks();
            System.exit(0);
        });
        stageDemon.showMusicott(primaryStage);
        LOG.debug("Showing root stage");
    }
}
