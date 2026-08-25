package me.spoo.android.ui.theme

import androidx.compose.ui.text.TextStyle

/** Tabular figures for anything that counts — metrics never jitter. */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")
