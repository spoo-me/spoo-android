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

data class LinkStats(
    val link: SpooLink,
    val dailyClicks: List<Int>,
    val countries: List<Slice>,
    val referrers: List<Slice>,
    val browsers: List<Slice>,
) {
    data class Slice(val label: String, val count: Int)
}
