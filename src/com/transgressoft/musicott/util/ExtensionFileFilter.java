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
import com.transgressoft.musicott.model.*;

import java.io.*;
import java.util.*;

/**
 * This class implements {@link FileFilter} to
 * accept a file with some of the given extensions. If no extensions are given
 * the file is not accepted. The extensions must be given without the dot.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class ExtensionFileFilter implements FileFilter {

    private String[] extensions;
    private int numExtensions;

    private MusicLibrary musicLibrary;

    @Inject
    public ExtensionFileFilter(MusicLibrary musicLibrary) {
        this.musicLibrary = musicLibrary;
        extensions = new String[]{};
        numExtensions = 0;
    }

    public void addExtension(String ext) {
        boolean contains = false;
        for (String e : extensions)
            if (e != null && ext.equals(e)) {
                contains = true;
            }
        if (! contains) {
            ensureArrayLength();
            extensions[numExtensions++] = ext;
        }
    }

    private void ensureArrayLength() {
        if (numExtensions == extensions.length)
            extensions = Arrays.copyOf(extensions, numExtensions == 0 ? 1 : 2 * numExtensions);
    }

    public void removeExtension(String ext) {
        for (int i = 0; i < extensions.length; i++)
            if (extensions[i].equals(ext)) {
                extensions[i] = null;
                numExtensions--;
            }
        extensions = Arrays.copyOf(extensions, numExtensions);
    }

    public boolean hasExtension(String ext) {
        for (String e : extensions)
            if (ext.equals(e)) {
                return true;
            }
        return false;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public void setExtensions(String... extensions) {
        if (extensions == null)
            this.extensions = new String[]{};
        else
            this.extensions = extensions;
        numExtensions = this.extensions.length;
    }

    @Override
    public boolean accept(File pathname) {
        boolean res = false;
        TracksLibrary tracksLibrary = musicLibrary.getTracksLibrary();
        if (! pathname.isDirectory() && ! pathname.isHidden()) {
            res = matchExtension(pathname) && ! tracksLibrary.containsTrackPath(pathname.getParent(), pathname.getName());
        }
        return res;
    }

    private boolean matchExtension(File pathname) {
        boolean res = false;
        int pos = pathname.getName().lastIndexOf('.');
        if (pos != - 1) {
            String extension = pathname.getName().substring(pos + 1);
            for (String requiredExtension : extensions)
                if (extension.equals(requiredExtension))
                    res = true;
        }
        return res;
    }
}