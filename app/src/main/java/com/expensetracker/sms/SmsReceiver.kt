package com.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.expensetracker.ExpenseApp

/**
 * Receives incoming SMS, parses bank/UPI transaction messages, and surfaces *debits* (expenses) for
 * the user to confirm — directly via [SmsTransactionBus] when the app is in the foreground,
 * otherwise as a notification. Credits and non-transaction messages are ignored. This receiver
 * never writes to the database; nothing is saved without explicit user confirmation.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Long transaction SMSes arrive split across several PDUs; reassemble the full body.
        val body = messages.joinToString(separator = "") {
            it.displayMessageBody ?: it.messageBody ?: ""
        }
        val timestamp = messages.first().timestampMillis

        val parsed = SmsParser.parse(body, timestamp)
        if (parsed == null) {
            Log.d(TAG, "SMS received (${messages.size} part(s)) — not a transaction, ignored")
            return
        }
        if (!parsed.isDebit) {
            Log.d(TAG, "Parsed a credit (amount=${parsed.amount}) — ignored, expenses only")
            return
        }

        val inForeground = context.applicationContext.let { it is ExpenseApp && it.isForeground }
        Log.d(
            TAG,
            "Parsed debit amount=${parsed.amount} merchant='${parsed.merchant}' " +
                "-> ${if (inForeground) "in-app sheet" else "notification"}"
        )
        if (inForeground) {
            SmsTransactionBus.post(parsed)
        } else {
            SmsNotifier.notify(context, parsed)
        }
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}
