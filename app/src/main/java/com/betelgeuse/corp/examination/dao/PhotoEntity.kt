package com.betelgeuse.corp.examination.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "cardId") val cardId: Int,
    @ColumnInfo(name = "comment") val comment: String?,
    @ColumnInfo(name = "imageUri") val imageUri: String?
)
