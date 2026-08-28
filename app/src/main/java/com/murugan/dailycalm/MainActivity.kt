package com.murugan.dailycalm

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.murugan.dailycalm.reminder.ReminderPreferences
import com.murugan.dailycalm.reminder.ReminderScheduler
import com.murugan.dailycalm.share.ShareUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import com.murugan.dailycalm.ui.festivals.FestivalsScreen
import com.murugan.dailycalm.ui.main.MainUiState
import com.murugan.dailycalm.ui.main.MainViewModel
import com.murugan.dailycalm.ui.main.TodayHighlights
import com.murugan.dailycalm.ui.main.WatchOnYouTubeCard
import com.murugan.dailycalm.ui.more.MoreScreen
import com.murugan.dailycalm.ui.nav.VetriBottomBar
import com.murugan.dailycalm.ui.nav.VetriTab
import com.murugan.dailycalm.ui.temples.TemplesScreen
import com.murugan.dailycalm.ui.theme.DailyCalmTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioUrl: String? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        currentAudioUrl = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyCalmTheme {
                val context = LocalContext.current
                val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
                val uiState by mainViewModel.uiState.collectAsState()
                val selectedDay by mainViewModel.selectedDay.collectAsState()
                val maxUnlockedDay by mainViewModel.maxUnlockedDay.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val reminderPrefs = remember(context) { ReminderPreferences(context) }

                var isPlaying by remember { mutableStateOf(false) }
                var isLoading by remember { mutableStateOf(false) }
                var reminderEnabled by remember { mutableStateOf(reminderPrefs.isEnabled()) }
                var reminderHour by remember { mutableStateOf(reminderPrefs.getHour()) }
                var reminderMinute by remember { mutableStateOf(reminderPrefs.getMinute()) }
                var festivalRemindersEnabled by remember {
                    mutableStateOf(reminderPrefs.isFestivalRemindersEnabled())
                }

                val audioUrl = if (uiState is MainUiState.Success) {
                    (uiState as MainUiState.Success).content.audio_url
                } else {
                    ""
                }
                val canGoNext = selectedDay < maxUnlockedDay
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        reminderEnabled = true
                        reminderPrefs.setEnabled(true)
                        ReminderScheduler.schedule(context, reminderHour, reminderMinute)
                    } else {
                        reminderEnabled = false
                        reminderPrefs.setEnabled(false)
                        scope.launch {
                            snackbarHostState.showSnackbar("Notification permission is required for reminders.")
                        }
                    }
                }

                LaunchedEffect(selectedDay) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    currentAudioUrl = null
                    isPlaying = false
                    isLoading = false
                }

                LaunchedEffect(reminderEnabled, reminderHour, reminderMinute) {
                    if (reminderEnabled) {
                        ReminderScheduler.schedule(context, reminderHour, reminderMinute)
                    } else {
                        ReminderScheduler.cancel(context)
                    }
                }

                LaunchedEffect(festivalRemindersEnabled) {
                    if (festivalRemindersEnabled) {
                        ReminderScheduler.scheduleFestivalReminders(context)
                    } else {
                        ReminderScheduler.cancelFestivalReminders(context)
                    }
                }

                var selectedTab by remember { mutableStateOf(VetriTab.TODAY) }

                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        VetriBottomBar(
                            selected = selectedTab,
                            onSelect = { selectedTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF071521),
                                        Color(0xFF0D2535),
                                        Color(0xFF13344A)
                                    )
                                )
                            )
                    ) {
                      when (selectedTab) {
                        VetriTab.TEMPLES -> TemplesScreen(
                            modifier = Modifier.padding(innerPadding)
                        )

                        VetriTab.FESTIVALS -> FestivalsScreen(
                            modifier = Modifier.padding(innerPadding)
                        )

                        VetriTab.MORE -> MoreScreen(
                            modifier = Modifier.padding(innerPadding)
                        )

                        VetriTab.TODAY ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0x1FFFFFFF)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.vetri_daily_logo),
                                        contentDescription = "VetriDaily Logo",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(Color.White, shape = RoundedCornerShape(14.dp))
                                            .padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "VetriDaily",
                                            color = Color(0xFFF7FBFF),
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Text(
                                            text = "Victory through devotion. Every single day.",
                                            color = Color(0xFFC8DFEE),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0x1FFFFFFF)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { mainViewModel.loadPreviousDay() },
                                        enabled = selectedDay > 1,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFE7F4FF)
                                        )
                                    ) {
                                        Text("Previous")
                                    }

                                    Text(
                                        text = "Day $selectedDay",
                                        color = Color(0xFFF3FAFF),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Button(
                                        onClick = {
                                            if (canGoNext) {
                                                mainViewModel.loadNextDay()
                                            } else {
                                                val lockMessage = if (uiState is MainUiState.JourneyComplete) {
                                                    "You have reached the latest published day."
                                                } else {
                                                    "This day unlocks tomorrow."
                                                }
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(lockMessage)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF4B73E),
                                            contentColor = Color(0xFF231A08)
                                        )
                                    ) {
                                        Text("Next")
                                    }
                                }
                            }

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0x1FFFFFFF)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Daily Reminder",
                                                color = Color(0xFFF2FAFF),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Time: ${formatTime(reminderHour, reminderMinute)}",
                                                color = Color(0xFFD1E6F3),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Switch(
                                            checked = reminderEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                        ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.POST_NOTIFICATIONS
                                                        ) != PackageManager.PERMISSION_GRANTED
                                                    if (needsPermission) {
                                                        notificationPermissionLauncher.launch(
                                                            Manifest.permission.POST_NOTIFICATIONS
                                                        )
                                                    } else {
                                                        reminderEnabled = true
                                                        reminderPrefs.setEnabled(true)
                                                    }
                                                } else {
                                                    reminderEnabled = false
                                                    reminderPrefs.setEnabled(false)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF1D170A),
                                                checkedTrackColor = Color(0xFFF4B73E),
                                                uncheckedThumbColor = Color(0xFFD3DCE3),
                                                uncheckedTrackColor = Color(0xFF5A6F7E)
                                            )
                                        )
                                    }

                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    reminderHour = hourOfDay
                                                    reminderMinute = minute
                                                    reminderPrefs.setTime(hourOfDay, minute)
                                                },
                                                reminderHour,
                                                reminderMinute,
                                                false
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFE7F4FF)
                                        )
                                    ) {
                                        Text("Set Reminder Time")
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "பண்டிகை · சஷ்டி நினைவூட்டல்",
                                                color = Color(0xFFF2FAFF),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Evening alert the day before",
                                                color = Color(0xFFD1E6F3),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Switch(
                                            checked = festivalRemindersEnabled,
                                            onCheckedChange = { checked ->
                                                festivalRemindersEnabled = checked
                                                reminderPrefs.setFestivalRemindersEnabled(checked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF1D170A),
                                                checkedTrackColor = Color(0xFFF4B73E),
                                                uncheckedThumbColor = Color(0xFFD3DCE3),
                                                uncheckedTrackColor = Color(0xFF5A6F7E)
                                            )
                                        )
                                    }
                                }
                            }

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0xEAF8FBFF)
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp)
                                ) {
                                    when (val state = uiState) {
                                        MainUiState.Loading -> {
                                            Text(
                                                text = "Loading...",
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Fetching your daily calm content.",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }

                                        is MainUiState.Success -> {
                                            Text(
                                                text = state.content.title,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = state.content.body,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }

                                        is MainUiState.JourneyComplete -> {
                                            Text(
                                                text = "Journey complete",
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "You have finished all ${state.lastDay} days of VetriDaily. New days appear here as they are published.",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { mainViewModel.loadPreviousDay() }
                                            ) {
                                                Text("Revisit Day ${state.lastDay}")
                                            }
                                        }

                                        is MainUiState.Error -> {
                                            Text(
                                                text = "Unable to load content",
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { mainViewModel.retry() }
                                            ) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState is MainUiState.Success && audioUrl.isNotEmpty() && !isLoading,
                                onClick = {
                                    if (!isPlaying) {
                                        try {
                                            if (mediaPlayer != null && currentAudioUrl == audioUrl) {
                                                mediaPlayer?.start()
                                                isPlaying = true
                                                return@Button
                                            }

                                            isLoading = true
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            currentAudioUrl = audioUrl

                                            mediaPlayer = MediaPlayer().apply {
                                                setAudioAttributes(
                                                    AudioAttributes.Builder()
                                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                                        .build()
                                                )
                                                setDataSource(audioUrl)

                                                setOnPreparedListener {
                                                    Log.d("DailyCalm", "MediaPlayer prepared")
                                                    isLoading = false
                                                    it.start()
                                                    isPlaying = true
                                                }

                                                setOnCompletionListener {
                                                    Log.d("DailyCalm", "MediaPlayer completed")
                                                    it.seekTo(0)
                                                    isPlaying = false
                                                }

                                                setOnErrorListener { _, what, extra ->
                                                    Log.e("DailyCalm", "MediaPlayer Error: what=$what extra=$extra")
                                                    isLoading = false
                                                    isPlaying = false
                                                    false
                                                }

                                                prepareAsync()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("DailyCalm", "Error setting up MediaPlayer", e)
                                            isLoading = false
                                        }
                                    } else {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF4B73E),
                                    contentColor = Color(0xFF231A08)
                                )
                            ) {
                                Text(
                                    if (isLoading) "Loading audio..."
                                    else if (isPlaying) "Pause Audio"
                                    else "Play Audio"
                                )
                            }

                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState is MainUiState.Success &&
                                    !isLoading &&
                                    mediaPlayer != null &&
                                    currentAudioUrl == audioUrl,
                                onClick = {
                                    mediaPlayer?.seekTo(0)
                                    mediaPlayer?.start()
                                    isPlaying = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFE7F4FF)
                                )
                            ) {
                                Text("Replay")
                            }

                            // Sits below the day's practice, never above it — Sashti and the next
                            // festival are the reason to come back, not the reason to be here.
                            TodayHighlights()

                            // Handoff to the channel, right after the audio has been heard.
                            WatchOnYouTubeCard()

                            if (uiState is MainUiState.Success) {
                                val content = (uiState as MainUiState.Success).content
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = Color(0x1FFFFFFF)
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Share & spread the blessing",
                                            color = Color(0xFFF2FAFF),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                ShareUtils.shareToWhatsApp(
                                                    context, content.title, content.body
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF25D366),
                                                contentColor = Color(0xFF06310F)
                                            )
                                        ) {
                                            Text("Share to WhatsApp / Status")
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                ShareUtils.shareCard(
                                                    context, content.title, content.body
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFE7F4FF)
                                            )
                                        ) {
                                            Text("Share as Image")
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { ShareUtils.sendToFriend(context, content.title) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFFE7F4FF)
                                            )
                                        ) {
                                            Text("Send App to a Friend")
                                        }
                                    }
                                }
                            }
                        }
                      }
                    }
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val normalizedHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", normalizedHour, minute, amPm)
}
