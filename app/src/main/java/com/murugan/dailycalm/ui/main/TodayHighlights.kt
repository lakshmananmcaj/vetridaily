package com.murugan.dailycalm.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murugan.dailycalm.data.info.FestivalRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private val SashtiBackground = Color(0x2EFFD54F)
private val FestivalBackground = Color(0x1FFFFFFF)
private val Accent = Color(0xFFFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

private data class Highlight(
    val tamilLabel: String,
    val englishLabel: String,
    val title: String,
    val date: String,
    val daysAway: Long?,
    val window: String?
)

/**
 * Next Sashti and next festival, shown above the daily card.
 *
 * Sashti is the recurring hook: it comes round every month and belongs to Murugan specifically,
 * so it gives the app a reason to be opened between festivals. Both cards fail quietly — the daily
 * practice is the product and must render even when this API is unreachable.
 */
@Composable
fun TodayHighlights(
    modifier: Modifier = Modifier,
    repository: FestivalRepository = FestivalRepository()
) {
    val sashti by produceState<Highlight?>(initialValue = null) {
        value = repository.getNextSashti().getOrNull()?.let { date ->
            val day = date.date?.take(10) ?: return@let null
            Highlight(
                tamilLabel = "அடுத்த சஷ்டி",
                englishLabel = "Next Sashti",
                title = day.toDisplayDate(),
                date = day,
                daysAway = day.daysFromToday(),
                window = listOfNotNull(
                    date.startingTime?.trim()?.takeIf { it.isNotBlank() },
                    date.endTime?.trim()?.takeIf { it.isNotBlank() }
                ).takeIf { it.isNotEmpty() }?.joinToString(" – ")
            )
        }
    }

    val festival by produceState<Highlight?>(initialValue = null) {
        value = repository.getNextFestival().getOrNull()?.let { occurrence ->
            val day = occurrence.date?.take(10) ?: return@let null
            Highlight(
                tamilLabel = "அடுத்த பண்டிகை",
                englishLabel = "Next festival",
                title = occurrence.name?.trim().orEmpty()
                    .ifBlank { occurrence.nameTamil?.trim().orEmpty() },
                date = day,
                daysAway = day.daysFromToday(),
                window = null
            )
        }
    }

    if (sashti == null && festival == null) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sashti?.let {
            HighlightCard(
                highlight = it,
                background = SashtiBackground,
                modifier = Modifier.weight(1f)
            )
        }
        festival?.let {
            HighlightCard(
                highlight = it,
                background = FestivalBackground,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HighlightCard(
    highlight: Highlight,
    background: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = background),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = highlight.tamilLabel, color = Faint, fontSize = 12.sp)

            Text(
                text = highlight.daysAway.toCountdown(),
                color = Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp
            )

            Text(
                text = highlight.title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2
            )

            highlight.window?.let {
                Text(text = it, color = Muted, fontSize = 11.sp, maxLines = 2)
            }
        }
    }
}

private fun Long?.toCountdown(): String = when {
    this == null -> "—"
    this <= 0L -> "இன்று"
    this == 1L -> "நாளை"
    else -> "$this நாட்கள்"
}

/** Days between today and an ISO `yyyy-MM-dd`, or null when it cannot be parsed. */
private fun String.daysFromToday(): Long? = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(this)
    if (parsed == null) {
        null
    } else {
        val target = Calendar.getInstance().apply { time = parsed }.startOfDay()
        val today = Calendar.getInstance().startOfDay()
        TimeUnit.MILLISECONDS.toDays(target.timeInMillis - today.timeInMillis)
    }
} catch (e: Exception) {
    null
}

/** Zeroing the clock keeps the day count off by one either side of midnight. */
private fun Calendar.startOfDay(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun String.toDisplayDate(): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val printer = SimpleDateFormat("dd MMM", Locale.US)
    parser.parse(this)?.let(printer::format) ?: this
} catch (e: Exception) {
    this
}
