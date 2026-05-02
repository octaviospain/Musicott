package net.transgressoft.musicott.view.custom.table;

import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test verifying the construction-time metadata of {@link FullAudioItemTableView}:
 * column ordering, per-column minimum widths, and the resize policy identity.
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("FullAudioItemTableView")
class FullAudioItemTableViewIT {

    FullAudioItemTableView tableView;

    @Start
    void start(Stage stage) {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        tableView = new FullAudioItemTableView(publisher);
        stage.setScene(new Scene(tableView, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("places Title column at index 0")
    void placesTitleColumnAtIndex0() {
        assertThat(tableView.getColumns().get(0).getText()).isEqualTo("Title");
    }

    @Test
    @DisplayName("orders columns Title-first per iTunes convention")
    void ordersColumnsTitleFirstPerITunesConvention() {
        List<String> headers = tableView.getColumns().stream()
                .map(TableColumn::getText)
                .collect(toList());
        assertThat(headers).containsExactly(
                "Title", "Artist", "Album", "Genre", "Label", "BPM", "Duration",
                "Year", "Size", "Track Num", "Disc Num", "Album Artist", "Comments",
                "BitRate", "Plays", "Modified", "Added");
    }

    @Test
    @DisplayName("enforces per-column minimum widths")
    void enforcesPerColumnMinimumWidths() {
        Map<String, Double> expected = Map.ofEntries(
                Map.entry("Title", 120.0),
                Map.entry("Artist", 100.0),
                Map.entry("Album", 100.0),
                Map.entry("Genre", 100.0),
                Map.entry("Label", 100.0),
                Map.entry("BPM", 50.0),
                Map.entry("Duration", 80.0),
                Map.entry("Year", 60.0),
                Map.entry("Size", 60.0),
                Map.entry("Track Num", 80.0),
                Map.entry("Disc Num", 80.0),
                Map.entry("Album Artist", 100.0),
                Map.entry("Comments", 100.0),
                Map.entry("BitRate", 70.0),
                Map.entry("Plays", 50.0),
                Map.entry("Modified", 100.0),
                Map.entry("Added", 100.0));
        for (TableColumn<?, ?> col : tableView.getColumns()) {
            assertThat(col.getMinWidth())
                    .as("min width of %s", col.getText())
                    .isEqualTo(expected.get(col.getText()));
        }
    }

    @Test
    @DisplayName("uses UNCONSTRAINED_RESIZE_POLICY")
    void usesUnconstrainedResizePolicy() {
        // The unconstrained policy lets the TableView's native horizontal AND vertical
        // scrollbars engage when content overflows the viewport. Constrained policies disable
        // the native horizontal scrollbar.
        assertThat(tableView.getColumnResizePolicy())
                .isSameAs(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
}
