package net.transgressoft.musicott.view;

import java.util.Collection;

/**
 * @author Octavio Calleya
 */
public interface ErrorPresenter {

    void show(String message);

    void show(String message, Throwable exception);

    void show(String message, String content);

    void show(String message, String content, Throwable exception);

    void showWithExpandableContent(String message, Collection<String> messagesToBeExpanded);

    void showWithExpandableContent(String message, String content, Collection<String> messagesToBeExpanded);
}
