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

package com.transgressoft.musicott.view.custom;

import com.google.inject.*;
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.model.*;

import javax.swing.*;
import java.awt.*;

/**
 * Swing panel paints the waveform of a track.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class WaveformPanel extends JPanel {

    private static final long serialVersionUID = 2195160480150957593L;
    private final float[] defaultWave;

    private final transient MusicLibrary musicLibrary;
    private float[] waveData;
    private int paneWidth;
    private Color backgroundColor;
    private Color foregroundColor;

    @Inject
    public WaveformPanel(MusicLibrary musicLibrary, @Assisted ("width") int width,
            @Assisted ("height") int height) {
        this.musicLibrary = musicLibrary;
        Dimension dim = new Dimension(width, height);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        paneWidth = width;
        defaultWave = new float[width];
        for (int i = 0; i < width; i++)
            defaultWave[i] = 0.28802148f;
        waveData = defaultWave;
        backgroundColor = new Color(34, 34, 34);
        foregroundColor = new Color(73, 73, 73);
        setBackground(backgroundColor);
        setForeground(backgroundColor);
    }

    public void setTrack(Track track) {
        WaveformsLibrary waveformsLibrary = musicLibrary.getWaveformsLibrary();
        if (waveformsLibrary.containsWaveform(track.getTrackId())) {
            waveData = waveformsLibrary.getWaveform(track.getTrackId());
            if (getForeground().equals(backgroundColor))
                setForeground(foregroundColor);
            repaint();
        }
        else
            clear();
    }

    public void clear() {
        waveData = defaultWave;
        setForeground(backgroundColor);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int nHeight = getHeight();
        for (int i = 0; i < paneWidth; i++) {
            int value = (int) (waveData[i] * nHeight);
            int y1 = (nHeight - 2 * value) / 2;
            int y2 = y1 + 2 * value;
            g.drawLine(i, y1, i, y2);
        }
    }
}
