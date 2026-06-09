package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Signals a request to start random playback from the active navigation context.
 *
 * <p>Published by {@code PlayerController} when the play/pause button is pressed
 * and no track is currently loaded. A listener in {@code MainController} resolves
 * the context-appropriate item pool and delegates to
 * {@code PlayerService.playRandom(Collection)}.
 *
 * @author Octavio Calleya
 */
public class PlayRandomFromContextEvent extends ApplicationEvent {

    public PlayRandomFromContextEvent(Object source) {
        super(source);
    }
}
