package net.transgressoft.musicott.view.custom;

import javafx.scene.image.Image;

import java.io.InputStream;

/**
 * @author Octavio Calleya
 */
public enum ApplicationImage {

    DEFAULT_COVER("/images/default-cover-image.png"),
    ABOUT_IMAGE("/images/musicott-about-logo.png"),
    COMMON_ERROR("/images/common-error.png"),
    DRAGBOARD_ICON("/icons/dragboard-icon.png"),
    APP_ICON("/icons/musicott.png");

    private final String path;
    private Image image;

    ApplicationImage(String path) {
        this.path = path;
    }

    public Image get() {
        if (image == null) {
            image = fromPath(path);
        }
        return image;
    }

    private static Image fromPath(String path) {
        InputStream stream = ApplicationImage.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing classpath resource: " + path);
        }
        return new Image(stream);
    }
}
