package at.aau.appdev.g7.pubquiz.providers.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import java.util.UUID

@Dao
interface GamesDao {
    @Query("select * from GameSetting")
    suspend fun findGameSettings(): List<GameSetting>

    @Upsert
    suspend fun saveGameSetting(model: GameSetting)

    @Query("delete from GameSetting where id = :id")
    suspend fun deleteGameSetting(id: String)
}
