/******************************************************************************
 *     Copyright (C) 2025  Octavio Calleya Garcia                             *
 *                                                                            *
 *     This program is free software: you can redistribute it and/or modify   *
 *     it under the terms of the GNU General Public License as published by   *
 *     the Free Software Foundation, either version 3 of the License, or      *
 *     (at your option) any later version.                                    *
 *                                                                            *
 *     This program is distributed in the hope that it will be useful,        *
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *     GNU General Public License for more details.                           *
 *                                                                            *
 *     You should have received a copy of the GNU General Public License      *
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>. *
 ******************************************************************************/

package net.transgressoft.musicott.services;

import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.events.StatusProgressUpdateEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MediaImportService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObservableAudioLibrary audioRepository;
    private final AtomicInteger progress = new AtomicInteger();
    private int totalFiles;

    public MediaImportService(ApplicationEventPublisher applicationEventPublisher, ObservableAudioLibrary audioRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.audioRepository = audioRepository;
    }

    public void importFiles(List<File> filesToOpen) {
        applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Importing files...", this));

        var paths = filesToOpen.stream().map(File::toPath).toList();
        totalFiles = paths.size();
        progress.set(0);
        var progressSubscription = audioRepository.subscribe(event ->
                applicationEventPublisher.publishEvent(new StatusProgressUpdateEvent((double) totalFiles / progress.getAndIncrement(), this)));

        audioRepository.createFromFileBatchAsync(paths)
                .exceptionally(exception -> {
                    log.error(exception.getMessage(), exception);
                    applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
                    return null;
                })
                .thenRun(() -> {
                    progressSubscription.cancel();
                    applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Import process completed", this));
                });
    }

    public void importDirectory(File directory, Set<AudioFileType> acceptedAudioFileExtensions) {
        applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Importing files...", this));
        applicationEventPublisher.publishEvent(new StatusProgressUpdateEvent(-1, this));


        progress.set(0);
        var progressSubscription = audioRepository.subscribe(event ->
                applicationEventPublisher.publishEvent(new StatusProgressUpdateEvent((double) totalFiles / progress.getAndIncrement(), this)));

        CompletableFuture.supplyAsync(scanForPaths(directory, acceptedAudioFileExtensions))
                .thenCompose(audioRepository::createFromFileBatchAsync)
                .exceptionally(exception -> {
                    log.error(exception.getMessage(), exception);
                    applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
                    return null;
                })
                .thenRun(() -> {
                    progressSubscription.cancel();
                    applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Import process completed", this));
                });
    }

    private Supplier<List<Path>> scanForPaths(File directory, Set<AudioFileType> acceptedAudioFileExtensions) {
        return () -> {
            var acceptedExtensions = acceptedAudioFileExtensions.stream()
                    .map(AudioFileType::getExtension)
                    .collect(Collectors.toUnmodifiableSet());

            return findAcceptedFiles(directory, acceptedExtensions);
        };
    }

    private @NotNull List<Path> findAcceptedFiles(File directory, Set<String> acceptedExtensions) {
        List<Path> paths;
        try (Stream<Path> pathStream = Files.walk(directory.toPath())) {
            paths = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        int lastDotIndex = fileName.lastIndexOf('.');
                        // Handle files without extension
                        if (lastDotIndex == -1) {
                            return false;
                        }
                        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
                        return acceptedExtensions.contains(extension);
                    })
                    .toList();
        } catch (IOException exception) {
            log.error("Error scanning directory: {}", exception.getMessage(), exception);
            applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
            return Collections.emptyList();
        }

        if (paths.isEmpty()) {
            applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("No supported audio files found", this));
        } else {
            applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Found " + paths.size() + " audio files", this));
        }
        totalFiles = paths.size();
        return paths;
    }
}
