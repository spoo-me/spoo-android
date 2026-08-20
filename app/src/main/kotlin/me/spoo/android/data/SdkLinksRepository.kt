package me.spoo.android.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import me.spoo.AccountStatsRequest
import me.spoo.AliasKind
import me.spoo.AuthenticationException
import me.spoo.Dimension
import me.spoo.FilterDimension
import me.spoo.LinkItem
import me.spoo.LinkStatus
import me.spoo.ListLinksRequest
import me.spoo.SessionExpiredException
import me.spoo.Metric
import me.spoo.SettableStatus
import me.spoo.SpooClient
import me.spoo.StatsQuery

/**
 * The real repository: me.spoo SDK against BuildConfig.SPOO_BASE_URL.
 * [clientProvider] hands back the current client (anonymous or session-
 * backed); the graph swaps it on auth changes.
 */
class SdkLinksRepository(
    private val clientProvider: () -> SpooClient,
    /** The refresh token is dead: the user must sign in again. */
    private val onSessionExpired: () -> Unit = {},
) : LinksRepository {

    private val _links = MutableStateFlow<List<SpooLink>>(emptyList())
    override val links: StateFlow<List<SpooLink>> = _links.asStateFlow()

    override suspend fun refresh() {
        _links.value = try {
            clientProvider().links
                .list(ListLinksRequest(pageSize = 100))
                .items
                .map { it.toUi() }
        } catch (_: AuthenticationException) {
            emptyList() // signed out: no owned links to show
        } catch (e: SessionExpiredException) {
            android.util.Log.w("SpooRepo", "session expired during refresh", e)
            onSessionExpired()
            emptyList()
        } catch (e: Exception) {
            android.util.Log.w("SpooRepo", "refresh failed", e)
            throw e
        }
    }

    override suspend fun create(request: CreateLinkRequest): SpooLink {
        val created = clientProvider().links.create {
            longUrl = request.url
            alias = request.alias
            if (request.alias == null && request.emojiAlias) aliasType = AliasKind.EMOJI
            password = request.password
            maxClicks = request.maxClicks?.toLong()
        }
        val ui = SpooLink(
            id = created.id,
            shortCode = created.alias,
            originalUrl = created.longUrl,
            totalClicks = 0,
            createdLabel = "Just now",
            hasPassword = request.password != null,
        )
        _links.update { listOf(ui) + it.filterNot { l -> l.id == ui.id } }
        return ui
    }

    override suspend fun update(id: String, edit: LinkEdit): SpooLink {
        val updated = clientProvider().links.update(id) {
            edit.longUrl?.let { longUrl(it) }
            edit.alias?.let { alias(it) }
            when {
                edit.clearPassword -> removePassword()
                edit.password != null -> password(edit.password)
            }
            when {
                edit.clearMaxClicks -> removeMaxClicks()
                edit.maxClicks != null -> maxClicks(edit.maxClicks)
            }
        }
        val old = _links.value.first { it.id == id }
        val ui = old.copy(
            shortCode = updated.alias ?: old.shortCode,
            originalUrl = updated.longUrl ?: old.originalUrl,
            hasPassword = updated.passwordSet,
        )
        _links.update { list -> list.map { if (it.id == id) ui else it } }
        return ui
    }

    override suspend fun delete(id: String) {
        clientProvider().links.delete(id)
        _links.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun bulkDelete(ids: List<String>) {
        clientProvider().links.bulkDelete(ids)
        // Partial failure is data, not an exception: resync with the server.
        refresh()
    }

    override suspend fun bulkSetStatus(ids: List<String>, active: Boolean) {
        clientProvider().links.bulkSetStatus(
            ids,
            if (active) SettableStatus.ACTIVE else SettableStatus.INACTIVE,
        )
        refresh()
    }

    override suspend fun bulkSetExpiry(ids: List<String>, expireAtMillis: Long?) {
        clientProvider().links.bulkSetExpiry(
            ids,
            expireAtMillis?.let { Instant.fromEpochMilliseconds(it) },
        )
        refresh()
    }

    override suspend fun stats(shortCode: String, params: StatsParams): LinkStats {
        val link = _links.value.first { it.shortCode == shortCode }
        val report = clientProvider().stats.forLink(urlId = link.id, query = params.toQuery())
        return report.metrics.toLinkStats(link)
    }

    override suspend fun accountStats(params: StatsParams): LinkStats {
        val report = clientProvider().stats.account(AccountStatsRequest(query = params.toQuery()))
        return report.metrics.toLinkStats(link = null)
    }

    private fun StatsParams.toQuery() = StatsQuery(
        startDate = customRange?.first?.let { Instant.fromEpochMilliseconds(it) }
            ?: days?.let { Clock.System.now() - it.days },
        endDate = customRange?.second?.let { Instant.fromEpochMilliseconds(it) },
        groupBy = listOf(
            Dimension.TIME,
            Dimension.COUNTRY,
            Dimension.BROWSER,
            Dimension.OS,
            Dimension.REFERRER,
        ),
        metrics = listOf(Metric.CLICKS),
        filters = filters.entries.associate { (dim, value) ->
            when (dim) {
                StatsDim.Country -> FilterDimension.COUNTRY
                StatsDim.Browser -> FilterDimension.BROWSER
                StatsDim.Os -> FilterDimension.OS
                StatsDim.Referrer -> FilterDimension.REFERRER
            } to listOf(value)
        },
    )

    private fun Map<String, List<JsonObject>>.toLinkStats(link: SpooLink?) = LinkStats(
        link = link,
        dailyClicks = rows("clicks_by_time").map { it.second },
        countries = slices("clicks_by_country"),
        browsers = slices("clicks_by_browser"),
        os = slices("clicks_by_os"),
        referrers = slices("clicks_by_referrer"),
    )

    private fun Map<String, List<JsonObject>>.slices(key: String): List<LinkStats.Slice> =
        rows(key).map { (label, count) -> LinkStats.Slice(label, count) }

    /**
     * Breakdown rows arrive as raw JSON objects keyed `{metric}_by_{dim}`;
     * each row carries the dimension value plus the metric counts. Parsed
     * defensively: label = first non-metric string field, count = `clicks`.
     */
    private fun Map<String, List<JsonObject>>.rows(key: String): List<Pair<String, Int>> =
        this[key].orEmpty().mapNotNull { row ->
            val count = row["clicks"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val label = row.entries
                .firstOrNull { it.key != "clicks" && it.key != "unique_clicks" }
                ?.value?.jsonPrimitive?.contentOrNull
                ?: "(unknown)"
            label to count.toInt()
        }

    private fun LinkItem.toUi() = SpooLink(
        id = id,
        shortCode = alias.orEmpty(),
        originalUrl = longUrl.orEmpty(),
        totalClicks = (totalClicks ?: 0L).toInt(),
        createdLabel = createdAt?.toDayLabel() ?: "",
        hasPassword = passwordSet,
        active = status == null || status == LinkStatus.ACTIVE,
    )

    private fun Instant.toDayLabel(): String =
        SimpleDateFormat("MMM d", Locale.US).format(Date(toEpochMilliseconds()))
}
