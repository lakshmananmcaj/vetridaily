package com.murugan.dailycalm.ui.temples

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murugan.dailycalm.data.info.InfoNeedsApi
import com.murugan.dailycalm.data.info.Temple
import com.murugan.dailycalm.data.info.TempleRepository

private val CardBackground = Color(0x1FFFFFFF)
private val Accent = Color(0xFFFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

@Composable
fun TempleDetailScreen(
    slug: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: TempleRepository = TempleRepository()
) {
    BackHandler(onBack = onBack)

    val state by produceState<Result<Temple>?>(initialValue = null, slug) {
        value = repository.getTemple(slug)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state == null -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            state?.isFailure == true -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state?.exceptionOrNull()?.message ?: "Could not load this temple",
                    color = Muted,
                    fontSize = 14.sp
                )
                Button(onClick = onBack) { Text("Go back") }
            }

            else -> TempleDetailContent(
                temple = state!!.getOrThrow(),
                onBack = onBack
            )
        }
    }
}

@Composable
private fun TempleDetailContent(temple: Temple, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    text = temple.nameTamil?.trim().orEmpty()
                        .ifBlank { temple.name?.trim().orEmpty() },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                )
                temple.name?.trim()?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = Muted, fontSize = 14.sp)
                }
                val deity = temple.deityTamil?.trim()?.takeIf { it.isNotBlank() }
                    ?: temple.deity?.trim()?.takeIf { it.isNotBlank() }
                deity?.let {
                    Text(text = it, color = Accent, fontSize = 14.sp)
                }
            }
        }

        // Significance in Tamil first — the audience reads Tamil before English.
        val significance = temple.significanceTamil?.trim()?.takeIf { it.isNotBlank() }
            ?: temple.significance?.trim()?.takeIf { it.isNotBlank() }
            ?: temple.description?.trim()?.takeIf { it.isNotBlank() }

        if (significance != null) {
            item {
                InfoCard {
                    Text(
                        text = significance,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        val timings = listOfNotNull(
            temple.openingTime?.trim()?.takeIf { it.isNotBlank() },
            temple.closingTime?.trim()?.takeIf { it.isNotBlank() }
        )
        if (timings.isNotEmpty()) {
            item {
                InfoCard {
                    LabelledRow("நேரம்", "Timings", timings.joinToString(" – "))
                }
            }
        }

        item {
            InfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOfNotNull(
                        temple.location?.trim()?.takeIf { it.isNotBlank() }
                            ?.let { Triple("இடம்", "Location", it) },
                        listOfNotNull(
                            temple.city?.trim()?.takeIf { it.isNotBlank() },
                            temple.district?.trim()?.takeIf { it.isNotBlank() },
                            temple.state?.trim()?.takeIf { it.isNotBlank() }
                        ).takeIf { it.isNotEmpty() }
                            ?.let { Triple("ஊர்", "Place", it.joinToString(", ")) },
                        temple.nearestRailway?.trim()?.takeIf { it.isNotBlank() }
                            ?.let { Triple("ரயில் நிலையம்", "Nearest railway", it) },
                        temple.nearestAirport?.trim()?.takeIf { it.isNotBlank() }
                            ?.let { Triple("விமான நிலையம்", "Nearest airport", it) }
                    ).forEach { (tamil, english, value) ->
                        LabelledRow(tamil, english, value)
                    }
                }
            }
        }

        val lat = temple.latitude
        val lng = temple.longitude
        if (lat != null && lng != null) {
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.openDirections(
                            lat = lat,
                            lng = lng,
                            label = temple.name?.trim().orEmpty()
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF06310F)
                    )
                ) {
                    Text("வழி காட்டு  ·  Get Directions")
                }
            }
        }

        // Sends the reader to the portal, which is where the web-side ads live.
        temple.slug?.let { slug ->
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { context.openUrl("${InfoNeedsApi.PORTAL_URL}/temples/$slug") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE7F4FF))
                ) {
                    Text("மேலும் படிக்க  ·  Read more")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun LabelledRow(tamil: String, english: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(0.4f)) {
            Text(text = tamil, color = Muted, fontSize = 13.sp)
            Text(text = english, color = Faint, fontSize = 11.sp)
        }
        Text(
            modifier = Modifier.weight(0.6f),
            text = value,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

/**
 * Opens the coordinates in whichever map app the phone has.
 * `geo:` is the standard Android intent; if nothing handles it — some devices ship without a
 * map app — fall back to Google Maps in the browser rather than failing silently.
 */
private fun Context.openDirections(lat: Double, lng: Double, label: String) {
    val encoded = Uri.encode(label)
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($encoded)"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        openUrl("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app available to open this", Toast.LENGTH_SHORT).show()
    }
}
