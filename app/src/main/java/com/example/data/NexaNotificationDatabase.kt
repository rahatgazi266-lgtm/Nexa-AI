package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "nexa_notifications", indices = [Index(value = ["dedupeKey"], unique = true)])
data class NexaNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val dedupeKey: String
)

@Dao
interface NexaNotificationDao {

    @Query("SELECT * FROM nexa_notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NexaNotification>>

    @Query("SELECT COUNT(*) FROM nexa_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NexaNotification): Long

    @Query("UPDATE nexa_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE nexa_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM nexa_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM nexa_notifications")
    suspend fun deleteAll()
}

@Database(
    entities = [NexaNotification::class],
    version = 1,
    exportSchema = false
)
abstract class NexaNotificationDatabase : RoomDatabase() {

    abstract fun notificationDao(): NexaNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NexaNotificationDatabase? = null

        fun getDatabase(context: Context): NexaNotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NexaNotificationDatabase::class.java,
                    "nexa_notifications.db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
