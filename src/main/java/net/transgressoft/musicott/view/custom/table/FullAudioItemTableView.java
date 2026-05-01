package net.transgressoft.musicott.view.custom.table;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class FullAudioItemTableView extends AudioItemTableViewBase {

    @SuppressWarnings ("unchecked")
    public FullAudioItemTableView(ApplicationEventPublisher applicationEventPublisher) {
        super(applicationEventPublisher);
        getColumns().addAll(artistCol, nameCol, albumCol, genreCol, labelCol, bpmCol, totalTimeCol);
        getColumns().addAll(yearCol, sizeCol, trackNumberCol, discNumberCol, albumArtistCol, commentsCol);
        getColumns().addAll(bitRateCol, playCountCol, dateModifiedCol, dateAddedCol);
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        getSortOrder().add(dateAddedCol);
    }
}
