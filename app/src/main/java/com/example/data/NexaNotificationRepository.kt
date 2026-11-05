package com.example.data

import kotlinx.coroutines.flow.Flow

class NexaNotificationRepository(
    private val dao: NexaNotificationDao
) {
    val notifications: Flow<List<NexaNotification>> =
        dao.observeAll()

    val unreadCount: Flow<Int> =
        dao.observeUnreadCount()

    suspend fun add(
        title: String,
        message: String,
        category: String,
        dedupeKey: String
    ): Boolean {
        val insertedId = dao.insert(
            NexaNotification(
                title = title,
                message = message,
                category = category,
                dedupeKey = dedupeKey
            )
        )
        return insertedId != -1L
    }

    suspend fun markAsRead(id: Long) {
        dao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        dao.markAllAsRead()
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
