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
import net.transgressoft.commons.fx.music.FXMusicLibrary
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy
import net.transgressoft.commons.music.waveform.AudioWaveform
import net.transgressoft.commons.music.waveform.AudioWaveformRepository
import org.apache.commons.lang3.SystemUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

@Configuration
class ApplicationConfiguration @Autowired constructor(private val applicationPaths: ApplicationPaths) {

    init {
        initializeApplicationFiles()
    }

    private fun initializeApplicationFiles() {
        try {
            applicationPaths.audioItemsPath.parent?.let { Files.createDirectories(it) }

            createFileIfNotExists(applicationPaths.audioItemsPath)
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
    fun musicLibrary(): FXMusicLibrary =
        FXMusicLibrary.builder()
            .audioLibraryJsonFile(applicationPaths.audioItemsPath.toFile())
            .playlistHierarchyJsonFile(applicationPaths.playlistsPath.toFile())
            .waveformRepositoryJsonFile(applicationPaths.waveformsPath.toFile())
            .build()

    @Bean
    fun audioLibrary(musicLibrary: FXMusicLibrary): ObservableAudioLibrary = musicLibrary.audioLibrary()

    @Bean
    fun playlistHierarchy(musicLibrary: FXMusicLibrary): ObservablePlaylistHierarchy = musicLibrary.playlistHierarchy()

    @Bean
    fun waveformRepository(musicLibrary: FXMusicLibrary): AudioWaveformRepository<AudioWaveform, ObservableAudioItem> =
        musicLibrary.waveformRepository()
}