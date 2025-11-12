package net.transgressoft.musicott.view.custom.table;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class FullAudioItemTableView extends AudioItemTableViewBase {

    @SuppressWarnings ("unchecked")
    public FullAudioItemTableView(ApplicationEventPublisher applicationEventPublisher) {
        super(applicationEventPublisher);
        getColumns().addAll(artistCol, nameCol, albumCol, genreCol, labelCol, bpmCol, totalTimeCol);
        getColumns().addAll(yearCol, sizeCol, trackNumberCol, discNumberCol, albumArtistCol, commentsCol);
        getColumns().addAll(bitRateCol, playCountCol, dateModifiedCol, dateAddedCol);
        setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);
        getSortOrder().add(dateAddedCol);
    }
}
