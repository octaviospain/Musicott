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

package net.transgressoft.config

import javafx.scene.input.KeyCombination
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem
import net.transgressoft.commons.fx.music.audio.ObservableAudioItemMapSerializer
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistMapSerializer
import net.transgressoft.commons.music.waveform.AudioWaveform
import net.transgressoft.commons.music.waveform.AudioWaveformMapSerializer
import net.transgressoft.commons.music.waveform.AudioWaveformRepository
import net.transgressoft.commons.music.waveform.DefaultAudioWaveformRepository
import net.transgressoft.commons.persistence.json.JsonFileRepository
import net.transgressoft.musicott.config.ApplicationPaths
import org.apache.commons.lang3.SystemUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Configuration
open class ApplicationConfiguration @Autowired constructor(private val applicationPaths: ApplicationPaths) {

    init {
        initializeApplicationFiles()
    }

    private fun initializeApplicationFiles() {
        try {
            applicationPaths.settingsPath.parent?.let { Files.createDirectories(it) }

            createFileIfNotExists(applicationPaths.settingsPath)
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
    open fun operativeSystemKeyModifier(): KeyCombination.Modifier =
        if (SystemUtils.IS_OS_MAC_OSX)
            KeyCombination.META_DOWN
        else
            KeyCombination.CONTROL_DOWN

    @Bean
    open fun settingsRepository(): SettingsRepository =
        SettingsRepository(applicationPaths.settingsPath.toFile())

    @Bean
    open fun audioLibrary(): ObservableAudioLibrary =
        ObservableAudioLibrary(
            JsonFileRepository(applicationPaths.audioItemsPath.toFile(), ObservableAudioItemMapSerializer))

    @Bean
    open fun playlistHierarchy(audioLibrary: ObservableAudioLibrary): ObservablePlaylistHierarchy =
        ObservablePlaylistHierarchy(
            JsonFileRepository(applicationPaths.playlistsPath.toFile(), ObservablePlaylistMapSerializer),
            audioLibrary)

    @Bean
    open fun waveformRepository(
        observablePlaylistHierarchy: ObservablePlaylistHierarchy,
        audioLibrary: ObservableAudioLibrary
    ): AudioWaveformRepository<AudioWaveform, ObservableAudioItem> {

        val audioWaveformRepository =
            DefaultAudioWaveformRepository<ObservableAudioItem>(
                JsonFileRepository(applicationPaths.waveformsPath.toFile(), AudioWaveformMapSerializer))

        audioLibrary.subscribe(observablePlaylistHierarchy)
        audioLibrary.subscribe(audioWaveformRepository)
        return audioWaveformRepository
    }
}