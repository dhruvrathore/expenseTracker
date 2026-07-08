package com.expensetracker.sms

/**
 * Recognizes a debit SMS as a transfer to savings/investments (SIPs, brokerages, FDs/RDs) rather
 * than spending, using known-platform keywords — the same conservative, curated-keyword approach
 * as [CategoryMatcher] and [TagMatcher], to avoid false-positives on ordinary bank transfers.
 */
object SavingsMatcher {

    private val KEYWORDS = listOf(
        "sip", "mutual fund", "elss", "nps", "ppf",
        "fixed deposit", "recurring deposit",
        "zerodha", "groww", "upstox", "angel one", "icici direct", "kotak securities",
        "5paisa", "paytm money", "kuvera", "coin by zerodha",
    )

    fun isSavingsTransfer(text: String): Boolean {
        val s = text.lowercase()
        return KEYWORDS.any { s.contains(it) }
    }
}
