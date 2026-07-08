package com.expensetracker

import com.expensetracker.sms.TagMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagMatcherTest {

    @Test
    fun `tags known recurring merchants case-insensitively`() {
        assertEquals("Swiggy", TagMatcher.tagFor("SWIGGY ORDER PAYMENT"))
        assertEquals("Amazon", TagMatcher.tagFor("Amazon Pay Balance"))
        assertEquals("Blinkit", TagMatcher.tagFor("UPI-Blinkit-blinkit@icici"))
        assertEquals("Zepto", TagMatcher.tagFor("zepto marketplace"))
    }

    @Test
    fun `returns null for unknown merchants`() {
        assertNull(TagMatcher.tagFor("Unknown"))
        assertNull(TagMatcher.tagFor("Some Random Merchant"))
    }
}
