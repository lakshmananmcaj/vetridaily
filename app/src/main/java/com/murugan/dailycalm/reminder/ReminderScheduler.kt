package com.murugan.dailycalm.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val UNIQUE_WORK_NAME = "daily_calm_daily_reminder"
    private const val FESTIVAL_WORK_NAME = "vetridaily_festival_reminder"

    /** 7 PM — in time to plan tomorrow, late enough to still be remembered. */
    private const val FESTIVAL_CHECK_HOUR = 19

    fun schedule(context: Context, hour: Int, minute: Int) {
        val delayMinutes = calculateInitialDelayMinutes(hour, minute)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    /**
     * Daily check for whether tomorrow is a festival or Sashti.
     *
     * Fires in the evening, which is when a reminder about tomorrow is useful — early enough to
     * plan a temple visit or a fast, late enough not to be forgotten by morning.
     */
    fun scheduleFestivalReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<FestivalReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(
                calculateInitialDelayMinutes(FESTIVAL_CHECK_HOUR, 0),
                TimeUnit.MINUTES
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FESTIVAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelFestivalReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FESTIVAL_WORK_NAME)
    }

    private fun calculateInitialDelayMinutes(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val diffMillis = target.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(diffMillis).coerceAtLeast(1)
    }
}
