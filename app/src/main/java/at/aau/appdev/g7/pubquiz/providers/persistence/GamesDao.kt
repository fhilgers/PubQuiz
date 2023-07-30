package at.aau.appdev.g7.pubquiz.providers.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GamesDao {
    @Query("select * from GameSetting")
    fun findGameSettings(): List<GameSetting>

    @Insert
    fun createGameSetting(model: GameSetting)

    @Update
    fun updateGameSetting(model: GameSetting)

    @Delete
    fun deleteGameSetting(model: GameSetting)
}
