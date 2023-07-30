package at.aau.appdev.g7.pubquiz.domain.interfaces

import at.aau.appdev.g7.pubquiz.domain.GameConfiguration

interface DataProvider {
    fun saveGameConfiguration(gameConfiguration: GameConfiguration)

    fun deleteGameConfiguration(gameConfiguration: GameConfiguration)

    suspend fun getGameConfigurations(): List<GameConfiguration>
}