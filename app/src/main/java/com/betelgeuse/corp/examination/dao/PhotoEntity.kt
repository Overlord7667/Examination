package com.betelgeuse.corp.examination.dao

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "cardId") val cardId: Int,
    @ColumnInfo(name = "comment") val comment: String?,
    @ColumnInfo(name = "imageUri") val imageUri: String?,
    @ColumnInfo(name = "imageUri2") val imageUri2: String?,
    @ColumnInfo(name = "buildSpText") val buildSpText: String?,
    @ColumnInfo(name = "buildRoom") val buildRoom: String?,
    @ColumnInfo(name = "buildObject") val buildObject: String?,
    @ColumnInfo(name = "typeRoom") val typeRoom: String?,
    @ColumnInfo(name = "typeWork") val typeWork: String?
)
