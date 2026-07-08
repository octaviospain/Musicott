package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Published to route a caught {@link Throwable} to the centralized error dialog,
 * which shows its message and an expandable stack trace.
 */
public class ExceptionEvent extends ApplicationEvent {

    public final Throwable exception;

    public ExceptionEvent(Throwable exception, Object source) {
        super(source);
        this.exception = exception;
    }
}
