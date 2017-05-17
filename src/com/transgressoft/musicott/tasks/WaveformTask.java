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

package com.transgressoft.musicott.tasks;

import be.tarsos.transcoder.*;
import be.tarsos.transcoder.ffmpeg.*;
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import org.slf4j.*;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.file.StandardCopyOption.*;

/**
 * Class that extends from {@link Thread} that performs the operation of processing
 * an audio file to get the waveform image. It waits for a {@link Semaphore} to process
 * a new {@link Track} in an endless loop, instead of terminating the execution for
 * each {@code track} process.
 * <p>
 * If the audio file is not WAV, is converted into WAV before get
 * the wav amplitudes from the audio file.
 * </p><p>
 * This way is rudimentary, an inefficient. It should be improved.
 * </p>
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @see <a href="https://github.com/JorenSix/TarsosTranscoder">Tarsos Transcoder</a>
 */
public class WaveformTask extends Thread {

    private static final double WAVEFORM_HEIGHT_COEFFICIENT = 2.6; // This fits the waveform to the swing node height
    private static final double WAVEFORM_WIDTH = 520.0;
    private static final CopyOption[] options = new CopyOption[]{COPY_ATTRIBUTES, REPLACE_EXISTING};

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private final Provider<PlayerFacade> playerFacade;
    private final Provider<TaskDemon> taskDemon;
    private final WaveformsLibrary waveformsLibrary;

    private Track trackToAnalyze;
    private float[] resultingWaveform;
    private PlayerController playerController;
    private NavigationController navigationController;

    @Inject
    public WaveformTask(WaveformsLibrary waveformsLibrary, Provider<PlayerFacade> playerFacade,
            Provider<TaskDemon> taskDemon) {
        this.waveformsLibrary = waveformsLibrary;
        this.taskDemon = taskDemon;
        this.playerFacade = playerFacade;
    }

    @Override
    public void run() {
        while (true) {
            try {
                trackToAnalyze = taskDemon.get().getNextTrackToAnalyzeWaveform();
                LOG.debug("Processing resultingWaveform of trackToAnalyze {}", trackToAnalyze);

                String fileFormat = trackToAnalyze.getFileFormat();
                if ("wav".equals(fileFormat))
                    resultingWaveform = processFromWavFile();
                else if ("mp3".equals(fileFormat) || "m4a".equals(fileFormat))
                    resultingWaveform = processFromNoWavFile(fileFormat);

                if (resultingWaveform != null) {
                    waveformsLibrary.addWaveform(trackToAnalyze.getTrackId(), resultingWaveform);
                    Optional<Track> currentTrack = playerFacade.get().getCurrentTrack();
                    currentTrack.ifPresent(this::checkAnalyzedTrackIsCurrentPlaying);
                    Platform.runLater(() -> navigationController.setStatusMessage(""));
                }
            }
            catch (IOException | UnsupportedAudioFileException | EncoderException | InterruptedException exception) {
                LOG.warn("Error processing waveform of {}", trackToAnalyze, exception);
                String message = "Waveform not processed successfully";
                Platform.runLater(() -> navigationController.setStatusMessage(message));
            }
        }
    }

    private float[] processFromWavFile() throws IOException, UnsupportedAudioFileException {
        File trackFile = new File(trackToAnalyze.getFileFolder(), trackToAnalyze.getFileName());
        return processAmplitudes(getWavAmplitudes(trackFile));
    }

    private float[] processFromNoWavFile(String fileFormat) throws IOException, UnsupportedAudioFileException,
                                                                   EncoderException {
        int trackId = trackToAnalyze.getTrackId();
        Path trackPath = FileSystems.getDefault().getPath(trackToAnalyze.getFileFolder(), trackToAnalyze.getFileName());
        File temporalDecodedFile = File.createTempFile("decoded_" + trackId, ".wav");
        File temporalCopiedFile = File.createTempFile("original_" + trackId, "." + fileFormat);

        Files.copy(trackPath, temporalCopiedFile.toPath(), options);
        transcodeToWav(temporalCopiedFile, temporalDecodedFile);
        return processAmplitudes(getWavAmplitudes(temporalDecodedFile));
    }

    private float[] processAmplitudes(int[] sourcePcmData) {
        int width = (int) WAVEFORM_WIDTH;    // the width of the resulting waveform panel
        float[] waveData = new float[width];
        int samplesPerPixel = sourcePcmData.length / width;

        for (int w = 0; w < width; w++) {
            float nValue = 0.0f;

            for (int s = 0; s < samplesPerPixel; s++) {
                nValue += (Math.abs(sourcePcmData[w * samplesPerPixel + s]) / 65536.0f);
            }
            nValue /= samplesPerPixel;
            waveData[w] = nValue;
        }
        return waveData;
    }

    private int[] getWavAmplitudes(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream input = AudioSystem.getAudioInputStream(file);
        AudioFormat baseFormat = input.getFormat();

        Encoding encoding = AudioFormat.Encoding.PCM_UNSIGNED;
        float sampleRate = baseFormat.getSampleRate();
        int numChannels = baseFormat.getChannels();

        AudioFormat decodedFormat = new AudioFormat(encoding, sampleRate, 16, numChannels, numChannels * 2, sampleRate,
                                                    false);
        AudioInputStream pcmDecodedInput = AudioSystem.getAudioInputStream(decodedFormat, input);

        int available = input.available();
        int[] amplitudes = new int[available];
        byte[] buffer = new byte[available];
        pcmDecodedInput.read(buffer, 0, available);
        for (int i = 0; i < available - 1; i += 2) {
            amplitudes[i] = ((buffer[i + 1] << 8) | buffer[i] & 0xff) << 16;
            amplitudes[i] /= 32767;
            amplitudes[i] *= WAVEFORM_HEIGHT_COEFFICIENT;
        }
        input.close();
        pcmDecodedInput.close();
        return amplitudes;
    }

    private void transcodeToWav(File sourceFile, File destinationFile) throws EncoderException {
        Attributes attributes = DefaultAttributes.WAV_PCM_S16LE_STEREO_44KHZ.getAttributes();
        try {
            Transcoder.transcode(sourceFile.toString(), destinationFile.toString(), attributes);
        }
        catch (EncoderException exception) {
            if (exception.getMessage().startsWith("Source and target should")) {
                // even with this error message the library does the conversion, who knows why
            }
            else {
                throw exception;
            }
        }
    }

    private void checkAnalyzedTrackIsCurrentPlaying(Track currentPlayingTrack) {
        if (currentPlayingTrack.equals(trackToAnalyze))
            SwingUtilities.invokeLater(() -> playerController.setWaveform(trackToAnalyze));
    }

    @Inject
    public void setPlayerController(@PlayerCtrl PlayerController playerController) {
        this.playerController = playerController;
    }

    @Inject
    public void setNavigationController(@NavigationCtrl NavigationController navigationController) {
        this.navigationController = navigationController;
    }
}
