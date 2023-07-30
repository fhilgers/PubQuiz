package at.aau.appdev.g7.pubquiz.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class GameConfiguration(
    val name: String,
    val numberOfRounds: Int,
    val numberOfQuestions: Int,
    val numberOfAnswers: Int,
    val timePerQuestion: Int,
    val id: UUID = UUID.randomUUID(),
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable