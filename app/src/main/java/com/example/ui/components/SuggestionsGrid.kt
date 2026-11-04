package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexaGreenBorder
import com.example.ui.theme.NexaSurface
import com.example.ui.theme.NexaTextMuted
import com.example.ui.theme.NexaTextWhite

data class SuggestionItem(
    val title: String,
    val subtitle: String,
    val prompt: String
)

@Composable
fun SuggestionsGrid(
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        SuggestionItem("💡 Generate Ideas", "Plan something new", "Help me come up with a new idea"),
        SuggestionItem("📚 Learn Something", "Make complex topics easier", "Explain a topic to me in simple terms"),
        SuggestionItem("📋 Planning", "Organize my tasks", "Create a task plan for me"),
        SuggestionItem("✍️ Writing", "Create and improve text", "Help me create a writing draft")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestionCard(
                item = items[0],
                onClick = { onSelect(items[0].prompt) },
                modifier = Modifier.weight(1f)
            )
            SuggestionCard(
                item = items[1],
                onClick = { onSelect(items[1].prompt) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestionCard(
                item = items[2],
                onClick = { onSelect(items[2].prompt) },
                modifier = Modifier.weight(1f)
            )
            SuggestionCard(
                item = items[3],
                onClick = { onSelect(items[3].prompt) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    item: SuggestionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NexaSurface)
            .border(1.dp, NexaGreenBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("suggestion_card_${item.title.take(6)}")
    ) {
        Text(
            text = item.title,
            color = NexaTextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.subtitle,
            color = NexaTextMuted,
            fontSize = 11.sp
        )
    }
}
