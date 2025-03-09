package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class StatusMessageUpdateEvent extends ApplicationEvent {

    public final String statusMessage;

    public StatusMessageUpdateEvent(String statusMessage, Object source) {
        super(source);
        this.statusMessage = statusMessage;
    }
}
