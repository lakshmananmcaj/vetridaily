package com.murugan.dailycalm.ui.temples

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.murugan.dailycalm.data.info.Temple

private val CardBackground = Color(0x1FFFFFFF)
private val AbodeBackground = Color(0x2EFFD54F)
private val Muted = Color(0xB3FFFFFF)
private val Faint = Color(0x80FFFFFF)

@Composable
fun TemplesScreen(
    modifier: Modifier = Modifier,
    viewModel: TemplesViewModel = viewModel(factory = TemplesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val showAll by viewModel.showAllTemples.collectAsState()

    // Kept here rather than in the ViewModel so the tab's own back stack is one line.
    // rememberSaveable keeps the open temple across rotation and process death.
    var selectedSlug by rememberSaveable { mutableStateOf<String?>(null) }

    selectedSlug?.let { slug ->
        TempleDetailScreen(
            slug = slug,
            onBack = { selectedSlug = null },
            modifier = modifier
        )
        return
    }

    val onTempleClick: (String) -> Unit = { slug -> selectedSlug = slug }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is TemplesUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            is TemplesUiState.Error -> Column(
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

            is TemplesUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "அறுபடை வீடு",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "The six abodes of Murugan",
                            color = Faint,
                            fontSize = 12.sp
                        )
                    }
                }

                itemsIndexed(state.arupadaiVeedu) { index, temple ->
                    AbodeRow(
                        position = index + 1,
                        temple = temple,
                        onClick = { temple.slug?.let(onTempleClick) }
                    )
                }

                if (state.otherTemples.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAllTemples() }
                                .padding(top = 16.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "மற்ற கோவில்கள்",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Other temples · ${state.otherTemples.size}",
                                    color = Faint,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = if (showAll) "Hide" else "Show",
                                color = Muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (showAll) {
                        items(state.otherTemples, key = { "temple-${it.templeId}" }) { temple ->
                            TempleRow(
                                temple = temple,
                                onClick = { temple.slug?.let(onTempleClick) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One of the six, numbered in traditional sequence. */
@Composable
private fun AbodeRow(position: Int, temple: Temple, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (temple.slug != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.elevatedCardColors(containerColor = AbodeBackground),
        shape = RoundedCornerShape(14.dp)
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
                color = Color(0x33000000),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$position",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
            TempleText(temple = temple, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TempleRow(temple: Temple, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (temple.slug != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            TempleText(temple = temple)
        }
    }
}

@Composable
private fun TempleText(temple: Temple, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val tamil = temple.nameTamil?.trim().orEmpty()
        val english = temple.name?.trim().orEmpty()

        Text(
            text = tamil.ifBlank { english },
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
        if (english.isNotBlank() && tamil.isNotBlank()) {
            Text(text = english, color = Muted, fontSize = 13.sp)
        }

        val place = listOfNotNull(
            temple.city?.trim()?.takeIf { it.isNotBlank() },
            temple.deityTamil?.trim()?.takeIf { it.isNotBlank() }
                ?: temple.deity?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        if (place.isNotBlank()) {
            Text(text = place, color = Faint, fontSize = 12.sp)
        }
    }
}
