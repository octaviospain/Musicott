package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Published when a component requests that the log viewer window be opened.
 *
 * <p>Consumers must not block the publishing thread. The viewer opens asynchronously
 * on the JavaFX Application Thread via {@code Platform.runLater}.
 *
 * @author Octavio Calleya
 */
public class OpenLogViewerEvent extends ApplicationEvent {

    public OpenLogViewerEvent(Object source) {
        super(source);
    }
}
