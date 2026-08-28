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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murugan.dailycalm.Links
import com.murugan.dailycalm.data.info.InfoNeedsApi

private val CardBackground = Color(0x1FFFFFFF)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

/**
 * The public channel. Not the studio.youtube.com address, which is the creator dashboard and
 * would send viewers to a page they cannot open.
 *
 * Left as an https link rather than a `vnd.youtube:` scheme: Android app-links route it to the
 * installed YouTube app anyway, where the viewer is signed in and the view counts toward watch
 * time, and it still works on a phone without the app.
 */
private val YOUTUBE_CHANNEL_URL = Links.YOUTUBE_CHANNEL_URL

private data class MoreLink(
    val tamil: String,
    val english: String,
    val detail: String,
    val url: String
)

@Composable
fun MoreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val links = buildList {
        add(
            MoreLink(
                tamil = "பண்டிகை விவரங்கள்",
                english = "All festivals on the web",
                detail = "informationneeds.com/festivals",
                url = "${InfoNeedsApi.PORTAL_URL}/festivals"
            )
        )
        add(
            MoreLink(
                tamil = "ராசி பலன்",
                english = "Horoscope",
                detail = "informationneeds.com/horoscope",
                url = "${InfoNeedsApi.PORTAL_URL}/horoscope"
            )
        )
        if (YOUTUBE_CHANNEL_URL.isNotBlank()) {
            add(
                MoreLink(
                    tamil = "முருகன் பாடல்கள்",
                    english = "Watch on YouTube",
                    detail = Links.YOUTUBE_HANDLE,
                    url = YOUTUBE_CHANNEL_URL
                )
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "மேலும் படிக்க",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp
                )
                Text(text = "Read more", color = Faint, fontSize = 12.sp)
            }
        }

        items(links.size) { index ->
            val link = links[index]
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.openUrl(link.url) },
                colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = link.tamil,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Text(text = link.english, color = Muted, fontSize = 13.sp)
                    Text(text = link.detail, color = Faint, fontSize = 11.sp)
                }
            }
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
