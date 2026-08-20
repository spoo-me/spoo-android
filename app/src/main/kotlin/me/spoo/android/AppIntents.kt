package me.spoo.android

/** Intent contract shared by the share target, QS tile, and shortcuts. */
object AppIntents {
    /** Open straight into the create sheet. */
    const val ACTION_SHORTEN = "me.spoo.android.action.SHORTEN"

    /** Optional URL to prefill the create sheet with. */
    const val EXTRA_PREFILL_URL = "me.spoo.android.extra.PREFILL_URL"
}
