package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Published when the user requests to import an M3U playlist file into the music library.
 * The listener resolves the actual file path via a {@link javafx.stage.FileChooser} dialog.
 *
 * @author Octavio Calleya
 */
public class ImportPlaylistsFromM3uEvent extends ApplicationEvent {

    public ImportPlaylistsFromM3uEvent(Object source) {
        super(source);
    }
}
