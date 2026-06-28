package com.expensetracker.sms

/**
 * Maps a merchant/description string to a default category from
 * [com.expensetracker.domain.Categories] using simple keyword matching. Falls back to "Other";
 * the user can always override the suggestion in the confirmation sheet.
 */
object CategoryMatcher {

    // First matching rule wins, so order from most specific to most general.
    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf(
            "zomato", "swiggy", "restaurant", "cafe", "dominos", "mcdonald", "kfc",
            "bigbasket", "blinkit", "grofers", "zepto", "dunzo"
        ) to "Food",
        listOf(
            "ola", "uber", "rapido", "irctc", "redbus", "metro", "fuel", "petrol",
            "hpcl", "iocl", "bpcl", "fastag"
        ) to "Transport",
        listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq") to "Shopping",
        listOf(
            "electricity", "broadband", "recharge", "airtel", "jio", "vodafone",
            "bescom", "dth", "postpaid", "prepaid", "bill"
        ) to "Bills",
        listOf("netflix", "spotify", "hotstar", "bookmyshow", "pvr", "inox") to "Entertainment",
        listOf("pharmacy", "pharmeasy", "1mg", "apollo", "hospital", "clinic", "medical", "diagnostic") to "Health",
        listOf("rent", "landlord", "housing") to "Rent",
    )

    fun categorize(text: String): String {
        val s = text.lowercase()
        for ((keywords, category) in RULES) {
            if (keywords.any { s.contains(it) }) return category
        }
        return "Other"
    }
}
