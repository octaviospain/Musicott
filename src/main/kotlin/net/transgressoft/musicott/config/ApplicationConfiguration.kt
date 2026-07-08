/******************************************************************************
 * Copyright (C) 2025  Octavio Calleya Garcia                                 *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.     *
 ******************************************************************************/

package net.transgressoft.musicott.config

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.input.KeyCombination
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import net.transgressoft.commons.fx.music.FXMusicLibrary
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy
import net.transgressoft.commons.media.persistence.waveform.AudioWaveformMapSerializer
import net.transgressoft.commons.music.m3u.M3uImportService
import net.transgressoft.commons.persistence.fx.music.audio.FXAudioItemSqlTableDef
import net.transgressoft.commons.persistence.fx.music.playlist.ObservablePlaylistMapSerializer
import net.transgressoft.commons.music.audio.AudioMetadataIO
import net.transgressoft.commons.music.audio.JAudioTaggerMetadataIO
import net.transgressoft.commons.music.waveform.AudioWaveform
import net.transgressoft.commons.music.waveform.AudioWaveformRepository
import net.transgressoft.lirp.persistence.Repository
import net.transgressoft.lirp.persistence.json.JsonFileRepository
import net.transgressoft.lirp.persistence.sql.SqliteRepository
import net.transgressoft.musicott.MusicottApplication
import org.apache.commons.lang3.SystemUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.function.Supplier

/**
 * Central Spring bean definitions for the domain and persistence layer.
 *
 * Creates the application data directory and files on startup, then exposes the
 * music-commons repositories as beans: the audio library backed by a lirp SQLite
 * repository, and the playlist hierarchy and waveform cache backed by lirp JSON file
 * repositories. Also provides supporting beans such as the metadata I/O service, the
 * m3u import service, file/directory choosers, and the OS-appropriate key modifier.
 */
@Configuration
class ApplicationConfiguration @Autowired constructor(private val applicationPaths: MusicottApplication.ApplicationPaths) {

    init {
        initializeApplicationFiles()
    }

    private fun initializeApplicationFiles() {
        try {
            applicationPaths.audioItemsDatabasePath.parent?.let { Files.createDirectories(it) }

            // The audio item SQLite database file is created by SqliteRepository on first connection.
            createFileIfNotExists(applicationPaths.playlistsPath)
            createFileIfNotExists(applicationPaths.waveformsPath)
        } catch (exception: IOException) {
            throw RuntimeException("Could not create application files", exception)
        }
    }

    @Throws(IOException::class)
    private fun createFileIfNotExists(path: Path) {
        if (!Files.exists(path)) {
            Files.createFile(path)
        }
    }

    @Bean
    fun operativeSystemKeyModifier(): KeyCombination.Modifier =
        if (SystemUtils.IS_OS_MAC_OSX)
            KeyCombination.META_DOWN
        else
            KeyCombination.CONTROL_DOWN

    /**
     * Shared property tracking the currently selected playlist in the playlist tree.
     * Injected into [net.transgressoft.musicott.view.custom.PlaylistTreeView] so that
     * tests and production code share the same observable instance.
     */
    @Bean
    fun selectedPlaylistProperty(): ObjectProperty<Optional<ObservablePlaylist>> =
        SimpleObjectProperty(null, "selected playlist", Optional.empty())

    @Bean
    fun audioMetadataIO(): AudioMetadataIO = JAudioTaggerMetadataIO()

    // The three persistence repositories are exposed as dedicated beans so the Spring
    // container owns their lifecycle: each is AutoCloseable, so on context shutdown Spring
    // invokes close() — flushing the JSON repositories and, for the SQLite audio repository,
    // checkpointing the WAL and shutting down the connection pool. Spring Boot's shutdown
    // hook runs on the application's System.exit path, so the close is deterministic.
    @Bean
    fun audioItemRepository(): Repository<Int, ObservableAudioItem> =
        SqliteRepository.fileBacked(applicationPaths.audioItemsDatabasePath, FXAudioItemSqlTableDef)

    @Bean
    fun playlistFileRepository(): Repository<Int, ObservablePlaylist> =
        JsonFileRepository(applicationPaths.playlistsPath.toFile(), ObservablePlaylistMapSerializer, loadOnInit = false)

    @Bean
    fun waveformFileRepository(): Repository<Int, AudioWaveform> =
        JsonFileRepository(applicationPaths.waveformsPath.toFile(), AudioWaveformMapSerializer)

    @Bean
    fun musicLibrary(
        audioItemRepository: Repository<Int, ObservableAudioItem>,
        playlistFileRepository: Repository<Int, ObservablePlaylist>,
        waveformFileRepository: Repository<Int, AudioWaveform>
    ): FXMusicLibrary =
        FXMusicLibrary.builder()
            .audioRepository(audioItemRepository)
            .playlistRepository(playlistFileRepository)
            .waveformRepository(waveformFileRepository)
            .build()

    @Bean
    fun audioLibrary(musicLibrary: FXMusicLibrary): ObservableAudioLibrary = musicLibrary.audioLibrary()

    @Bean
    fun playlistHierarchy(musicLibrary: FXMusicLibrary): ObservablePlaylistHierarchy = musicLibrary.playlistHierarchy()

    @Bean
    fun waveformRepository(musicLibrary: FXMusicLibrary): AudioWaveformRepository<AudioWaveform, ObservableAudioItem> =
        musicLibrary.waveformRepository()

    /**
     * Provides a [DirectoryChooser] factory for production use. The [Supplier] indirection
     * allows integration tests to inject a pre-configured mock without modifying production code.
     */
    @Bean
    fun directoryChooserSupplier(): Supplier<DirectoryChooser> = Supplier { DirectoryChooser() }

    /**
     * Provides a [FileChooser] factory for production use. The [Supplier] indirection
     * allows integration tests to inject a pre-configured mock without modifying production code.
     */
    @Bean
    fun fileChooserSupplier(): Supplier<FileChooser> = Supplier { FileChooser() }

    /**
     * Creates the M3U import service bound to the application's FX music library.
     *
     * Registered with [destroyMethod] = "close" so that the backing [kotlinx.coroutines.CoroutineScope]
     * is cancelled on Spring context shutdown, preventing resource leaks.
     */
    @Bean(destroyMethod = "close")
    fun m3uImportService(musicLibrary: FXMusicLibrary): M3uImportService<ObservableAudioItem, ObservablePlaylist> =
        M3uImportService(musicLibrary)
}
