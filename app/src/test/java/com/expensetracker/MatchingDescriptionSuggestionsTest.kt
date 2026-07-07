package com.expensetracker

import com.expensetracker.ui.matchingDescriptionSuggestions
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [matchingDescriptionSuggestions] backs the description autocomplete chips on both the
 * add/edit screen and the SMS confirmation sheet, so it's tested once here independent of Compose.
 */
class MatchingDescriptionSuggestionsTest {

    @Test
    fun `blank input returns every suggestion`() {
        val suggestions = listOf("Coffee", "Lunch", "Groceries")
        assertEquals(suggestions, matchingDescriptionSuggestions("", suggestions))
    }

    @Test
    fun `filters case-insensitively and excludes an exact match`() {
        val suggestions = listOf("Zomato dinner", "Swiggy lunch", "Zomato")

        assertEquals(listOf("Zomato dinner"), matchingDescriptionSuggestions("zomato d", suggestions))
        // Typing the exact suggestion shouldn't re-suggest itself.
        assertEquals(emptyList<String>(), matchingDescriptionSuggestions("Zomato", listOf("Zomato")))
    }

    @Test
    fun `caps results at six`() {
        val suggestions = (1..10).map { "Item $it" }
        assertEquals(6, matchingDescriptionSuggestions("Item", suggestions).size)
    }
}
