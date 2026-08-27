package me.spoo.android.ui.components

import java.security.SecureRandom

// Same recipe and word list as the web composer's Suggest button. The
// "." separators satisfy the backend's URL-password rule: a letter, a
// digit, and an "@" or "." with no two consecutive specials.
private val WORDS =
    (
        "amber basil cedar delta ember fable garnet hazel indigo juniper " +
            "koala lumen maple nectar onyx pixel quartz raven sable tundra " +
            "umber velvet willow zephyr"
    ).split(" ")

private val rng = SecureRandom()

internal fun suggestPassword(): String {
    fun word() = WORDS[rng.nextInt(WORDS.size)]
    return "${word()}.${word()}.${10 + rng.nextInt(89)}"
}
