package com.expensetracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.AlertLevel
import com.expensetracker.domain.CategoryAlert
import com.expensetracker.ui.CategoryAlertDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the alert box. The ViewModel tests prove [pendingAlert] is set on both add and
 * edit; this proves the dialog that consumes it (rendered app-wide in MainActivity) shows the
 * right content and dismisses — so it surfaces correctly on the home screen and after edits.
 */
@RunWith(AndroidJUnit4::class)
class CategoryAlertDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nearingAlert_showsCategoryTitleAndPercent() {
        composeRule.setContent {
            CategoryAlertDialog(
                alert = CategoryAlert("Food", AlertLevel.NEARING, spent = 950.0, limit = 1000.0),
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("Approaching Food budget").assertIsDisplayed()
        composeRule.onNodeWithText("95% used").assertIsDisplayed()
    }

    @Test
    fun overAlert_showsExceededTitle() {
        composeRule.setContent {
            CategoryAlertDialog(
                alert = CategoryAlert("Transport", AlertLevel.OVER, spent = 400.0, limit = 300.0),
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("Transport budget exceeded").assertIsDisplayed()
    }

    @Test
    fun gotItButton_invokesDismiss() {
        var dismissed = false
        composeRule.setContent {
            CategoryAlertDialog(
                alert = CategoryAlert("Food", AlertLevel.OVER, spent = 1200.0, limit = 1000.0),
                onDismiss = { dismissed = true }
            )
        }

        composeRule.onNodeWithText("Got it").performClick()
        composeRule.runOnIdle { assertTrue(dismissed) }
    }
}
