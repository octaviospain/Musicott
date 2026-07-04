package net.transgressoft.musicott;

import kotlin.Unit;
import net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog;
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
 * End-to-end regression proving that editing a track's artist propagates through the reactive model
 * to the Artists navigation view: the track leaves its previous artist catalog row and appears under
 * the new artist.
 *
 * <p>The edit is driven by publishing the {@link EditAudioItemsMetadataEvent} the editor's OK button
 * emits — the metadata editor is a modal {@code showAndWait} dialog whose UI mapping is covered by
 * {@code EditControllerIT}; this test exercises the downstream propagation the bug broke.
 */
@SpringBootTest(classes = {MusicottApplication.class, ArtistEditPropagationE2E.E2eTestConfiguration.class})
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArtistEditPropagationE2E {

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
    @DisplayName("editing a track artist moves it out of the previous artist catalog and into the new one")
    void editingTrackArtistMovesItBetweenArtistCatalogs(FxRobot fxRobot) throws Exception {
        ObservableAudioItem movingTrack = seedTrack("Solo", "Alpha Artist", "Alpha Album");
        ObservableAudioItem stableTrack = seedTrack("Duet", "Beta Artist", "Beta Album");

        waitFor(15, TimeUnit.SECONDS, () -> audioLibrary.getArtistCatalog("Alpha Artist").isPresent()
                && audioLibrary.getArtistCatalog("Beta Artist").isPresent());

        selectNavigationMode(fxRobot, NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
        FullAudioItemTableView table = visibleTrackTable(fxRobot);
        waitFor(10, TimeUnit.SECONDS, () -> table.getItems().contains(movingTrack) && table.getItems().contains(stableTrack));

        publishArtistEdit(movingTrack, "Gamma Artist");

        waitFor(15, TimeUnit.SECONDS, () -> "Gamma Artist".equals(movingTrack.getArtist().getName()));
        waitFor(15, TimeUnit.SECONDS, () -> !artistCatalogContains("Alpha Artist", movingTrack));
        waitFor(15, TimeUnit.SECONDS, () -> artistCatalogContains("Gamma Artist", movingTrack));

        // The untouched track stays put.
        assertThat(artistCatalogContains("Beta Artist", stableTrack)).isTrue();

        // The Artists navigation view (list of artist catalogs) reflects the move.
        selectNavigationMode(fxRobot, NavigationController.NavigationMode.ARTISTS);
        ListView<?> artistsList = fxRobot.lookup("#artistsListView").queryListView();
        waitFor(10, TimeUnit.SECONDS, () -> artistsList.getItems().stream()
                .anyMatch(item -> item instanceof ObservableArtistCatalog catalog
                        && "Gamma Artist".equals(catalog.getArtistProperty().get().getName())));
        assertThat(audioLibrary.getArtistCatalogsProperty())
                .anyMatch(catalog -> "Gamma Artist".equals(catalog.getArtistProperty().get().getName()))
                .noneMatch(catalog -> "Alpha Artist".equals(catalog.getArtistProperty().get().getName()));
    }

    private void publishArtistEdit(ObservableAudioItem track, String newArtist) {
        // Artist catalogs aggregate the track's involved artists, which include the album artist, so a
        // faithful "move this track to a new artist" edit changes both the primary artist and the
        // album artist — mirroring how the editor's Artist and Album Artist fields drive membership.
        AudioItemMetadataChange change = new AudioItemMetadataChange(
                null, Artist.of(newArtist), null, Artist.of(newArtist), null, null, null, null, null, null, null, null, null);
        Platform.runLater(() ->
                applicationEventPublisher.publishEvent(new EditAudioItemsMetadataEvent(Set.of(track), change, this)));
        waitForFxEvents();
    }

    private boolean artistCatalogContains(String artistName, ObservableAudioItem track) {
        return audioLibrary.getArtistCatalog(artistName)
                .map(catalog -> catalog.albumAudioItemsProperty(track.getAlbum().getName()).contains(track))
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
        Path target = Files.createTempFile(tempDir, "artist-e2e-" + label.replaceAll("\\W", "-") + "-", ".mp3");
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
            Path tempDir = Files.createTempDirectory("musicott-artist-edit-e2e");
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
