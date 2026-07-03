package net.transgressoft.musicott.search

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.transgressoft.musicott.view.NavigationController.NavigationMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SearchCoordinator")
class SearchCoordinatorTest {

    val dispatcher = StandardTestDispatcher()

    val applicationEventPublisher: ApplicationEventPublisher = mock(ApplicationEventPublisher::class.java)

    lateinit var coordinator: SearchCoordinator

    // A fake Searchable that records how many times computeMatchIds was called
    inner class FakeSearchable(private val matchIds: Set<Int> = setOf(1, 2, 3)) : Searchable<Int> {
        var computeCallCount = 0
        var lastAppliedQuery: String? = null
        var lastAppliedIds: Set<Int>? = null

        override fun computeMatchIds(query: String): Set<Int> {
            computeCallCount++
            return matchIds
        }

        override fun applyMatchIds(query: String, ids: Set<Int>) {
            lastAppliedQuery = query
            lastAppliedIds = ids
        }
    }

    @BeforeEach
    fun setup() {
        // Use the same StandardTestDispatcher for both compute and FX dispatching so
        // all coroutine work is virtual-time-controlled without needing a real JavaFX toolkit.
        coordinator = SearchCoordinator(
            applicationEventPublisher = applicationEventPublisher,
            dispatcher = dispatcher,
            fxDispatcher = dispatcher
        )
    }

    @Test
    @DisplayName("SearchCoordinator does not compute for keystrokes arriving within the 400ms debounce gap")
    fun debouncePreventsDuplicateCompute() = runTest(dispatcher) {
        val fake = FakeSearchable()
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, fake)

        // First query starts a 400ms debounce
        coordinator.onQuery("app")
        // Second query within the gap cancels the first and restarts the debounce
        coordinator.onQuery("appl")

        // Advance past the debounce only once (501ms total from the second onQuery)
        advanceTimeBy(501)
        advanceUntilIdle()

        // Only one compute invocation despite two queries
        assert(fake.computeCallCount == 1) {
            "Expected exactly 1 compute call after debounce, but got ${fake.computeCallCount}"
        }
    }

    @Test
    @DisplayName("SearchCoordinator cancels the prior in-flight job when a new query arrives")
    fun newQueryCancelsPriorJob() = runTest(dispatcher) {
        val fake = FakeSearchable()
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, fake)

        // Issue a query and advance only partway through the debounce
        coordinator.onQuery("mus")
        advanceTimeBy(200)

        // New query arrives before the first debounce fires
        coordinator.onQuery("musi")
        advanceTimeBy(200)

        // First debounce would have fired at t=400 (from the first query),
        // but it was cancelled. The second debounce fires at t=400 from
        // the second query, i.e. at t=200+400=600 from test start.
        advanceUntilIdle()

        // Only the second query's compute should run
        assert(fake.computeCallCount == 1) {
            "Expected 1 compute call (second query only), but got ${fake.computeCallCount}"
        }
    }

    @Test
    @DisplayName("SearchCoordinator applies only the newest query's result across a rapid succession of queries")
    fun newestQueryResultWins() = runTest(dispatcher) {
        val computedQueries = mutableListOf<String>()
        val appliedQueries = mutableListOf<String>()
        val searchable = object : Searchable<Int> {
            override fun computeMatchIds(query: String): Set<Int> {
                computedQueries += query
                return setOf(query.length)
            }

            override fun applyMatchIds(query: String, ids: Set<Int>) {
                appliedQueries += query
            }
        }
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, searchable)

        // First query debounces at t=400; a second query arrives before it fires, superseding it.
        coordinator.onQuery("mus")
        advanceTimeBy(200)
        coordinator.onQuery("music")
        advanceUntilIdle()

        // Only the newest query is computed and applied; the superseded one never reaches compute
        // (cancellation) and, even if it had, the generation guard would block its apply.
        assert(computedQueries == listOf("music")) {
            "Expected only the newest query to be computed, but got $computedQueries"
        }
        assert(appliedQueries == listOf("music")) {
            "Expected only the newest query to be applied, but got $appliedQueries"
        }
    }

    @Test
    @DisplayName("SearchCoordinator resets immediately without debounce when the query is blank")
    fun clearedQueryResetsImmediately() = runTest(dispatcher) {
        val fake = FakeSearchable()
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, fake)

        // Empty query — reset path, no debounce delay
        coordinator.onQuery("")
        // Advance only a tiny amount — much less than the 400ms debounce
        advanceTimeBy(10)
        advanceUntilIdle()

        // Reset applies an empty set without waiting for the debounce
        assert(fake.lastAppliedIds == emptySet<Int>()) {
            "Expected empty ID set on reset, got ${fake.lastAppliedIds}"
        }
        assert(fake.lastAppliedQuery == "") {
            "Expected empty query on reset, got '${fake.lastAppliedQuery}'"
        }
    }

    @Test
    @DisplayName("SearchCoordinator triggers a debounced search for a single-character query")
    fun singleCharQueryTriggersSearch() = runTest(dispatcher) {
        val fake = FakeSearchable()
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, fake)

        // One-character query — must trigger a debounced search, not a reset
        coordinator.onQuery("m")
        advanceTimeBy(401)
        advanceUntilIdle()

        assert(fake.computeCallCount == 1) {
            "Expected 1 compute call for single-character query, got ${fake.computeCallCount}"
        }
        assert(fake.lastAppliedQuery == "m") {
            "Expected query 'm' applied, got '${fake.lastAppliedQuery}'"
        }
    }

    @Test
    @DisplayName("SearchCoordinator triggers a debounced search for a two-character query")
    fun twoCharQueryTriggersSearch() = runTest(dispatcher) {
        val fake = FakeSearchable()
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, fake)

        // Two-character query — must trigger a debounced search, not a reset
        coordinator.onQuery("mu")
        advanceTimeBy(401)
        advanceUntilIdle()

        assert(fake.computeCallCount == 1) {
            "Expected 1 compute call for two-character query, got ${fake.computeCallCount}"
        }
        assert(fake.lastAppliedQuery == "mu") {
            "Expected query 'mu' applied, got '${fake.lastAppliedQuery}'"
        }
    }

    @Test
    @DisplayName("SearchCoordinator drives every registered Searchable so all navigation modes stay filtered")
    fun allRegisteredSearchablesAreDriven() = runTest(dispatcher) {
        val audioSearchable = FakeSearchable()
        val artistSearchable = FakeSearchable()

        // Register searchables for two different navigation modes
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, audioSearchable)
        coordinator.register(NavigationMode.ARTISTS, artistSearchable)

        coordinator.onQuery("mus")
        advanceTimeBy(500)
        advanceUntilIdle()

        // Both views are filtered on the same query — not just the active one — so switching
        // navigation modes shows an already-filtered view.
        assert(audioSearchable.computeCallCount == 1) {
            "Expected ALL_AUDIO_ITEMS Searchable to be computed once, got ${audioSearchable.computeCallCount}"
        }
        assert(artistSearchable.computeCallCount == 1) {
            "Expected ARTISTS Searchable to be computed once, got ${artistSearchable.computeCallCount}"
        }
        assert(audioSearchable.lastAppliedQuery == "mus" && artistSearchable.lastAppliedQuery == "mus") {
            "Expected both Searchables to have the query applied"
        }
    }

    @Test
    @DisplayName("SearchCoordinator cancels its scope on close")
    fun closeStopsAllFutureCompute() = runTest(dispatcher) {
        val fake = FakeSearchable()
        coordinator.register(NavigationMode.ALL_AUDIO_ITEMS, fake)

        // Cancel the coordinator's scope before the debounce fires
        coordinator.onQuery("mus")
        coordinator.close()

        advanceUntilIdle()

        // Compute should not have run because scope was cancelled
        assert(fake.computeCallCount == 0) {
            "Expected 0 compute calls after close(), got ${fake.computeCallCount}"
        }
    }
}
