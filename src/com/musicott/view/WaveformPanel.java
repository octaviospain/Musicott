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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;

import be.tarsos.transcoder.DefaultAttributes;
import be.tarsos.transcoder.Transcoder;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import javafx.application.Platform;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;;

/**
 * @author Octavio Calleya
 *
 */
public class WaveformPanel extends JPanel {
	
	private static final long serialVersionUID = 2195160480150957593L;
	
	private final double HEIGHT_COEFICIENT = 4.4;
	private float[] waveData;
	private int width;
	private Map<Track,float[]> waveforms = MusicLibrary.getInstance().getWaveforms();
	private Color backgroundColor;
	
	public WaveformPanel(int width, int height) {
		this.width = width;
		waveData = new float[width];
		Dimension dim = new Dimension(width, height);
		setMinimumSize(dim);
		setMaximumSize(dim);
		setPreferredSize(dim);
		backgroundColor = new Color(244,244,244);
		setBackground(backgroundColor);
		setForeground(backgroundColor);
	}
	
	public void clear() {
		waveData = new float[width];
		setForeground(backgroundColor);
		repaint();
	}
	
	public void setTrack(Track track) {
		boolean done = false;
		if(waveforms.containsKey(track))
			waveData = waveforms.get(track);
		else
			if(track.getFileFormat().equals("wav"))
				done = processWav(track);
			else
				if(track.getFileFormat().equals("mp3")) {
					done = processMp3(track);
				}
		if(done) {
			if(getForeground().equals(backgroundColor))
				setForeground(Color.GRAY);
			repaint();
		}
		else
			clear();
	}
	
	private boolean processMp3(Track track) {
		boolean res;
	//	String fileNameTrimmed = track.getFileName().replaceAll("\\s",""); // spaces in the file name causes error on Tarsos transcoder
		Path temporalDecodedPath = FileSystems.getDefault().getPath("temp", "decoded.wav");
		Path trackPath = FileSystems.getDefault().getPath(track.getFileFolder(), track.getFileName());
		Path temporalCoppiedPath = FileSystems.getDefault().getPath("temp", "original.mp3");
		File trackFileCopiedTrimmed = temporalCoppiedPath.toFile();
		File temporalDecodedFile = temporalDecodedPath.toFile();
		try {
			temporalDecodedFile.createNewFile();
			Files.copy(trackPath, temporalCoppiedPath, COPY_ATTRIBUTES);
			Transcoder.transcode(temporalCoppiedPath.toString(), temporalDecodedPath.toString(), DefaultAttributes.WAV_PCM_S16LE_STEREO_44KHZ.getAttributes());
			processAmplitudes(getWavAmplitudes(temporalDecodedFile));
			waveforms.put(track, waveData);
			SceneManager.getInstance().saveLibrary(); // save the state of the waveform map
			res = true;
		} catch (EncoderException | UnsupportedAudioFileException | IOException e) {
			res = false;
			Platform.runLater(() -> {
				ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
				ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
			});
		} finally {
			if(temporalDecodedFile.exists())
				temporalDecodedFile.delete();
			if(trackFileCopiedTrimmed.exists())
				trackFileCopiedTrimmed.delete();
		}
		return res;
	}
	
	private boolean processWav(Track track) {
		boolean res;
		String trackPath = track.getFileFolder()+"/"+track.getFileName();
		File trackFile = new File(trackPath);
		try {
			processAmplitudes(getWavAmplitudes(trackFile));
			waveforms.put(track, waveData);
			SceneManager.getInstance().saveLibrary(); // save the state of the waveform map
			res = true;
		} catch (UnsupportedAudioFileException | IOException e) {
			res = false;
			Platform.runLater(() -> {
				ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
				ErrorHandler.getInstance().showErrorDialog(ErrorType.COMMON);
			});
		}
		return res;
	}
	
	private int[] getWavAmplitudes(File file) throws UnsupportedAudioFileException, IOException {
		int[] amp = null;
		AudioInputStream input = AudioSystem.getAudioInputStream(file);
		AudioFormat baseFormat = input.getFormat();
		AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 
		                                            baseFormat.getSampleRate(),
		                                            16,
		                                            baseFormat.getChannels(),
		                                            baseFormat.getChannels() * 2,
		                                            baseFormat.getSampleRate(),
		                                            false);
		AudioInputStream pcmDecodedInput = AudioSystem.getAudioInputStream(decodedFormat, input);			
		int available = input.available();
		amp = new int[available];
		byte[] buffer = new byte[available];
		pcmDecodedInput.read(buffer, 0, available);
		for(int i=0; i<available-1 ; i+=2) {
			amp[i] = ((buffer[i+1] << 8) | buffer[i]) << 16;
			amp[i] /= 32767;
			amp[i] *= HEIGHT_COEFICIENT;
		}
		input.close();
		pcmDecodedInput.close();
		return amp;
	}
	
	private void processAmplitudes(int[] sourcePCMData) {
		int	nSamplesPerPixel = sourcePCMData.length / width;
		for (int i = 0; i<width; i++) {
			float nValue = 0.0f;
			for (int j = 0; j<nSamplesPerPixel; j++) {
				nValue += (float) (Math.abs(sourcePCMData[i * nSamplesPerPixel + j]) / 65536.0f);
			}
			nValue /= nSamplesPerPixel;
			waveData[i] = nValue;
		}
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