package com.expensetracker.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expensetracker.MainActivity
import com.expensetracker.R
import com.expensetracker.util.asCurrency

/**
 * Owns the "transaction detected" notification channel and posts the notification shown when an SMS
 * arrives while the app is backgrounded. Tapping it opens [MainActivity] with the parsed fields as
 * extras so the confirmation sheet can be shown.
 */
object SmsNotifier {

    private const val CHANNEL_ID = "sms_transactions"
    private const val NOTIFICATION_ID = 4401

    const val ACTION_CONFIRM_SMS_TXN = "com.expensetracker.action.CONFIRM_SMS_TXN"
    const val EXTRA_AMOUNT = "sms_amount"
    const val EXTRA_MERCHANT = "sms_merchant"
    const val EXTRA_IS_DEBIT = "sms_is_debit"
    const val EXTRA_TIMESTAMP = "sms_timestamp"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transaction alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Prompts to add a transaction detected in an SMS" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun notify(context: Context, transaction: ParsedTransaction) {
        // The receiver fires regardless of notification permission; bail out if we can't post.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CONFIRM_SMS_TXN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_AMOUNT, transaction.amount)
            putExtra(EXTRA_MERCHANT, transaction.merchant)
            putExtra(EXTRA_IS_DEBIT, transaction.isDebit)
            putExtra(EXTRA_TIMESTAMP, transaction.smsTimestamp)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Add transaction?")
            .setContentText("${transaction.amount.asCurrency()} at ${transaction.merchant} — tap to add")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // notify() can still throw on API 33+ without POST_NOTIFICATIONS; stay defensive.
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}
