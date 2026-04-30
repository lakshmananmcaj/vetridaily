package com.murugan.dailycalm.ui.main

import com.murugan.dailycalm.DailyContent
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
}

private class FakeRepository(
    private val responses: Map<Int, Result<DailyContent>>
) : DailyContentDataSource {
    override suspend fun getDailyContent(day: Int): Result<DailyContent> {
        return responses[day] ?: Result.failure(IllegalStateException("Missing day $day"))
    }
}

private class FakeDayProgressProvider(
    private val maxUnlockedDay: Int
) : DayProgressProvider {
    override fun getMaxUnlockedDay(): Int = maxUnlockedDay
}
