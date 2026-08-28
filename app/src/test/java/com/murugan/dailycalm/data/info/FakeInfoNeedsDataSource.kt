package com.murugan.dailycalm.data.info

/**
 * In-memory stand-in for the informationneeds.com API.
 *
 * Every method defaults to an empty success so a test only has to supply the data it cares about.
 */
class FakeInfoNeedsDataSource(
    private val masters: Result<List<FestivalMaster>> = Result.success(emptyList()),
    private val upcoming: Result<List<FestivalOccurrence>> = Result.success(emptyList()),
    private val byYear: Result<List<FestivalOccurrence>> = Result.success(emptyList()),
    private val byMonth: Result<List<FestivalOccurrence>> = Result.success(emptyList()),
    private val detail: Result<FestivalDetail> = Result.success(FestivalDetail()),
    private val temples: Result<List<Temple>> = Result.success(emptyList()),
    private val temple: Result<Temple> = Result.success(Temple())
) : InfoNeedsDataSource {

    /** Call counts, so caching can be asserted rather than assumed. */
    var masterCallCount = 0
        private set
    var templeCallCount = 0
        private set

    var lastRequestedMonth: Int? = null
        private set
    var lastRequestedYear: Int? = null
        private set

    override suspend fun getFestivalMasters(): Result<List<FestivalMaster>> {
        masterCallCount++
        return masters
    }

    override suspend fun getUpcomingFestivals(count: Int): Result<List<FestivalOccurrence>> = upcoming

    override suspend fun getFestivalsByYear(year: Int): Result<List<FestivalOccurrence>> {
        lastRequestedYear = year
        return byYear
    }

    override suspend fun getFestivalsByMonth(
        month: Int,
        year: Int
    ): Result<List<FestivalOccurrence>> {
        lastRequestedMonth = month
        lastRequestedYear = year
        return byMonth
    }

    override suspend fun getFestival(slug: String): Result<FestivalDetail> = detail

    override suspend fun getAllTemples(): Result<List<Temple>> {
        templeCallCount++
        return temples
    }

    override suspend fun getTemple(slug: String): Result<Temple> = temple
}

// ---- builders -------------------------------------------------------------

internal fun master(id: Int, tamil: String, slug: String = "master-$id") = FestivalMaster(
    masterId = id,
    name = "Master $id",
    nameTamil = tamil,
    slug = slug
)

internal fun occurrence(
    id: Int,
    date: String,
    tamil: String? = null,
    name: String? = null,
    masterId: Int? = null,
    slug: String? = null,
    start: String? = null,
    end: String? = null
) = FestivalOccurrence(
    festivalId = id,
    masterId = masterId,
    name = name,
    nameTamil = tamil,
    date = "${date}T00:00:00",
    slug = slug,
    startingTime = start,
    endTime = end
)

internal fun temple(
    id: Int,
    name: String,
    type: String?,
    city: String? = null,
    slug: String? = null
) = Temple(
    templeId = id,
    name = name,
    slug = slug ?: name.lowercase().replace(' ', '-'),
    city = city,
    templeType = type
)
