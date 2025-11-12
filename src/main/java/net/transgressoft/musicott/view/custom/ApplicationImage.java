package net.transgressoft.musicott.view.custom;

import javafx.scene.image.Image;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Octavio Calleya
 */
public enum ApplicationImage {

    DEFAULT_COVER(Paths.get("/images", "default-cover-image.png")),
    ABOUT_IMAGE(Paths.get("/images", "musicott-about-logo.png")),
    COMMON_ERROR(Paths.get("/images", "common-error.png")),
    DRAGBOARD_ICON(Paths.get("/icons", "dragboard-icon.png")),
    LASTFM_LOGO(Paths.get("/images", "lastfm-logo.png"));

    private final Path path;
    private Image image;

    ApplicationImage(Path path) {
        this.path = path;
    }

    public Image get() {
        if (image == null) {
            image = fromPath(path.toString());
        }
        return image;
    }

    private static Image fromPath(String path) {
        return new Image(ApplicationImage.class.getResourceAsStream(path));
    }
}
