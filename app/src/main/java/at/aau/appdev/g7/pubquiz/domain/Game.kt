package at.aau.appdev.g7.pubquiz.domain

import at.aau.appdev.g7.pubquiz.domain.UserRole.*
import at.aau.appdev.g7.pubquiz.domain.GamePhase.*
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolMessage

class Game (
    val userRole: UserRole,
    val connectivityProvider: ConnectivityProvider,
    val dataProvider: DataProvider
    ) {
    private lateinit var rounds: List<Round>
    private var phase = INIT

    lateinit var playerName: String

    var currentRoundIdx = -1
    var currentQuestionIdx = -1

    // Master events
    var onPlayerJoined: () -> Unit = {}

    // Player events


    /**
     * 1. As a Master, I can set up a new pub quiz session
     */
    fun setupGame(numberOfRounds: Int, questionsPerRound: Int, answersPerQuestion: Int) {
        expectRole(MASTER, "setup game")
        expectPhase(INIT, "setup game")

        rounds = (1..numberOfRounds).map {r ->
            val questions = (1..questionsPerRound).map {q ->
                val answers = 'A'.rangeTo('A'.plus(answersPerQuestion - 1))
                    .map { a -> "$a" }.toList()
                Question(q, "Question $q", answers)
            }.toList()
            Round(r, questions)
        }.toList()
        phase = SETUP
    }

    // TODO add overloaded setup() for import use case as soon as it will be needed

    fun createGame() {
        expectRole(MASTER, "create game")
        expectPhase(SETUP, "create game")

        // TODO advertise game, wait for players
        connectivityProvider.advertise()
        phase = CREATED
    }

    fun start() {
        expectRole(MASTER, "start game")
        expectPhase(CREATED, "start game")

        // TODO start game, notify players, wait for readiness
        connectivityProvider.sendData(GameMessage(GameMessageType.PLAYER_READY))
        phase = STARTING
    }

    /**
     * 3. As a Player, I can search for a new game and join it.
     */
    fun searchGame() {
        expectRole(PLAYER, "search game")
        expectPhase(INIT, "search game")
        // TODO search game, after game is found, join game.
        connectivityProvider.connect()

        phase = CREATED
    }

    /**
     * 3a
     */
    fun joinGameAs(name: String) {
        expectRole(PLAYER, "join game")
        expectPhase(CREATED, "join game")

        playerName = name
        connectivityProvider.sendData(GameMessage(GameMessageType.PLAYER_JOIN, playerName))

        phase = STARTING
    }

    fun readyPlayer() {
        expect(PLAYER, CREATED, "ready player")

        connectivityProvider.sendData(GameMessage(GameMessageType.PLAYER_READY, playerName))

        phase = READY
    }

    fun startNextRound() {
        expect(MASTER, READY, "start round")

        currentRoundIdx++
        if (currentRoundIdx == 0) {
            currentQuestionIdx = -1
        }
        val currentRound = rounds[currentRoundIdx]
        val roundName = "Round ${currentRound.index}"
        connectivityProvider.sendData(GameMessage(GameMessageType.ROUND_START, roundName))

        phase = ROUND_STARTED
    }

    fun endRound() {
        expect(MASTER, ROUND_STARTED, "end round")

        connectivityProvider.sendData(GameMessage(GameMessageType.ROUND_END))

        phase = ROUND_ENDED
    }

    fun startNextQuestion() {
        expect(MASTER, ROUND_STARTED, "start question")

        currentQuestionIdx++
        val currentRound = rounds[currentRoundIdx]
        val question = currentRound.questions[currentRoundIdx]

        connectivityProvider.sendData(GameMessage(GameMessageType.QUESTION, question.text, question.answers))

        phase = QUESTION_ACTIVE
    }

    /**
     * 12. As a Player, I can select the answer for current question
     */
    fun answerQuestion(answer: String) {
        expect(PLAYER, QUESTION_ACTIVE, "answer question")

        phase = QUESTION_ANSWERED
    }

    fun submitRoundAnswers() {
        expect(PLAYER, QUESTION_ANSWERED, "submit round answers")

        phase = ROUND_ENDED
    }

    /**
     * 13. As a Master, I can see same screen as a player and select the correct answer
     */
    fun selectCorrectAnswer(answer: String) {
        expect(MASTER, QUESTION_ACTIVE, "select correct answer")

        // phase is not changed!
    }

    private fun expect(role: UserRole, phase: GamePhase, action: String = "") {
        expectRole(role, action)
        expectPhase(phase, action)
    }

    private fun expectRole(role: UserRole, action: String) {
        if (userRole != role) {
            throw IllegalStateException("Illegal role ($userRole) to $action")
        }
    }

    private fun expectPhase(expectedPhase: GamePhase, action: String) {
        if (phase != expectedPhase) {
            throw IllegalStateException("Invalid game phase ($phase) to $action")
        }
    }
}

enum class GamePhase {
    INIT,
    SETUP,
    CREATED,
    STARTING,
    READY,
    ROUND_STARTED,
    ROUND_ENDED,
    QUESTION_ACTIVE,
    QUESTION_ANSWERED
}

class GameMessage(val type: GameMessageType,
                  val name: String? = null,
                  val answers: List<String>? = null) : ProtocolMessage {

}

enum class GameMessageType {
    PLAYER_JOIN,
    PLAYER_READY,
    ROUND_START,
    ROUND_END,
    QUESTION,
    ANSWER
}

class GameProtocol: ConnectivityProtocol {
    override fun serialize(data: ProtocolMessage): String {
        TODO("Not yet implemented")
    }

    override fun deserialize(data: String): ProtocolMessage {
        TODO("Not yet implemented")
    }
}

data class Round(val index: Int, val questions: List<Question>) {

}

data class Question(val index: Int, val text: String?, val answers: List<String>) {

}
