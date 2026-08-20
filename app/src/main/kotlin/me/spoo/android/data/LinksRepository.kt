package me.spoo.android.data

import kotlinx.coroutines.flow.StateFlow

interface LinksRepository {
    val links: StateFlow<List<SpooLink>>

    /** Reload from the source of truth; clears on auth loss. */
    suspend fun refresh()
    suspend fun create(request: CreateLinkRequest): SpooLink
    suspend fun update(id: String, edit: LinkEdit): SpooLink
    suspend fun delete(id: String)
    suspend fun bulkDelete(ids: List<String>)
    suspend fun bulkSetStatus(ids: List<String>, active: Boolean)

    /** null clears the expiry. */
    suspend fun bulkSetExpiry(ids: List<String>, expireAtMillis: Long?)
    suspend fun stats(shortCode: String, params: StatsParams): LinkStats
    suspend fun accountStats(params: StatsParams): LinkStats
}
