package com.expensetracker

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.sms.ParsedTransaction
import com.expensetracker.ui.SmsConfirmSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test: the SMS confirmation sheet used to omit description autocomplete entirely
 * (unlike the manual add/edit screen), so past descriptions never showed up as suggestions here.
 */
@RunWith(AndroidJUnit4::class)
class SmsConfirmSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val unknownMerchantTxn = ParsedTransaction(
        amount = 250.0,
        merchant = "Unknown",
        isDebit = true,
        smsTimestamp = 0L
    )

    @Test
    fun suggestionChips_showPastDescriptions() {
        composeRule.setContent {
            SmsConfirmSheet(
                transaction = unknownMerchantTxn,
                suggestions = listOf("Coffee", "Lunch"),
                onSave = { _, _, _, _ -> true },
                onSaveSavings = { _, _ -> true },
                onDismiss = {}
            )
        }

        composeRule.onAllNodesWithTag("description_suggestions").assertCountEquals(1)
        composeRule.onNodeWithText("Coffee").assertIsDisplayed()
        composeRule.onNodeWithText("Lunch").assertIsDisplayed()
    }

    @Test
    fun tappingSuggestionChip_fillsDescriptionField() {
        composeRule.setContent {
            SmsConfirmSheet(
                transaction = unknownMerchantTxn,
                suggestions = listOf("Coffee", "Lunch"),
                onSave = { _, _, _, _ -> true },
                onSaveSavings = { _, _ -> true },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("Coffee").performClick()

        // Selecting the suggestion fills the description field...
        composeRule.onNodeWithTag("description_field").assert(hasText("Coffee"))
        // ...and since the field now exactly matches "Coffee", neither past description is a
        // useful suggestion of what's already typed, so the chip row drops away entirely.
        composeRule.onAllNodesWithTag("description_suggestions").assertCountEquals(0)
    }

    @Test
    fun noSuggestions_rendersNoChips() {
        composeRule.setContent {
            SmsConfirmSheet(
                transaction = unknownMerchantTxn,
                suggestions = emptyList(),
                onSave = { _, _, _, _ -> true },
                onSaveSavings = { _, _ -> true },
                onDismiss = {}
            )
        }

        composeRule.onAllNodesWithTag("description_suggestions").assertCountEquals(0)
    }

    @Test
    fun knownMerchant_prefillsAutoTag() {
        val swiggyTxn = ParsedTransaction(
            amount = 350.0,
            merchant = "SWIGGY ORDER",
            isDebit = true,
            smsTimestamp = 0L
        )

        composeRule.setContent {
            SmsConfirmSheet(
                transaction = swiggyTxn,
                onSave = { _, _, _, _ -> true },
                onSaveSavings = { _, _ -> true },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithTag("tag_field").assert(hasText("Swiggy"))
    }
}
