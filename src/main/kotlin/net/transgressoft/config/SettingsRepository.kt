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

import net.transgressoft.commons.music.audio.AudioFileType
import net.transgressoft.lirp.persistence.json.FlexibleJsonFileRepository
import net.transgressoft.lirp.persistence.json.primitives.ReactiveBoolean
import java.io.File

/**
 * A repository for managing application settings, backed by a JSON file.
 *
 * @param jsonFile The file used for persistence.
 */
class SettingsRepository(jsonFile: File) : FlexibleJsonFileRepository(jsonFile) {

    private val itunesImportMetadataPolicyProperty = getReactiveBoolean("itunes.policy.metadata.file", true)

    private val itunesImportHoldPlayCountPolicyProperty = getReactiveBoolean("itunes.policy.playCount.hold", true)

    private val itunesImportWriteMetadataPolicyProperty = getReactiveBoolean("itunes.policy.writeMetadata", true)

    private val itunesImportIgnoreNotFoundPolicyProperty = getReactiveBoolean("itunes.policy.ignoreNotFound", true)

    /**
     * The policy when importing music from an iTunes file.
     * True means the information from the file metadata will be used.
     * False means the information from iTunes will be used.
     */
    var itunesImportMetadataPolicy: Boolean
        get() = itunesImportMetadataPolicyProperty.value == true
        set(value) {
            itunesImportMetadataPolicyProperty.value = value
        }

    /**
     * The policy when importing the play count from iTunes files.
     */
    var itunesImportHoldPlayCountPolicy: Boolean
        get() = itunesImportHoldPlayCountPolicyProperty.value == true
        set(value) {
            itunesImportHoldPlayCountPolicyProperty.value = value
        }

    /**
     * The policy to write iTunes library information to the metadata of files.
     */
    var itunesImportWriteMetadataPolicy: Boolean
        get() = itunesImportWriteMetadataPolicyProperty.value == true
        set(value) {
            itunesImportWriteMetadataPolicyProperty.value = value
        }

    /**
     * The policy to ignore files that are not found in the iTunes library.
     */
    var itunesImportIgnoreNotFoundPolicy: Boolean
        get() = itunesImportIgnoreNotFoundPolicyProperty.value == true
        set(value) {
            itunesImportIgnoreNotFoundPolicyProperty.value = value
        }

    private val audioFilePolicies: Map<AudioFileType, ReactiveBoolean> =
        mapOf(
            AudioFileType.MP3 to getReactiveBoolean("itunes.policy.import.mp3", true),
            AudioFileType.M4A to getReactiveBoolean("itunes.policy.import.m4a", true),
            AudioFileType.WAV to getReactiveBoolean("itunes.policy.import.wav", true),
            AudioFileType.FLAC to getReactiveBoolean("itunes.policy.import.flac", true)
        )

    /**
     * The set of audio file types that are currently accepted for import.
     */
    val acceptedAudioFileExtensions: Set<AudioFileType>
        get() = audioFilePolicies
            .filterValues { it.value == true }
            .keys

    /**
     * Sets the accepted audio file extensions based on the provided list.
     * The `List` parameter type is enough and preferred over `MutableList`
     * if the list is only being read.
     */
    fun setAcceptedAudioFileExtensions(acceptedAudioFileExtensions: List<AudioFileType>) {
        audioFilePolicies.forEach { (extension, property) ->
            // Use property.value to update the ReactiveBoolean
            property.value = acceptedAudioFileExtensions.contains(extension)
        }
    }
}