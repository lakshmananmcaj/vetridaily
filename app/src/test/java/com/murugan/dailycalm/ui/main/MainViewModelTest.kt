package com.murugan.dailycalm.ui.main

import com.murugan.dailycalm.DailyContent
import com.murugan.dailycalm.data.ContentNotFoundException
import com.murugan.dailycalm.data.DailyContentDataSource
import com.murugan.dailycalm.data.DayProgressProvider
import com.murugan.dailycalm.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsRealUnlockedDayContent() = runTest {
        val repo = FakeRepository(
            mapOf(
                3 to Result.success(
                    DailyContent(
                        title = "Day 3",
                        body = "Body 3",
                        audio_url = "audio3"
                    )
                )
            )
        )
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 3)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )

        advanceUntilIdle()

        assertEquals(3, viewModel.selectedDay.value)
        assertEquals(3, viewModel.maxUnlockedDay.value)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainUiState.Success)
        assertEquals("Day 3", (uiState as MainUiState.Success).content.title)
    }

    @Test
    fun loadNextDay_whenDayFails_fallsBackToDay1() = runTest {
        val repo = FakeRepository(
            mapOf(
                1 to Result.success(
                    DailyContent(
                        title = "Day 1",
                        body = "Body 1",
                        audio_url = "audio1"
                    )
                ),
                2 to Result.failure(IllegalStateException("Missing Day 2"))
            )
        )
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 2)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )
        advanceUntilIdle()

        viewModel.loadNextDay()
        advanceUntilIdle()

        assertEquals(1, viewModel.selectedDay.value)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainUiState.Success)
        assertEquals("Day 1", (uiState as MainUiState.Success).content.title)
    }

    @Test
    fun loadNextDay_whenAtMaxUnlockedDay_doesNothing() = runTest {
        val repo = FakeRepository(
            mapOf(
                1 to Result.success(
                    DailyContent(
                        title = "Day 1",
                        body = "Body 1",
                        audio_url = "audio1"
                    )
                )
            )
        )
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 1)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )
        advanceUntilIdle()

        viewModel.loadNextDay()
        advanceUntilIdle()

        assertEquals(1, viewModel.selectedDay.value)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainUiState.Success)
        assertEquals("Day 1", (uiState as MainUiState.Success).content.title)
    }

    @Test
    fun init_whenEnrolledLongerThanContent_completesJourneyAtLastPublishedDay() = runTest {
        val repo = FakeRepository(publishedDays(1..70))
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 85)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )
        advanceUntilIdle()

        assertEquals(70, viewModel.selectedDay.value)
        assertEquals(70, viewModel.maxUnlockedDay.value)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainUiState.JourneyComplete)
        assertEquals(70, (uiState as MainUiState.JourneyComplete).lastDay)
    }

    @Test
    fun loadPreviousDay_fromCompletedJourney_revisitsLastPublishedDay() = runTest {
        val repo = FakeRepository(publishedDays(1..70))
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 85)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MainUiState.JourneyComplete)

        viewModel.loadPreviousDay()
        advanceUntilIdle()

        assertEquals(70, viewModel.selectedDay.value)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainUiState.Success)
        assertEquals("Day 70", (uiState as MainUiState.Success).content.title)
    }

    @Test
    fun whenDayIsNotPublished_completesJourneyInsteadOfShowingError() = runTest {
        val repo = FakeRepository(
            responses = mapOf(
                1 to Result.success(
                    DailyContent(
                        title = "Day 1",
                        body = "Body 1",
                        audio_url = "audio1"
                    )
                ),
                2 to Result.failure(ContentNotFoundException(day = 2))
            ),
            latestPublishedDay = 2
        )
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 2)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainUiState.JourneyComplete)
        assertEquals(1, (uiState as MainUiState.JourneyComplete).lastDay)
        assertEquals(1, viewModel.selectedDay.value)
        assertEquals(1, viewModel.maxUnlockedDay.value)
    }

    @Test
    fun whenNetworkFails_showsErrorRatherThanCompletingJourney() = runTest {
        val repo = FakeRepository(
            responses = mapOf(1 to Result.failure(IOException("offline"))),
            latestPublishedDay = null
        )
        val dayProgress = FakeDayProgressProvider(maxUnlockedDay = 1)

        val viewModel = MainViewModel(
            repository = repo,
            dayProgressStore = dayProgress,
            debugForceMaxUnlockedDay = 0
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MainUiState.Error)
    }
}

private fun publishedDays(days: IntRange): Map<Int, Result<DailyContent>> =
    days.associateWith { day ->
        Result.success(
            DailyContent(
                title = "Day $day",
                body = "Body $day",
                audio_url = "audio$day"
            )
        )
    }

private class FakeRepository(
    private val responses: Map<Int, Result<DailyContent>>,
    private val latestPublishedDay: Int? = responses.keys.maxOrNull()
) : DailyContentDataSource {

    override suspend fun getDailyContent(day: Int): Result<DailyContent> {
        return responses[day] ?: Result.failure(ContentNotFoundException(day))
    }

    override suspend fun getLatestPublishedDay(): Result<Int> {
        val latest = latestPublishedDay ?: return Result.failure(IOException("offline"))
        return Result.success(latest)
    }
}

private class FakeDayProgressProvider(
    private val maxUnlockedDay: Int
) : DayProgressProvider {
    override fun getMaxUnlockedDay(): Int = maxUnlockedDay
}
