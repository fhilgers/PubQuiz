package at.aau.appdev.g7.pubquiz.providers.persistence

import android.content.Context
import androidx.room.Room
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider

class PersistenceDataProvider(applicationContext: Context) : DataProvider {
    private val db = Room.databaseBuilder(
            applicationContext, GamesDatabase::class.java, "games")
            .build()

}