package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexaGreenBorder
import com.example.ui.theme.NexaGreenDark
import com.example.ui.theme.NexaNeonGreen
import com.example.ui.theme.NexaSurface
import com.example.ui.theme.NexaSurfaceVariant
import com.example.ui.theme.NexaTextMuted
import com.example.ui.theme.NexaTextWhite

@Composable
fun EmojiKeyboardPanel(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            "😀 Smileys" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😎", "🥳", "🤔", "🫡", "🤝"),
            "💚 Vibes" to listOf("💚", "❤️", "💖", "✨", "🌟", "🔥", "⚡", "🎯", "💎", "🚀", "🪐", "💡", "🛡️", "🌌", "🎉", "👑"),
            "💻 Tech" to listOf("💻", "📱", "🤖", "🧠", "⚙️", "🔒", "📡", "💾", "🖥️", "📊", "🔬", "🛰️", "🔌", "🌐", "⌨️", "🕹️"),
            "👍 Gestures" to listOf("👍", "👎", "👏", "🙌", "🤝", "✌️", "🤞", "🤙", "✍️", "👊", "🫡", "🙏", "💪", "🫶")
        )
    }

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val currentEmojis = categories[selectedCategoryIndex].second

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NexaSurfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, NexaGreenBorder, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        // Categories row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEachIndexed { index, (name, _) ->
                val isSelected = index == selectedCategoryIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NexaGreenDark else NexaSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NexaNeonGreen else NexaGreenBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategoryIndex = index }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else NexaTextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}
