package com.expensetracker.sms

/**
 * A transaction detected in a bank/payment SMS. [merchant] is a best-effort payee name used as the
 * default description; [isDebit] is true for money leaving the account (i.e. an expense).
 */
data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val isDebit: Boolean,
    val smsTimestamp: Long
)

/**
 * Heuristic parser for Indian bank/UPI transaction SMSes (HDFC, SBI, ICICI, Paytm, generic UPI).
 *
 * Pure and device-independent so it can be unit-tested without Android. Returns null for messages
 * that don't look like a transaction (OTPs, promotions, balance-only alerts). Merchant extraction
 * is best-effort and falls back to "Unknown" — the user confirms/edits before anything is saved.
 */
object SmsParser {

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "sent", "paid", "withdrawn", "withdrawal", "deducted",
        "purchase", "transaction", "used", "using", "swiped", "pos"
    )
    private val CREDIT_KEYWORDS =
        listOf("credited", "credit", "received", "deposited", "refund")

    /** Words that mark a non-completed transaction we must not record. */
    private val FAILURE_KEYWORDS = listOf("declined", "failed", "unsuccessful", "reversed", "could not")

    /** Rs / Rs. / INR / ₹ followed by an amount, allowing Indian-style comma grouping. */
    private val AMOUNT = Regex("""(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)

    /** A UPI handle like "zomato@okhdfcbank" — the part before '@' is the payee. */
    private val VPA = Regex("""\b([a-z0-9][a-z0-9.\-_]+)@[a-z][a-z0-9.\-]+\b""", RegexOption.IGNORE_CASE)

    /** "to <name>" / "at <name>" up to a stop-word or punctuation (UPI transfers and card swipes). */
    private val TO_AT = Regex(
        """\b(?:to|at)\s+(?:vpa\s+)?([a-z0-9][a-z0-9&.\-_ ]*?)""" +
            """(?=\s+(?:on|ref|upi|via|vide|dt|dated|date|avl|avbl|bal|not|if|info|a/c|acct|account|using|for)\b|[.,;:!]|\d{2}[-/]|$)""",
        RegexOption.IGNORE_CASE
    )

    /** "<merchant> credited" — some banks name the payee before "credited" (e.g. ICICI). */
    private val CREDITED_NAME =
        Regex("""[;.]\s*([a-z0-9][a-z0-9&.\-_ ]*?)\s+credited""", RegexOption.IGNORE_CASE)

    fun parse(body: String, timestamp: Long): ParsedTransaction? {
        val text = body.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase()

        // OTPs / verification codes contain amounts and keywords but are never transactions.
        if ("otp" in lower || "one time password" in lower || "verification code" in lower) return null
        // Declined/failed/reversed attempts moved no money — skip them.
        if (FAILURE_KEYWORDS.any { lower.contains(it) }) return null

        val hasDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val hasCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
        if (!hasDebit && !hasCredit) return null

        val amount = AMOUNT.find(text)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?.takeIf { it > 0.0 } ?: return null

        // A "debited ... credited" message (your a/c debited, payee credited) is the holder's expense.
        return ParsedTransaction(
            amount = amount,
            merchant = extractMerchant(text) ?: "Unknown",
            isDebit = hasDebit,
            smsTimestamp = timestamp
        )
    }

    private fun extractMerchant(text: String): String? {
        VPA.find(text)?.groupValues?.get(1)?.let { return clean(it) }

        TO_AT.findAll(text)
            .map { it.groupValues[1].substringBefore('@') }
            .firstOrNull { isName(it) }
            ?.let { return clean(it) }

        CREDITED_NAME.find(text)?.groupValues?.get(1)?.takeIf { isName(it) }?.let { return clean(it) }

        return null
    }

    /** A plausible merchant name: has letters, isn't just digits/spaces, and is a sane length. */
    private fun isName(raw: String): Boolean {
        val s = raw.trim()
        return s.length in 2..40 && s.any { it.isLetter() } && !s.all { it.isDigit() || it == ' ' }
    }

    private fun clean(raw: String): String =
        raw.trim().trim('.', ',', ';', ':', '-').replace(Regex("""\s+"""), " ")
}
