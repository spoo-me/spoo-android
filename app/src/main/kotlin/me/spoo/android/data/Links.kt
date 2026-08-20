package me.spoo.android.data

/**
 * UI-facing link model. Deliberately narrower than the SDK's — the fake and
 * SDK-backed repositories both map into this, keeping screens SDK-agnostic.
 */
data class SpooLink(
    val id: String,
    val shortCode: String,
    val originalUrl: String,
    val totalClicks: Int,
    val createdLabel: String,
    val hasPassword: Boolean = false,
) {
    val shortUrl: String get() = "spoo.me/$shortCode"
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
enum class StatsDim { Country, Browser, Referrer }

/** Stats query surface: preset date window + tap-to-filter selections. */
data class StatsParams(
    /** Look-back window in days; null = all time. */
    val days: Int? = 30,
    /** Raw dimension values, exactly as the API stored them. */
    val filters: Map<StatsDim, String> = emptyMap(),
)

data class LinkStats(
    /** Null for account-wide stats. */
    val link: SpooLink?,
    val dailyClicks: List<Int>,
    val countries: List<Slice>,
    val referrers: List<Slice>,
    val browsers: List<Slice>,
) {
    data class Slice(val label: String, val count: Int)
}
