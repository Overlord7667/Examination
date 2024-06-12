package com.betelgeuse.corp.examination.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CardDao {
    @Query("SELECT * FROM cards")
    fun getAllCards(): LiveData<List<CardEntity>>

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insert(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(card: CardEntity): Long

    @Update
    fun update(card: CardEntity)

    @Delete
    fun delete(card: CardEntity)
}