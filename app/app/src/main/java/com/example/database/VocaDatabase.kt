package com.example.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.models.UserSession
import com.example.models.ChatMessage
import com.example.models.AvatarPreferences
import com.example.models.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface VocaDao {
    // --- User Account Authentication API ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserAccount(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts ORDER BY createdAtTimestamp DESC")
    suspend fun getAllUserAccounts(): List<UserAccount>

    @Query("DELETE FROM user_accounts WHERE email = :email")
    suspend fun deleteUserAccount(email: String)

    // --- Session History API ---
    @Query("SELECT * FROM user_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<UserSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession)

    @Query("DELETE FROM user_sessions")
    suspend fun clearAllSessions()

    // --- AI Companion Coaching Chat API ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    // --- Virtual Companion Customization Settings API ---
    @Query("SELECT * FROM avatar_preferences WHERE id = 1 LIMIT 1")
    suspend fun getAvatarConfig(): AvatarPreferences?

    @Query("SELECT * FROM avatar_preferences WHERE id = 1 LIMIT 1")
    fun getAvatarConfigFlow(): Flow<AvatarPreferences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAvatarConfig(prefs: AvatarPreferences)
}

@Database(
    entities = [UserSession::class, ChatMessage::class, AvatarPreferences::class, UserAccount::class],
    version = 2,
    exportSchema = false
)
abstract class VocaDatabase : RoomDatabase() {
    abstract fun vocaDao(): VocaDao

    companion object {
        @Volatile
        private var INSTANCE: VocaDatabase? = null

        fun getDatabase(context: Context): VocaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VocaDatabase::class.java,
                    "voca_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
