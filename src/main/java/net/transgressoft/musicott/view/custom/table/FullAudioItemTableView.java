package net.transgressoft.musicott.view.custom.table;

import javafx.scene.control.TableColumn;
import org.jspecify.annotations.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.*;

@Component
public class FullAudioItemTableView extends AudioItemTableViewBase {

    @SuppressWarnings ("unchecked")
    public FullAudioItemTableView(ApplicationEventPublisher applicationEventPublisher) {
        super(applicationEventPublisher);
        // Title-first column order — Title is the column the user reads most often.
        getColumns().addAll(nameCol, artistCol, albumCol, genreCol, labelCol, bpmCol, totalTimeCol);
        getColumns().addAll(yearCol, sizeCol, trackNumberCol, discNumberCol, albumArtistCol, commentsCol);
        getColumns().addAll(bitRateCol, playCountCol, dateModifiedCol, dateAddedCol);

        // Use the unconstrained resize policy so the TableView's own horizontal AND vertical
        // scrollbars engage natively when content overflows. Constrained policies disable the
        // horizontal scrollbar, which is incompatible with "scroll horizontally when the
        // window is narrower than the minimum table width"
        setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);

        // Replicate the flex-grow effect of a constrained policy by distributing horizontal
        // slack across a fixed set of "absorber" text columns (Artist, Album, Genre) — the
        // long-text columns where extra width is most useful for the user.
        //
        // The redistribute helper takes a "source" column to exclude from the absorbers when
        // computing targets:
        //   - On window resize: source is null → targets = all absorbers.
        //   - On a non-absorber column drag: source is that column → not an absorber, so
        //     targets = all absorbers.
        //   - On an absorber column drag (the user is actively resizing one of the three):
        //     targets = the other two absorbers, leaving the user's drag intact while the
        //     remaining absorbers pick up the slack.
        //
        // Effect: the trailing region after the last column is never blank — total column
        // width always equals table width (until the table is narrower than the column total
        // can shrink, at which point the native horizontal scrollbar engages).
        var redistribute = getTableColumnConsumer();

        // Window-resize trigger — source is null so all absorbers participate.
        widthProperty().addListener((obs, oldW, newW) -> redistribute.accept(null));

        // Column-drag trigger — every column reports its source so the absorber being
        // dragged is excluded from the targets (preserving the user's drag).
        for (TableColumn<?, ?> column : getColumns()) {
            TableColumn<?, ?> source = column;
            column.widthProperty().addListener((obs, oldV, newV) -> redistribute.accept(source));
        }

        getSortOrder().add(dateAddedCol);
    }

    private @NonNull Consumer<TableColumn<?, ?>> getTableColumnConsumer() {
        List<TableColumn<?, ?>> absorbers = List.of(artistCol, albumCol, genreCol);
        boolean[] inUpdate = {false};

        return source -> {
            if (inUpdate[0]) return;
            inUpdate[0] = true;
            try {
                List<TableColumn<?, ?>> targets = absorbers.stream()
                    .filter(absorber -> absorber != source)
                    .toList();
                if (targets.isEmpty()) return;
                double tableWidth = getWidth();
                double othersTotal = getColumns().stream()
                    .filter(column -> !targets.contains(column))
                    .mapToDouble(TableColumn::getWidth)
                    .sum();
                double available = tableWidth - othersTotal;
                double targetsPrefSum = targets.stream()
                    .mapToDouble(TableColumn::getPrefWidth)
                    .sum();
                if (targetsPrefSum <= 0) return;
                for (TableColumn<?, ?> target : targets) {
                    double share = (target.getPrefWidth() / targetsPrefSum) * available;
                    target.setPrefWidth(Math.max(share, target.getMinWidth()));
                }
            } finally {
                inUpdate[0] = false;
            }
        };
    }
}
