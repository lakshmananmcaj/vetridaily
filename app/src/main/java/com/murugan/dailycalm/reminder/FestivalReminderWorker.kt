package com.murugan.dailycalm.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.murugan.dailycalm.MainActivity
import com.murugan.dailycalm.R
import com.murugan.dailycalm.data.info.FestivalOccurrence
import com.murugan.dailycalm.data.info.FestivalRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Runs once a day and, if tomorrow is a festival or Sashti, posts a reminder.
 *
 * A daily check rather than one scheduled alarm per festival: the schedule is then self-healing
 * across reboots, app updates and content changes on the server, with one piece of state instead
 * of dozens of pending work requests.
 */
class FestivalReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // Constructed here rather than injected: WorkManager's default factory reflects for an exact
    // (Context, WorkerParameters) constructor, and a defaulted third parameter does not produce
    // that signature — it would compile and then fail at runtime.
    private val repository = FestivalRepository()

    override suspend fun doWork(): Result {
        if (!ReminderPreferences(applicationContext).isFestivalRemindersEnabled()) {
            return Result.success()
        }

        val tomorrow = tomorrowIso()
        val notable = repository.getNotableOn(tomorrow).getOrElse {
            // Almost always a transient network failure; WorkManager will back off and retry.
            return Result.retry()
        }

        if (notable.isEmpty()) return Result.success()

        createChannelIfNeeded()
        notify(notable)
        return Result.success()
    }

    private fun notify(occurrences: List<FestivalOccurrence>) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val names = occurrences
            .mapNotNull { occurrence ->
                occurrence.nameTamil?.trim()?.takeIf { it.isNotBlank() }
                    ?: occurrence.name?.trim()?.takeIf { it.isNotBlank() }
            }
            .distinct()

        val title = names.firstOrNull() ?: "நாளை விசேஷ நாள்"
        val body = if (names.size > 1) {
            "நாளை · ${names.joinToString(", ")}"
        } else {
            "நாளை · Tomorrow. நேரம் பார்க்க தட்டவும்."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // POST_NOTIFICATIONS may have been revoked since scheduling; notify() throws if so.
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Nothing to recover — the user has withdrawn permission.
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Festival and Sashti reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Evening reminder the day before a festival or Sashti"
        }
        applicationContext.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    private fun tomorrowIso(): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    companion object {
        const val CHANNEL_ID = "vetridaily_festival_channel"
        private const val NOTIFICATION_ID = 1002
    }
}
