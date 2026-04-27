package net.transgressoft.musicott.view.custom.table;

import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

/**
 * @author Octavio Calleya
 */
@Component
@Scope("prototype")
public class SimpleAudioItemTableView extends AudioItemTableViewBase {

    private static final String SIMPLE_TRACK_TABLE_STYLE = "/css/tracktable-trackareaset.css";

    @SuppressWarnings ("unchecked")
    public SimpleAudioItemTableView(ApplicationEventPublisher applicationEventPublisher) {
        super(applicationEventPublisher);

        getColumns().addAll(trackNumberCol, nameCol, totalTimeCol);
        getSortOrder().add(trackNumberCol);
        getStylesheets().add(getClass().getResource(SIMPLE_TRACK_TABLE_STYLE).toExternalForm());
        getStyleClass().add("no-header");
        setFixedCellSize(25);
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);

        nameCol.setStyle(CENTER_LEFT_ALIGN);
        artistCol.setPrefWidth(150);
        artistCol.setStyle(CENTER_LEFT_ALIGN);
        trackNumberCol.setMinWidth(24);
        trackNumberCol.setMaxWidth(24);
        totalTimeCol.setMinWidth(68);
        totalTimeCol.setMaxWidth(68);
    }

    public void removeArtistColumn() {
        getColumns().remove(artistCol);
    }

    public void placeArtistColumn() {
        if (! getColumns().contains(artistCol)) {
            getColumns().add(2, artistCol);
        }
    }
}
