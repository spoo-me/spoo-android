package me.spoo.android.data

import kotlinx.coroutines.flow.StateFlow

interface LinksRepository {
    val links: StateFlow<List<SpooLink>>

    /** Whether another page exists beyond what [links] holds. */
    val hasMore: StateFlow<Boolean>

    /**
     * Reload page one for [query]; null re-runs the last query. Clears
     * on auth loss.
     */
    suspend fun refresh(query: LinksQuery? = null)

    /** Append the next page, when one exists. */
    suspend fun loadMore()

    suspend fun create(request: CreateLinkRequest): SpooLink

    suspend fun update(
        id: String,
        edit: LinkEdit,
    ): SpooLink

    suspend fun delete(id: String)

    suspend fun bulkDelete(ids: List<String>)

    suspend fun bulkSetStatus(
        ids: List<String>,
        active: Boolean,
    )

    /** null clears the expiry. */
    suspend fun bulkSetExpiry(
        ids: List<String>,
        expireAtMillis: Long?,
    )

    suspend fun stats(
        shortCode: String,
        params: StatsParams,
    ): LinkStats

    suspend fun accountStats(params: StatsParams): LinkStats

    /**
     * Last fetched stats for this exact query, if any — screens paint
     * these instantly and refetch silently (stale-while-revalidate).
     * Null [shortCode] addresses the account-wide report.
     */
    fun cachedStats(
        shortCode: String?,
        params: StatsParams,
    ): LinkStats?

    /** The accepted emoji-alias catalogue; changes rarely, cached upstream. */
    suspend fun emojiCatalog(): EmojiCatalog
}
