package me.spoo.android.data

import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Debug-only design fixture, the app-side twin of spoo-landing's mock mode.
 * Rich, plausible data shapes (weekly rhythm, a launch spike, weighted
 * geo/referrer mixes) that honor the stats params, so filters and the
 * choropleth are fully demonstrable offline.
 */
class MockLinksRepository : LinksRepository {

    private val seed = listOf(
        link("m1", "ga1n", "https://github.com/spoo-me/spoo", 48_211, "Jul 2", ageDays = 49),
        link("m2", "launch", "https://spoo.me/blog/android-app", 21_874, "Jul 9", ageDays = 42),
        link("m3", "🚀🔗", "https://play.google.com/store/apps/details?id=me.spoo.android", 9_454, "Jul 14", ageDays = 37),
        link("m4", "docs", "https://docs.spoo.me/getting-started/introduction", 6_120, "Jun 11", ageDays = 70),
        link("m5", "yt-demo", "https://www.youtube.com/watch?v=T2QDL9uAnQI", 4_082, "Jul 21", ageDays = 30),
        link("m6", "drop", "https://spoo.me/blog/self-hosting-guide", 2_310, "Aug 3", ageDays = 17, password = true, clickLimited = true),
        link("m7", "api", "https://docs.spoo.me/api-reference/shorten", 1_207, "Aug 8", ageDays = 12),
        link("m8", "promo", "https://spoo.me/pricing", 640, "Aug 11", ageDays = 9, status = LinkUiStatus.Inactive),
        link("m9", "flash", "https://spoo.me/blog/flash-sale", 512, "Aug 12", ageDays = 8, status = LinkUiStatus.Expired, clickLimited = true),
        link("m10", "blog", "https://spoo.me/blog", 214, "Aug 15", ageDays = 5),
        link("m11", "sus", "https://example-flagged.net/dl", 88, "Aug 16", ageDays = 4, status = LinkUiStatus.Blocked),
        link("m12", "beta", "https://beta.spoo.me/onboarding", 37, "Aug 18", ageDays = 2),
    )

    private val _links = MutableStateFlow(seed)
    override val links: StateFlow<List<SpooLink>> = _links.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun create(request: CreateLinkRequest): SpooLink {
        delay(500)
        val code = when {
            !request.alias.isNullOrBlank() -> request.alias
            request.emojiAlias -> listOf("✨🎯", "🐍📦", "🎧🌊", "🌵🔥").random()
            else -> (1..5).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")
        }
        val created = SpooLink(
            id = "mock-$code",
            shortCode = code,
            originalUrl = request.url,
            totalClicks = 0,
            createdLabel = "Just now",
            hasPassword = request.password != null,
        )
        _links.update { listOf(created) + it }
        return created
    }

    override suspend fun update(id: String, edit: LinkEdit): SpooLink {
        delay(300)
        val updated = _links.value.first { it.id == id }.let { old ->
            old.copy(
                originalUrl = edit.longUrl ?: old.originalUrl,
                shortCode = edit.alias ?: old.shortCode,
                hasPassword = when {
                    edit.clearPassword -> false
                    edit.password != null -> true
                    else -> old.hasPassword
                },
            )
        }
        _links.update { list -> list.map { if (it.id == id) updated else it } }
        return updated
    }

    override suspend fun delete(id: String) {
        delay(250)
        _links.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun bulkDelete(ids: List<String>) {
        delay(400)
        _links.update { list -> list.filterNot { it.id in ids } }
    }

    override suspend fun bulkSetStatus(ids: List<String>, active: Boolean) {
        delay(400)
        val status = if (active) LinkUiStatus.Active else LinkUiStatus.Inactive
        _links.update { list -> list.map { if (it.id in ids) it.copy(status = status) else it } }
    }

    override suspend fun bulkSetExpiry(ids: List<String>, expireAtMillis: Long?) {
        delay(400)
    }

    override suspend fun stats(shortCode: String, params: StatsParams): LinkStats {
        delay(450)
        val link = _links.value.first { it.shortCode == shortCode }
        return generate(params, base = link.totalClicks, seed = link.id.hashCode(), link = link)
    }

    override suspend fun accountStats(params: StatsParams): LinkStats {
        delay(550)
        return generate(
            params,
            base = _links.value.sumOf { it.totalClicks },
            seed = 20_26,
            link = null,
        )
    }

    private fun generate(params: StatsParams, base: Int, seed: Int, link: SpooLink?): LinkStats {
        val rng = Random(seed * 31 + params.hashCode())
        val days = params.customRange
            ?.let { ((it.second - it.first) / 86_400_000L).toInt().coerceAtLeast(1) }
            ?: params.days ?: 120

        // The whole history is ~120 days; a window sees its share of it.
        var total = base * (days.coerceAtMost(120) / 120f)

        // Filters cut the total by the value's share and pin its dimension.
        fun share(pool: List<Pair<String, Float>>, value: String?) =
            value?.let { v -> pool.firstOrNull { it.first == v }?.second ?: 0.02f }

        share(COUNTRIES, params.filters[StatsDim.Country])?.let { total *= it }
        share(REFERRERS, params.filters[StatsDim.Referrer])?.let { total *= it }
        share(BROWSERS, params.filters[StatsDim.Browser])?.let { total *= it }
        share(OSES, params.filters[StatsDim.Os])?.let { total *= it }

        val points = days.coerceIn(7, 120)
        val weights = List(points) { i ->
            val weekly = 1f + 0.35f * sin(i * 2f * Math.PI.toFloat() / 7f)
            val spike = if (i == (points * 0.7f).toInt()) 3.2f else 1f
            weekly * spike * (0.6f + rng.nextFloat() * 0.8f)
        }
        val weightSum = weights.sum()
        val daily = weights.map { (total * it / weightSum).roundToInt() }

        fun slices(pool: List<Pair<String, Float>>, filter: String?): List<LinkStats.Slice> {
            val chosen = filter?.let { f -> pool.filter { it.first == f } } ?: pool
            val poolSum = chosen.sumOf { it.second.toDouble() }.toFloat()
            return chosen.mapNotNull { (label, share) ->
                val count = (total * share / poolSum).roundToInt()
                if (count == 0) null else LinkStats.Slice(label, count)
            }
        }

        return LinkStats(
            link = link,
            dailyClicks = daily,
            countries = slices(COUNTRIES, params.filters[StatsDim.Country]),
            browsers = slices(BROWSERS, params.filters[StatsDim.Browser]),
            os = slices(OSES, params.filters[StatsDim.Os]),
            referrers = slices(REFERRERS, params.filters[StatsDim.Referrer]),
        )
    }

    private fun link(
        id: String,
        code: String,
        url: String,
        clicks: Int,
        created: String,
        ageDays: Int,
        password: Boolean = false,
        status: LinkUiStatus = LinkUiStatus.Active,
        clickLimited: Boolean = false,
    ) = SpooLink(
        id = id,
        shortCode = code,
        originalUrl = url,
        totalClicks = clicks,
        createdLabel = created,
        hasPassword = password,
        status = status,
        clickLimited = clickLimited,
        createdAtMillis = System.currentTimeMillis() - ageDays * 86_400_000L,
    )

    private companion object {
        val COUNTRIES = listOf(
            "US" to 0.30f, "IN" to 0.17f, "DE" to 0.09f, "GB" to 0.08f,
            "BR" to 0.07f, "JP" to 0.06f, "FR" to 0.05f, "CA" to 0.05f,
            "AU" to 0.04f, "NL" to 0.03f, "SE" to 0.03f, "KR" to 0.03f,
        )
        val REFERRERS = listOf(
            "github.com" to 0.28f, "Direct" to 0.22f, "t.co" to 0.14f,
            "reddit.com" to 0.11f, "news.ycombinator.com" to 0.09f,
            "linkedin.com" to 0.07f, "duckduckgo.com" to 0.05f, "dev.to" to 0.04f,
        )
        val BROWSERS = listOf(
            "Chrome" to 0.52f, "Safari" to 0.18f, "Firefox" to 0.12f,
            "Edge" to 0.08f, "Samsung Internet" to 0.06f, "Opera" to 0.04f,
        )
        val OSES = listOf(
            "Android" to 0.38f, "Windows" to 0.27f, "iOS" to 0.14f,
            "macOS" to 0.12f, "Linux" to 0.09f,
        )
    }
}
