package com.murugan.dailycalm.data.info

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Which list a festival belongs in.
 *
 * `festivals/year` returns 590 entries for 2026 and 242 of them are purchase or muhurat days.
 * Showing them in one flat list buries what a devotee actually opened the app for, so they are
 * split into three sections and [AUSPICIOUS] is collapsed by default.
 */
enum class FestivalSection {
    /** பண்டிகைகள் — the ones people celebrate. */
    FESTIVAL,

    /** விரத நாட்கள் — recurring observances. The retention engine. */
    VRATHAM,

    /** நல்ல நாள் — muhurat, purchase days, eclipses. Useful, but noisy. */
    AUSPICIOUS
}

data class FestivalGroup(
    val section: FestivalSection,
    val occurrences: List<FestivalOccurrence>
)

class FestivalRepository(private val api: InfoNeedsApi = InfoNeedsApi) {

    /**
     * Section for each of the 25 festival masters, keyed by `festivalMasterID`.
     * IDs are stable and embedded in every slug (`skanda-shashti-25`), which makes them a safer
     * key than names — the API returns names with trailing tabs and spaces.
     */
    private val sectionByMasterId: Map<Int, FestivalSection> = mapOf(
        1 to FestivalSection.VRATHAM,      // Pournami
        2 to FestivalSection.AUSPICIOUS,   // Suba Muhurtham
        3 to FestivalSection.FESTIVAL,     // Karthigai
        4 to FestivalSection.VRATHAM,      // Shashti
        5 to FestivalSection.FESTIVAL,     // Shivaratri
        6 to FestivalSection.VRATHAM,      // Ekadasi
        7 to FestivalSection.FESTIVAL,     // Sangadahara Chathurti
        8 to FestivalSection.VRATHAM,      // Pradosham
        9 to FestivalSection.FESTIVAL,     // Chandra Dharshan
        10 to FestivalSection.VRATHAM,     // Amavasya
        11 to FestivalSection.VRATHAM,     // Ashtami
        12 to FestivalSection.VRATHAM,     // Navami
        13 to FestivalSection.AUSPICIOUS,  // Property Purchase Auspicious Days
        14 to FestivalSection.AUSPICIOUS,  // Vehicle Purchase Days
        15 to FestivalSection.AUSPICIOUS,  // Sunday Suba Muhurtham
        16 to FestivalSection.FESTIVAL,    // Hindu Festival Days
        17 to FestivalSection.FESTIVAL,    // Muslim Festival Days
        18 to FestivalSection.FESTIVAL,    // Christian Festival Days
        19 to FestivalSection.AUSPICIOUS,  // Valarpirai and Theipirai Suba Muhurtham
        20 to FestivalSection.AUSPICIOUS,  // Saturn Transit
        21 to FestivalSection.AUSPICIOUS,  // Girivalam Days Time
        22 to FestivalSection.AUSPICIOUS,  // Lunar Eclipse
        23 to FestivalSection.AUSPICIOUS,  // Graha Pravesham
        24 to FestivalSection.AUSPICIOUS,  // Solar Eclipse
        25 to FestivalSection.FESTIVAL     // Skanda Shashti
    )

    /**
     * Pinned to the top of [FestivalSection.FESTIVAL]. This ordering is the whole difference
     * between VetriDaily and a general Tamil calendar: the same API, arranged around one deity.
     * Skanda Shashti is the annual peak; monthly Shashti recurs twice a month.
     */
    private val muruganMasterIds = listOf(25, 4, 3)

    /** Masters are static; fetch once per process. */
    @Volatile
    private var cachedMasters: List<FestivalMaster>? = null

    suspend fun getMasters(): Result<List<FestivalMaster>> = withContext(Dispatchers.IO) {
        cachedMasters?.let { return@withContext Result.success(it) }
        api.getFestivalMasters().onSuccess { cachedMasters = it }
    }

    /**
     * The next few festivals, for the home-screen card.
     * Past entries are dropped because the API does not do it for us.
     */
    suspend fun getUpcoming(count: Int = 10): Result<List<FestivalOccurrence>> =
        withContext(Dispatchers.IO) {
            api.getUpcomingFestivals(count).map { it.upcomingOnly().sortedByDate() }
        }

    /**
     * Every festival in a year, split into sections and ordered for a Murugan audience.
     *
     * `festivals/year` returns a slimmer DTO with no `slug` and no `festivalMasterID`, so the
     * section cannot be read off the row directly. Its `festivalNameTamil` however holds the
     * *master's* Tamil name — "Guru Pradosh Vrat" comes back as "பிரதோஷம் நாட்கள்" — so the
     * masters list is used as a lookup to recover the master id.
     */
    suspend fun getYearGrouped(year: Int): Result<List<FestivalGroup>> =
        withContext(Dispatchers.IO) {
            val masters = getMasters().getOrElse { return@withContext Result.failure(it) }
            val occurrences = api.getFestivalsByYear(year).getOrElse {
                return@withContext Result.failure(it)
            }

            val masterByTamilName = masters
                .filter { !it.nameTamil.isNullOrBlank() }
                .associateBy { it.nameTamil!!.clean() }

            val grouped = occurrences
                .upcomingOnly()
                .sortedByDate()
                // The year endpoint omits slug and masterId, which would leave every row
                // untappable. Both are recovered from the matched master so detail pages open.
                .map { occurrence ->
                    val master = masterByTamilName[occurrence.nameTamil?.clean().orEmpty()]
                    if (occurrence.slug != null && occurrence.masterId != null) {
                        occurrence
                    } else {
                        occurrence.copy(
                            slug = occurrence.slug ?: master?.slug,
                            masterId = occurrence.masterId ?: master?.masterId
                        )
                    }
                }
                .groupBy { occurrence ->
                    sectionByMasterId[occurrence.masterId] ?: FestivalSection.AUSPICIOUS
                }

            // Fixed section order; an empty section is omitted rather than shown blank.
            val sections = listOf(
                FestivalSection.FESTIVAL,
                FestivalSection.VRATHAM,
                FestivalSection.AUSPICIOUS
            ).mapNotNull { section ->
                val rows = grouped[section].orEmpty()
                if (rows.isEmpty()) null else FestivalGroup(section, rows)
            }

            Result.success(sections)
        }

    suspend fun getMonth(month: Int, year: Int): Result<List<FestivalOccurrence>> =
        withContext(Dispatchers.IO) {
            api.getFestivalsByMonth(month, year).map { it.sortedByDate() }
        }

    /**
     * One month, split into the same three sections as [getYearGrouped].
     *
     * Preferred over the year view: a month returns around 40 rows instead of 590, and unlike the
     * year endpoint it includes both `slug` and `festivalMasterID`, so no name-matching is needed
     * to work out the section or to open a detail page.
     *
     * Past dates are kept here — someone browsing a month wants the whole month, including the
     * days already gone.
     */
    suspend fun getMonthGrouped(month: Int, year: Int): Result<List<FestivalGroup>> =
        withContext(Dispatchers.IO) {
            val masters = getMasters().getOrNull().orEmpty()
            val masterByTamilName = masters
                .filter { !it.nameTamil.isNullOrBlank() }
                .associateBy { it.nameTamil!!.clean() }

            api.getFestivalsByMonth(month, year).map { occurrences ->
                val grouped = occurrences
                    .sortedByDate()
                    .map { occurrence ->
                        // Present on this endpoint, but fall back to the name match in case a row
                        // is missing one.
                        val master = masterByTamilName[occurrence.nameTamil?.clean().orEmpty()]
                        occurrence.copy(
                            slug = occurrence.slug ?: master?.slug,
                            masterId = occurrence.masterId ?: master?.masterId
                        )
                    }
                    .groupBy { sectionByMasterId[it.masterId] ?: FestivalSection.AUSPICIOUS }

                listOf(
                    FestivalSection.FESTIVAL,
                    FestivalSection.VRATHAM,
                    FestivalSection.AUSPICIOUS
                ).mapNotNull { section ->
                    val rows = grouped[section].orEmpty()
                    if (rows.isEmpty()) null else FestivalGroup(section, rows)
                }
            }
        }

    /**
     * Detail for one festival type, with [FestivalDetail.upcomingDates] filtered to the future.
     * The API returns dates back to 2023 in a field named "upcoming".
     */
    suspend fun getFestival(slug: String): Result<FestivalDetail> = withContext(Dispatchers.IO) {
        api.getFestival(slug).map { detail ->
            val today = todayIso()
            detail.copy(
                upcomingDates = detail.upcomingDates
                    ?.filter { (it.date?.toIsoDay() ?: "") >= today }
                    ?.sortedBy { it.date?.toIsoDay() ?: "" }
            )
        }
    }

    /** The three Murugan masters, in pinned order, for the top of the festival list. */
    suspend fun getMuruganMasters(): Result<List<FestivalMaster>> = getMasters().map { masters ->
        muruganMasterIds.mapNotNull { id -> masters.firstOrNull { it.masterId == id } }
    }

    /**
     * The next Sashti, for the home-screen countdown.
     *
     * Read from `skanda-shashti-25` rather than `shashti-4`: both return the same dates, but only
     * the former carries the muhurat window, and this app is Murugan-specific anyway.
     */
    suspend fun getNextSashti(): Result<FestivalDate?> =
        getFestival(SKANDA_SHASHTI_SLUG).map { it.upcomingDates?.firstOrNull() }

    /**
     * The next actual festival, for the home-screen countdown.
     *
     * `festivals/upcoming` is dominated by purchase and muhurat days — the next several entries are
     * routinely Vehicle Purchase and Property Purchase Muhurat — so the result is filtered to the
     * masters that belong in [FestivalSection.FESTIVAL]. A countdown to a vehicle-purchase window
     * is not what someone opens a devotional app for.
     */
    suspend fun getNextFestival(): Result<FestivalOccurrence?> = withContext(Dispatchers.IO) {
        val masters = getMasters().getOrNull().orEmpty()
        val masterByTamilName = masters
            .filter { !it.nameTamil.isNullOrBlank() }
            .associateBy { it.nameTamil!!.clean() }

        api.getUpcomingFestivals(count = UPCOMING_SCAN_COUNT).map { occurrences ->
            occurrences
                .upcomingOnly()
                .sortedByDate()
                .map { occurrence ->
                    val master = masterByTamilName[occurrence.nameTamil?.clean().orEmpty()]
                    occurrence.copy(
                        slug = occurrence.slug ?: master?.slug,
                        masterId = occurrence.masterId ?: master?.masterId
                    )
                }
                .firstOrNull { sectionByMasterId[it.masterId] == FestivalSection.FESTIVAL }
        }
    }

    /**
     * What is worth a notification on a given `yyyy-MM-dd`.
     *
     * Deliberately narrow: real festivals plus Sashti, and nothing else. Including Pournami,
     * Amavasai, Ekadasi and Pradosham as well would mean sixty-odd notifications a year, which is
     * the fastest way to get an app muted or uninstalled. Those days are all still browsable in
     * the festivals tab; they just do not interrupt anyone.
     */
    suspend fun getNotableOn(isoDay: String): Result<List<FestivalOccurrence>> =
        withContext(Dispatchers.IO) {
            val month = isoDay.substring(5, 7).toIntOrNull()
                ?: return@withContext Result.success(emptyList())
            val year = isoDay.substring(0, 4).toIntOrNull()
                ?: return@withContext Result.success(emptyList())

            getMonthGrouped(month = month, year = year).map { groups ->
                groups
                    .filter { group ->
                        group.section == FestivalSection.FESTIVAL ||
                            group.occurrences.any { it.masterId == MONTHLY_SASHTI_MASTER_ID }
                    }
                    .flatMap { it.occurrences }
                    .filter { occurrence ->
                        occurrence.date?.toIsoDay() == isoDay &&
                            (occurrence.masterId == MONTHLY_SASHTI_MASTER_ID ||
                                sectionByMasterId[occurrence.masterId] == FestivalSection.FESTIVAL)
                    }
                    .distinctBy { it.festivalId }
            }
        }

    private fun List<FestivalOccurrence>.upcomingOnly(): List<FestivalOccurrence> {
        val today = todayIso()
        return filter { (it.date?.toIsoDay() ?: "") >= today }
    }

    private fun List<FestivalOccurrence>.sortedByDate(): List<FestivalOccurrence> =
        sortedBy { it.date?.toIsoDay() ?: "" }

    private companion object {
        const val SKANDA_SHASHTI_SLUG = "skanda-shashti-25"

        /** Monthly Sashti. Sits in the vratham section but still earns a notification. */
        const val MONTHLY_SASHTI_MASTER_ID = 4

        /** Enough rows to find a real festival past the muhurat days that crowd the front. */
        const val UPCOMING_SCAN_COUNT = 40

        /**
         * Dates arrive as `2026-01-01T00:00:00`. Comparing the `yyyy-MM-dd` prefix as a string is
         * correct for ISO dates and avoids `java.time`, which needs core-library desugaring below
         * API 26 — this app supports API 23.
         */
        fun String.toIsoDay(): String = take(10)

        /** Names come back with trailing tabs and spaces that split identical festivals apart. */
        fun String.clean(): String = trim().replace('\t', ' ').replace(Regex("\\s+"), " ")

        fun todayIso(): String {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return format.format(Calendar.getInstance().time)
        }
    }
}
