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

package com.transgressoft.musicott.util;

import com.google.inject.*;
import javafx.application.Preloader.*;

/**
 * Extends from {@link PreloaderNotification} to encapsulate progress and message
 * of a preloader application process
 *
 * @author Octavio Calleya
 */
public class CustomProgressNotification implements PreloaderNotification {

    private double progress;
    private String details;
    private Injector injector;

    public CustomProgressNotification(Injector injector, String details) {
        this.injector = injector;
        this.details = details;
    }

    public CustomProgressNotification(double progress, String details) {
        this.progress = progress;
        this.details = details;
    }

    public double getProgress() {
        return progress;
    }

    public String getDetails() {
        return details;
    }

    public Injector getInjector() {
        return injector;
    }
}
