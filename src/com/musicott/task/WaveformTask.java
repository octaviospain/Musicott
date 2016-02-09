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

package com.musicott.task;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;

import be.tarsos.transcoder.DefaultAttributes;
import be.tarsos.transcoder.Transcoder;
import be.tarsos.transcoder.ffmpeg.EncoderException;
import javafx.application.Platform;

/**
 * @author Octavio Calleya
 *
 */
public class WaveformTask extends Thread {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final double HEIGHT_COEFICIENT = 4.2; // This fits the waveform to the swingnode height
	
	private MusicLibrary ml;
	private SceneManager sc;
	private TaskPoolManager tpm;
	private Track track;
	private float[] waveform;
	private Semaphore taskSemaphore;
	
	public WaveformTask(String id, Semaphore taskSemaphore, TaskPoolManager taskPoolManager) {
		super(id);
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
		tpm = taskPoolManager;
		this.taskSemaphore = taskSemaphore;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				taskSemaphore.acquire();
				track = tpm.getTrackToProcess();
				LOG.debug("Processing waveform of track {}", track);
				String fileFormat = track.getFileFormat();
				if(fileFormat.equals("wav"))
					waveform = processWav();
				else if(fileFormat.equals("mp3") || fileFormat.equals("m4a"))
					waveform = processNoWav(fileFormat);
				
				if(waveform != null) {
					ml.addWaveform(track.getTrackID(), waveform);
					Track currentTrack = PlayerFacade.getInstance().getCurrentTrack();
					if(currentTrack != null && currentTrack.equals(track))
						SwingUtilities.invokeLater(() -> sc.getPlayerController().setWaveform(track));
					LOG.debug("Waveform of track {} completed", track);
					Platform.runLater(() -> sc.getNavigationController().setStatusMessage(""));
					ml.saveLibrary(false, true, false);
				}
				else
					Platform.runLater(() -> sc.getNavigationController().setStatusMessage("Fail processing waveform of "+track.getName()));
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