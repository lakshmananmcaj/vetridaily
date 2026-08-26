package com.murugan.dailycalm.data

import com.murugan.dailycalm.DailyContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyContentRepository : DailyContentDataSource {

    override suspend fun getDailyContent(day: Int): Result<DailyContent> = withContext(Dispatchers.IO) {
        SupabaseApi.getDailyContent(day)
    }

    override suspend fun getLatestPublishedDay(): Result<Int> = withContext(Dispatchers.IO) {
        SupabaseApi.getLatestPublishedDay()
    }
}
