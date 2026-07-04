package net.transgressoft.musicott;

import kotlin.Unit;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.AlbumDetails;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.events.EditAudioItemsMetadataEvent;
import net.transgressoft.musicott.view.EditController.AudioItemMetadataChange;
import net.transgressoft.musicott.view.MainController;
import net.transgressoft.musicott.view.NavigationController;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static net.transgressoft.commons.music.audio.GenreExtensionsKt.parseGenre;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitFor;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * End-to-end regression proving that editing a track's album name propagates through the reactive
 * model to the Albums navigation view: the track leaves its previous album grouping and appears
 * under the new album.
 *
 * <p>The edit is driven by publishing the {@link EditAudioItemsMetadataEvent} the editor's OK button
 * emits — the metadata editor is a modal {@code showAndWait} dialog whose UI mapping is covered by
 * {@code EditControllerIT}; this test exercises the downstream propagation the bug broke.
 */
@SpringBootTest(classes = {MusicottApplication.class, AlbumEditPropagationE2E.E2eTestConfiguration.class})
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AlbumEditPropagationE2E {

    static Stage testStage;

    @Autowired
    FxWeaver fxWeaver;

    @Autowired
    ObservableAudioLibrary audioLibrary;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void beforeAll() throws Exception {
        testStage = FxToolkit.registerPrimaryStage();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        FxToolkit.setupFixture(() -> {
            Scene scene = new Scene(fxWeaver.loadView(MainController.class), 1200, 800);
            testStage.setScene(scene);
            testStage.show();
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        FxToolkit.setupFixture(() -> {
            if (testStage.isShowing()) {
                testStage.hide();
            }
            testStage.setScene(null);
        });
    }

    @AfterAll
    static void afterAll() throws Exception {
        FxToolkit.cleanupStages();
    }

    @Test
    @DisplayName("editing a track album name moves it out of the previous album grouping and into the new one")
    void editingTrackAlbumMovesItBetweenAlbumGroupings(FxRobot fxRobot) throws Exception {
        ObservableAudioItem movingTrack = seedTrack("Opener", "The Movers", "Original Album");
        ObservableAudioItem stableTrack = seedTrack("Closer", "The Keepers", "Untouched Album");

        waitFor(15, TimeUnit.SECONDS, () -> audioLibrary.getAlbum("Original Album").isPresent()
                && audioLibrary.getAlbum("Untouched Album").isPresent());

        selectNavigationMode(fxRobot, NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
        FullAudioItemTableView table = visibleTrackTable(fxRobot);
        waitFor(10, TimeUnit.SECONDS, () -> table.getItems().contains(movingTrack) && table.getItems().contains(stableTrack));

        publishAlbumNameEdit(movingTrack, "Relocated Album");

        waitFor(15, TimeUnit.SECONDS, () -> "Relocated Album".equals(movingTrack.getAlbum().getName()));
        waitFor(15, TimeUnit.SECONDS, () -> !albumGroupingContains("Original Album", movingTrack));
        waitFor(15, TimeUnit.SECONDS, () -> albumGroupingContains("Relocated Album", movingTrack));

        // The untouched track stays put.
        assertThat(albumGroupingContains("Untouched Album", stableTrack)).isTrue();

        // The Albums navigation projection the grid binds to reflects the move.
        selectNavigationMode(fxRobot, NavigationController.NavigationMode.ALBUMS);
        assertThat(audioLibrary.getAlbumsProperty())
                .noneMatch(album -> "Original Album".equals(album.getAlbumProperty().get().getName())
                        && !album.getTracksProperty().isEmpty());
        assertThat(audioLibrary.getAlbum("Relocated Album")).isPresent()
                .hasValueSatisfying(album -> assertThat(album.getTracksProperty()).contains(movingTrack));
    }

    private void publishAlbumNameEdit(ObservableAudioItem track, String newAlbumName) {
        AudioItemMetadataChange change = new AudioItemMetadataChange(
                null, null, newAlbumName, null, null, null, null, null, null, null, null, null, null);
        Platform.runLater(() ->
                applicationEventPublisher.publishEvent(new EditAudioItemsMetadataEvent(Set.of(track), change, this)));
        waitForFxEvents();
    }

    private boolean albumGroupingContains(String albumName, ObservableAudioItem track) {
        return audioLibrary.getAlbum(albumName)
                .map(album -> album.getTracksProperty().contains(track))
                .orElse(false);
    }

    private ObservableAudioItem seedTrack(String title, String artist, String albumName) throws Exception {
        Path fixture = copyFixture(title);
        AtomicReference<ObservableAudioItem> created = new AtomicReference<>();
        FxToolkit.setupFixture(() -> created.set(audioLibrary.createFromFile(fixture)));
        ObservableAudioItem item = created.get();
        FxToolkit.setupFixture(() -> item.mutate(it -> {
            it.setTitle(title);
            it.setArtist(Artist.of(artist));
            it.setGenres(parseGenre("Electronic"));
            it.setAlbum(new AlbumDetails(albumName, Artist.of(artist), false, (Short) null, Label.UNKNOWN));
            return Unit.INSTANCE;
        }));
        return item;
    }

    private Path copyFixture(String label) throws IOException {
        Path target = Files.createTempFile(tempDir, "album-e2e-" + label.replaceAll("\\W", "-") + "-", ".mp3");
        try (InputStream in = Objects.requireNonNull(
                getClass().getResourceAsStream("/testfiles/testeable.mp3"), "Missing test resource: /testfiles/testeable.mp3")) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static void selectNavigationMode(FxRobot fxRobot, NavigationController.NavigationMode mode) {
        ListView<NavigationController.NavigationMode> navigation =
                fxRobot.lookup("#navigationModeListView").queryListView();
        Platform.runLater(() -> navigation.getSelectionModel().select(mode));
        waitForFxEvents();
    }

    private static FullAudioItemTableView visibleTrackTable(FxRobot fxRobot) {
        return fxRobot.lookup(node -> node instanceof FullAudioItemTableView && node.isVisible())
                .queryAs(FullAudioItemTableView.class);
    }

    @TestConfiguration
    static class E2eTestConfiguration {

        @Bean
        @Primary
        public MusicottApplication.ApplicationPaths applicationPaths() throws IOException {
            Path tempDir = Files.createTempDirectory("musicott-album-edit-e2e");
            // File.deleteOnExit no-ops on a non-empty directory, and the framework writes the db/json
            // files into this dir, so it would leak on every run; recursively delete the tree instead.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(tempDir)));
            return new MusicottApplication.ApplicationPaths(
                    tempDir.resolve("audioItems.db"),
                    tempDir.resolve("playlists.json"),
                    tempDir.resolve("waveforms.json"));
        }

        private static void deleteRecursively(Path root) {
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best-effort cleanup on JVM shutdown; a leftover file is not worth failing on.
                    }
                });
            } catch (IOException ignored) {
                // The directory may already be gone; nothing to clean up.
            }
        }
    }
}
