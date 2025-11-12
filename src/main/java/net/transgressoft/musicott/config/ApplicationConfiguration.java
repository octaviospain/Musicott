package net.transgressoft.musicott.config;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.DefaultAudioWaveformRepository;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.commons.persistence.json.JsonFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.transgressoft.commons.fx.music.audio.ObservableAudioItemSerializerKt.ObservableAudioItemMapSerializer;
import static net.transgressoft.commons.fx.music.playlist.ObservablePlaylistSerializerKt.ObservablePlaylistMapSerializer;
import static net.transgressoft.commons.music.waveform.AudioWaveformSerializerKt.AudioWaveformMapSerializer;

@Configuration
public class ApplicationConfiguration {

    private final ApplicationPaths applicationPaths;

    @Autowired
    public ApplicationConfiguration(ApplicationPaths applicationPaths) {
        this.applicationPaths = applicationPaths;
        initializeApplicationFiles();
    }

    private void initializeApplicationFiles() {
        try {
            Path applicationDirectory = applicationPaths.settingsPath().getParent();
            if (applicationDirectory != null) {
                Files.createDirectories(applicationDirectory);
            }
            
            createFileIfNotExists(applicationPaths.settingsPath());
            createFileIfNotExists(applicationPaths.audioItemsPath());
            createFileIfNotExists(applicationPaths.playlistsPath());
            createFileIfNotExists(applicationPaths.waveformsPath());
        } catch (IOException exception) {
            throw new RuntimeException("Could not create application files", exception);
        }
    }

    private void createFileIfNotExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    @Bean
    public SettingsRepository settingsRepository() {
        return new SettingsRepository(applicationPaths.settingsPath().toFile());
    }

    @Bean
    public ObservableAudioLibrary audioLibrary() {
        return new ObservableAudioLibrary(
                new JsonFileRepository<>(applicationPaths.audioItemsPath().toFile(), ObservableAudioItemMapSerializer()));
    }

    @Bean
    public ObservablePlaylistHierarchy playlistHierarchy(ObservableAudioLibrary audioLibrary) {
        return new ObservablePlaylistHierarchy(
                new JsonFileRepository<>(applicationPaths.playlistsPath().toFile(), ObservablePlaylistMapSerializer()),
                audioLibrary);
    }

    @Bean
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository(ObservablePlaylistHierarchy observablePlaylistHierarchy,
                                                                                          ObservableAudioLibrary audioLibrary) {
        var audioWaveformRepository =
                new DefaultAudioWaveformRepository<ObservableAudioItem>(
                        new JsonFileRepository<>(applicationPaths.waveformsPath().toFile(), AudioWaveformMapSerializer()));

        audioLibrary.subscribe(observablePlaylistHierarchy.getAudioItemEventSubscriber());
        audioLibrary.subscribe(audioWaveformRepository.getAudioItemEventSubscriber());
        return audioWaveformRepository;
    }
}
