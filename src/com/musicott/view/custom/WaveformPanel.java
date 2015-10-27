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
 */

package com.musicott.view.custom;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Map;

import javax.swing.JPanel;

import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;

/**
 * @author Octavio Calleya
 *
 */
public class WaveformPanel extends JPanel {
	
	private static final long serialVersionUID = 2195160480150957593L;
	private final float[] defaultWave;
	
	private float[] waveData;
	private int width;
	private Map<Integer,float[]> waveforms = MusicLibrary.getInstance().getWaveforms();
	private Color backgroundColor;
	
	public WaveformPanel(int width, int height) {
		this.width = width;
		defaultWave = new float[width];
		for(int i=0; i<width; i++)
				defaultWave[i] = 0.41192132f;
		waveData = defaultWave;
		Dimension dim = new Dimension(width, height);
		setMinimumSize(dim);
		setMaximumSize(dim);
		setPreferredSize(dim);
		backgroundColor = new Color(244,244,244);
		setBackground(backgroundColor);
		setForeground(backgroundColor);
	}
	
	public void clear() {
		waveData = defaultWave;
		setForeground(backgroundColor);
		repaint();
	}
	
	public void setTrack(Track track) {
		if(waveforms.containsKey(track.getTrackID())) {
			waveData = waveforms.get(track.getTrackID());
			if(getForeground().equals(backgroundColor))
				setForeground(Color.GRAY);
			repaint();
		}
		else
			clear();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int nHeight = getHeight();
		for (int i = 0; i<width; i++) {
			int value = (int) (waveData[i] * nHeight);
			int y1 = (nHeight - 2 * value) / 2;
			int y2 = y1 + 2 * value;
			g.drawLine(i, y1, i, y2);
		}
	}
}