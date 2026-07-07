package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Published whenever a component needs to update the status-bar text. Carries an optional
 * {@link #warnErrorDelta} count so the status label can signal how many WARN/ERROR records were
 * observed while an operation ran. A delta of {@code 0} means a clean run; a positive value
 * drives the amber clickable status signal.
 *
 * <p>The delta is derived from a process-wide WARN/ERROR counter bracketed around the operation,
 * so it is a best-effort signal rather than an exact attribution: WARN/ERROR records emitted by an
 * unrelated thread or a concurrent operation during the same window are counted too, and the delta
 * may over-count. It is reliable for strictly serialized operations (e.g. file/directory imports,
 * which are mutually exclusive). Consumers should treat any positive value as "warnings or errors
 * occurred — check the log viewer" rather than an exact per-operation total.
 *
 * <p>Use the two-argument constructor for intermediate progress messages or clean completions
 * (delta defaults to {@code 0}). Use the three-argument constructor at the final completion
 * publish point of any operation that brackets a per-operation ring-buffer mark/delta.
 *
 * @author Octavio Calleya
 */
public class StatusMessageUpdateEvent extends ApplicationEvent {

    /** The text to display in the status bar. */
    public final String statusMessage;

    /**
     * The best-effort number of WARN or ERROR records observed in the ring buffer while the
     * operation ran. {@code 0} means no warnings or errors were observed; a positive value drives
     * the amber clickable status signal. May over-count under concurrent operations — see the
     * class documentation.
     */
    public final int warnErrorDelta;

    /**
     * Creates a status update with no WARN/ERROR delta (clean run or intermediate progress).
     * All existing call sites that do not bracket a per-operation mark/delta use this constructor.
     *
     * @param statusMessage the text to display in the status bar
     * @param source        the object that published the event
     */
    public StatusMessageUpdateEvent(String statusMessage, Object source) {
        this(statusMessage, 0, source);
    }

    /**
     * Creates a status update carrying the per-operation WARN/ERROR count. Use this at the final
     * completion publish point of an operation that captures a ring-buffer mark at its start and
     * computes the delta on completion.
     *
     * @param statusMessage  the text to display in the status bar
     * @param warnErrorDelta non-negative count of WARN/ERROR records observed during the operation
     * @param source         the object that published the event
     */
    public StatusMessageUpdateEvent(String statusMessage, int warnErrorDelta, Object source) {
        super(source);
        this.statusMessage = statusMessage;
        this.warnErrorDelta = warnErrorDelta;
    }
}
