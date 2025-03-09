package net.transgressoft.musicott.config;

import net.transgressoft.commons.data.json.FlexibleJsonFileRepository;
import net.transgressoft.commons.data.json.primitives.ReactiveBoolean;

import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class SettingsRepository extends FlexibleJsonFileRepository {

    public static final KeyCombination.Modifier OS_SPECIFIC_KEY_MODIFIER =
            SystemUtils.IS_OS_MAC_OSX ? KeyCodeCombination.META_DOWN : KeyCodeCombination.CONTROL_DOWN;

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

    /**
     * The audio file extensions that are accepted by the application.
     */
    private final ReactiveBoolean
            importMp3Policy = createReactiveBoolean("itunes.policy.import.mp3", true),
            importM4aPolicy = createReactiveBoolean("itunes.policy.import.m4a",true),
            importWavPolicy = createReactiveBoolean("itunes.policy.import.wav",true),
            importFlacPolicy = createReactiveBoolean("itunes.policy.import.flac",true);

    public SettingsRepository(File jsonFile) {
        super(jsonFile);
    }

    public void setItunesImportHoldPlayCountPolicy(boolean value) {
        itunesImportMetadataPolicy.setValue(value);
    }

    public boolean getItunesImportHoldPlayCountPolicy() {
        return Boolean.TRUE.equals(itunesImportMetadataPolicy.getValue());
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

    public Set<String> getAcceptedAudioFileExtensions() {
        var set = new LinkedHashSet<String>(4);
        if (Boolean.TRUE.equals(importMp3Policy.getValue()))
            set.add("mp3");
        if (Boolean.TRUE.equals(importM4aPolicy.getValue()))
            set.add("m4a");
        if (Boolean.TRUE.equals(importWavPolicy.getValue()))
            set.add("wav");
        if (Boolean.TRUE.equals(importFlacPolicy.getValue()))
            set.add("flac");
        return set;
    }

    public void setAcceptedAudioFileExtensions(String... extensions) {
        importMp3Policy.setValue(false);
        importM4aPolicy.setValue(false);
        importWavPolicy.setValue(false);
        importFlacPolicy.setValue(false);
        for (String extension : extensions) {
            switch (extension) {
                case "mp3":
                    importMp3Policy.setValue(true);
                    break;
                case "m4a":
                    importM4aPolicy.setValue(true);
                    break;
                case "wav":
                    importWavPolicy.setValue(true);
                    break;
                case "flac":
                    importFlacPolicy.setValue(true);
                    break;
            }
        }
    }
}
