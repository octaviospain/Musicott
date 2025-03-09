package net.transgressoft.musicott.view.custom;

import javafx.scene.image.Image;

/**
 * @author Octavio Calleya
 */
public enum ApplicationImage {

    DEFAULT_COVER(fromPath("/images/default-cover-image.png")),
    ABOUT_IMAGE(fromPath("/images/musicott-about-logo.png")),
    COMMON_ERROR(fromPath("/images/common-error.png")),
    DRAGBOARD_ICON(fromPath("/icons/dragboard-icon.png")),
    LASTFM_LOGO(fromPath("/images/lastfm-logo.png"));

    private final Image image;

    ApplicationImage(Image image) {
        this.image = image;
    }

    public Image get() {
        return image;
    }

    private static Image fromPath(String path) {
        return new Image(ApplicationImage.class.getResourceAsStream(path));
    }
}
