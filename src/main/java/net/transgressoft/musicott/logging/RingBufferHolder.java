package net.transgressoft.musicott.logging;

import ch.qos.logback.classic.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Shared ring buffer that bridges the logback {@link RingBufferAppender} and any Spring-managed
 * component that needs to read captured log records. The appender calls {@link #add} from logback
 * background threads; Spring beans read the buffer from the JavaFX Application Thread.
 *
 * <p>Each record is stored as a {@link LogRecord} pairing the pre-encoded text with its logback
 * {@link Level}, so consumers can filter by level without re-parsing the encoded string. The
 * buffer is bounded at {@link #MAX_RECORDS}; the oldest record is evicted whenever the cap is
 * reached, ensuring a deterministic memory ceiling regardless of how many log events are produced.
 * {@link #add}, {@link #snapshot}, {@link #snapshotRecords} and {@link #attachAndSnapshot} acquire
 * the same {@link ReentrantLock} to guarantee a consistent view across threads.
 *
 * <p>A monotonically increasing WARN+ERROR counter is maintained separately via an
 * {@link AtomicLong}, so callers can read the count without acquiring the buffer lock. It supports
 * a best-effort per-operation delta (capture the count before an operation, subtract after). The
 * delta is reliable only for strictly serialized operations: because the counter is process-wide,
 * any WARN/ERROR emitted by an unrelated thread or a concurrent operation during the window is
 * attributed to whichever operation happens to be measuring, so the delta may over-count.
 *
 * <p>A single live-tail listener can be registered atomically together with a seed snapshot via
 * {@link #attachAndSnapshot}. Registering the listener and copying the buffer under the same lock
 * guarantees every record is delivered exactly once — either it is present in the returned
 * snapshot, or it is delivered to the listener, never both and never neither. The listener is
 * invoked on the logback appender thread, outside the buffer lock, so it must dispatch to the
 * JavaFX Application Thread itself (e.g. via {@code Platform.runLater}).
 *
 * @author Octavio Calleya
 */
public final class RingBufferHolder {

    /** Singleton instance; created once at class-load time before any Spring context. */
    public static final RingBufferHolder INSTANCE = new RingBufferHolder();

    /** Hard cap on the number of records held in the buffer. */
    public static final int MAX_RECORDS = 5_000;

    private final ArrayDeque<LogRecord> buffer = new ArrayDeque<>(MAX_RECORDS);
    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicLong warnErrorCount = new AtomicLong(0);

    /**
     * Live-tail listener called for each new record; {@code volatile} so the appender thread
     * always sees the latest assignment without holding the buffer lock. Registered and cleared
     * under {@link #lock} so it stays consistent with the seed snapshot returned by
     * {@link #attachAndSnapshot}.
     */
    private volatile Consumer<LogRecord> liveTailListener;

    private RingBufferHolder() {}

    /**
     * A single captured log record: the pre-encoded, newline-terminated text together with the
     * logback {@link Level} of the originating event.
     *
     * @param text  the encoded log line, always ending in a newline
     * @param level the logback level of the originating event
     */
    public record LogRecord(String text, Level level) {}

    /**
     * Appends {@code line} to the buffer, evicting the oldest record if the buffer is already
     * at capacity. The line is normalised to end with a newline so consumers can concatenate
     * records without depending on the encoder pattern. Increments the WARN+ERROR counter when
     * {@code level} is WARN or higher. The live-tail listener, if registered, is invoked after
     * the buffer lock is released.
     *
     * @param line  the pre-encoded log record string to store
     * @param level the logback level of the originating event
     */
    public void add(String line, Level level) {
        if (level.isGreaterOrEqual(Level.WARN)) {
            warnErrorCount.incrementAndGet();
        }
        String normalised = line.endsWith("\n") ? line : line + "\n";
        LogRecord record = new LogRecord(normalised, level);
        Consumer<LogRecord> listener;
        lock.lock();
        try {
            if (buffer.size() == MAX_RECORDS) {
                buffer.pollFirst();
            }
            buffer.addLast(record);
            listener = liveTailListener;
        } finally {
            lock.unlock();
        }
        // Invoke listener outside the lock to prevent deadlock if the FX thread
        // attempts a snapshot() while the listener is dispatching.
        if (listener != null) {
            listener.accept(record);
        }
    }

    /**
     * Returns a point-in-time snapshot of all held record texts, ordered oldest-first.
     * The returned list is a detached copy; mutations do not affect the buffer.
     *
     * @return a new list containing the encoded text of all currently buffered records
     */
    public List<String> snapshot() {
        lock.lock();
        try {
            List<String> texts = new ArrayList<>(buffer.size());
            for (LogRecord record : buffer) {
                texts.add(record.text());
            }
            return texts;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a point-in-time snapshot of all held records (text and level), ordered oldest-first.
     * The returned list is a detached copy; mutations do not affect the buffer.
     *
     * @return a new list containing all currently buffered records
     */
    public List<LogRecord> snapshotRecords() {
        lock.lock();
        try {
            return new ArrayList<>(buffer);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically registers {@code listener} as the live-tail listener and returns a seed snapshot
     * of the records already buffered. Because both happen under the same lock, every record is
     * delivered exactly once: records present at registration time are in the returned snapshot
     * and are not delivered to the listener; records added afterwards are delivered to the
     * listener and are not in the snapshot. This closes the gap between seeding a view and
     * subscribing to new records.
     *
     * @param listener the consumer to call with each new record added after registration
     * @return a detached, oldest-first snapshot of the records buffered at registration time
     */
    public List<LogRecord> attachAndSnapshot(Consumer<LogRecord> listener) {
        lock.lock();
        try {
            this.liveTailListener = listener;
            return new ArrayList<>(buffer);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the cumulative count of WARN and ERROR records ever added since the buffer
     * was created (or last reset). This value is readable without acquiring the buffer lock,
     * making it suitable for capturing a mark before an operation and computing the delta
     * after completion. The delta is a best-effort signal — see the class documentation for the
     * over-counting caveat under concurrent operations.
     *
     * @return cumulative WARN+ERROR record count
     */
    public long warnErrorCount() {
        return warnErrorCount.get();
    }

    /**
     * Deregisters {@code listener} as the live-tail listener, but only if it is the currently
     * registered one. The compare-and-clear avoids a later-registered listener being silently
     * dropped by a stale caller's detach.
     *
     * @param listener the listener to remove
     */
    public void removeLiveTailListener(Consumer<LogRecord> listener) {
        lock.lock();
        try {
            if (this.liveTailListener == listener) {
                this.liveTailListener = null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears the buffer, resets the WARN+ERROR counter to zero, and deregisters any live-tail
     * listener. Intended for test isolation only — call this in {@code @BeforeEach} to ensure a
     * clean buffer state between tests.
     */
    public void resetForTest() {
        lock.lock();
        try {
            buffer.clear();
            warnErrorCount.set(0);
            liveTailListener = null;
        } finally {
            lock.unlock();
        }
    }
}
