package me.spoo.android.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // Seed favors popular destinations with vibrant, distinct favicons so
    // the link list demos well: green, red, blurple, multicolor, orange...
    private val seed = listOf(
        link("m1", "mixtape", "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M", 48_211, "Jul 2", ageDays = 49),
        link("m2", "trailer", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 21_874, "Jul 9", ageDays = 42),
        link("m3", "🎮🔥", "https://discord.gg/8vXk3qYd", 9_454, "Jul 14", ageDays = 37),
        link("m4", "handoff", "https://www.figma.com/design/Kx2fO1aB/checkout-flow-v3", 6_120, "Jun 11", ageDays = 70),
        link("m5", "thread", "https://www.reddit.com/r/android/comments/1m2k3x/pixel_10_first_impressions/", 4_082, "Jul 21", ageDays = 30),
        link("m6", "drop", "https://www.instagram.com/reel/C9tKf2xW/", 2_310, "Aug 3", ageDays = 17, password = true, maxClicks = 5_000, expiresInDays = 21),
        link("m7", "notes", "https://www.notion.so/product-launch-checklist-8a2f31bc", 1_207, "Aug 8", ageDays = 12),
        link("m8", "stream", "https://www.twitch.tv/directory/category/just-chatting", 640, "Aug 11", ageDays = 9, status = LinkUiStatus.Inactive),
        link("m9", "flash", "https://www.amazon.com/dp/B0C7XKQ3L4", 512, "Aug 12", ageDays = 8, status = LinkUiStatus.Expired, maxClicks = 512, expiresInDays = -2),
        link("m10", "stay", "https://www.airbnb.com/rooms/45872913", 214, "Aug 15", ageDays = 5),
        link("m11", "sus", "https://example-flagged.net/dl", 88, "Aug 16", ageDays = 4, status = LinkUiStatus.Blocked),
        link("m12", "sprint", "https://linear.app/team/issue/APP-142", 37, "Aug 18", ageDays = 2),
    )

    // Backing store (seed + a generated long tail so pagination is
    // demonstrable); _links is the queried, paged view of it.
    private val all = MutableStateFlow(seed + longTail())
    private var query = LinksQuery()
    private var visible = MOCK_PAGE

    private val _links = MutableStateFlow(seed)
    override val links: StateFlow<List<SpooLink>> = _links.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    override val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    init {
        applyQuery()
    }

    override suspend fun refresh(query: LinksQuery?) {
        delay(650) // pull-to-refresh should feel like it did something
        query?.let { this.query = it }
        visible = MOCK_PAGE
        applyQuery()
    }

    override suspend fun loadMore() {
        delay(500) // let the footer loader be seen
        visible += MOCK_PAGE
        applyQuery()
    }

    private fun applyQuery() {
        val q = query.search?.takeIf { it.isNotBlank() }
        val full = all.value
            .filter(query.filter::matches)
            .filter {
                q == null || it.shortCode.contains(q, ignoreCase = true) ||
                    it.originalUrl.contains(q, ignoreCase = true)
            }
            .let { list ->
                when (query.sort) {
                    LinkSort.Recent -> list
                    LinkSort.Clicks -> list.sortedByDescending { it.totalClicks }
                }
            }
        _links.value = full.take(visible)
        _hasMore.value = full.size > visible
    }

    /** ~130 plausible extras behind the curated seed. */
    private fun longTail(): List<SpooLink> {
        val domains = listOf(
            "www.youtube.com/watch?v=", "open.spotify.com/track/", "github.com/",
            "medium.com/@", "www.twitch.tv/", "dev.to/", "www.behance.net/",
            "soundcloud.com/", "www.etsy.com/listing/", "news.ycombinator.com/item?id=",
        )
        val words = listOf(
            "promo", "beat", "guide", "setup", "merch", "vlog", "patch", "combo",
            "remix", "board", "pitch", "study", "route", "batch",
        )
        val rng = Random(7)
        return (1..130).map { i ->
            val age = 20 + i
            link(
                id = "t$i",
                code = "${words[i % words.size]}${100 + i}",
                url = "https://${domains[i % domains.size]}mock-$i",
                clicks = (9_000_000 / (i + 40) - 800 + rng.nextInt(500)).coerceAtLeast(5),
                created = SimpleDateFormat("MMM d", Locale.US)
                    .format(Date(System.currentTimeMillis() - age * 86_400_000L)),
                ageDays = age,
                password = i % 9 == 0,
                maxClicks = if (i % 7 == 0) (i * 100L) else null,
            )
        }
    }

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
            maxClicks = request.maxClicks?.toLong(),
            expireAtMillis = request.expireAtMillis,
            privateStats = request.privateStats,
            blockBots = request.blockBots,
        )
        all.update { listOf(created) + it }
        visible += 1
        applyQuery()
        return created
    }

    override suspend fun update(id: String, edit: LinkEdit): SpooLink {
        delay(300)
        val updated = all.value.first { it.id == id }.let { old ->
            old.copy(
                originalUrl = edit.longUrl ?: old.originalUrl,
                shortCode = edit.alias ?: old.shortCode,
                hasPassword = when {
                    edit.clearPassword -> false
                    edit.password != null -> true
                    else -> old.hasPassword
                },
                maxClicks = when {
                    edit.clearMaxClicks -> null
                    edit.maxClicks != null -> edit.maxClicks
                    else -> old.maxClicks
                },
                expireAtMillis = when {
                    edit.clearExpiry -> null
                    edit.expireAtMillis != null -> edit.expireAtMillis
                    else -> old.expireAtMillis
                },
                privateStats = edit.privateStats ?: old.privateStats,
                blockBots = edit.blockBots ?: old.blockBots,
            )
        }
        all.update { list -> list.map { if (it.id == id) updated else it } }
        applyQuery()
        return updated
    }

    override suspend fun delete(id: String) {
        delay(250)
        all.update { list -> list.filterNot { it.id == id } }
        applyQuery()
    }

    override suspend fun bulkDelete(ids: List<String>) {
        delay(400)
        all.update { list -> list.filterNot { it.id in ids } }
        applyQuery()
    }

    override suspend fun bulkSetStatus(ids: List<String>, active: Boolean) {
        delay(400)
        val status = if (active) LinkUiStatus.Active else LinkUiStatus.Inactive
        all.update { list -> list.map { if (it.id in ids) it.copy(status = status) else it } }
        applyQuery()
    }

    override suspend fun bulkSetExpiry(ids: List<String>, expireAtMillis: Long?) {
        delay(400)
        all.update { list ->
            list.map { if (it.id in ids) it.copy(expireAtMillis = expireAtMillis) else it }
        }
        applyQuery()
    }

    override suspend fun emojiCatalog(): EmojiCatalog {
        delay(200)
        return EmojiCatalog(maxGraphemes = 15, entries = MOCK_EMOJI)
    }

    private val statsCache = mutableMapOf<String, LinkStats>()

    override fun cachedStats(shortCode: String?, params: StatsParams): LinkStats? =
        statsCache["$shortCode|$params"]

    override suspend fun stats(shortCode: String, params: StatsParams): LinkStats {
        delay(450)
        val link = all.value.first { it.shortCode == shortCode }
        return generate(params, base = link.totalClicks, seed = link.id.hashCode(), link = link)
            .also { statsCache["$shortCode|$params"] = it }
    }

    override suspend fun accountStats(params: StatsParams): LinkStats {
        delay(550)
        return generate(
            params,
            base = all.value.sumOf { it.totalClicks },
            seed = 20_26,
            link = null,
        ).also { statsCache["null|$params"] = it }
    }

    private fun generate(params: StatsParams, base: Int, seed: Int, link: SpooLink?): LinkStats {
        val rng = Random(seed * 31 + params.hashCode())
        val days = params.customRange
            ?.let { ((it.second - it.first) / 86_400_000L).toInt().coerceAtLeast(1) }
            ?: params.days ?: 120

        // The whole history is ~120 days; a window sees its share of it.
        var total = base * (days.coerceAtMost(120) / 120f)

        // Unique visitors are a stable-ish fraction of raw clicks.
        if (params.metric == StatsMetric.UniqueClicks) total *= 0.72f

        // Filters cut the total by the selected values' combined share.
        fun share(pool: List<Pair<String, Float>>, values: Set<String>?) =
            values?.takeIf { it.isNotEmpty() }?.let { vs ->
                pool.filter { it.first in vs }
                    .sumOf { it.second.toDouble() }.toFloat()
                    .coerceAtLeast(0.02f)
            }

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

        fun slices(pool: List<Pair<String, Float>>, filter: Set<String>?): List<LinkStats.Slice> {
            val chosen = filter?.takeIf { it.isNotEmpty() }
                ?.let { f -> pool.filter { it.first in f } } ?: pool
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
        maxClicks: Long? = null,
        expiresInDays: Int? = null,
    ) = SpooLink(
        id = id,
        shortCode = code,
        originalUrl = url,
        totalClicks = clicks,
        createdLabel = created,
        hasPassword = password,
        status = status,
        maxClicks = maxClicks,
        expireAtMillis = expiresInDays?.let { System.currentTimeMillis() + it * 86_400_000L },
        createdAtMillis = System.currentTimeMillis() - ageDays * 86_400_000L,
    )

    private companion object {
        const val MOCK_PAGE = 25

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

        // Offline stand-in for /api/v1/emoji-set: real groups, tiny slices.
        val MOCK_EMOJI = listOf(
            "Smileys & Emotion" to listOf(
                "😀" to "grinning face", "😂" to "face with tears of joy",
                "😍" to "smiling face with heart-eyes", "😎" to "smiling face with sunglasses",
                "🤔" to "thinking face", "🥳" to "partying face",
                "😴" to "sleeping face", "🤯" to "exploding head",
            ),
            "People & Body" to listOf(
                "👋" to "waving hand", "👍" to "thumbs up", "🙌" to "raising hands",
                "💪" to "flexed biceps", "🧠" to "brain", "👀" to "eyes",
            ),
            "Animals & Nature" to listOf(
                "🐶" to "dog face", "🐱" to "cat face", "🦊" to "fox",
                "🐢" to "turtle", "🌵" to "cactus", "🌊" to "water wave",
            ),
            "Food & Drink" to listOf(
                "🍕" to "pizza", "🍩" to "doughnut", "🍜" to "steaming bowl",
                "🍿" to "popcorn", "☕" to "hot beverage", "🧋" to "bubble tea",
            ),
            "Travel & Places" to listOf(
                "🌍" to "globe showing europe-africa", "🗻" to "mount fuji",
                "🏝" to "desert island", "🚀" to "rocket", "🛸" to "flying saucer",
            ),
            "Activities" to listOf(
                "🎉" to "party popper", "🎯" to "bullseye", "🎮" to "video game",
                "🎧" to "headphone", "🏆" to "trophy", "✨" to "sparkles",
            ),
            "Objects" to listOf(
                "🔗" to "link", "🔑" to "key", "💡" to "light bulb",
                "📦" to "package", "🧲" to "magnet", "🔥" to "fire",
            ),
            "Symbols" to listOf(
                "❤" to "red heart", "⚡" to "high voltage", "♻" to "recycling symbol",
                "💯" to "hundred points", "✅" to "check mark button",
            ),
            "Flags" to listOf("🏁" to "chequered flag", "🚩" to "triangular flag"),
        ).flatMap { (group, entries) ->
            entries.map { (char, name) -> EmojiChoice(char = char, name = name, group = group) }
        }
    }
}
