package com.murugan.dailycalm.data.info

/**
 * Read surface for the informationneeds.com API.
 *
 * Exists so the repositories can be exercised against fakes — the concrete [InfoNeedsApi] is an
 * object, so without this every test would go over the network and depend on live festival data.
 * Mirrors [com.murugan.dailycalm.data.DailyContentDataSource].
 */
interface InfoNeedsDataSource {

    suspend fun getFestivalMasters(): Result<List<FestivalMaster>>

    suspend fun getUpcomingFestivals(count: Int = 10): Result<List<FestivalOccurrence>>

    suspend fun getFestivalsByYear(year: Int): Result<List<FestivalOccurrence>>

    suspend fun getFestivalsByMonth(month: Int, year: Int): Result<List<FestivalOccurrence>>

    suspend fun getFestival(slug: String): Result<FestivalDetail>

    suspend fun getAllTemples(): Result<List<Temple>>

    suspend fun getTemple(slug: String): Result<Temple>
}
