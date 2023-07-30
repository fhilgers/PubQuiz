package at.aau.appdev.g7.pubquiz.domain

import java.util.UUID

data class GameConfiguration(
    val name: String,
    val numberOfRounds: Int,
    val numberOfQuestions: Int,
    val numberOfAnswers: Int,
    val timePerQuestion: Int,
    val id: UUID = UUID.randomUUID()
)