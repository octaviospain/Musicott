package net.transgressoft.musicott.benchmark;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.persistence.fx.music.audio.FXAudioItemSqlTableDef;
import net.transgressoft.commons.persistence.fx.music.audio.ObservableAudioItemMapSerializerKt;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.commons.music.itunes.ItunesImportPolicy;
import net.transgressoft.commons.music.itunes.ItunesImportService;
import net.transgressoft.commons.music.itunes.ItunesLibrary;
import net.transgressoft.commons.music.itunes.ItunesLibraryParser;
import net.transgressoft.commons.music.itunes.ItunesPlaylist;
import net.transgressoft.lirp.persistence.json.JsonFileRepository;
import net.transgressoft.lirp.persistence.sql.SqliteRepository;
import javafx.application.Platform;
import kotlin.Unit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark measuring the time to load a pre-seeded audio library into memory at
 * three dataset scales (small ~85 tracks, medium ~1,200 tracks, large ~17,732 tracks)
 * across the two persistence backends: JSON file and SQLite.
 *
 * <p>Covers scenario (c) boot time with pre-existing data, and provides the direct
 * before/after comparison for the audio-repository migration from JSON to SQLite.
 *
 * <p><b>JSON backend</b> mirrors the legacy boot path: constructing a
 * {@link JsonFileRepository} with {@code loadOnInit = true} triggers {@code loadFromStore},
 * which deserializes the full JSON file, runs {@code applyRawInitializerSilently}, and
 * executes {@code reconcileDanglingRefs} (an O(n) reflection walk over all entities) —
 * the primary boot-time cost at large dataset scales.
 *
 * <p><b>SQLite backend</b> mirrors the current production boot path in
 * {@code ApplicationConfiguration.musicLibrary()}: constructing
 * {@link SqliteRepository#fileBacked} with {@code loadOnInit = true} loads rows from the
 * database file, avoiding the full-file JSON decode and reflective ref-reconciliation.
 *
 * <p><b>Seeding approach:</b> {@link #setup()} imports the matching iTunes XML into a
 * temporary {@link FXMusicLibrary} backed by a real repository of the selected backend and
 * waits for all pending writes to flush before closing. The resulting on-disk artifact
 * (a {@code .json} file or a {@code .db} database) is copied to a stable seed path; each
 * iteration restores it before the benchmark call. Only repository
 * construction-to-load-complete time is measured.
 *
 * <p><b>No Spring context:</b> Repositories are wired directly without
 * {@code @SpringBootTest} (Pitfall 1). The seeding step does require a JavaFX toolkit
 * (it imports into an {@link FXMusicLibrary}), initialized once per fork in
 * {@link #initFxToolkit()}.
 *
 * <p><b>Known limitation — seeding does not scale past the small tier.</b> Seeding via
 * {@link ItunesImportService} into an {@link FXMusicLibrary} floods {@code Platform.runLater}
 * with per-item observable-property updates faster than the single FX thread drains them;
 * at the medium ({@code ~1,173}) and large ({@code ~17,732}) tiers this exhausts the heap
 * (OutOfMemoryError during {@code @Setup}), independent of heap size or Monocle-vs-Gtk
 * toolkit. Only the small tier seeds reliably today. Profiling the audio repository at scale
 * is therefore done through {@link MediaImportBenchmark} (which uses the non-FX
 * {@code CoreMusicLibrary} and does not hit this path). Making the boot comparison run at
 * medium/large requires a seeding strategy that does not marshal every insert through the FX
 * thread — e.g. a bulk/event-suppressed import, or pre-generating the seed {@code .json}/
 * {@code .db} artifacts out of band.
 *
 * <p>Dataset XML paths follow D-08:
 * <ul>
 *   <li>small  &rarr; {@code ~/software/itunes-library-uat.xml}</li>
 *   <li>medium &rarr; {@code ~/software/musicott-benchmark-medium.xml}</li>
 *   <li>large  &rarr; {@code ~/software/musicott-benchmark-large.xml}</li>
 * </ul>
 *
 * <p>Run via {@code gradle jmh}. Results are written to
 * {@code build/reports/jmh/results.json}.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 3)
public class BootTimeBenchmark {

    /**
     * Dataset tier selector. Restricted to {@code small} because the FX-library seeding
     * step OOMs at the medium/large tiers (see the class-level "Known limitation" note);
     * widen this once a scale-safe seeding strategy is in place.
     */
    @Param({"small"})
    public String dataset;

    /**
     * Persistence backend under test: the legacy {@code json} file repository or the
     * current {@code sqlite} repository.
     */
    @Param({"json", "sqlite"})
    public String backend;

    private Path seedPath;
    private Path iterationPath;
    private Path tempDir;
    private AutoCloseable loadedRepo;

    private static volatile boolean fxToolkitStarted = false;

    /**
     * Initializes the JavaFX toolkit once per fork. Seeding imports into an
     * {@link FXMusicLibrary}, whose observable property updates require a running toolkit;
     * without this the import fails with {@code Toolkit not initialized}. Benchmarks are
     * local-only (run on a developer machine with a display), so a plain
     * {@link Platform#startup} suffices — no Stage is shown.
     */
    @Setup(Level.Trial)
    public void initFxToolkit() {
        if (!fxToolkitStarted) {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyRunning) {
                // Toolkit already initialized in this JVM — nothing to do.
            }
            Platform.setImplicitExit(false);
            fxToolkitStarted = true;
        }
    }

    /**
     * Generates the seed artifact for the current dataset tier and backend, then restores
     * a clean copy at {@code iterationPath} for the measured method to load. The seeding
     * and copy happen outside the measured method so only repository construction and load
     * are timed.
     *
     * @throws Exception if seeding or file copy operations fail
     */
    @Setup(Level.Iteration)
    public void setup() throws Exception {
        String home = System.getProperty("user.home");
        Path xmlPath = switch (dataset) {
            case "small"  -> Paths.get(home, "software", "itunes-library-uat.xml");
            case "medium" -> Paths.get(home, "software", "musicott-benchmark-medium.xml");
            case "large"  -> Paths.get(home, "software", "musicott-benchmark-large.xml");
            default -> throw new IllegalArgumentException("Unknown dataset tier: " + dataset);
        };

        tempDir = Files.createTempDirectory("musicott-bench-boot-" + dataset + "-" + backend + "-");

        if ("json".equals(backend)) {
            seedPath = tempDir.resolve("seed-audioItems.json");
            iterationPath = tempDir.resolve("audioItems.json");
            seedJson(xmlPath);
        } else {
            seedPath = tempDir.resolve("seed-audioItems.db");
            iterationPath = tempDir.resolve("audioItems.db");
            seedSqlite(xmlPath);
        }

        Files.copy(seedPath, iterationPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Constructs a repository of the selected backend pointed at the pre-seeded artifact
     * with {@code loadOnInit = true} and consumes it via {@link Blackhole} to prevent
     * dead-code elimination. Only construction-to-load-complete is timed.
     *
     * @param bh JMH blackhole that prevents the result from being optimised away
     */
    @Benchmark
    public void bootWithExistingData(Blackhole bh) {
        if ("json".equals(backend)) {
            var repo = new JsonFileRepository<>(
                    iterationPath.toFile(),
                    ObservableAudioItemMapSerializerKt.ObservableAudioItemMapSerializer()
            );
            loadedRepo = repo;
            bh.consume(repo);
        } else {
            var repo = SqliteRepository.fileBacked(iterationPath, FXAudioItemSqlTableDef.INSTANCE);
            loadedRepo = repo;
            bh.consume(repo);
        }
    }

    /**
     * Closes the loaded repository (releasing the SQLite connection pool when present) and
     * deletes the temp directory for this iteration.
     *
     * @throws Exception if cleanup fails
     */
    @TearDown(Level.Iteration)
    public void tearDown() throws Exception {
        if (loadedRepo != null) {
            loadedRepo.close();
            loadedRepo = null;
        }
        deleteDirectory(tempDir);
    }

    private void seedJson(Path xmlPath) throws Exception {
        Path seedAudioItemsJson = tempDir.resolve("seed-source-audioItems.json");
        Files.createFile(seedAudioItemsJson);

        FXMusicLibrary seedLibrary = FXMusicLibrary.builder()
                .audioRepository(new JsonFileRepository<>(
                        seedAudioItemsJson.toFile(),
                        ObservableAudioItemMapSerializerKt.ObservableAudioItemMapSerializer()
                ))
                .build();

        try {
            importInto(seedLibrary, xmlPath);
            // Allow the debounce window to flush all pending writes before closing.
            Thread.sleep(600);
        } finally {
            seedLibrary.close();
        }

        Files.copy(seedAudioItemsJson, seedPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void seedSqlite(Path xmlPath) throws Exception {
        Path seedSourceDb = tempDir.resolve("seed-source-audioItems.db");

        FXMusicLibrary seedLibrary = FXMusicLibrary.builder()
                .audioRepository(SqliteRepository.fileBacked(seedSourceDb, FXAudioItemSqlTableDef.INSTANCE))
                .build();

        try {
            importInto(seedLibrary, xmlPath);
            Thread.sleep(600);
        } finally {
            // Closing the repository checkpoints the WAL and closes the connection pool,
            // leaving a self-contained .db file to copy as the seed.
            seedLibrary.close();
        }

        Files.copy(seedSourceDb, seedPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private void importInto(FXMusicLibrary library, Path xmlPath) throws Exception {
        ItunesImportService<?, ?> importService = new ItunesImportService<>(library);
        ItunesLibrary itunesLibrary = ItunesLibraryParser.INSTANCE.parse(xmlPath);
        List<ItunesPlaylist> playlists = itunesLibrary.getPlaylists().stream()
                .filter(p -> !p.isFolder())
                .toList();

        importService
                .importAsync(playlists, itunesLibrary, importPolicy(), null, progress -> Unit.INSTANCE)
                .get(300, TimeUnit.SECONDS);
    }

    private static ItunesImportPolicy importPolicy() {
        Set<AudioFileType> acceptedFileTypes = Set.copyOf(Arrays.asList(AudioFileType.values()));
        return new ItunesImportPolicy(false, true, false, acceptedFileTypes);
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }
}
