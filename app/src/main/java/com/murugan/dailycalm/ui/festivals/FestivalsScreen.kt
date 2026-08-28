package com.murugan.dailycalm.ui.festivals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.murugan.dailycalm.data.info.FestivalMaster
import com.murugan.dailycalm.data.info.FestivalOccurrence
import com.murugan.dailycalm.data.info.FestivalSection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val CardBackground = Color(0x1FFFFFFF)
private val PinnedBackground = Color(0x2EFFD54F)
private val SelectedChip = Color(0xFFFFD54F)
private val UnselectedChip = Color(0x1FFFFFFF)
private val Accent = Color(0xFFFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

/** Gregorian months as Tamil readers name them. */
private val TAMIL_MONTHS = listOf(
    "ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்",
    "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்"
)

private val TAMIL_WEEKDAYS = mapOf(
    Calendar.SUNDAY to "ஞாயிறு",
    Calendar.MONDAY to "திங்கள்",
    Calendar.TUESDAY to "செவ்வாய்",
    Calendar.WEDNESDAY to "புதன்",
    Calendar.THURSDAY to "வியாழன்",
    Calendar.FRIDAY to "வெள்ளி",
    Calendar.SATURDAY to "சனி"
)

@Composable
fun FestivalsScreen(
    modifier: Modifier = Modifier,
    viewModel: FestivalsViewModel = viewModel(factory = FestivalsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val expanded by viewModel.expandedSections.collectAsState()
    val period by viewModel.period.collectAsState()

    var selectedSlug by rememberSaveable { mutableStateOf<String?>(null) }

    selectedSlug?.let { slug ->
        FestivalDetailScreen(
            slug = slug,
            onBack = { selectedSlug = null },
            modifier = modifier
        )
        return
    }

    val onFestivalClick: (String) -> Unit = { slug -> selectedSlug = slug }

    Column(modifier = modifier.fillMaxSize()) {
        YearRow(
            years = viewModel.years,
            selected = period.year,
            onSelect = viewModel::selectYear
        )
        MonthRow(
            selected = period.month,
            onSelect = viewModel::selectMonth
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is FestivalsUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is FestivalsUiState.Error -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = state.message, color = Muted, fontSize = 14.sp)
                    Button(onClick = viewModel::retry) { Text("Retry") }
                }

                is FestivalsUiState.Success -> {
                    if (state.groups.isEmpty()) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            text = "இந்த மாதம் நாட்கள் இல்லை\nNothing listed for this month",
                            color = Faint,
                            fontSize = 14.sp
                        )
                    } else {
                        FestivalList(
                            state = state,
                            expanded = expanded,
                            onToggleSection = viewModel::toggleSection,
                            onFestivalClick = onFestivalClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearRow(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        years.forEach { year ->
            val isSelected = year == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) SelectedChip else UnselectedChip)
                    .clickable { onSelect(year) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = year.toString(),
                    color = if (isSelected) Color(0xFF06310F) else Muted,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun MonthRow(selected: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()

    // Keep the chosen month visible when the screen opens on the current month.
    LaunchedEffect(selected) {
        listState.animateScrollToItem((selected - 2).coerceAtLeast(0))
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TAMIL_MONTHS.size) { index ->
            val month = index + 1
            val isSelected = month == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) Accent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(month) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = TAMIL_MONTHS[index],
                    color = if (isSelected) Accent else Faint,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun FestivalList(
    state: FestivalsUiState.Success,
    expanded: Set<FestivalSection>,
    onToggleSection: (FestivalSection) -> Unit,
    onFestivalClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.muruganPinned.isNotEmpty()) {
            item {
                SectionHeader(
                    tamil = "முருகன் நாட்கள்",
                    english = "Murugan days",
                    count = state.muruganPinned.size,
                    expandable = false
                )
            }
            items(state.muruganPinned, key = { "pinned-${it.masterId}" }) { master ->
                PinnedMasterRow(master = master, onClick = onFestivalClick)
            }
        }

        state.groups.forEach { group ->
            val isExpanded = group.section in expanded

            item(key = "header-${group.section}") {
                SectionHeader(
                    tamil = group.section.tamilLabel(),
                    english = group.section.englishLabel(),
                    count = group.occurrences.size,
                    expandable = true,
                    expanded = isExpanded,
                    onClick = { onToggleSection(group.section) }
                )
            }

            if (isExpanded) {
                items(
                    items = group.occurrences,
                    key = { "${group.section}-${it.festivalId}-${it.date.orEmpty()}" }
                ) { occurrence ->
                    OccurrenceRow(
                        occurrence = occurrence,
                        onClick = { occurrence.slug?.let(onFestivalClick) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    tamil: String,
    english: String,
    count: Int,
    expandable: Boolean,
    expanded: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (expandable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tamil,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Text(text = "$english · $count", color = Faint, fontSize = 12.sp)
        }
        if (expandable) {
            Text(
                text = if (expanded) "Hide" else "Show",
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PinnedMasterRow(master: FestivalMaster, onClick: (String) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(master.slug?.let { slug -> Modifier.clickable { onClick(slug) } } ?: Modifier),
        colors = CardDefaults.elevatedCardColors(containerColor = PinnedBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = master.nameTamil?.tidy().orEmpty().ifBlank { master.name?.tidy().orEmpty() },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            master.name?.tidy()?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OccurrenceRow(occurrence: FestivalOccurrence, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (occurrence.slug != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day number and Tamil weekday, the way Tamil calendars present a date.
            occurrence.date?.let { raw ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = raw.dayOfMonth(),
                        color = Accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                    Text(text = raw.tamilWeekday(), color = Faint, fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val tamil = occurrence.nameTamil?.tidy().orEmpty()
                val english = occurrence.name?.tidy().orEmpty()

                Text(
                    text = english.ifBlank { tamil },
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                if (tamil.isNotBlank() && english.isNotBlank()) {
                    Text(text = tamil, color = Muted, fontSize = 13.sp)
                }
                occurrence.muhurtamWindow()?.let {
                    Text(text = it, color = Faint, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun FestivalSection.tamilLabel(): String = when (this) {
    FestivalSection.FESTIVAL -> "பண்டிகைகள்"
    FestivalSection.VRATHAM -> "விரத நாட்கள்"
    FestivalSection.AUSPICIOUS -> "நல்ல நாள்"
}

private fun FestivalSection.englishLabel(): String = when (this) {
    FestivalSection.FESTIVAL -> "Festivals"
    FestivalSection.VRATHAM -> "Vratham days"
    FestivalSection.AUSPICIOUS -> "Auspicious days"
}

/** Names arrive with trailing tabs and doubled spaces. */
private fun String.tidy(): String = trim().replace('\t', ' ').replace(Regex("\\s+"), " ")

private fun String.toCalendar(): Calendar? {
    val day = take(10)
    if (day.length != 10) return null
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day) ?: return null
        Calendar.getInstance().apply { time = parsed }
    } catch (e: Exception) {
        null
    }
}

private fun String.dayOfMonth(): String =
    toCalendar()?.get(Calendar.DAY_OF_MONTH)?.toString()?.padStart(2, '0') ?: take(10)

private fun String.tamilWeekday(): String =
    toCalendar()?.let { TAMIL_WEEKDAYS[it.get(Calendar.DAY_OF_WEEK)] }.orEmpty()

/**
 * The muhurat window as given. Times arrive in several shapes — `"09:08  "`,
 * `" 01:47 AM, Jan 01 "`, `"22:22"` — so they are trimmed rather than reformatted.
 */
private fun FestivalOccurrence.muhurtamWindow(): String? {
    val start = startingTime?.tidy().orEmpty()
    val end = endTime?.tidy().orEmpty()
    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start – $end"
        start.isNotBlank() -> start
        end.isNotBlank() -> end
        else -> null
    }
}
