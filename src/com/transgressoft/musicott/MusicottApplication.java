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

import com.google.inject.*;
import com.sun.javafx.application.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.tasks.load.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.stage.Stage;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.transgressoft.musicott.MainPreloader.*;
import static com.transgressoft.musicott.model.Layout.*;

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
 * @version 0.10-b
 * @see <a href="https://octaviospain.github.io/Musicott">Musicott</a>
 */
public class MusicottApplication extends Application implements InjectedApplication {

    private static final String CONFIG_FILE = "resources/config/config.properties";

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    static Injector injector = Guice.createInjector(new PreloaderModule());

    private StageDemon stageDemon;
    private TaskDemon taskDemon;

    public static void main(String[] args) {
        Utils.initializeLogger();
        LauncherImpl.launchApplication(MusicottApplication.class, MainPreloader.class, args);
    }

    @Override
    public void init() throws Exception {
        MainPreferences preferences = injector.getInstance(MainPreferences.class);
        if (isFirstUse(preferences))
            LauncherImpl.notifyPreloader(this, new CustomProgressNotification(0, FIRST_USE_EVENT));

        String applicationFolder = preferences.getMusicottUserFolder();
        LastFmPreferences lastFmPreferences = injector.getInstance(LastFmPreferences.class);
        loadConfigProperties(lastFmPreferences);

        injector.getInstance(MusicLibrary.class);
        stageDemon = injector.getInstance(StageDemon.class);
        stageDemon.setInjector(injector);

        taskDemon = injector.getInstance(TaskDemon.class);
        taskDemon.deactivateLibrarySaving();
        ForkJoinPool loadForkJoinPool = new ForkJoinPool(4);
        LoadActionFactory loadActionFactory = injector.getInstance(LoadActionFactory.class);
        loadForkJoinPool.invoke(loadActionFactory.createWaveformsAction(applicationFolder, this));
        loadForkJoinPool.invoke(loadActionFactory.createPlaylistAction(applicationFolder, this));
        loadForkJoinPool.invoke(loadActionFactory.createTracksAction(null, -1, applicationFolder, this));
        loadForkJoinPool.shutdown();
        taskDemon.activateLibrarySaving();
    }

    private boolean isFirstUse(MainPreferences preferences) {
        boolean res = false;
        if (preferences.getMusicottUserFolder() == null || ! new File(preferences.getMusicottUserFolder()).exists())
            res = true;
        return res;
    }

    /**
     * Loads required configuration parameters from a {@code .properties} file
     */
    private void loadConfigProperties(LastFmPreferences servicePreferences) {
        Properties properties = new Properties();
        try {
            BaseLoadAction.notifyPreloader(0, 4, "Loading configuration...", this);
            properties.load(new FileInputStream(CONFIG_FILE));
            String apiKey = properties.getProperty("lastfm_api_key");
            String apiSecret = properties.getProperty("lastfm_api_secret");
            servicePreferences.setApiSecret(apiSecret);
            servicePreferences.setApiKey(apiKey);
        }
        catch (IOException exception) {
            LOG.warn("Error loading configuration properties", exception);
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException, ReflectiveOperationException {
        primaryStage.setOnCloseRequest(event -> {
            LOG.info("Exiting Musicott");
            taskDemon.shutDownTasks();
            System.exit(0);
        });
        injector = injector.createChildInjector(new MusicottModule());

        ControllerModule<ErrorDialogController> errorModule = createController(ERROR_DIALOG, injector);
        Stage errorStage = new Stage();
        errorStage.setScene(new Scene(errorModule.providesController().getRoot()));
        errorStage.show();

        ControllerModule<EditController> editModule = createController(EDITION, injector);
        Stage editStage = new Stage();
        editStage.setScene(new Scene(editModule.providesController().getRoot()));
        editModule.providesController().setStage(editStage);

        ControllerModule<PreferencesController> prefsModule = createController(PREFERENCES, injector);
        Stage prefsStage = new Stage();
        prefsStage.setScene(new Scene(prefsModule.providesController().getRoot()));
        prefsStage.show();

        injector = injector.createChildInjector(errorModule, editModule, prefsModule);
        injector = injector.createChildInjector(new TrackSetAreaRowFactoryModule());

        ControllerModule<RootController> rootModule = createController(ROOT, injector);
        primaryStage.setScene(new Scene(rootModule.providesController().getRoot()));

        primaryStage.show();

        //        stageDemon.initErrorController();
//        stageDemon.setApplicationHostServices(getHostServices());
//        stageDemon.showMusicott(primaryStage);
    }
}
