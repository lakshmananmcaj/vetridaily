package com.murugan.dailycalm.data.info

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempleRepositoryTest {

    /**
     * Mirrors the live directory: the six abodes carry their own deity names, so `deity=Murugan`
     * matches only two of them. Ids 1-6 follow the traditional sequence.
     */
    private val directory = listOf(
        temple(3, "Palani Murugan Temple", "Arupadai Veedu", "Palani"),
        temple(1, "Thiruparankundram Murugan Temple", "Arupadai Veedu", "Thiruparankundram"),
        temple(20, "Alangudi Guru Temple", "Navagraha Temple", "Alangudi"),
        temple(6, "Pazhamudhircholai Murugan Temple", "Arupadai Veedu", "Madurai"),
        temple(2, "Thiruchendur Murugan Temple", "Arupadai Veedu", "Thiruchendur"),
        temple(11, "Kapaleeshwarar Temple", "Paadal Petra Sthalam", "Chennai"),
        temple(4, "Swamimalai Murugan Temple", "Arupadai Veedu", "Swamimalai"),
        temple(5, "Thiruthani Murugan Temple", "Arupadai Veedu", "Thiruthani"),
        temple(30, "Velankanni Church", "Church", "Velankanni")
    )

    private fun repo(fake: FakeInfoNeedsDataSource) = TempleRepository(fake)

    @Test
    fun getArupadaiVeedu_returnsAllSixMatchedOnTempleType() = runTest {
        val fake = FakeInfoNeedsDataSource(temples = Result.success(directory))

        val six = repo(fake).getArupadaiVeedu().getOrThrow()

        assertEquals(6, six.size)
        assertTrue(six.all { it.templeType == "Arupadai Veedu" })
    }

    @Test
    fun getArupadaiVeedu_ordersByTempleIdSoTheSequenceIsTraditional() = runTest {
        val fake = FakeInfoNeedsDataSource(temples = Result.success(directory))

        val names = repo(fake).getArupadaiVeedu().getOrThrow().map { it.templeId }

        assertEquals(listOf(1, 2, 3, 4, 5, 6), names)
    }

    @Test
    fun getArupadaiVeedu_toleratesTrailingWhitespaceOnTheType() = runTest {
        val fake = FakeInfoNeedsDataSource(
            temples = Result.success(listOf(temple(1, "A", "  Arupadai Veedu ")))
        )

        assertEquals(1, repo(fake).getArupadaiVeedu().getOrThrow().size)
    }

    @Test
    fun getOtherTemples_excludesTheSixAndSortsByName() = runTest {
        val fake = FakeInfoNeedsDataSource(temples = Result.success(directory))

        val others = repo(fake).getOtherTemples().getOrThrow()

        assertEquals(3, others.size)
        assertTrue(others.none { it.templeType == "Arupadai Veedu" })
        assertEquals(
            listOf("Alangudi Guru Temple", "Kapaleeshwarar Temple", "Velankanni Church"),
            others.map { it.name }
        )
    }

    @Test
    fun getTemplesInCity_matchesIgnoringCaseAndPadding() = runTest {
        val fake = FakeInfoNeedsDataSource(temples = Result.success(directory))

        val found = repo(fake).getTemplesInCity(" palani ").getOrThrow()

        assertEquals(listOf(3), found.map { it.templeId })
    }

    @Test
    fun getGroupedByType_keepsEveryTempleExactlyOnce() = runTest {
        val fake = FakeInfoNeedsDataSource(temples = Result.success(directory))

        val grouped = repo(fake).getGroupedByType().getOrThrow()

        assertEquals(directory.size, grouped.values.sumOf { it.size })
        assertEquals(6, grouped["Arupadai Veedu"]?.size)
    }

    @Test
    fun getAllTemples_fetchesOnceThenServesFromCache() = runTest {
        // Both lists on the temples tab are backed by one request.
        val fake = FakeInfoNeedsDataSource(temples = Result.success(directory))
        val repository = repo(fake)

        repository.getArupadaiVeedu()
        repository.getOtherTemples()
        repository.getAllTemples()

        assertEquals(1, fake.templeCallCount)
    }

    @Test
    fun getArupadaiVeedu_propagatesFailure() = runTest {
        val fake = FakeInfoNeedsDataSource(temples = Result.failure(InfoNeedsException("offline")))

        assertTrue(repo(fake).getArupadaiVeedu().isFailure)
    }
}
