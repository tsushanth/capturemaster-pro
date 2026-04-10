package com.factory.capturemasterpro.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val duration: Long = 0L,
    val fileSize: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
    val bitRate: Int = 8_000_000,
    val hasAudio: Boolean = true,
    val hasMicrophone: Boolean = false,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val tags: String = ""
)
