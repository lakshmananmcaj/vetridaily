package com.murugan.dailycalm.data.info

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TempleRepository(private val api: InfoNeedsApi = InfoNeedsApi) {

    /**
     * The six abodes of Murugan. Matched on `templeType` rather than `deity`, because each shrine
     * carries its own deity name — Palani is "Lord Dhandayuthapani", Thiruchendur is
     * "Lord Senthilnathan" — so the API's `deity=Murugan` filter returns only two of the six.
     */
    private val arupadaiVeeduType = "Arupadai Veedu"

    /** Only 30 temples exist and they never change during a session. */
    @Volatile
    private var cachedTemples: List<Temple>? = null

    suspend fun getAllTemples(): Result<List<Temple>> = withContext(Dispatchers.IO) {
        cachedTemples?.let { return@withContext Result.success(it) }
        api.getAllTemples().onSuccess { cachedTemples = it }
    }

    /**
     * The Arupadai Veedu in traditional order.
     *
     * `templeID` 1-6 already follows the canonical sequence — Thiruparankundram, Thiruchendur,
     * Palani, Swamimalai, Thiruthani, Pazhamudhircholai — so sorting by id preserves it without a
     * hardcoded name list that would break if a temple were renamed.
     */
    suspend fun getArupadaiVeedu(): Result<List<Temple>> = getAllTemples().map { temples ->
        temples
            .filter { it.templeType?.trim().equals(arupadaiVeeduType, ignoreCase = true) }
            .sortedBy { it.templeId }
    }

    /** Everything that is not one of the six, for browsing the wider directory. */
    suspend fun getOtherTemples(): Result<List<Temple>> = getAllTemples().map { temples ->
        temples
            .filterNot { it.templeType?.trim().equals(arupadaiVeeduType, ignoreCase = true) }
            .sortedBy { it.name?.trim().orEmpty() }
    }

    /** Temple types present in the directory, each with its temples, for a grouped browser. */
    suspend fun getGroupedByType(): Result<Map<String, List<Temple>>> = getAllTemples().map { temples ->
        temples
            .groupBy { it.templeType?.trim().orEmpty().ifBlank { "Other" } }
            .toSortedMap()
    }

    suspend fun getTemplesInCity(city: String): Result<List<Temple>> = getAllTemples().map { temples ->
        temples.filter { it.city?.trim().equals(city.trim(), ignoreCase = true) }
    }

    suspend fun getTemple(slug: String): Result<Temple> = withContext(Dispatchers.IO) {
        api.getTemple(slug)
    }
}
