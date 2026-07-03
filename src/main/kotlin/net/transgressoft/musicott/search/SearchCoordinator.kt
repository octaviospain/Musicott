package net.transgressoft.musicott.search

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.transgressoft.musicott.events.StatusMessageUpdateEvent
import net.transgressoft.musicott.view.NavigationController.NavigationMode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

/**
 * Central Spring service that owns the asynchronous search lifecycle for all navigation modes.
 *
 * When [onQuery] is called (from the JavaFX Application Thread via [net.transgressoft.musicott.view.MainController]),
 * the coordinator:
 * 1. Cancels any in-flight job (including any pending reset or prior qualifying search).
 * 2. Increments the generation counter and assigns a new coroutine to [currentJob] so every
 *    path — both reset and qualifying — participates in the same cancel/coalesce discipline.
 * 3. For blank (empty or whitespace-only) queries, the coroutine resets all registered views on
 *    the FX thread without debouncing.
 * 4. For any non-blank query (even a single character), the coroutine debounces for [debounceMillis]
 *    milliseconds (default 400), then for EVERY registered view snapshots its backing collection on
 *    the FX thread via [Searchable.prepareSnapshot], computes match IDs off-thread on
 *    [Dispatchers.Default], and applies results on [Dispatchers.JavaFx].
 *
 * Every registered view is filtered on each query — not just the currently visible one — so all
 * navigation modes stay in sync: switching to another mode shows an already-filtered view, and
 * editing or clearing the query updates every view.
 *
 * A monotonically increasing generation counter prevents a slow earlier query from overwriting
 * results already produced by a newer query.
 *
 * Views register themselves by calling [register] from their FXML `initialize()` method.
 * No view controller is constructor-injected, avoiding circular Spring dependencies.
 *
 * The internal [CoroutineScope] is backed by a [SupervisorJob] so that a cancelled or failing
 * child job does not cascade to cancel sibling searches. The scope is cancelled on Spring
 * context shutdown via [@PreDestroy][PreDestroy].
 *
 * @param applicationEventPublisher used to publish "Searching…" status messages
 * @param dispatcher the background dispatcher used for [Searchable.computeMatchIds];
 *        defaults to [Dispatchers.Default] and injectable for deterministic testing
 * @param fxDispatcher the FX-thread dispatcher used for [Searchable.applyMatchIds];
 *        defaults to [Dispatchers.JavaFx] and injectable for deterministic testing
 * @param debounceMillis how long to wait before executing a non-blank query, in milliseconds;
 *        defaults to 400 ms and injectable so tests can pass 0 for deterministic execution
 */
@Service
class SearchCoordinator(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val fxDispatcher: CoroutineContext = Dispatchers.JavaFx,
    private val debounceMillis: Long = DEBOUNCE_MILLIS
) {
    private val logger = KotlinLogging.logger {}

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + dispatcher)

    @Volatile private var currentJob: Job? = null
    private val generation = AtomicLong(0)

    private val searchables = ConcurrentHashMap<NavigationMode, Searchable<*>>()

    companion object {
        private const val DEBOUNCE_MILLIS = 400L
    }

    /**
     * Registers a [Searchable] implementation for the given navigation [mode].
     *
     * Typically called from a view controller's `initialize()` method. Replaces any previously
     * registered [Searchable] for that mode.
     *
     * @param mode the navigation mode this [Searchable] handles
     * @param searchable the view implementing [Searchable] for [mode]
     */
    fun register(mode: NavigationMode, searchable: Searchable<*>) {
        searchables[mode] = searchable
        logger.debug { "Registered Searchable for mode $mode: ${searchable::class.simpleName}" }
    }

    /**
     * Entry point called by [net.transgressoft.musicott.view.MainController] on each search field change.
     *
     * This is a plain (non-suspending) method so it is directly callable from Java without any
     * coroutine machinery on the call site. The async lifecycle is entirely internal.
     *
     * @param query the current search field text; blank triggers an immediate reset across all views
     */
    fun onQuery(query: String) {
        currentJob?.cancel()
        val gen = generation.incrementAndGet()
        val trimmed = query.trim()

        currentJob = scope.launch {
            if (trimmed.isBlank()) {
                withContext(fxDispatcher) {
                    if (generation.get() == gen) {
                        // Distinct instances only: a single Searchable registered under several modes
                        // (e.g. the audio table under ALL_AUDIO_ITEMS and PLAYLIST) is reset once.
                        searchables.values.distinct().forEach { searchable ->
                            @Suppress("UNCHECKED_CAST")
                            (searchable as? Searchable<Any>)?.applyMatchIds("", emptySet())
                        }
                        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("", this@SearchCoordinator))
                    }
                }
                return@launch
            }

            delay(debounceMillis)

            applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("Searching...", this@SearchCoordinator))

            val lowerQuery = trimmed.lowercase()

            // Filter EVERY registered view, not just the visible one, so all navigation modes stay
            // in sync — switching modes shows an already-filtered view. A stable copy guards against
            // concurrent registrations during the scan; distinct() ensures a single Searchable
            // registered under several modes (e.g. the audio table under ALL_AUDIO_ITEMS and
            // PLAYLIST) is scanned once rather than once per mode.
            @Suppress("UNCHECKED_CAST")
            val targets = searchables.values.distinct().map { it as Searchable<Any> }.toList()

            // Snapshot each view's backing collection on the FX thread so the subsequent off-thread
            // scan reads a stable list rather than a live ObservableList that concurrent
            // import/edit operations may structurally modify.
            withContext(fxDispatcher) {
                targets.forEach { it.prepareSnapshot() }
            }

            val results = try {
                withContext(dispatcher) {
                    targets.map { searchable -> searchable to searchable.computeMatchIds(lowerQuery) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Search failed for query='$lowerQuery'" }
                // Clear the "Searching…" status the failed run published, so it does not linger
                // until the next query. Guarded by generation so a newer query's status wins.
                withContext(fxDispatcher) {
                    if (generation.get() == gen) {
                        applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("", this@SearchCoordinator))
                    }
                }
                return@launch
            }

            withContext(fxDispatcher) {
                if (generation.get() == gen) {
                    results.forEach { (searchable, ids) ->
                        try {
                            searchable.applyMatchIds(lowerQuery, ids)
                        } catch (e: Exception) {
                            logger.error(e) { "Apply search results failed for query='$lowerQuery'" }
                        }
                    }
                    applicationEventPublisher.publishEvent(StatusMessageUpdateEvent("", this@SearchCoordinator))
                }
            }
        }
    }

    /**
     * Returns `true` when no search job is currently in-flight.
     *
     * Intended for use in tests that need to await quiescence before proceeding.
     * Because [onQuery] assigns [currentJob] synchronously on the call site, this
     * method is safe to call from any thread — [currentJob] is marked `@Volatile`.
     */
    fun isIdle(): Boolean = currentJob?.isActive != true

    /**
     * Cancels the internal [CoroutineScope], stopping any in-flight or pending search jobs.
     *
     * Invoked automatically by Spring on context shutdown.
     */
    @PreDestroy
    fun close() {
        job.cancel()
        logger.debug { "SearchCoordinator scope cancelled" }
    }
}
