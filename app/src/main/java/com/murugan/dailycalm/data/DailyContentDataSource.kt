package com.murugan.dailycalm.data

import com.murugan.dailycalm.DailyContent

interface DailyContentDataSource {
    suspend fun getDailyContent(day: Int): Result<DailyContent>
}
