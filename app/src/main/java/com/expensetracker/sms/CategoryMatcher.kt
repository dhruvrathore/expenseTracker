package com.expensetracker.sms

/**
 * Maps a merchant/description string to a default category from
 * [com.expensetracker.domain.Categories] using simple keyword matching. Falls back to "Other";
 * the user can always override the suggestion in the confirmation sheet.
 */
object CategoryMatcher {

    // First matching rule wins, so order from most specific to most general.
    private val RULES: List<Pair<List<String>, String>> = listOf(
        // Restaurants & prepared-food delivery.
        listOf(
            "zomato", "swiggy", "restaurant", "cafe", "coffee", "dominos", "mcdonald", "kfc",
            "pizza", "burger", "faasos", "eatfit", "dineout", "biryani", "dunzo"
        ) to "Eating Out",
        // Groceries & instant grocery delivery. Checked before Utilities so "jiomart" beats "jio".
        listOf(
            "bigbasket", "blinkit", "grofers", "zepto", "instamart", "jiomart", "dmart",
            "reliancefresh", "grocery", "supermarket"
        ) to "Food",
        listOf(
            "ola", "uber", "rapido", "irctc", "redbus", "metro", "fuel", "petrol",
            "hpcl", "iocl", "bpcl", "fastag"
        ) to "Transport",
        listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq") to "Shopping",
        listOf(
            "electricity", "broadband", "recharge", "airtel", "jio", "vodafone",
            "bescom", "dth", "postpaid", "prepaid", "bill", "water", "gas"
        ) to "Utilities",
        listOf("netflix", "spotify", "hotstar", "bookmyshow", "pvr", "inox") to "Entertainment",
        listOf("pharmacy", "pharmeasy", "1mg", "apollo", "hospital", "clinic", "medical", "diagnostic") to "Health",
        listOf("rent", "landlord", "housing") to "Rent",
        listOf(
            "irctc", "makemytrip", "goibibo", "yatra", "cleartrip", "airbnb", "oyo", "indigo",
            "spicejet", "vistara", "airlines", "airways", "flight", "hotel"
        ) to "Travel",
    )

    fun categorize(text: String): String {
        val s = text.lowercase()
        for ((keywords, category) in RULES) {
            if (keywords.any { s.contains(it) }) return category
        }
        return "Other"
    }
}
