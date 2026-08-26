package com.murugan.dailycalm.data

import com.murugan.dailycalm.DailyContent

/**
 * Raised when the request reached the backend and succeeded, but no published day exists.
 *
 * Kept distinct from a transport failure so the UI can end the journey gracefully instead of
 * reporting a network error the user cannot act on.
 */
class ContentNotFoundException(val day: Int) : Exception("No content published for day $day")

interface DailyContentDataSource {
    suspend fun getDailyContent(day: Int): Result<DailyContent>

    /** Highest day number that has audio published, used to cap the journey. */
    suspend fun getLatestPublishedDay(): Result<Int>
}
