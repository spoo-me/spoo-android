package me.spoo.android.data

/**
 * UI-facing link model. Deliberately narrower than the SDK's — the fake and
 * SDK-backed repositories both map into this, keeping screens SDK-agnostic.
 */
enum class LinkUiStatus { Active, Inactive, Expired, Blocked }

data class SpooLink(
    val id: String,
    val shortCode: String,
    val originalUrl: String,
    val totalClicks: Int,
    val createdLabel: String,
    val hasPassword: Boolean = false,
    val status: LinkUiStatus = LinkUiStatus.Active,
    /** True when a max-clicks limit is set. */
    val clickLimited: Boolean = false,
    val createdAtMillis: Long? = null,
) {
    val shortUrl: String get() = "spoo.me/$shortCode"
    val active: Boolean get() = status == LinkUiStatus.Active
}

/** Client-side filters for the links list, mirroring the webapp's set. */
data class LinksFilter(
    val status: LinkUiStatus? = null,
    val passwordProtected: Boolean = false,
    val clickLimited: Boolean = false,
    /** Created-at window in epoch millis; null = all time. */
    val createdRange: Pair<Long, Long>? = null,
) {
    val count: Int
        get() = listOf(
            status != null,
            passwordProtected,
            clickLimited,
            createdRange != null,
        ).count { it }

    fun matches(link: SpooLink): Boolean =
        (status == null || link.status == status) &&
            (!passwordProtected || link.hasPassword) &&
            (!clickLimited || link.clickLimited) &&
            (
                createdRange == null || link.createdAtMillis == null ||
                    link.createdAtMillis in createdRange.first..(createdRange.second + 86_399_999)
                )
}

data class CreateLinkRequest(
    val url: String,
    val alias: String? = null,
    val password: String? = null,
    val maxClicks: Int? = null,
    val emojiAlias: Boolean = false,
)

/**
 * A partial edit. Null field = keep as is; the clear flags express
 * explicit removal (the API's tri-state patch semantics).
 */
data class LinkEdit(
    val longUrl: String? = null,
    val alias: String? = null,
    val password: String? = null,
    val clearPassword: Boolean = false,
    val maxClicks: Long? = null,
    val clearMaxClicks: Boolean = false,
)

/** A dimension the stats screens can filter by. */
enum class StatsDim { Country, Browser, Os, Referrer }

/** The countable thing a stats query asks for. */
enum class StatsMetric { Clicks, UniqueClicks }

/** Stats query surface: date window + dimension filters. */
data class StatsParams(
    /** Look-back window in days; null = all time. Ignored with [customRange]. */
    val days: Int? = 30,
    /** Explicit start..end in epoch millis; wins over [days]. */
    val customRange: Pair<Long, Long>? = null,
    /** Raw dimension values, exactly as the API stored them. */
    val filters: Map<StatsDim, String> = emptyMap(),
    val metric: StatsMetric = StatsMetric.Clicks,
)

data class LinkStats(
    /** Null for account-wide stats. */
    val link: SpooLink?,
    val dailyClicks: List<Int>,
    val countries: List<Slice>,
    val browsers: List<Slice>,
    val os: List<Slice>,
    val referrers: List<Slice>,
) {
    data class Slice(val label: String, val count: Int)
}
