package com.murugan.dailycalm.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The four tabs.
 *
 * [TODAY] is the practice and stays first — it is the reason this is not a calendar app. The other
 * three are utility and are always open, which is what keeps the app worth opening after the daily
 * journey completes.
 */
enum class VetriTab(val tamil: String, val english: String) {
    TODAY("இன்று", "Today"),
    TEMPLES("கோவில்", "Temples"),
    FESTIVALS("பண்டிகை", "Festivals"),
    MORE("மேலும்", "More")
}

private val BarBackground = Color(0xFF06121C)
private val Selected = Color(0xFFFFD54F)
private val Unselected = Color(0x99FFFFFF)

@Composable
fun VetriBottomBar(
    selected: VetriTab,
    onSelect: (VetriTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BarBackground,
        tonalElevation = 0.dp
    ) {
        VetriTab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Selected,
                    unselectedIconColor = Unselected,
                    indicatorColor = Color(0x1FFFD54F)
                ),
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.tamil,
                            color = if (isSelected) Selected else Unselected,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = tab.english,
                            color = if (isSelected) Selected else Unselected,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            )
        }
    }
}
