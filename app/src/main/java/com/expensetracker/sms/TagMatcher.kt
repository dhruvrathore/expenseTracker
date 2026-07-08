package com.expensetracker.sms

/**
 * Suggests an auto-tag for well-known recurring merchants (e.g. so every Swiggy order is tagged
 * "Swiggy" without retyping it). Returns null when no known merchant is recognized; the user can
 * always type their own tag instead.
 */
object TagMatcher {

    // First matching rule wins, so order from most specific to most general.
    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf("swiggy") to "Swiggy",
        listOf("amazon") to "Amazon",
        listOf("blinkit") to "Blinkit",
        listOf("zepto") to "Zepto",
    )

    fun tagFor(text: String): String? {
        val s = text.lowercase()
        return RULES.firstOrNull { (keywords, _) -> keywords.any { s.contains(it) } }?.second
    }
}
