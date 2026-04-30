package com.murugan.dailycalm.data

import com.murugan.dailycalm.DailyContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyContentRepository : DailyContentDataSource {

    override suspend fun getDailyContent(day: Int): Result<DailyContent> = withContext(Dispatchers.IO) {
        val content = SupabaseApi.getDailyContent(day)
        if (content != null) {
            Result.success(content)
        } else {
            Result.failure(IllegalStateException("Unable to load daily content"))
        }
    }
}
