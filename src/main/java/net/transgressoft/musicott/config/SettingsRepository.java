package net.transgressoft.musicott.config;

import javafx.scene.input.KeyCombination;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.commons.persistence.json.FlexibleJsonFileRepository;
import net.transgressoft.commons.persistence.json.primitives.ReactiveBoolean;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.transgressoft.commons.music.audio.AudioFileType.MP3;
import static net.transgressoft.commons.music.audio.AudioFileType.M4A;
import static net.transgressoft.commons.music.audio.AudioFileType.FLAC;
import static net.transgressoft.commons.music.audio.AudioFileType.WAV;

public class SettingsRepository extends FlexibleJsonFileRepository {

    public static final KeyCombination.Modifier OS_SPECIFIC_KEY_MODIFIER =
            SystemUtils.IS_OS_MAC_OSX ? KeyCombination.META_DOWN : KeyCombination.CONTROL_DOWN;

    /**
     * The policy when importing music from iTunes file.
     * True means that the information from the file metadata will be used. False means that the information from iTunes will be used.
     */
    private final ReactiveBoolean itunesImportMetadataPolicy = createReactiveBoolean("itunes.policy.metadata.file", true);

    /**
     * The policy when importing the play count from iTunes files.
     */
    private final ReactiveBoolean itunesImportHoldPlayCountPolicy = createReactiveBoolean("itunes.policy.playCount.hold", true);

    /**
     * The policy to write iTunes library information to the metadata of files.
     */
    private final ReactiveBoolean itunesImportWriteMetadataPolicy = createReactiveBoolean("itunes.policy.writeMetadata", true);

    /**
     * The policy to ignore files that are not found in the iTunes library.
     */
    private final ReactiveBoolean itunesImportIgnoreNotFoundPolicy = createReactiveBoolean("itunes.policy.ignoreNotFound", true);

    private final Map<AudioFileType, ReactiveBoolean> audioFilePolicies = Map.of(
            MP3, createReactiveBoolean("itunes.policy.import.mp3", true),
            M4A, createReactiveBoolean("itunes.policy.import.m4a", true),
            WAV, createReactiveBoolean("itunes.policy.import.wav", true),
            FLAC, createReactiveBoolean("itunes.policy.import.flac", true)
    );

    public SettingsRepository(File jsonFile) {
        super(jsonFile);
    }

    public void setItunesImportHoldPlayCountPolicy(boolean value) {
        itunesImportHoldPlayCountPolicy.setValue(value);
    }

    public boolean getItunesImportHoldPlayCountPolicy() {
        return Boolean.TRUE.equals(itunesImportHoldPlayCountPolicy.getValue());
    }

    public void setItunesImportMetadataPolicy(boolean value) {
        itunesImportMetadataPolicy.setValue(value);
    }

    public boolean getItunesImportMetadataPolicy() {
        return Boolean.TRUE.equals(itunesImportMetadataPolicy.getValue());
    }

    public void setItunesImportWriteMetadataPolicy(boolean value) {
        itunesImportWriteMetadataPolicy.setValue(value);
    }

    public boolean getItunesImportWriteMetadataPolicy() {
        return Boolean.TRUE.equals(itunesImportWriteMetadataPolicy.getValue());
    }

    public void setItunesImportIgnoreNotFoundPolicy(boolean value) {
        itunesImportIgnoreNotFoundPolicy.setValue(value);
    }

    public boolean getItunesImportIgnoreNotFoundPolicy() {
        return Boolean.TRUE.equals(itunesImportIgnoreNotFoundPolicy.getValue());
    }

    public Set<AudioFileType> getAcceptedAudioFileExtensions() {
        return audioFilePolicies.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue().getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void setAcceptedAudioFileExtensions(List<AudioFileType> acceptedAudioFileExtensions) {
        audioFilePolicies.forEach(
                (extension, property) -> property.setValue(acceptedAudioFileExtensions.contains(extension)));
    }
}
