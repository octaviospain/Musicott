/******************************************************************************
 * Copyright (C) 2025  Octavio Calleya Garcia                                 *
 * *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 * *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https:></https:>//www.gnu.org/licenses/>.     *
 */
package net.transgressoft.musicott.service

import mu.KotlinLogging
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary
import net.transgressoft.commons.music.audio.AudioFileType
import net.transgressoft.lirp.entity.LirpEntity
import net.transgressoft.lirp.event.CrudEvent
import net.transgressoft.lirp.event.LirpEventSubscription
import net.transgressoft.musicott.events.ExceptionEvent
import net.transgressoft.musicott.events.StatusMessageUpdateEvent
import net.transgressoft.musicott.events.StatusProgressUpdateEvent
import org.jetbrains.kotlin.gradle.utils.toSetOrEmpty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.io.path.extension

@Service
class MediaImportService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val audioRepository: ObservableAudioLibrary
) {
    private val logger = KotlinLogging.logger {}

    private val progress = AtomicInteger()
    private var totalFiles = 0

    fun importFiles(filesToOpen: List<File>) {
        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Importing files...", this))

        val paths = filesToOpen.map(File::toPath)
        totalFiles = paths.size
        progress.set(0)
        val progressSubscription =
            audioRepository.subscribe {
                applicationEventPublisher.publishEvent(
                    StatusProgressUpdateEvent(totalFiles.toDouble() / progress.getAndIncrement(), this)
                )
            }

        audioRepository.createFromFileBatchAsync(paths)
            .exceptionally {
                logger.error(it.message, it)
                applicationEventPublisher.publishEvent(ExceptionEvent(it, this))
                null
            }
            .thenRun {
                progressSubscription.cancel()
                applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Import process completed", this))
            }
    }

    fun importDirectory(directory: File, acceptedAudioFileExtensions: Set<AudioFileType>) {
        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Importing files...", this))
        applicationEventPublisher.publishEvent(StatusProgressUpdateEvent(-1.0, this))

        progress.set(0)
        val progressSubscription =
            audioRepository.subscribe {
                // for each new created audio item update the progress bar
                if (it.isCreate()) {
                    applicationEventPublisher.publishEvent(
                        StatusProgressUpdateEvent(totalFiles.toDouble() / progress.getAndIncrement(), this)
                    )
                }
            }

        val extensions = acceptedAudioFileExtensions
            .map(AudioFileType::extension)
            .toSet()

        CompletableFuture.supplyAsync { findAcceptedFiles(directory, extensions) }
            .thenCompose(audioRepository::createFromFileBatchAsync)
            .exceptionally {
                logger.error(it.message, it)
                applicationEventPublisher.publishEvent(ExceptionEvent(it, this))
                null
            }
            .thenRun {
                progressSubscription.cancel()
                applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Import process completed", this))
            }
    }

    private fun findAcceptedFiles(directory: File, acceptedExtensions: Set<String>): List<Path> {
        val paths = try {
            Files.walk(directory.toPath()).use { pathStream ->
                pathStream
                    .filter(Files::isRegularFile)
                    .filter { it.extension.lowercase() in acceptedExtensions }
                    .toList()
            }
        } catch (exception: IOException) {
            logger.error("Error scanning directory: {}", exception.message, exception)
            applicationEventPublisher.publishEvent(ExceptionEvent(exception, this))
            return emptyList()
        }

        val message = if (paths.isEmpty()) {
            "No supported audio files found"
        } else {
            "Found ${paths.size} audio files"
        }
        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent(message, this))

        totalFiles = paths.size
        return paths
    }
}
