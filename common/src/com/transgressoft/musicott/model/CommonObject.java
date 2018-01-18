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

package com.transgressoft.musicott.model;

import java.io.*;

/**
 * Stores constants of layout files, logos and css stylesheets
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.9
 */
public enum CommonObject {

    TRACKS_FILE("Musicott-tracks.json"),
    WAVEFORMS_FILE("Musicott-waveforms.json"),
    PLAYLISTS_FILE("Musicott-playlists.json"),
    LAYOUTS_PATH("/" + "view" + "/"),
    IMAGES_PATH("/" + "images" + "/"),
    STYLES_PATH("/" + "css" + "/"),
    ICONS_PATH("/" + "icons" + "/"),
    DEFAULT_COVER(IMAGES_PATH +  "default-cover-image.png"),
    LASTFM_LOGO(IMAGES_PATH + "lastfm-logo.png"),
    COMMON_ERROR_IMAGE(IMAGES_PATH + "common-error.png"),
    MUSICOTT_ABOUT_LOGO(IMAGES_PATH + "musicott-about-logo.png"),
    MUSICOTT_APP_ICON(ICONS_PATH + "musicott-app-icon.png"),
    DRAGBOARD_ICON_PATH(ICONS_PATH + "dragboard-icon.png"),
    DIALOG_STYLE(STYLES_PATH + "dialog.css"),
    BASE_STYLE(STYLES_PATH + "base.css"),
    TRACK_TABLE_STYLE(STYLES_PATH + "tracktable.css"),
    TRACKAREASET_TRACK_TABLE_STYLE(STYLES_PATH + "tracktable-trackareaset.css"),
    ITUNES_XSD("/config/PropertyList-1.0.xsd");

    private String path;

    CommonObject(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }
}
