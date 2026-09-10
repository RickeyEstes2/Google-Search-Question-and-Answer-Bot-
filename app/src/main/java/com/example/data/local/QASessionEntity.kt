package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qa_sessions")
data class QASessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val answer: String,
    val modelUsed: String,
    val mode: String, // "search_grounded", "high_thinking", "fast"
    val webQueries: String = "", // comma-separated or json
    val sourcesJson: String = "", // serialized list of {title, url}
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
