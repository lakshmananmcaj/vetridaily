package com.murugan.dailycalm.reminder

import android.content.Context

class ReminderPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getHour(): Int = prefs.getInt(KEY_HOUR, 20)

    fun getMinute(): Int = prefs.getInt(KEY_MINUTE, 0)

    fun setTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    /**
     * Evening reminder the day before a festival or Sashti. Independent of the daily reminder, so
     * someone can keep one without the other. Off until asked for.
     */
    fun isFestivalRemindersEnabled(): Boolean = prefs.getBoolean(KEY_FESTIVAL_ENABLED, false)

    fun setFestivalRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FESTIVAL_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "daily_calm_reminder_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_FESTIVAL_ENABLED = "festival_reminders_enabled"
    }
}
