package com.betelgeuse.corp.examination.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE cardId = :cardId")
    @Transaction
    fun getPhotosByCardId(cardId: Int): LiveData<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(photo: PhotoEntity)

    @Update
    fun update(photo: PhotoEntity)

    @Delete
    fun delete(photo: PhotoEntity)
}