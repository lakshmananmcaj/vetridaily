package com.murugan.dailycalm.data

import android.content.Context
import java.util.Calendar

class DayProgressStore(context: Context) : DayProgressProvider {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getMaxUnlockedDay(): Int {
        val startDayMillis = getOrCreateStartDayMillis()
        val todayMillis = normalizeToLocalDay(System.currentTimeMillis())
        val daysBetween = ((todayMillis - startDayMillis) / DAY_IN_MILLIS).toInt().coerceAtLeast(0)
        return daysBetween + 1
    }

    private fun getOrCreateStartDayMillis(): Long {
        val stored = prefs.getLong(KEY_START_DAY_MILLIS, -1L)
        if (stored > 0L) return stored

        val today = normalizeToLocalDay(System.currentTimeMillis())
        prefs.edit().putLong(KEY_START_DAY_MILLIS, today).apply()
        return today
    }

    private fun normalizeToLocalDay(timeMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        private const val PREFS_NAME = "daily_calm_prefs"
        private const val KEY_START_DAY_MILLIS = "program_start_day_millis"
        private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }
}
