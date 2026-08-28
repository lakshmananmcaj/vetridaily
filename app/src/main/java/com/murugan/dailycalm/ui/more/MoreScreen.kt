package com.murugan.dailycalm.ui.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murugan.dailycalm.Links
import com.murugan.dailycalm.data.info.InfoNeedsApi

private val CardBackground = Color(0x1FFFFFFF)
private val YouTubeBackground = Color(0x2EE33122)
private val Accent = Color(0xFFFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

private data class MoreLink(
    val tamil: String,
    val english: String,
    val url: String,
    val highlighted: Boolean = false
)

private data class MoreSection(
    val tamil: String,
    val english: String,
    val links: List<MoreLink>
)

private fun portal(path: String) = "${InfoNeedsApi.PORTAL_URL}$path"

/**
 * Outbound links to the portal and the channel.
 *
 * Every portal tap is a page view on informationneeds.com, which carries ads — so this tab earns
 * without any billing integration in the app. Grouped rather than listed flat: ten destinations in
 * one column is a wall, and the auspicious-day pages are a different errand from the devotional ones.
 */
private val SECTIONS = listOf(
    MoreSection(
        tamil = "ஆன்மீகம்",
        english = "Spiritual",
        links = listOf(
            MoreLink("விரத நாட்கள்", "Vratham days", portal("/special-dates")),
            MoreLink("பண்டிகைகள்", "Festivals", portal("/festivals")),
            MoreLink("இன்றைய பஞ்சாங்கம்", "Today's panchangam", portal("/panchangam"))
        )
    ),
    MoreSection(
        tamil = "ஜோதிடம்",
        english = "Astrology",
        links = listOf(
            MoreLink("ராசி பலன்", "Horoscope", portal("/horoscope")),
            MoreLink("சந்திராஷ்டமம்", "Chandrashtama", portal("/horoscope/chandrashtama"))
        )
    ),
    MoreSection(
        tamil = "நல்ல நாள்",
        english = "Auspicious days",
        links = listOf(
            MoreLink(
                "சொத்து வாங்க",
                "Property purchase",
                portal("/auspicious-dates/property-purchase")
            ),
            MoreLink(
                "வாகனம் வாங்க",
                "Vehicle purchase",
                portal("/auspicious-dates/vehicle-purchase")
            ),
            MoreLink(
                "தங்கம் வாங்க",
                "Gold purchase",
                portal("/auspicious-dates/gold-purchase")
            ),
            MoreLink(
                "தொழில் தொடங்க",
                "Starting a business",
                portal("/auspicious-dates/business-start")
            )
        )
    ),
    MoreSection(
        tamil = "யூடியூப்",
        english = "YouTube",
        links = listOf(
            MoreLink(
                tamil = "முருகன் பாடல்கள்",
                english = Links.YOUTUBE_HANDLE,
                url = Links.YOUTUBE_CHANNEL_URL,
                highlighted = true
            )
        )
    )
)

@Composable
fun MoreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SECTIONS.forEach { section ->
            item(key = "header-${section.english}") {
                Column(
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = section.tamil,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                    Text(text = section.english, color = Faint, fontSize = 12.sp)
                }
            }

            items(section.links, key = { it.url }) { link ->
                LinkRow(link = link, onClick = { context.openUrl(link.url) })
            }
        }
    }
}

@Composable
private fun LinkRow(link: MoreLink, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (link.highlighted) YouTubeBackground else CardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = link.tamil,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(text = link.english, color = Muted, fontSize = 13.sp)
            }
            Text(text = "↗", color = Accent, fontSize = 16.sp)
        }
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
