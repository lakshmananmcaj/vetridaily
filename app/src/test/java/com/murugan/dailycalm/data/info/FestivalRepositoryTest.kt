package com.murugan.dailycalm.data.info

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dates are deliberately far past (2000) or far future (2099) so "is it upcoming" never depends on
 * when the suite runs.
 */
class FestivalRepositoryTest {

    private val sashtiTamil = "சஷ்டி விரத நாட்கள்"
    private val pradoshamTamil = "பிரதோஷம் நாட்கள்"
    private val hinduTamil = "இந்து பண்டிகை நாட்கள்"
    private val vehicleTamil = "வாகனம் வாங்க நல்ல நாள்"

    private val allMasters = listOf(
        master(4, sashtiTamil, "shashti-4"),
        master(8, pradoshamTamil, "pradosham-8"),
        master(14, vehicleTamil, "vehicle-purchase-days-14"),
        master(16, hinduTamil, "hindu-festival-days-16"),
        master(25, "கந்த சஷ்டி நாட்கள்", "skanda-shashti-25")
    )

    private fun repo(fake: FakeInfoNeedsDataSource) = FestivalRepository(fake)

    // ---- past-date filtering -------------------------------------------------

    @Test
    fun getYearGrouped_dropsDatesAlreadyPast() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byYear = Result.success(
                listOf(
                    occurrence(1, "2000-01-01", hinduTamil),
                    occurrence(2, "2099-01-01", hinduTamil)
                )
            )
        )

        val groups = repo(fake).getYearGrouped(2099).getOrThrow()
        val ids = groups.flatMap { it.occurrences }.map { it.festivalId }

        assertEquals(listOf(2), ids)
    }

    @Test
    fun getMonthGrouped_keepsDatesAlreadyPast() = runTest {
        // Browsing a month should show the whole month, including days gone by.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byMonth = Result.success(
                listOf(
                    occurrence(1, "2000-01-01", hinduTamil, masterId = 16),
                    occurrence(2, "2000-01-15", hinduTamil, masterId = 16)
                )
            )
        )

        val rows = repo(fake).getMonthGrouped(1, 2000).getOrThrow().flatMap { it.occurrences }

        assertEquals(2, rows.size)
    }

    // ---- recovering slug and master id ---------------------------------------

    @Test
    fun getYearGrouped_recoversSlugAndMasterIdFromTamilName() = runTest {
        // The year endpoint omits both; without this the row cannot open a detail page.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byYear = Result.success(listOf(occurrence(1, "2099-05-05", sashtiTamil)))
        )

        val row = repo(fake).getYearGrouped(2099).getOrThrow()
            .flatMap { it.occurrences }
            .single()

        assertEquals("shashti-4", row.slug)
        assertEquals(4, row.masterId)
    }

    @Test
    fun getYearGrouped_matchesNamesCarryingTrailingWhitespace() = runTest {
        // Live data returns "Krishna Paksha Ashtami\t" and similar, which split identical
        // festivals into separate groups before the names were normalised.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byYear = Result.success(
                listOf(occurrence(1, "2099-05-05", "  $sashtiTamil\t"))
            )
        )

        val row = repo(fake).getYearGrouped(2099).getOrThrow()
            .flatMap { it.occurrences }
            .single()

        assertEquals(4, row.masterId)
    }

    @Test
    fun getYearGrouped_keepsMasterIdSuppliedByTheServer() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byYear = Result.success(
                listOf(occurrence(1, "2099-05-05", "unrelated name", masterId = 16, slug = "given"))
            )
        )

        val row = repo(fake).getYearGrouped(2099).getOrThrow()
            .flatMap { it.occurrences }
            .single()

        assertEquals(16, row.masterId)
        assertEquals("given", row.slug)
    }

    // ---- sectioning ----------------------------------------------------------

    @Test
    fun getMonthGrouped_splitsSectionsInFixedOrder() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byMonth = Result.success(
                listOf(
                    occurrence(1, "2099-01-02", vehicleTamil, masterId = 14),
                    occurrence(2, "2099-01-03", hinduTamil, masterId = 16),
                    occurrence(3, "2099-01-04", sashtiTamil, masterId = 4)
                )
            )
        )

        val sections = repo(fake).getMonthGrouped(1, 2099).getOrThrow().map { it.section }

        assertEquals(
            listOf(
                FestivalSection.FESTIVAL,
                FestivalSection.VRATHAM,
                FestivalSection.AUSPICIOUS
            ),
            sections
        )
    }

    @Test
    fun getMonthGrouped_treatsUnknownMasterAsAuspicious() = runTest {
        // Unrecognised rows must not vanish; they land in the collapsed section.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byMonth = Result.success(
                listOf(occurrence(1, "2099-01-02", "something new", masterId = 999))
            )
        )

        val groups = repo(fake).getMonthGrouped(1, 2099).getOrThrow()

        assertEquals(1, groups.size)
        assertEquals(FestivalSection.AUSPICIOUS, groups.single().section)
    }

    @Test
    fun getMonthGrouped_omitsEmptySections() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byMonth = Result.success(
                listOf(occurrence(1, "2099-01-02", hinduTamil, masterId = 16))
            )
        )

        val groups = repo(fake).getMonthGrouped(1, 2099).getOrThrow()

        assertEquals(1, groups.size)
        assertEquals(FestivalSection.FESTIVAL, groups.single().section)
    }

    @Test
    fun getMonthGrouped_requestsTheMonthAsked() = runTest {
        val fake = FakeInfoNeedsDataSource(masters = Result.success(allMasters))

        repo(fake).getMonthGrouped(9, 2026)

        assertEquals(9, fake.lastRequestedMonth)
        assertEquals(2026, fake.lastRequestedYear)
    }

    // ---- detail --------------------------------------------------------------

    @Test
    fun getFestival_dropsUpcomingDatesThatArePast() = runTest {
        // The field is named "upcoming" but the API returns dates going back to 2023.
        val fake = FakeInfoNeedsDataSource(
            detail = Result.success(
                FestivalDetail(
                    masterId = 25,
                    slug = "skanda-shashti-25",
                    upcomingDates = listOf(
                        FestivalDate(1, "2000-01-01T00:00:00"),
                        FestivalDate(2, "2099-03-03T00:00:00"),
                        FestivalDate(3, "2099-01-01T00:00:00")
                    )
                )
            )
        )

        val dates = repo(fake).getFestival("skanda-shashti-25").getOrThrow().upcomingDates

        assertEquals(listOf(3, 2), dates?.map { it.festivalId })
    }

    // ---- notification rule ---------------------------------------------------

    @Test
    fun getNotableOn_returnsFestivalsAndSashtiOnly() = runTest {
        // Pradosham and vehicle-purchase days are browsable but must not raise a notification;
        // including every vratham would mean sixty-odd alerts a year.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byMonth = Result.success(
                listOf(
                    occurrence(1, "2099-01-10", hinduTamil, masterId = 16),
                    occurrence(2, "2099-01-10", sashtiTamil, masterId = 4),
                    occurrence(3, "2099-01-10", pradoshamTamil, masterId = 8),
                    occurrence(4, "2099-01-10", vehicleTamil, masterId = 14)
                )
            )
        )

        val notable = repo(fake).getNotableOn("2099-01-10").getOrThrow().map { it.festivalId }

        assertEquals(setOf(1, 2), notable.toSet())
    }

    @Test
    fun getNotableOn_ignoresOtherDaysInTheSameMonth() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byMonth = Result.success(
                listOf(
                    occurrence(1, "2099-01-10", hinduTamil, masterId = 16),
                    occurrence(2, "2099-01-11", hinduTamil, masterId = 16)
                )
            )
        )

        val notable = repo(fake).getNotableOn("2099-01-11").getOrThrow()

        assertEquals(listOf(2), notable.map { it.festivalId })
    }

    @Test
    fun getNotableOn_returnsEmptyForAnUnparseableDate() = runTest {
        val fake = FakeInfoNeedsDataSource(masters = Result.success(allMasters))

        val notable = repo(fake).getNotableOn("not-a-date").getOrThrow()

        assertTrue(notable.isEmpty())
    }

    // ---- next-festival countdown --------------------------------------------

    @Test
    fun getNextFestival_skipsPurchaseAndMuhuratDays() = runTest {
        // festivals/upcoming leads with Vehicle and Property Purchase Muhurat; a naive countdown
        // would announce those instead of a festival.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            upcoming = Result.success(
                listOf(
                    occurrence(1, "2099-01-01", vehicleTamil, masterId = 14),
                    occurrence(2, "2099-01-02", pradoshamTamil, masterId = 8),
                    occurrence(3, "2099-01-03", hinduTamil, masterId = 16)
                )
            )
        )

        val next = repo(fake).getNextFestival().getOrThrow()

        assertEquals(3, next?.festivalId)
    }

    @Test
    fun getNextFestival_returnsNullWhenOnlyMuhuratDaysRemain() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            upcoming = Result.success(
                listOf(occurrence(1, "2099-01-01", vehicleTamil, masterId = 14))
            )
        )

        assertNull(repo(fake).getNextFestival().getOrThrow())
    }

    // ---- caching -------------------------------------------------------------

    @Test
    fun getMasters_fetchesOnceThenServesFromCache() = runTest {
        val fake = FakeInfoNeedsDataSource(masters = Result.success(allMasters))
        val repository = repo(fake)

        repository.getMasters()
        repository.getMasters()
        repository.getMasters()

        assertEquals(1, fake.masterCallCount)
    }

    // ---- failure propagation -------------------------------------------------

    @Test
    fun getYearGrouped_failsWhenTheYearRequestFails() = runTest {
        val fake = FakeInfoNeedsDataSource(
            masters = Result.success(allMasters),
            byYear = Result.failure(InfoNeedsException("boom"))
        )

        assertTrue(repo(fake).getYearGrouped(2099).isFailure)
    }

    @Test
    fun getMonthGrouped_stillGroupsWhenMastersAreUnavailable() = runTest {
        // Masters only supply the name fallback; rows that carry their own id must survive.
        val fake = FakeInfoNeedsDataSource(
            masters = Result.failure(InfoNeedsException("offline")),
            byMonth = Result.success(
                listOf(occurrence(1, "2099-01-02", hinduTamil, masterId = 16))
            )
        )

        val groups = repo(fake).getMonthGrouped(1, 2099).getOrThrow()

        assertEquals(FestivalSection.FESTIVAL, groups.single().section)
    }
}
