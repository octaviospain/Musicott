package net.transgressoft.musicott.view.custom.table;

import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Octavio Calleya
 */
public class SimpleAudioItemTableView extends AudioItemTableViewBase {

    private static final String SIMPLE_TRACK_TABLE_STYLE = "/css/tracktable-trackareaset.css";

    @SuppressWarnings ("unchecked")
    public SimpleAudioItemTableView(ApplicationEventPublisher applicationEventPublisher) {
        super(applicationEventPublisher);

        getColumns().addAll(trackNumberCol, nameCol, totalTimeCol);
        getSortOrder().add(trackNumberCol);
        getStylesheets().add(getClass().getResource(SIMPLE_TRACK_TABLE_STYLE).toExternalForm());
        getStyleClass().add("no-header");
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        setFixedCellSize(25);

        nameCol.setMinWidth(150);
        nameCol.setPrefWidth(USE_COMPUTED_SIZE);
        nameCol.setStyle(CENTER_LEFT_ALIGN);
        artistCol.setPrefWidth(150);
        artistCol.setStyle(CENTER_LEFT_ALIGN);
        trackNumberCol.setMinWidth(24);
        trackNumberCol.setMaxWidth(24);
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
