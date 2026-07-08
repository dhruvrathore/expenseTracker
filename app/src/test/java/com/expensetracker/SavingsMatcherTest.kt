package com.expensetracker

import com.expensetracker.sms.SavingsMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsMatcherTest {

    @Test
    fun `recognizes known SIP, brokerage, and deposit SMS`() {
        assertTrue(SavingsMatcher.isSavingsTransfer("Rs 5000 debited for SIP - HDFC Mutual Fund"))
        assertTrue(SavingsMatcher.isSavingsTransfer("INR 10000 debited towards Zerodha Coin purchase"))
        assertTrue(SavingsMatcher.isSavingsTransfer("Rs.15000 debited a/c XX123 UPI to Groww"))
        assertTrue(SavingsMatcher.isSavingsTransfer("Amount debited for Fixed Deposit booking"))
        assertTrue(SavingsMatcher.isSavingsTransfer("Recurring Deposit installment of Rs 2000 debited"))
    }

    @Test
    fun `does not flag ordinary spending or P2P transfers`() {
        assertFalse(SavingsMatcher.isSavingsTransfer("Rs 450 debited at Swiggy"))
        assertFalse(SavingsMatcher.isSavingsTransfer("Rs 2000 sent to Rahul via UPI"))
        assertFalse(SavingsMatcher.isSavingsTransfer("Rs 1299 debited for Amazon purchase"))
    }
}
