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

import javafx.application.Platform
import mu.KotlinLogging
import net.transgressoft.commons.fx.music.FXMusicLibrary
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.music.audio.AudioFileType
import net.transgressoft.commons.music.itunes.ImportResult
import net.transgressoft.commons.music.itunes.ItunesImportPolicy
import net.transgressoft.commons.music.itunes.ItunesImportService
import net.transgressoft.commons.music.itunes.ItunesLibrary
import net.transgressoft.commons.music.itunes.ItunesLibraryParser
import net.transgressoft.commons.music.itunes.ItunesPlaylist
import net.transgressoft.lirp.entity.LirpEntity
import net.transgressoft.lirp.event.CrudEvent
import net.transgressoft.lirp.event.LirpEventSubscription
import net.transgressoft.musicott.config.SettingsRepository
import net.transgressoft.musicott.events.ExceptionEvent
import net.transgressoft.musicott.events.StatusMessageUpdateEvent
import net.transgressoft.musicott.events.StatusProgressUpdateEvent
import net.transgressoft.musicott.view.custom.alerts.AlertFactory
import org.jetbrains.annotations.UnknownNullability
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import kotlin.io.path.extension

/**
 * Unified service handling all media import operations — file import, directory import,
 * and iTunes library import — with mutual exclusion to prevent concurrent imports.
 *
 * Only one import operation can run at a time. Attempting a second import while one is
 * in progress shows a warning dialog to the user.
 */
@Service
class MediaImportService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val musicLibrary: FXMusicLibrary,
    private val settingsRepository: SettingsRepository,
    private val alertFactory: AlertFactory
) {
    private val logger = KotlinLogging.logger {}

    private val importing = AtomicBoolean(false)
    private val progress = AtomicInteger()
    private var totalFiles = 0

    private val coreItunesImportService = ItunesImportService(musicLibrary)
    private var currentImportFuture: CompletableFuture<ImportResult>? = null
    private val audioLibrary = musicLibrary.audioLibrary()

    final var lastParsedLibrary: ItunesLibrary? = null
        private set

    fun isImporting(): Boolean = importing.get()

    fun importFiles(filesToOpen: List<File>) {
        if (!tryStartImport()) return

        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Importing files...", this))

        val paths = filesToOpen.map(File::toPath)
        totalFiles = paths.size
        progress.set(0)
        val progressSubscription =
            audioLibrary.subscribe {
                applicationEventPublisher.publishEvent(
                    StatusProgressUpdateEvent(totalFiles.toDouble() / progress.getAndIncrement(), this)
                )
            }

        audioLibrary.createFromFileBatchAsync(paths)
            .whenComplete(completeImport(progressSubscription))
    }

    fun importDirectory(directory: File, acceptedAudioFileExtensions: Set<AudioFileType>) {
        if (!tryStartImport()) return

        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Importing files...", this))
        applicationEventPublisher.publishEvent(StatusProgressUpdateEvent(-1.0, this))

        progress.set(0)
        val progressSubscription =
            audioLibrary.subscribe {
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
            .thenCompose(audioLibrary::createFromFileBatchAsync)
            .whenComplete(completeImport(progressSubscription))
    }

    private fun completeImport(
        progressSubscription: LirpEventSubscription<in LirpEntity, CrudEvent.Type, CrudEvent<Int, ObservableAudioItem>>):
            BiConsumer<in @UnknownNullability List<ObservableAudioItem>, in @UnknownNullability Throwable?> =
        { _, ex ->
            progressSubscription.cancel()
            if (ex != null) {
                logger.error(ex.message, ex)
                applicationEventPublisher.publishEvent(ExceptionEvent(ex, this))
            }
            applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Import process completed", this))
            finishImport()
        }

    fun parseLibrary(xmlPath: Path): ItunesLibrary {
        logger.info { "Parsing iTunes library from $xmlPath" }
        val library = ItunesLibraryParser.parse(xmlPath)
        lastParsedLibrary = library
        logger.info { "Parsed ${library.tracks.size} tracks and ${library.playlists.size} playlists" }
        return library
    }

    fun importSelectedPlaylists(selectedPlaylists: List<ItunesPlaylist>): CompletableFuture<ImportResult> {
        if (!tryStartImport()) return CompletableFuture.completedFuture(null)

        val library = lastParsedLibrary
            ?: run { finishImport(); throw IllegalStateException("No iTunes library parsed. Call parseLibrary() first.") }
        val policy = buildPolicy()
        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Importing from iTunes...", this))

        val future = coreItunesImportService.importAsync(selectedPlaylists, library, policy) { progress ->
            Platform.runLater {
                val fraction = progress.itemsProcessed.toDouble() / progress.totalItems
                applicationEventPublisher.publishEvent(StatusProgressUpdateEvent(fraction, this))
                applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Importing: ${progress.currentFile}", this))
            }
        }.whenComplete { result, ex ->
            Platform.runLater {
                currentImportFuture = null
                if (ex != null) {
                    logger.error(ex.message, ex)
                    applicationEventPublisher.publishEvent(ExceptionEvent(ex, this))
                } else if (result != null) {
                    alertFactory.itunesImportResultAlert(result).showAndWait()
                }
                applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("iTunes import completed", this))
                applicationEventPublisher.publishEvent(StatusProgressUpdateEvent(0.0, this))
                finishImport()
            }
        }

        currentImportFuture = future
        return future
    }

    fun cancelImport() {
        currentImportFuture?.cancel(true)
        currentImportFuture = null
        finishImport()
        logger.info { "iTunes import cancelled" }
    }

    private fun tryStartImport(): Boolean {
        if (!importing.compareAndSet(false, true)) {
            Platform.runLater { alertFactory.importInProgressAlert().showAndWait() }
            return false
        }
        return true
    }

    private fun finishImport() {
        importing.set(false)
    }

    private fun buildPolicy(): ItunesImportPolicy =
        ItunesImportPolicy(
            useFileMetadata = settingsRepository.itunesImportMetadataPolicy,
            holdPlayCount = settingsRepository.itunesImportHoldPlayCountPolicy,
            writeMetadata = settingsRepository.itunesImportWriteMetadataPolicy,
            acceptedFileTypes = settingsRepository.acceptedAudioFileExtensions
        )

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
