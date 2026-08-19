package me.spoo.android.data

import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface LinksRepository {
    val links: StateFlow<List<SpooLink>>
    suspend fun create(request: CreateLinkRequest): SpooLink
    suspend fun stats(shortCode: String): LinkStats
}

/**
 * Fixture-backed repository so the UI can be built and reviewed before the
 * auth flow lands. Swapped for the me.spoo SDK implementation behind the
 * same interface (clientTag app-android) once PKCE + apps.yaml are live.
 */
class FakeLinksRepository : LinksRepository {

    private val seed = listOf(
        SpooLink("ga1n", "https://github.com/spoo-me/spoo", 12_483, "Jul 2", hasPassword = false),
        SpooLink("py-sdk", "https://pypi.org/project/spoo/", 4_207, "Jul 19"),
        SpooLink("docs", "https://docs.spoo.me/getting-started/introduction", 2_954, "Jun 11"),
        SpooLink("drop", "https://spoo.me/blog/self-hosting-guide", 1_310, "Aug 3", hasPassword = true),
        SpooLink("yt", "https://www.youtube.com/watch?v=T2QDL9uAnQI", 862, "Aug 8"),
        SpooLink("cli", "https://github.com/spoo-me/spoo-cli#installation", 415, "Aug 14"),
        SpooLink("beta", "https://beta.spoo.me/onboarding", 96, "Aug 17"),
        SpooLink("kmp", "https://central.sonatype.com/artifact/me.spoo/spoo", 12, "Aug 19"),
    )

    private val _links = MutableStateFlow(seed)
    override val links: StateFlow<List<SpooLink>> = _links.asStateFlow()

    override suspend fun create(request: CreateLinkRequest): SpooLink {
        delay(600)
        val code = when {
            !request.alias.isNullOrBlank() -> request.alias
            request.emojiAlias -> EMOJI_POOL.random()
            else -> randomCode()
        }
        val link = SpooLink(
            shortCode = code,
            originalUrl = request.url,
            totalClicks = 0,
            createdLabel = "Just now",
            hasPassword = request.password != null,
        )
        _links.update { listOf(link) + it }
        return link
    }

    override suspend fun stats(shortCode: String): LinkStats {
        delay(700)
        val link = _links.value.first { it.shortCode == shortCode }
        val rng = Random(shortCode.hashCode())
        val scale = (link.totalClicks / 30).coerceAtLeast(1)
        val raw = List(30) { day ->
            val trend = 0.6f + day / 30f * 0.8f
            val spike = if (day == 21) 2.8f else 1f
            scale * trend * spike * (0.55f + rng.nextFloat() * 0.9f)
        }
        // A 30-day window can never exceed the link's all-time total.
        val budget = link.totalClicks * 0.6f
        val factor = (budget / raw.sum()).coerceAtMost(1f)
        val daily = raw.map { (it * factor).toInt() }
        return LinkStats(
            link = link,
            dailyClicks = daily,
            countries = slices(rng, link.totalClicks, "United States", "India", "Germany", "Brazil", "Japan"),
            referrers = slices(rng, link.totalClicks, "github.com", "Direct", "t.co", "reddit.com", "news.ycombinator.com"),
            browsers = slices(rng, link.totalClicks, "Chrome", "Safari", "Firefox", "Edge", "Other"),
        )
    }

    private fun slices(rng: Random, total: Int, vararg labels: String): List<LinkStats.Slice> {
        var remaining = total
        return labels.mapIndexed { i, label ->
            val share = if (i == labels.lastIndex) remaining else {
                (remaining * (0.30f + rng.nextFloat() * 0.25f)).toInt().also { remaining -= it }
            }
            LinkStats.Slice(label, share)
        }
    }

    private fun randomCode() = (1..5)
        .map { "abcdefghijkmnpqrstuvwxyz23456789".random() }
        .joinToString("")

    private companion object {
        val EMOJI_POOL = listOf("🚀🔗", "✨🎯", "🐍📦", "🎧🌊", "🌵🔥")
    }
}

/** Single composition-wide graph; replaced by Hilt when real wiring lands. */
object ServiceLocator {
    val linksRepository: LinksRepository = FakeLinksRepository()
}
