package com.betelgeuse.corp.examination.repository

import androidx.lifecycle.LiveData
import com.betelgeuse.corp.examination.dao.CardDao
import com.betelgeuse.corp.examination.dao.CardEntity
import com.betelgeuse.corp.examination.dao.PhotoEntity

class CardRepository(private val cardDao: CardDao) {
    val allCards: LiveData<List<CardEntity>> = cardDao.getAllCards()

    fun insertSync(card: CardEntity): Int {
        return cardDao.insertSync(card).toInt()
    }

    fun update(card: CardEntity) {
        cardDao.update(card)
    }

    fun delete(card: CardEntity) {
        cardDao.delete(card)
    }

    fun deletePhoto(photo: PhotoEntity): Int {
        return cardDao.deletePhoto(photo)
    }

    fun loadCards() {
        // Загрузка карточек из источника данных
        // Этот метод должен обновить LiveData, которая возвращается из getAllCards(),
        // чтобы Observer в ListWork мог обновить список карточек
    }
}