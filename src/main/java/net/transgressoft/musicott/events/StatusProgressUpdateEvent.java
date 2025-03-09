package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class StatusProgressUpdateEvent extends ApplicationEvent {

    public final double statusProgress;

    public StatusProgressUpdateEvent(double statusProgress, Object object) {
        super(object);
        this.statusProgress = statusProgress;
    }
}
