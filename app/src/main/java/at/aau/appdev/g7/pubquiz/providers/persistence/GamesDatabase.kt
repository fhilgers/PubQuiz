package at.aau.appdev.g7.pubquiz.providers.persistence

import androidx.room.BuiltInTypeConverters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [GameSetting::class], version = 1)
//@TypeConverters(Converters::class)
//@BuiltInTypeConverters(uuid = )
abstract class GamesDatabase: RoomDatabase() {
    abstract fun dao(): GamesDao
}