package at.aau.appdev.g7.pubquiz.providers.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameSetting::class], version = 1)
abstract class GamesDatabase: RoomDatabase() {
    abstract fun dao(): GamesDao
}