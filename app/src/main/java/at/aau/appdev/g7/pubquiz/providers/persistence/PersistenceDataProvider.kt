package at.aau.appdev.g7.pubquiz.providers.persistence

import android.content.Context
import android.util.Log
import androidx.room.Room
import at.aau.appdev.g7.pubquiz.domain.GameConfiguration
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PersistenceDataProvider(applicationContext: Context) : DataProvider {
    companion object {
        const val TAG = "PersistenceDataProvider"
    }

    private val db = Room.databaseBuilder(
            applicationContext, GamesDatabase::class.java, "games")
            .build()

    override fun saveGameConfiguration(gameConfiguration: GameConfiguration) {
        CoroutineScope(Dispatchers.IO).launch {
            val model = GameSetting(
                gameConfiguration.id.toString(),
                gameConfiguration.name,
                gameConfiguration.numberOfRounds,
                gameConfiguration.numberOfQuestions,
                gameConfiguration.numberOfAnswers,
                gameConfiguration.timePerQuestion
            )

            db.dao().saveGameSetting(model)
            Log.d(TAG, "Saved game configuration: $model")
        }
    }

    override fun deleteGameConfiguration(gameConfiguration: GameConfiguration) {
        CoroutineScope(Dispatchers.IO).launch {
            db.dao().deleteGameSetting(gameConfiguration.id.toString())
            Log.d(TAG, "Deleted game configuration: ${gameConfiguration.id}")
        }
    }

    override suspend fun getGameConfigurations(): List<GameConfiguration> {
        val models = db.dao().findGameSettings().map {
                GameConfiguration(
                    it.name,
                    it.roundsCount,
                    it.questionsPerRound,
                    it.answersPerQuestion,
                    it.timePerQuestion,
                    UUID.fromString(it.id)
                )
            }
        Log.d(TAG, "Loaded game configurations: $models")
        return models
    }

}