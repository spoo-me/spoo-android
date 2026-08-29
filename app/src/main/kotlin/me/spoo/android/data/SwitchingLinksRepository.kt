package me.spoo.android.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Routes between the real SDK repository and the debug mock fixture. */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchingLinksRepository(
    private val real: LinksRepository,
    private val mock: LinksRepository,
    scope: CoroutineScope,
    mockEnabled: Flow<Boolean>,
) : LinksRepository {
    @Volatile
    private var useMock = false

    private val active: LinksRepository get() = if (useMock) mock else real

    init {
        // The newly active source must actually load when the toggle flips —
        // otherwise switching back from mock shows a stale empty list.
        scope.launch {
            mockEnabled.collect { enabled ->
                useMock = enabled
                runCatching { active.refresh() }
            }
        }
    }

    override val links: StateFlow<List<SpooLink>> =
        mockEnabled
            .flatMapLatest { if (it) mock.links else real.links }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val hasMore: StateFlow<Boolean> =
        mockEnabled
            .flatMapLatest { if (it) mock.hasMore else real.hasMore }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override suspend fun refresh(query: LinksQuery?) = active.refresh(query)

    override suspend fun loadMore() = active.loadMore()

    override suspend fun create(request: CreateLinkRequest) = active.create(request)

    override suspend fun update(
        id: String,
        edit: LinkEdit,
    ) = active.update(id, edit)

    override suspend fun delete(id: String) = active.delete(id)

    override suspend fun bulkDelete(ids: List<String>) = active.bulkDelete(ids)

    override suspend fun bulkSetStatus(
        ids: List<String>,
        active: Boolean,
    ) = this.active.bulkSetStatus(ids, active)

    override suspend fun bulkSetExpiry(
        ids: List<String>,
        expireAtMillis: Long?,
    ) = active.bulkSetExpiry(ids, expireAtMillis)

    override suspend fun stats(
        shortCode: String,
        params: StatsParams,
    ) = active.stats(shortCode, params)

    override suspend fun accountStats(params: StatsParams) = active.accountStats(params)

    override fun cachedStats(
        shortCode: String?,
        params: StatsParams,
    ) = active.cachedStats(shortCode, params)

    override suspend fun emojiCatalog() = active.emojiCatalog()

    override suspend fun aliasStatus(alias: String) = active.aliasStatus(alias)
}
