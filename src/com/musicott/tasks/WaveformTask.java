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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott.tasks;

import be.tarsos.transcoder.*;
import be.tarsos.transcoder.ffmpeg.*;
import com.musicott.*;
import com.musicott.model.*;
import com.musicott.player.*;
import javafx.application.*;
import org.slf4j.*;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * @author Octavio Calleya
 *
 */
public class WaveformTask extends Thread {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final double HEIGHT_COEFICIENT = 4.2; // This fits the waveform to the swingnode height
	
	private MusicLibrary musicLibrary;
	private StageDemon stageDemon;
	private TaskDemon taskDemon;
	private Track track;
	private float[] waveform;
	private Semaphore taskSemaphore;
	
	public WaveformTask(String id, Semaphore taskSemaphore, TaskDemon taskPoolManager) {
		super(id);
		stageDemon = StageDemon.getInstance();
		musicLibrary = MusicLibrary.getInstance();
		taskDemon = taskPoolManager;
		this.taskSemaphore = taskSemaphore;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				taskSemaphore.acquire();
				track = taskDemon.getTrackToProcess();
				LOG.debug("Processing waveform of track {}", track);
				String fileFormat = track.getFileFormat();
				if(fileFormat.equals("wav"))
					waveform = processWav();
				else if(fileFormat.equals("mp3") || fileFormat.equals("m4a"))
					waveform = processNoWav(fileFormat);
				
				if(waveform != null) {
					musicLibrary.addWaveform(track.getTrackID(), waveform);
					Track currentTrack = PlayerFacade.getInstance().getCurrentTrack();
					if(currentTrack != null && currentTrack.equals(track))
						SwingUtilities.invokeLater(() -> stageDemon.getPlayerController().setWaveform(track));
					LOG.debug("Waveform of track {} completed", track);
					Platform.runLater(() -> stageDemon.getNavigationController().setStatusMessage(""));
					musicLibrary.saveLibrary(false, true, false);
				}
				else
					Platform.runLater(() -> stageDemon.getNavigationController().setStatusMessage("Fail processing waveform of "+track.getName()));
			} catch (Exception e) {
				LOG.warn("Waveform thread error: {}", e);
			}
		}
	}

	private float[] processNoWav(String fileFormat) {
		float[] waveData = null;
		int trackID = track.getTrackID();
		Path trackPath = FileSystems.getDefault().getPath(track.getFileFolder(), track.getFileName());
		File temporalDecodedFile;
		File temporalCoppiedFile;
		try {
			temporalDecodedFile = File.createTempFile("decoded_" + trackID, ".wav");
			temporalCoppiedFile = File.createTempFile("original_" + trackID, "."+fileFormat);
			CopyOption[] options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING}; 
			Files.copy(trackPath, temporalCoppiedFile.toPath(), options);
			try {
				Transcoder.transcode(temporalCoppiedFile.toString(), temporalDecodedFile.toString(), DefaultAttributes.WAV_PCM_S16LE_STEREO_44KHZ.getAttributes());
				waveData = processAmplitudes(getWavAmplitudes(temporalDecodedFile));
			} catch (EncoderException e) {
				if(e.getMessage().startsWith("Source and target should"))
					waveData = processAmplitudes(getWavAmplitudes(temporalDecodedFile));
				else
					LOG.warn("Error processing audio waveform of {}: "+e.getMessage(), track);
			}
		} catch (IOException | UnsupportedAudioFileException e) {
			LOG.warn("Error processing audio waveform of {}: "+e.getMessage(), track);
		}
		return waveData;
	}
	
	private float[] processWav() {
		float[] waveData;
		String trackPath = track.getFileFolder()+"/"+track.getFileName();
		File trackFile = new File(trackPath);
		try {
			waveData = processAmplitudes(getWavAmplitudes(trackFile));
		} catch (UnsupportedAudioFileException | IOException e) {
			LOG.warn("Error processing audio waveform of {}: "+e.getMessage(), track);
			waveData = null;
		}
		return waveData;
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
	
	private float[] processAmplitudes(int[] sourcePCMData) {
		int width = 520;	// the width of th waveform panel
		float[] waveData = new float[width];
		int	nSamplesPerPixel = sourcePCMData.length / width;
		for (int i = 0; i<width; i++) {
			float nValue = 0.0f;
			for (int j = 0; j<nSamplesPerPixel; j++) {
				nValue += (float) (Math.abs(sourcePCMData[i * nSamplesPerPixel + j]) / 65536.0f);
			}
			nValue /= nSamplesPerPixel;
			waveData[i] = nValue;
		}
		return waveData;
	}
}