package com.expensetracker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.expensetracker.data.AppDatabase
import com.expensetracker.data.RoomExpenseRepository
import com.expensetracker.domain.ExpenseRepository
import com.expensetracker.sms.SmsNotifier

/** Application holding the app-scoped database and repository (manual DI). */
class ExpenseApp : Application() {

    val repository: ExpenseRepository by lazy {
        val db = AppDatabase.get(this)
        RoomExpenseRepository(
            db.budgetDao(), db.transactionDao(), db.categoryLimitDao(), db.incomeDao(), db.savingsEntryDao()
        )
    }

    /**
     * Whether any activity is currently in the started/foreground state. The [SmsReceiver] reads
     * this to decide between showing the in-app confirmation sheet and posting a notification.
     */
    @Volatile
    var isForeground: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        SmsNotifier.createChannel(this)
        registerActivityLifecycleCallbacks(ForegroundTracker())
    }

    private inner class ForegroundTracker : ActivityLifecycleCallbacks {
        private var startedActivities = 0

        override fun onActivityStarted(activity: Activity) {
            startedActivities++
            isForeground = true
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities--
            if (startedActivities <= 0) isForeground = false
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
