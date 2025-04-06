package net.transgressoft.musicott.config;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItemJsonRepository;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistJsonRepository;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformJsonRepository;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ApplicationConfiguration {

    private static final Path DEFAULT_APPLICATION_DIRECTORY = Paths.get((System.getProperty("user.home")), ".config", "musicott");
    private static final Path settingsPath = DEFAULT_APPLICATION_DIRECTORY.resolve("settings.json");
    private static final Path audioItemsPath = DEFAULT_APPLICATION_DIRECTORY.resolve("audioItems.json");
    private static final Path playlistsPath = DEFAULT_APPLICATION_DIRECTORY.resolve("playlists.json");
    private static final Path waveformsPath = DEFAULT_APPLICATION_DIRECTORY.resolve("waveforms.json");

    public ApplicationConfiguration() {
        try {
            Files.createDirectories(DEFAULT_APPLICATION_DIRECTORY);
            if (! Files.exists(settingsPath)) {
                Files.createFile(settingsPath);
            }
            if (! Files.exists(audioItemsPath)) {
                Files.createFile(audioItemsPath);
            }
            if (! Files.exists(playlistsPath)) {
                Files.createFile(playlistsPath);
            }
            if (! Files.exists(waveformsPath)) {
                Files.createFile(waveformsPath);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Could not create application files", exception);
        }
    }

    @Bean
    public SettingsRepository settingsRepository() {
        return new SettingsRepository(settingsPath.toFile());
    }

    @Bean
    public ObservableAudioItemJsonRepository audioRepository() {
        return new ObservableAudioItemJsonRepository("AudioItems", audioItemsPath.toFile());
    }

    @Bean
    public ObservablePlaylistJsonRepository playlistRepository(ObservableAudioItemJsonRepository audioRepository) {
        return ObservablePlaylistJsonRepository.Companion.loadExisting("Playlists", playlistsPath.toFile(), audioRepository);
    }

    @Bean
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository(ObservablePlaylistJsonRepository observablePlaylistJsonRepository,
                                                                                          ObservableAudioItemJsonRepository audioRepository) {
        var audioWaveformRepository = new AudioWaveformJsonRepository<ObservableAudioItem>("Waveforms", waveformsPath.toFile());
        audioRepository.subscribe(observablePlaylistJsonRepository.getAudioItemEventSubscriber());
        audioRepository.subscribe(audioWaveformRepository.getAudioItemEventSubscriber());
        return audioWaveformRepository;
    }
}
