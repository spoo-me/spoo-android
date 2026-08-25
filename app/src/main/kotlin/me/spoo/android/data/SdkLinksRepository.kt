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

    /**
     * A dead refresh token means signed-out, no matter which call finds
     * out first: flag it globally, then let the caller surface the error.
     */
    private suspend fun <T> withSession(block: suspend () -> T): T = try {
        block()
    } catch (e: SessionExpiredException) {
        onSessionExpired()
        throw e
    }

    override suspend fun create(request: CreateLinkRequest): SpooLink = withSession {
        val created = clientProvider().links.create {
            longUrl = request.url
            alias = request.alias
            if (request.alias == null && request.emojiAlias) aliasType = AliasKind.EMOJI
            password = request.password
            maxClicks = request.maxClicks?.toLong()
            expireAfter = request.expireAtMillis?.let { Instant.fromEpochMilliseconds(it) }
            privateStats = request.privateStats
            blockBots = request.blockBots
        }
        val ui = SpooLink(
            id = created.id,
            shortCode = created.alias,
            originalUrl = created.longUrl,
            totalClicks = 0,
            createdLabel = "Just now",
            hasPassword = request.password != null,
            maxClicks = request.maxClicks?.toLong(),
            expireAtMillis = request.expireAtMillis,
            privateStats = request.privateStats,
            blockBots = request.blockBots,
        )
        _links.update { listOf(ui) + it.filterNot { l -> l.id == ui.id } }
        ui
    }

    override suspend fun update(id: String, edit: LinkEdit): SpooLink = withSession {
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
            when {
                edit.clearExpiry -> removeExpiry()
                edit.expireAtMillis != null ->
                    expireAfter(Instant.fromEpochMilliseconds(edit.expireAtMillis))
            }
            edit.privateStats?.let { privateStats(it) }
            edit.blockBots?.let { blockBots(it) }
        }
        val old = _links.value.first { it.id == id }
        val ui = old.copy(
            shortCode = updated.alias ?: old.shortCode,
            originalUrl = updated.longUrl ?: old.originalUrl,
            hasPassword = updated.passwordSet,
            maxClicks = updated.maxClicks,
            expireAtMillis = updated.expireAfter?.toEpochMilliseconds(),
            privateStats = updated.privateStats ?: old.privateStats,
            blockBots = updated.blockBots ?: old.blockBots,
        )
        _links.update { list -> list.map { if (it.id == id) ui else it } }
        ui
    }

    override suspend fun delete(id: String) = withSession {
        clientProvider().links.delete(id)
        _links.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun bulkDelete(ids: List<String>) = withSession {
        clientProvider().links.bulkDelete(ids)
        // Partial failure is data, not an exception: resync with the server.
        refresh()
    }

    override suspend fun bulkSetStatus(ids: List<String>, active: Boolean) = withSession {
        clientProvider().links.bulkSetStatus(
            ids,
            if (active) SettableStatus.ACTIVE else SettableStatus.INACTIVE,
        )
        refresh()
    }

    override suspend fun bulkSetExpiry(ids: List<String>, expireAtMillis: Long?) = withSession {
        clientProvider().links.bulkSetExpiry(
            ids,
            expireAtMillis?.let { Instant.fromEpochMilliseconds(it) },
        )
        refresh()
    }

    // The SDK ETag-caches per client, but the graph swaps clients on auth
    // changes; the set is public and near-static, so one fetch per process.
    private var cachedCatalog: EmojiCatalog? = null

    override suspend fun emojiCatalog(): EmojiCatalog {
        cachedCatalog?.let { return it }
        val set = clientProvider().emoji.set()
        return EmojiCatalog(
            maxGraphemes = set.maxGraphemes,
            entries = set.emoji.map {
                EmojiChoice(
                    char = it.character,
                    name = it.name,
                    group = it.group,
                    keywords = it.keywords.orEmpty(),
                )
            },
        ).also { cachedCatalog = it }
    }

    override suspend fun stats(shortCode: String, params: StatsParams): LinkStats = withSession {
        val link = _links.value.first { it.shortCode == shortCode }
        val report = clientProvider().stats.forLink(urlId = link.id, query = params.toQuery())
        report.metrics.toLinkStats(link, params.metric.wire())
    }

    override suspend fun accountStats(params: StatsParams): LinkStats = withSession {
        val report = clientProvider().stats.account(AccountStatsRequest(query = params.toQuery()))
        report.metrics.toLinkStats(link = null, params.metric.wire())
    }

    private fun StatsMetric.wire() = when (this) {
        StatsMetric.Clicks -> "clicks"
        StatsMetric.UniqueClicks -> "unique_clicks"
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
        metrics = listOf(
            when (metric) {
                StatsMetric.Clicks -> Metric.CLICKS
                StatsMetric.UniqueClicks -> Metric.UNIQUE_CLICKS
            },
        ),
        filters = filters.entries.associate { (dim, values) ->
            when (dim) {
                StatsDim.Country -> FilterDimension.COUNTRY
                StatsDim.Browser -> FilterDimension.BROWSER
                StatsDim.Os -> FilterDimension.OS
                StatsDim.Referrer -> FilterDimension.REFERRER
            } to values.toList()
        },
    )

    private fun Map<String, List<JsonObject>>.toLinkStats(link: SpooLink?, metricKey: String) =
        LinkStats(
            link = link,
            dailyClicks = rows("${metricKey}_by_time", metricKey).map { it.second },
            countries = slices("${metricKey}_by_country", metricKey),
            browsers = slices("${metricKey}_by_browser", metricKey),
            os = slices("${metricKey}_by_os", metricKey),
            referrers = slices("${metricKey}_by_referrer", metricKey),
        )

    private fun Map<String, List<JsonObject>>.slices(key: String, metricKey: String): List<LinkStats.Slice> =
        rows(key, metricKey).map { (label, count) -> LinkStats.Slice(label, count) }

    /**
     * Breakdown rows arrive as raw JSON objects keyed `{metric}_by_{dim}`;
     * each row carries the dimension value plus the metric counts. Parsed
     * defensively: label = first non-metric string field, count = [metricKey].
     */
    private fun Map<String, List<JsonObject>>.rows(key: String, metricKey: String): List<Pair<String, Int>> =
        this[key].orEmpty().mapNotNull { row ->
            val count = row[metricKey]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
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
        status = when (status) {
            LinkStatus.INACTIVE -> LinkUiStatus.Inactive
            LinkStatus.EXPIRED -> LinkUiStatus.Expired
            LinkStatus.BLOCKED -> LinkUiStatus.Blocked
            else -> LinkUiStatus.Active
        },
        maxClicks = maxClicks,
        expireAtMillis = expireAfter?.toEpochMilliseconds(),
        privateStats = privateStats ?: false,
        blockBots = blockBots ?: false,
        createdAtMillis = createdAt?.toEpochMilliseconds(),
    )

    private fun Instant.toDayLabel(): String =
        SimpleDateFormat("MMM d", Locale.US).format(Date(toEpochMilliseconds()))
}
