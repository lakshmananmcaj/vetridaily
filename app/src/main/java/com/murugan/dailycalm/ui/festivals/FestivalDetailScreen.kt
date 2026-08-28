package com.murugan.dailycalm.ui.festivals

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murugan.dailycalm.data.info.FestivalDate
import com.murugan.dailycalm.data.info.FestivalDetail
import com.murugan.dailycalm.data.info.FestivalRepository
import com.murugan.dailycalm.Links
import java.text.SimpleDateFormat
import java.util.Locale

private val CardBackground = Color(0x1FFFFFFF)
private val NextBackground = Color(0x2EFFD54F)
private val Accent = Color(0xFFFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

/** Beyond this many dates the list stops being useful and starts being a wall. */
private const val MAX_DATES_SHOWN = 12

@Composable
fun FestivalDetailScreen(
    slug: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: FestivalRepository = FestivalRepository()
) {
    BackHandler(onBack = onBack)

    val state by produceState<Result<FestivalDetail>?>(initialValue = null, slug) {
        value = repository.getFestival(slug)
    }

    Box(modifier = modifier.fillMaxSize()) {
        val result = state
        when {
            result == null -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            result.isFailure -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = result.exceptionOrNull()?.message ?: "Could not load this festival",
                    color = Muted,
                    fontSize = 14.sp
                )
                Button(onClick = onBack) { Text("Go back") }
            }

            else -> FestivalDetailContent(detail = result.getOrThrow(), onBack = onBack)
        }
    }
}

@Composable
private fun FestivalDetailContent(detail: FestivalDetail, onBack: () -> Unit) {
    val context = LocalContext.current
    val dates = detail.upcomingDates.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("‹  திரும்பு", color = Accent, fontSize = 15.sp)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.nameTamil?.trim().orEmpty()
                        .ifBlank { detail.name?.trim().orEmpty() },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                )
                detail.name?.trim()?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = Muted, fontSize = 14.sp)
                }
                val short = detail.shortDescTamil?.trim()?.takeIf { it.isNotBlank() }
                    ?: detail.shortDesc?.trim()?.takeIf { it.isNotBlank() }
                short?.let {
                    Text(text = it, color = Faint, fontSize = 13.sp)
                }
            }
        }

        // The next occurrence gets its own emphasised card — on the day itself this is the
        // number people reopen the app to check.
        dates.firstOrNull()?.let { next ->
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = NextBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "அடுத்த நாள்  ·  Next", color = Faint, fontSize = 12.sp)
                        Text(
                            text = next.date?.toDisplayDate() ?: "—",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        next.window()?.let {
                            Text(text = it, color = Accent, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (dates.size > 1) {
            item {
                Text(
                    text = "வரும் நாட்கள்  ·  Upcoming dates",
                    color = Muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(dates.drop(1).take(MAX_DATES_SHOWN).size) { index ->
                val date = dates.drop(1).take(MAX_DATES_SHOWN)[index]
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = date.date?.toDisplayDate() ?: "—",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        date.window()?.let {
                            Text(text = it, color = Faint, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // festivalDetail arrives as 10-12 KB of HTML; Text() would render the tags literally.
        detail.detail?.htmlToPlainText()?.takeIf { it.isNotBlank() }?.let { body ->
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(14.dp),
                        text = body,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Purchase-muhurat masters have their own portal page; everything else falls back to the
        // festival's own entry.
        Links.portalUrlForFestival(detail.masterId, detail.slug)?.let { url ->
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { context.openUrl(url) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE7F4FF))
                ) {
                    Text("மேலும் படிக்க  ·  Read more")
                }
            }
        }
    }
}

/**
 * Turns the API's HTML into readable plain text.
 *
 * Block ends are converted to newlines first, because [Html.fromHtml] collapses them and the
 * Tamil prose would otherwise arrive as one unbroken paragraph. Uses the platform parser rather
 * than a regex so entities decode correctly.
 */
private fun String.htmlToPlainText(): String {
    val withBreaks = replace(Regex("(?i)</p\\s*>|<br\\s*/?>|</div\\s*>|</li\\s*>"), "\n")
    @Suppress("DEPRECATION")
    val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(withBreaks, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(withBreaks)
    }
    return spanned.toString()
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun FestivalDate.window(): String? {
    val start = startingTime?.trim().orEmpty()
    val end = endTime?.trim().orEmpty()
    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start – $end"
        start.isNotBlank() -> start
        end.isNotBlank() -> end
        else -> null
    }
}

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

private fun Context.openUrl(url: String) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
    }
}
