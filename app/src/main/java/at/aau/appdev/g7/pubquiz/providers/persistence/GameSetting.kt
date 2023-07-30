package at.aau.appdev.g7.pubquiz.providers.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class GameSetting(
    @PrimaryKey
    val id: UUID,

    val name: String,

    @ColumnInfo(name = "rounds_count")
    val roundsCount: Int,

    @ColumnInfo(name = "questions_per_round")
    val questionsPerRound: Int,

    @ColumnInfo(name = "answers_per_question")
    val answersPerQuestion: Int,

    @ColumnInfo(name = "time_per_question")
    val timePerQuestion: Int
)