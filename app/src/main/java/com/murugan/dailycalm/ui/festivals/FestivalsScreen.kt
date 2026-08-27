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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.murugan.dailycalm.data.info.FestivalMaster
import com.murugan.dailycalm.data.info.FestivalOccurrence
import com.murugan.dailycalm.data.info.FestivalSection
import java.text.SimpleDateFormat
import java.util.Locale

private val CardBackground = Color(0x1FFFFFFF)
private val PinnedBackground = Color(0x2EFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

@Composable
fun FestivalsScreen(
    modifier: Modifier = Modifier,
    viewModel: FestivalsViewModel = viewModel(factory = FestivalsViewModel.Factory),
    onFestivalClick: (slug: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val expanded by viewModel.expandedSections.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
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
                Text(
                    text = state.message,
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = viewModel::retry) { Text("Retry") }
            }

            is FestivalsUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
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
                        PinnedMasterRow(master = master, onClick = { onFestivalClick(it) })
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
                            onClick = { viewModel.toggleSection(group.section) }
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
            .padding(top = 14.dp, bottom = 2.dp),
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
                text = master.nameTamil?.trim().orEmpty().ifBlank { master.name?.trim().orEmpty() },
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            master.name?.trim()?.takeIf { it.isNotBlank() }?.let {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val tamil = occurrence.nameTamil?.tidy().orEmpty()
            val english = occurrence.name?.tidy().orEmpty()

            Text(
                text = tamil.ifBlank { english },
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            if (english.isNotBlank() && tamil.isNotBlank()) {
                Text(text = english, color = Muted, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                occurrence.date?.toDisplayDate()?.let {
                    Text(text = it, color = Faint, fontSize = 12.sp)
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

/** `2026-01-01T00:00:00` becomes `01 Jan 2026`. Falls back to the raw prefix if unparseable. */
private fun String.toDisplayDate(): String? {
    val day = take(10)
    if (day.length != 10) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val printer = SimpleDateFormat("dd MMM yyyy", Locale.US)
        parser.parse(day)?.let(printer::format) ?: day
    } catch (e: Exception) {
        day
    }
}

/**
 * The muhurat window, when the API supplies one. Times come in several shapes
 * (`"09:08  "`, `" 01:47 AM, Jan 01 "`, `"22:22"`), so they are shown as given, just trimmed.
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
