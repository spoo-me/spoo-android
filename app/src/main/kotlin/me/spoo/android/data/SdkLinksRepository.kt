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
import me.spoo.AliasKind
import me.spoo.AuthenticationException
import me.spoo.Dimension
import me.spoo.LinkItem
import me.spoo.ListLinksRequest
import me.spoo.Metric
import me.spoo.SpooClient
import me.spoo.StatsQuery

/**
 * The real repository: me.spoo SDK against BuildConfig.SPOO_BASE_URL.
 * [clientProvider] hands back the current client (anonymous or session-
 * backed); the graph swaps it on auth changes.
 */
class SdkLinksRepository(
    private val clientProvider: () -> SpooClient,
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

    override suspend fun stats(shortCode: String): LinkStats {
        val link = _links.value.first { it.shortCode == shortCode }
        val report = clientProvider().stats.forLink(
            urlId = link.id,
            query = StatsQuery(
                startDate = Clock.System.now() - 30.days,
                groupBy = listOf(Dimension.TIME, Dimension.COUNTRY, Dimension.REFERRER, Dimension.BROWSER),
                metrics = listOf(Metric.CLICKS),
            ),
        )
        return LinkStats(
            link = link,
            dailyClicks = report.rows("clicks_by_time").map { it.second },
            countries = report.slices("clicks_by_country"),
            referrers = report.slices("clicks_by_referrer"),
            browsers = report.slices("clicks_by_browser"),
        )
    }

    private fun me.spoo.LinkStatsReport.slices(key: String): List<LinkStats.Slice> =
        rows(key).map { (label, count) -> LinkStats.Slice(label, count) }

    /**
     * Breakdown rows arrive as raw JSON objects keyed `{metric}_by_{dim}`;
     * each row carries the dimension value plus the metric counts. Parsed
     * defensively: label = first non-metric string field, count = `clicks`.
     */
    private fun me.spoo.LinkStatsReport.rows(key: String): List<Pair<String, Int>> =
        metrics[key].orEmpty().mapNotNull { row ->
            val obj = row as? JsonObject ?: return@mapNotNull null
            val count = obj["clicks"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val label = obj.entries
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
    )

    private fun Instant.toDayLabel(): String =
        SimpleDateFormat("MMM d", Locale.US).format(Date(toEpochMilliseconds()))
}
