package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.example.data.NexaNotification
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterDialog(
    notifications: List<NexaNotification>,
    onDismiss: () -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val unreadCount = notifications.count { !it.isRead }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NexaSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NexaGreenBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔔",
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Notification Center",
                        color = NexaTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NexaTextMuted
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "${notifications.size} notifications • $unreadCount unread",
                    color = NexaTextMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onMarkAllRead,
                        enabled = unreadCount > 0
                    ) {
                        Text("Mark All Read", color = NexaNeonGreen, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = onClearAll,
                        enabled = notifications.isNotEmpty()
                    ) {
                        Text("Clear All", color = NexaRed, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = NexaGreenBorder)

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No notifications yet",
                            color = NexaTextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationItem(
                                item = item,
                                onMarkRead = { onMarkRead(item.id) },
                                onDelete = { onDelete(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    item: NexaNotification,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.isRead) NexaCardBg else NexaSurfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .clickable { onMarkRead() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                color = NexaTextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                item.message,
                color = NexaTextMuted,
                fontSize = 12.sp
            )
            Text(
                item.category.uppercase() + " • " +
                    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        .format(Date(item.createdAt)),
                color = NexaNeonGreen,
                fontSize = 10.sp
            )
            if (!item.isRead) {
                Text("UNREAD", color = NexaNeonGreen, fontSize = 10.sp)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete notification",
                tint = NexaRed
            )
        }
    }
}
