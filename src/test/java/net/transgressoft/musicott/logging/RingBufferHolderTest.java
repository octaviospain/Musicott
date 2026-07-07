package net.transgressoft.musicott.logging;

import ch.qos.logback.classic.Level;
import net.transgressoft.musicott.logging.RingBufferHolder.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RingBufferHolder}, pinning the eviction cap, WARN/ERROR counter,
 * newline normalisation, atomic seed-and-subscribe, level-carrying records, and test-isolation
 * reset contracts.
 */
@DisplayName("RingBufferHolder")
class RingBufferHolderTest {

    @BeforeEach
    void resetBuffer() {
        RingBufferHolder.INSTANCE.resetForTest();
    }

    @Test
    @DisplayName("evicts oldest record when buffer reaches capacity")
    void evictsOldestRecordWhenBufferReachesCapacity() {
        String firstLine = "INFO  net.transgressoft.first - first message\n";
        RingBufferHolder.INSTANCE.add(firstLine, Level.INFO);

        // Fill the buffer to capacity with subsequent lines
        IntStream.rangeClosed(2, RingBufferHolder.MAX_RECORDS).forEach(i ->
            RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.x - line " + i + "\n", Level.INFO)
        );

        assertThat(RingBufferHolder.INSTANCE.snapshot()).hasSize(RingBufferHolder.MAX_RECORDS);

        // Adding one more record beyond capacity evicts the oldest
        String newestLine = "INFO  net.transgressoft.x - newest line\n";
        RingBufferHolder.INSTANCE.add(newestLine, Level.INFO);

        List<String> snapshot = RingBufferHolder.INSTANCE.snapshot();
        assertThat(snapshot).hasSize(RingBufferHolder.MAX_RECORDS);
        assertThat(snapshot).doesNotContain(firstLine);
        assertThat(snapshot.get(snapshot.size() - 1)).isEqualTo(newestLine);
    }

    @Test
    @DisplayName("increments warn/error counter for WARN and ERROR but not DEBUG or INFO")
    void incrementsWarnErrorCounterForWarnAndErrorButNotDebugOrInfo() {
        assertThat(RingBufferHolder.INSTANCE.warnErrorCount()).isZero();

        RingBufferHolder.INSTANCE.add("DEBUG net.transgressoft.x - debug msg\n", Level.DEBUG);
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.x - info msg\n", Level.INFO);

        assertThat(RingBufferHolder.INSTANCE.warnErrorCount())
            .as("DEBUG and INFO must not increment the warn/error counter")
            .isZero();

        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.x - warn msg\n", Level.WARN);
        assertThat(RingBufferHolder.INSTANCE.warnErrorCount())
            .as("WARN must increment the counter by 1")
            .isEqualTo(1L);

        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.x - error msg\n", Level.ERROR);
        assertThat(RingBufferHolder.INSTANCE.warnErrorCount())
            .as("ERROR must increment the counter to 2")
            .isEqualTo(2L);
    }

    @Test
    @DisplayName("level filter snapshot returns only records at or above the requested level")
    void levelFilterSnapshotReturnsOnlyRecordsAtOrAboveRequestedLevel() {
        RingBufferHolder.INSTANCE.add("DEBUG net.transgressoft.x - debug line\n", Level.DEBUG);
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.x - info line\n", Level.INFO);
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.x - warn line\n", Level.WARN);
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.x - error line\n", Level.ERROR);

        // Filter at WARN level: only WARN and ERROR tokens should pass
        List<String> warnAndAbove = RingBufferHolder.INSTANCE.snapshot().stream()
            .filter(line -> {
                String token = line.stripLeading().split("\\s+")[0];
                return Level.toLevel(token, Level.OFF).isGreaterOrEqual(Level.WARN);
            })
            .collect(Collectors.toList());

        assertThat(warnAndAbove).hasSize(2);
        assertThat(warnAndAbove).anyMatch(l -> l.contains("warn line"));
        assertThat(warnAndAbove).anyMatch(l -> l.contains("error line"));
        assertThat(warnAndAbove).noneMatch(l -> l.contains("debug line"));
        assertThat(warnAndAbove).noneMatch(l -> l.contains("info line"));
    }

    @Test
    @DisplayName("appends a trailing newline to lines that lack one")
    void appendsTrailingNewlineToLinesThatLackOne() {
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.x - no newline", Level.WARN);
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.x - has newline\n", Level.INFO);

        List<String> snapshot = RingBufferHolder.INSTANCE.snapshot();
        assertThat(snapshot.get(0))
            .as("a line stored without a newline is normalised to end with one")
            .isEqualTo("WARN  net.transgressoft.x - no newline\n");
        assertThat(snapshot.get(1))
            .as("an already-terminated line is stored unchanged")
            .isEqualTo("INFO  net.transgressoft.x - has newline\n");
    }

    @Test
    @DisplayName("snapshotRecords preserves the level of each record")
    void snapshotRecordsPreservesTheLevelOfEachRecord() {
        RingBufferHolder.INSTANCE.add("DEBUG net.transgressoft.x - d\n", Level.DEBUG);
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.x - e\n", Level.ERROR);

        List<LogRecord> records = RingBufferHolder.INSTANCE.snapshotRecords();
        assertThat(records).hasSize(2);
        assertThat(records.get(0).level()).isEqualTo(Level.DEBUG);
        assertThat(records.get(1).level()).isEqualTo(Level.ERROR);
    }

    @Test
    @DisplayName("attachAndSnapshot delivers each record exactly once across the seed handoff")
    void attachAndSnapshotDeliversEachRecordExactlyOnceAcrossTheSeedHandoff() {
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.x - before attach\n", Level.INFO);

        List<LogRecord> tailed = new ArrayList<>();
        List<LogRecord> seed = RingBufferHolder.INSTANCE.attachAndSnapshot(tailed::add);

        // The pre-existing record is in the seed snapshot and must NOT also be tailed.
        assertThat(seed).hasSize(1);
        assertThat(seed.get(0).text()).contains("before attach");
        assertThat(tailed).isEmpty();

        // A record added after attach is delivered to the listener and is NOT in the seed.
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.x - after attach\n", Level.WARN);
        assertThat(tailed).hasSize(1);
        assertThat(tailed.get(0).text()).contains("after attach");
        assertThat(tailed.get(0).level()).isEqualTo(Level.WARN);
    }

    @Test
    @DisplayName("removeLiveTailListener only clears the currently registered listener")
    void removeLiveTailListenerOnlyClearsTheCurrentlyRegisteredListener() {
        List<LogRecord> current = new ArrayList<>();
        Consumer<LogRecord> listener = current::add;
        RingBufferHolder.INSTANCE.attachAndSnapshot(listener);

        // A stale caller trying to remove a different listener must not detach the active one.
        RingBufferHolder.INSTANCE.removeLiveTailListener(r -> {});
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.x - still tailed\n", Level.ERROR);
        assertThat(current).as("active listener survives a mismatched remove").hasSize(1);

        // Removing the actual listener reference detaches it; no further records are tailed.
        RingBufferHolder.INSTANCE.removeLiveTailListener(listener);
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.x - not tailed\n", Level.ERROR);
        assertThat(current).as("listener stops receiving records once removed").hasSize(1);
    }

    @Test
    @DisplayName("resetForTest empties the buffer and zeroes the warn/error counter")
    void resetForTestEmptiesBufferAndZeroesCounter() {
        RingBufferHolder.INSTANCE.add("ERROR net.transgressoft.x - some error\n", Level.ERROR);
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.x - some warning\n", Level.WARN);

        assertThat(RingBufferHolder.INSTANCE.snapshot()).isNotEmpty();
        assertThat(RingBufferHolder.INSTANCE.warnErrorCount()).isGreaterThan(0L);

        RingBufferHolder.INSTANCE.resetForTest();

        assertThat(RingBufferHolder.INSTANCE.snapshot()).isEmpty();
        assertThat(RingBufferHolder.INSTANCE.warnErrorCount()).isZero();
    }
}
