package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

import java.nio.file.Path;

/**
 * Published to relocate the application data directory to {@code newDirectory},
 * where the audio library database, playlists, and waveform cache are stored.
 */
public class ChangeApplicationDirectoryEvent extends ApplicationEvent {

    // Path isn't Serializable on all impls; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final Path newDirectory;

    public ChangeApplicationDirectoryEvent(Path newDirectory, Object source) {
        super(source);
        this.newDirectory = newDirectory;
    }
}
