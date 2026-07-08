package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Published to display a user-facing error message with a {@code title} and
 * {@code content} body. Use {@link ExceptionEvent} instead when the source is a
 * {@link Throwable} whose stack trace should be surfaced.
 */
public class ErrorEvent extends ApplicationEvent {

    public final String title;
    public final String content;

    public ErrorEvent(String title, String content, Object source) {
        super(source);
        this.title = title;
        this.content = content;
    }
}
