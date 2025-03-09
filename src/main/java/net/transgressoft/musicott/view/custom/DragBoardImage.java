package net.transgressoft.musicott.view.custom;

import javafx.scene.image.Image;

/**
 * @author Octavio Calleya
 */
public class DragBoardImage extends Image {

    private static final String DRAG_BOARD_ICON_PATH = "/icons/dragboard-icon.png"; //TODO refactor

    public DragBoardImage() {
        super(DragBoardImage.class.getResourceAsStream(DRAG_BOARD_ICON_PATH));
    }
}
