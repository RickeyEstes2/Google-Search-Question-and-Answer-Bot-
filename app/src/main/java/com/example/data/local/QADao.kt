package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QADao {
    @Query("SELECT * FROM qa_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<QASessionEntity>>

    @Query("SELECT * FROM qa_sessions WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteSessions(): Flow<List<QASessionEntity>>

    @Query("SELECT * FROM qa_sessions WHERE question LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSessions(query: String): Flow<List<QASessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: QASessionEntity): Long

    @Update
    suspend fun updateSession(session: QASessionEntity)

    @Delete
    suspend fun deleteSession(session: QASessionEntity)

    @Query("DELETE FROM qa_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM qa_sessions")
    suspend fun clearAll()
}
