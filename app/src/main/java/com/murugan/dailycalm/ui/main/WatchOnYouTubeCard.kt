package com.murugan.dailycalm.ui.main

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
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

private val CardBackground = Color(0x1FFFFFFF)
private val PlayRed = Color(0xFFE33122)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

/**
 * Sends listeners to the channel, placed just after the audio controls.
 *
 * That moment is the best handoff in the app: the listener has finished the day's practice, is
 * unhurried and has nothing else to do here. One card and one destination — not a feed, which
 * would compete with the affirmation for attention.
 */
@Composable
fun WatchOnYouTubeCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { context.openChannel() },
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PlayRed,
                modifier = Modifier.size(38.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "முருகன் பாடல்கள் கேட்க",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = "Devotional songs on YouTube",
                    color = Muted,
                    fontSize = 13.sp
                )
                Text(text = Links.YOUTUBE_HANDLE, color = Faint, fontSize = 11.sp)
            }
        }
    }
}

/**
 * Opens the channel with a plain https link. Android app-links hand it to the installed YouTube
 * app, where the viewer is signed in and the view counts toward watch time, and it still works in
 * a browser on a phone without the app.
 */
private fun Context.openChannel() {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(Links.YOUTUBE_CHANNEL_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app available to open this", Toast.LENGTH_SHORT).show()
    }
}
