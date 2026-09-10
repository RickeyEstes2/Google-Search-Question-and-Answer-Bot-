package com.example.data.repository

import com.example.data.local.QADao
import com.example.data.local.QASessionEntity
import kotlinx.coroutines.flow.Flow

class QARepository(private val qaDao: QADao) {
    val allSessions: Flow<List<QASessionEntity>> = qaDao.getAllSessions()
    val favoriteSessions: Flow<List<QASessionEntity>> = qaDao.getFavoriteSessions()

    fun searchSessions(query: String): Flow<List<QASessionEntity>> = qaDao.searchSessions(query)

    suspend fun saveSession(session: QASessionEntity): Long = qaDao.insertSession(session)

    suspend fun toggleFavorite(session: QASessionEntity) {
        qaDao.updateSession(session.copy(isFavorite = !session.isFavorite))
    }

    suspend fun deleteSession(session: QASessionEntity) = qaDao.deleteSession(session)

    suspend fun deleteSessionById(id: Long) = qaDao.deleteSessionById(id)

    suspend fun clearHistory() = qaDao.clearAll()
}
