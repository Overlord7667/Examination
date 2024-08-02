package com.betelgeuse.corp.examination.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.betelgeuse.corp.examination.dao.CardDao
import com.betelgeuse.corp.examination.dao.CardDatabase
import com.betelgeuse.corp.examination.dao.CardEntity
import com.betelgeuse.corp.examination.dao.PhotoEntity
import com.betelgeuse.corp.examination.repository.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CardRepository
    val allCards: LiveData<List<CardEntity>>

    init {
        val cardDao = CardDatabase.getDatabase(application).cardDao()
        repository = CardRepository(cardDao)
        allCards = repository.allCards
    }

    fun insertSync(card: CardEntity): Int {
        return repository.insertSync(card)
    }

    fun update(card: CardEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(card)
    }

    fun delete(card: CardEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(card)
    }

    fun deletePhoto(photo: PhotoEntity): Int {
        return repository.deletePhoto(photo)
    }

    fun loadCards() {
        repository.loadCards()
    }
}