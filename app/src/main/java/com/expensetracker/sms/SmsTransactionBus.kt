package com.expensetracker.sms

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide hand-off of a single SMS-detected transaction awaiting user confirmation.
 *
 * Holds at most one pending item. The UI observes [pending] and shows the confirmation sheet, then
 * calls [clear] once handled. Both paths write here: the real-time path (app foreground) via the
 * [SmsReceiver], and the notification-tap path via the activity. Using a [StateFlow] (not an event)
 * means a value set during a cold start is replayed to the UI as soon as it subscribes.
 */
object SmsTransactionBus {
    private val _pending = MutableStateFlow<ParsedTransaction?>(null)
    val pending: StateFlow<ParsedTransaction?> = _pending.asStateFlow()

    fun post(transaction: ParsedTransaction) {
        _pending.value = transaction
    }

    fun clear() {
        _pending.value = null
    }
}
