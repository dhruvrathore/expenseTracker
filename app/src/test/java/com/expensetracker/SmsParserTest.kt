package com.expensetracker

import com.expensetracker.sms.CategoryMatcher
import com.expensetracker.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    private val ts = 1_700_000_000_000L

    @Test
    fun `parses HDFC UPI sent debit`() {
        val sms = "Sent Rs.500.00 From HDFC Bank A/C x1234 To ZOMATO On 26/06/26 " +
            "Ref 123456789012 Not You? Call 18002586161"
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun `parses HDFC VPA debit and extracts handle before the at-sign`() {
        val sms = "Rs.500.00 debited from a/c **1234 on 26-06-26 to VPA zomato@hdfcbank. " +
            "Avl bal Rs.4500.00"
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("zomato", parsed.merchant)
    }

    @Test
    fun `parses ICICI debit where the payee is named before credited`() {
        val sms = "ICICI Bank Acct XX123 debited for Rs 500.00 on 26-Jun-26; ZOMATO credited. " +
            "UPI:123456789012. Call 18002662 for dispute."
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun `parses SBI debit and ignores the helpline number`() {
        val sms = "Dear SBI User, your A/c X1234-debited by Rs.500.0 on 26Jun26 transfer to ZOMATO " +
            "Ref No 123456789012. If not done by you, forward this SMS to 9223008333"
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun `parses Paytm paid`() {
        val sms = "Paid Rs.500 to Zomato via Paytm UPI. UPI Ref 123456789012."
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("Zomato", parsed.merchant)
    }

    @Test
    fun `parses a card swipe with no explicit debit verb`() {
        val sms = "Thank you for using your HDFC Bank Card ending 1234 for Rs.500.00 at ZOMATO on 26-06-26."
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("ZOMATO", parsed.merchant)
    }

    @Test
    fun `parses the rupee symbol amount`() {
        val parsed = SmsParser.parse("Paid ₹250 to Uber via UPI.", ts)!!
        assertEquals(250.0, parsed.amount, 0.001)
        assertEquals("Uber", parsed.merchant)
    }

    @Test
    fun `parses a long multipart message and strips comma grouping`() {
        val sms = "Dear Customer, Rs.1,250.50 has been debited from your HDFC Bank account XX1234 " +
            "on 26-06-2026 towards UPI payment to swiggy@ybl. Your available balance is " +
            "Rs.12,345.67. Not you? Report at 18001234567 immediately to block."
        assertTrue("sample should exceed one SMS segment", sms.length > 160)
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(1250.50, parsed.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("swiggy", parsed.merchant)
    }

    @Test
    fun `recognizes a credit as non-debit`() {
        val sms = "Rs.50000.00 credited to a/c **1234 on 01-06-26 by NEFT-SALARY. Avl bal Rs.55000.00"
        val parsed = SmsParser.parse(sms, ts)!!
        assertEquals(50000.0, parsed.amount, 0.001)
        assertTrue(!parsed.isDebit)
    }

    @Test
    fun `preserves the sms timestamp`() {
        val parsed = SmsParser.parse("Paid Rs.10 to Uber.", ts)!!
        assertEquals(ts, parsed.smsTimestamp)
    }

    @Test
    fun `returns null for an OTP message`() {
        assertNull(SmsParser.parse("123456 is your OTP for HDFC NetBanking. Do not share it.", ts))
    }

    @Test
    fun `returns null for a promotional message`() {
        assertNull(SmsParser.parse("Get 50% OFF on your next order above Rs.500! Use code SAVE50. T&C apply.", ts))
    }

    @Test
    fun `returns null for a declined transaction`() {
        assertNull(
            SmsParser.parse("Your transaction of Rs.500 at ZOMATO was declined due to insufficient balance.", ts)
        )
    }

    @Test
    fun `returns null when there is no amount`() {
        assertNull(SmsParser.parse("Your account was debited. Check your balance.", ts))
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(SmsParser.parse("   ", ts))
    }

    @Test
    fun `falls back to Unknown merchant when none can be extracted`() {
        // Debit verb + amount but no recognizable payee phrasing.
        val parsed = SmsParser.parse("Rs.300 deducted as annual maintenance charge.", ts)
        assertNotNull(parsed)
        assertEquals("Unknown", parsed!!.merchant)
    }

    @Test
    fun `category matcher maps known merchants and defaults to Other`() {
        assertEquals("Food", CategoryMatcher.categorize("swiggy"))
        assertEquals("Transport", CategoryMatcher.categorize("Uber"))
        assertEquals("Shopping", CategoryMatcher.categorize("AMAZON"))
        assertEquals("Other", CategoryMatcher.categorize("Some Random Merchant"))
    }
}
