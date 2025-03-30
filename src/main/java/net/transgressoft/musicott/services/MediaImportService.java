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

import net.transgressoft.commons.fx.music.audio.ObservableAudioItemJsonRepository;
import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class MediaImportService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObservableAudioItemJsonRepository audioRepository;

    public MediaImportService(ApplicationEventPublisher applicationEventPublisher,
                              ObservableAudioItemJsonRepository audioRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.audioRepository = audioRepository;
    }

    public void importFiles(List<File> filesToOpen) {
        applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Importing files...", this));
        var paths = filesToOpen.stream().map(File::toPath).toList();
        audioRepository.createFromFileBatchAsync(paths)
                .exceptionally(exception -> {
                    applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
                    return null;
                })
                .thenRun(() -> applicationEventPublisher.publishEvent(new StatusMessageUpdateEvent("Import process completed", this)));
    }
}
