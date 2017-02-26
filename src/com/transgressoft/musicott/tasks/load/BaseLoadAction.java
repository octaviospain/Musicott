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

package com.transgressoft.musicott.tasks.load;

import com.cedarsoftware.util.io.*;
import com.sun.javafx.application.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import javafx.application.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * Base class of parse tasks of import music into the application.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public abstract class BaseLoadAction extends RecursiveAction implements Callable<Void> {

    protected final String applicationFolder;
    protected final MusicLibrary musicLibrary;
    protected final Application musicottApplication;

    public BaseLoadAction(String applicationFolder, MusicLibrary musicLibrary, Application musicottApplication) {
        this.applicationFolder = applicationFolder;
        this.musicLibrary = musicLibrary;
        this.musicottApplication = musicottApplication;
    }

    @Override
    public Void call() throws Exception {
        compute();
        return null;
    }

    /**
     * Parses an <tt>Object</tt> of a previously serialized instance using Json-IO
     *
     * @param jsonFormattedFile A JSON formatted {@link File}
     *
     * @return The parsed <tt>Object</tt>
     *
     * @throws IOException If something went bad
     * @see <a href="https://github.com/jdereg/json-io">Json-IO</a>
     */
    protected Object parseJsonFile(File jsonFormattedFile) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(jsonFormattedFile);
        JsonReader jsonReader = new JsonReader(fileInputStream);
        Object parsedObject = jsonReader.readObject();
        jsonReader.close();
        fileInputStream.close();
        return parsedObject;
    }

    /**
     * Handles a notification to be shown in the preloader stage
     *
     * @param step          The step number of the preloader process
     * @param totalSteps    The total number of steps of the preloader process
     * @param detailMessage A notification message to be shown in the preloader
     */
    protected void notifyPreloader(int step, int totalSteps, String detailMessage) {
        notifyPreloader(step, totalSteps, detailMessage, musicottApplication);
    }

    public static void notifyPreloader(int step, int totalSteps, String detailMessage, Application app) {
        int numPreloaderSteps;
        if (totalSteps != 0)
            numPreloaderSteps = totalSteps;
        else
            numPreloaderSteps = 3;
        double progress = (double) step / numPreloaderSteps;
        LauncherImpl.notifyPreloader(app, new CustomProgressNotification(progress, detailMessage));
    }
}
