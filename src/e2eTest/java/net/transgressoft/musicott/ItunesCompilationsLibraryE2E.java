package net.transgressoft.musicott;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.commons.music.itunes.ImportResult;
import net.transgressoft.commons.music.itunes.ItunesLibraryTestFixture;
import net.transgressoft.commons.music.itunes.ItunesImportPolicy;
import net.transgressoft.commons.music.itunes.ItunesPlaylist;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.test.itunes.CompilationsItunesLibraryExpectations;
import net.transgressoft.musicott.view.MainController;
import net.transgressoft.musicott.view.NavigationController;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitFor;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * End-to-end regression for the UAT-scale iTunes import and search flow.
 */
@SpringBootTest(classes = {MusicottApplication.class, ItunesCompilationsLibraryE2E.E2eTestConfiguration.class})
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ItunesCompilationsLibraryE2E {

    static Stage testStage;

    @Autowired
    FxWeaver fxWeaver;

    @Autowired
    MediaImportService mediaImportService;

    @Autowired
    ObservableAudioLibrary audioLibrary;

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
    @DisplayName("Musicott imports the Compilations iTunes library and filters All Tracks and Artists views")
    void importsCompilationsItunesLibraryAndFiltersAllTracksAndArtistsViews(FxRobot fxRobot) throws Exception {
        var fixture = ItunesLibraryTestFixture.prepare(
                tempDir,
                ItunesCompilationsLibraryE2E.class,
                "/itunes/compilations-library.xml");
        var expectations = CompilationsItunesLibraryExpectations.load();
        var parsedLibrary = mediaImportService.parseLibrary(fixture.xmlPath());
        List<ItunesPlaylist> selectedPlaylists = parsedLibrary.getPlaylists().stream()
                .filter(playlist -> !playlist.isFolder())
                .toList();

        // Importing the full compilations library (~1170 tracks) is I/O-bound and runs several times
        // slower on the Windows and macOS CI runners than on Linux; allow generous headroom so the
        // import future completing late on a cold runner is not mistaken for a failure.
        ImportResult result = mediaImportService.importSelectedPlaylists(selectedPlaylists, importPolicy())
                .get(180, TimeUnit.SECONDS);
        waitForFxEvents();

        assertThat(result.getUnresolved()).isEmpty();
        waitFor(30, TimeUnit.SECONDS, () -> audioLibrary.getAudioItemsProperty().size() == expectations.trackCount());
        assertThat(audioLibrary.getAudioItemsProperty()).hasSize(expectations.trackCount());
        waitForArtistCatalogs();

        selectNavigationMode(fxRobot, NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
        FullAudioItemTableView table = visibleTrackTable(fxRobot);
        waitFor(10, TimeUnit.SECONDS, () -> table.getItems().size() == expectations.trackCount());
        assertThat(table.getItems()).hasSize(expectations.trackCount());
        assertAllTracksFilters(fxRobot, table, expectations);

        selectNavigationMode(fxRobot, NavigationController.NavigationMode.ARTISTS);
        ListView<?> artistsList = fxRobot.lookup("#artistsListView").queryListView();
        waitFor(10, TimeUnit.SECONDS, () -> !artistsList.getItems().isEmpty());
        assertArtistsFilters(fxRobot, artistsList, expectations);
    }

    private static ItunesImportPolicy importPolicy() {
        Set<AudioFileType> acceptedFileTypes = Set.copyOf(Arrays.asList(AudioFileType.values()));
        return new ItunesImportPolicy(false, true, false, acceptedFileTypes);
    }

    private void waitForArtistCatalogs() throws TimeoutException {
        try {
            waitFor(30, TimeUnit.SECONDS, () -> audioLibrary.getAudioItemsProperty().stream()
                    .allMatch(item -> audioLibrary.getArtistCatalog(item.getArtist()).isPresent()));
        } catch (TimeoutException e) {
            long missingCount = audioLibrary.getAudioItemsProperty().stream()
                    .filter(item -> audioLibrary.getArtistCatalog(item.getArtist()).isEmpty())
                    .count();
            var sample = audioLibrary.getAudioItemsProperty().stream()
                    .filter(item -> audioLibrary.getArtistCatalog(item.getArtist()).isEmpty())
                    .limit(10)
                    .map(item -> item.getTitle() + " / " + item.getArtist().getName() + " / " + item.getUniqueId())
                    .toList();
            throw new AssertionError("Missing artist catalogs for " + missingCount + " audio items: " + sample, e);
        }
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

    private static void assertAllTracksFilters(
            FxRobot fxRobot,
            FullAudioItemTableView table,
            CompilationsItunesLibraryExpectations expectations) throws Exception {
        clearSearch(fxRobot);
        assertThat(table.getItems()).hasSize(expectations.trackCount());
        for (var filter : expectations.allTracksFilters()) {
            search(fxRobot, filter.query());
            waitForSize(
                    () -> table.getItems().size(),
                    filter.expectedCount(),
                    "All Tracks filter: " + filter.name());
            assertThat(table.getItems()).as("All Tracks filter: " + filter.name()).hasSize(filter.expectedCount());
            clearSearch(fxRobot);
            waitForSize(() -> table.getItems().size(), expectations.trackCount(), "All Tracks clear search");
        }
    }

    private static void assertArtistsFilters(
            FxRobot fxRobot,
            ListView<?> artistsList,
            CompilationsItunesLibraryExpectations expectations) throws Exception {
        clearSearch(fxRobot);
        int unfilteredCount = artistsList.getItems().size();
        assertThat(unfilteredCount).isPositive();
        for (var filter : expectations.artistsFilters()) {
            search(fxRobot, filter.query());
            waitForMinimumSize(
                    () -> artistsList.getItems().size(),
                    filter.expectedCount(),
                    "Artists filter: " + filter.name());
            assertThat(artistsList.getItems()).as("Artists filter: " + filter.name())
                    .hasSizeGreaterThanOrEqualTo(filter.expectedCount());
            clearSearch(fxRobot);
            waitForSize(() -> artistsList.getItems().size(), unfilteredCount, "Artists clear search");
        }
    }

    private static void waitForSize(IntSupplier actualSize, int expectedSize, String assertionName) throws Exception {
        try {
            waitFor(30, TimeUnit.SECONDS, () -> actualSize.getAsInt() == expectedSize);
        } catch (TimeoutException ex) {
            throw new AssertionError(assertionName + " expected " + expectedSize
                    + " items but found " + actualSize.getAsInt(), ex);
        }
    }

    private static void waitForMinimumSize(IntSupplier actualSize, int expectedSize, String assertionName) throws Exception {
        try {
            waitFor(30, TimeUnit.SECONDS, () -> actualSize.getAsInt() >= expectedSize);
        } catch (TimeoutException ex) {
            throw new AssertionError(assertionName + " expected at least " + expectedSize
                    + " items but found " + actualSize.getAsInt(), ex);
        }
    }

    private static void search(FxRobot fxRobot, String query) {
        TextField search = fxRobot.lookup("#searchTextField").queryAs(TextField.class);
        Platform.runLater(() -> search.setText(query));
        waitForFxEvents();
    }

    private static void clearSearch(FxRobot fxRobot) {
        search(fxRobot, "");
    }

    @TestConfiguration
    static class E2eTestConfiguration {

        @Bean
        @Primary
        public MusicottApplication.ApplicationPaths applicationPaths() throws IOException {
            Path tempDir = Files.createTempDirectory("musicott-itunes-e2e");
            tempDir.toFile().deleteOnExit();
            return new MusicottApplication.ApplicationPaths(
                    tempDir.resolve("audioItems.json"),
                    tempDir.resolve("playlists.json"),
                    tempDir.resolve("waveforms.json")
            );
        }

        @Bean
        @Primary
        public AlertFactory alertFactory() {
            AlertFactory alertFactory = Mockito.mock(AlertFactory.class);
            when(alertFactory.itunesImportResultAlert(any())).thenAnswer(invocation -> autoClosingAlert());
            when(alertFactory.importInProgressAlert()).thenAnswer(invocation -> autoClosingAlert());
            return alertFactory;
        }

        private static Alert autoClosingAlert() {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            Platform.runLater(alert::close);
            return alert;
        }
    }
}
