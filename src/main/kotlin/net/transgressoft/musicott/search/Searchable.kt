package net.transgressoft.musicott.search

/**
 * Contract for a view component that participates in asynchronous search filtering.
 *
 * Implementations split filtering into three phases:
 * - [prepareSnapshot] runs on the JavaFX Application Thread immediately before [computeMatchIds]
 *   is dispatched to the background. Implementations copy any live JavaFX [javafx.collections.ObservableList]
 *   they need to scan into a plain immutable list, preventing data races with concurrent FX-thread
 *   mutations (imports, edits, deletes). The default implementation is a no-op for views whose
 *   backing data is already thread-safe.
 * - [computeMatchIds] runs off the JavaFX Application Thread on a background dispatcher
 *   (typically [kotlinx.coroutines.Dispatchers.Default]), performing the expensive O(n)
 *   scan over the snapshot produced by [prepareSnapshot] and returning a lightweight set of
 *   matching identifiers. Implementations must not read or write any JavaFX observable property
 *   or [javafx.collections.FilteredList] predicate from within this method.
 * - [applyMatchIds] runs on the JavaFX Application Thread (via [kotlinx.coroutines.Dispatchers.JavaFx]),
 *   applying the pre-computed ID set as a cheap predicate to the view's [javafx.collections.FilteredList]
 *   and performing any additional FX-observable mutations. This method must do no substring
 *   scanning — all heavy matching must have been done in [computeMatchIds].
 *
 * Both [computeMatchIds] and [applyMatchIds] receive the raw `query` string.
 *
 * @param ID the type of identifier used to uniquely address items in this view's collection
 *           (e.g. [Int] for audio items, [String] for album or genre names)
 */
interface Searchable<ID : Any> {
    /**
     * Captures an immutable snapshot of the backing collection on the JavaFX Application Thread.
     *
     * Called on the JavaFX Application Thread immediately before [computeMatchIds] is dispatched
     * to the background. The snapshot isolates the off-thread scan from concurrent FX-thread
     * structural mutations (add/remove) of live [javafx.collections.ObservableList] instances.
     *
     * The default implementation is a no-op; views whose backing data is not a live observable list
     * (or is replaced atomically via `setAll`) do not need to override this method.
     */
    fun prepareSnapshot() {
        // No-op default: views whose backing data is not a live observable list have nothing to snapshot.
    }

    /**
     * Computes the set of item identifiers that match [query].
     *
     * Called off the JavaFX Application Thread. Implementations must read only from the immutable
     * snapshot captured by [prepareSnapshot] — never from live JavaFX observable properties or
     * [javafx.collections.FilteredList] instances.
     *
     * @param query the search text, already trimmed and lower-cased by the coordinator
     * @return the set of identifiers whose corresponding items match the query
     */
    fun computeMatchIds(query: String): Set<ID>

    /**
     * Applies [ids] to the view's filtered collection and performs any related FX-observable updates.
     *
     * Always called on the JavaFX Application Thread. This method must perform only cheap membership
     * checks against [ids] — no substring scanning. When [query] is blank (empty string), implementations
     * must reset their [javafx.collections.FilteredList] to show all items (the reset path). For a
     * non-blank [query], the predicate must admit only items whose ID is present in [ids]; an empty
     * [ids] with a non-blank [query] means the search matched nothing and zero items should be visible.
     * Any precomputed row-level or track-level results stored during [computeMatchIds] may be applied here.
     *
     * @param query the search text that produced [ids]; a blank string signals a reset (show all)
     * @param ids the set of identifiers computed by [computeMatchIds]; empty means no matches when query is non-blank
     */
    fun applyMatchIds(
        query: String,
        ids: @JvmSuppressWildcards Set<ID>
    )
}
