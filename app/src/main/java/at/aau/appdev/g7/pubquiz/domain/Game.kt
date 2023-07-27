package at.aau.appdev.g7.pubquiz.domain

import at.aau.appdev.g7.pubquiz.domain.UserRole.*
import at.aau.appdev.g7.pubquiz.domain.GamePhase.*
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolException
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolMessage
import java.util.Collections
import java.util.EnumSet

class Game (
    val userRole: UserRole,
    val connectivityProvider: ConnectivityProvider<GameMessage>,
    val dataProvider: DataProvider
    ) {
    private lateinit var rounds: MutableList<Round>
    var phase = INIT
        private set

    lateinit var playerName: String
    var masterId: String? = null

    val players = mutableMapOf<String,Player>()

    val endpointIds = Collections.synchronizedSet(mutableSetOf<String>())
    val connectedIds = Collections.synchronizedSet(mutableSetOf<String>())

    var currentRoundIdx = -1
        private set
    var currentQuestionIdx = -1
        private set

    val numberOfRounds: Int
        get() = rounds.size
    val roundNames: List<String>
        get() = rounds.map { it.name ?: "Round ${it.index}" }

    val currentRound: Round
        get() = rounds[currentRoundIdx]
    val currentQuestion: Question
        get() = rounds[currentRoundIdx].questions[currentQuestionIdx]

    val hasNextRound: Boolean
        get() = currentRoundIdx < rounds.size - 1
    val hasNextQuestion: Boolean
        get() = currentQuestionIdx < currentRound.questions.size - 1

    // Master UI events
    var onPlayerJoined: (player: String) -> Unit = {}
    var onPlayerLeft: (player: String) -> Unit = {}
    var onPlayerReady: (player: String) -> Unit = {}
    var onPlayerAnswer: (player: String) -> Unit = {}
    var onPlayerSubmitRound: (player: String) -> Unit = {}

    // Player UI events
    var onEndpointChange: (endpointIds: Set<String>) -> Unit = {}
    var onGameConnected: () -> Unit = {}
    var onGameStarting: () -> Unit = {}
    var onNewRoundStart: () -> Unit = {}
    var onNewQuestion: () -> Unit = {}
    var onRoundEnd: () -> Unit = {}
    var onGameOver: () -> Unit = {}

    init {
        connectivityProvider.protocol = GameProtocol()

        connectivityProvider.onEndpointFound = this::onEndpointFound
        connectivityProvider.onEndpointLost = this::onEndpointLost
        connectivityProvider.onConnectionRequest = this::onConnectionRequest
        connectivityProvider.onConnected = this::onConnected
        connectivityProvider.onDisconnected = this::onDisconnected

        connectivityProvider.onReceiveData = this::onReceiveData
    }

    private fun onEndpointFound(endpointId: String) {
        endpointIds += endpointId
        onEndpointChange(endpointIds)
    }

    private fun onEndpointLost(endpointId: String) {
        endpointIds -= endpointId
        onEndpointChange(endpointIds)
    }

    private fun onConnectionRequest(endpointId: String) {
        // TODO manually accept connections
        connectivityProvider.acceptConnection(endpointId)
    }

    private fun onConnected(endpointId: String) {
        when (userRole) {
            PLAYER -> {
                onGameConnected()
                masterId = endpointId
                // TODO move this
                phase = STARTING
                connectivityProvider.sendData(endpointId, GameMessage(GameMessageType.PLAYER_JOIN, playerName))

            }
            MASTER -> {
                // TODO send game state
                connectedIds += endpointId
            }
        }
    }

    private fun onDisconnected(endpointId: String) {
        when (userRole) {
            PLAYER -> {
                masterId = null
            }
            MASTER -> {
                connectedIds -= endpointId
            }
        }

    }

    private fun identifyPlayer(endpointId: String) : Player {
        return players[endpointId] ?: throw ProtocolException("Invalid endpointId: ${endpointId}")
    }

    private fun onReceiveData(endpointId: String, message: GameMessage) {
        when (message.type) {
            GameMessageType.PLAYER_JOIN -> {
                expect(MASTER, CREATED, "on player joined")
                val name = message.name ?: throw ProtocolException("Player name is required")
                if (players.containsKey(name)) {
                    throw ProtocolException("Player name is already used")
                }
                players[endpointId] = Player(name)
                onPlayerJoined(name)
            }
            GameMessageType.PLAYER_READY -> {
                when (userRole) {
                    PLAYER -> {
                        onGameStarting.invoke()
                    }
                    MASTER -> {
                        val player = identifyPlayer(endpointId)
                        player.ready = true
                        if (players.all { it.value.ready }) {
                            phase = READY
                        }

                        onPlayerReady(player.name)
                    }
                }
            }
            GameMessageType.ROUND_START -> {
                //expect(PLAYER, READY, "on round start")
                if (phase == READY) {
                    // first round
                    rounds = mutableListOf()
                }
                phase = ROUND_STARTED
                currentRoundIdx++
                currentQuestionIdx = -1
                rounds.add(currentRoundIdx, Round(currentRoundIdx, message.name))
                onNewRoundStart.invoke()
            }
            GameMessageType.ROUND_END -> {
                onRoundEnd.invoke()
            }
            GameMessageType.QUESTION -> {
                // TODO expect(PLAYER, )
                phase = QUESTION_ACTIVE
                currentQuestionIdx++
                currentRound.questions.add(Question(currentQuestionIdx, message.name, message.answers!!))
                currentRound.answers.add("")
                onNewQuestion.invoke()
            }
            GameMessageType.ANSWER -> {
                expect(MASTER, QUESTION_ACTIVE, "question answer")
                identifyPlayer(endpointId).answered = true
                if (players.all { it.value.answered }) {
                    phase = QUESTION_ANSWERED
                }
                onPlayerAnswer.invoke(message.name!!)
            }
            GameMessageType.SUBMIT_ROUND -> {
                expectRole(MASTER, "submit round")
                identifyPlayer(endpointId).answersPerRound.add(message.answers!!)
                onPlayerSubmitRound.invoke(message.name!!)
            }
            // TODO
            else -> {}
        }
    }

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
            }.toMutableList()
            Round(r, "Round $r", questions)
        }.toMutableList()
        phase = SETUP
    }

    // TODO add overloaded setup() for import use case as soon as it will be needed

    fun createGame() {
        expectRole(MASTER, "create game")
        expectPhase(SETUP, "create game")

        // TODO advertise game, wait for players
        connectivityProvider.startAdvertising()
        phase = CREATED
    }

    fun startGame() {
        expectRole(MASTER, "start game")
        expectPhase(CREATED, "start game")

        // TODO start game, notify players, wait for readiness
        connectedIds.forEach {
            connectivityProvider.sendData(it, GameMessage(GameMessageType.PLAYER_READY))
        }

        phase = STARTING
    }

    fun forceStartGame() {
        expectRole(MASTER, "force start game")
        if (phase == STARTING) {
            phase = READY
        }
    }

    /**
     * 3. As a Player, I can search for a new game and join it.
     */
    fun searchGame() {
        expectRole(PLAYER, "search game")
        expectPhase(INIT, "search game")
        // TODO search game, after game is found, join game.

        phase = CREATED

        connectivityProvider.startDiscovery()
    }

    /**
     * 3a
     */
    fun joinGameAs(id: String, name: String) {
        expectRole(PLAYER, "join game")
        expectPhase(CREATED, "join game")

        playerName = name

        connectivityProvider.requestConnection(id)

        // This is moved to the onConnected callback
        // phase = STARTING
    }

    fun readyPlayer() {
        expect(PLAYER, STARTING, "ready player")

        phase = READY

        connectivityProvider.sendData(masterId!!, GameMessage(GameMessageType.PLAYER_READY))
    }

    fun startNextRound() {
        expect(MASTER, anyOf(READY, ROUND_ENDED), "start round")

        currentRoundIdx++
        currentQuestionIdx = -1

        val currentRound = rounds[currentRoundIdx]

        val roundName = "Round ${currentRound.index}"

        connectedIds.forEach {
            connectivityProvider.sendData(it, GameMessage(GameMessageType.ROUND_START, roundName))
        }

        phase = ROUND_STARTED
    }

    fun endRound() {
        expectRole(MASTER, "end round")
        // TODO check phase

        connectedIds.forEach {
            connectivityProvider.sendData(it, GameMessage(GameMessageType.ROUND_END))
        }

        phase = ROUND_ENDED
    }

    fun startNextQuestion() {
        expect(MASTER, anyOf(ROUND_STARTED, QUESTION_ANSWERED), "start next question")

        currentQuestionIdx++
        val question = currentQuestion

        players.forEach { it.value.answered = false }

        connectedIds.forEach {
            connectivityProvider.sendData(it, GameMessage(GameMessageType.QUESTION, question.text, question.answers))
        }

        phase = QUESTION_ACTIVE
    }

    /**
     * 12. As a Player, I can select the answer for current question
     */
    fun answerQuestion(answer: String) {
        expect(PLAYER, QUESTION_ACTIVE, "answer question")

        rounds[currentRoundIdx].answers[currentQuestionIdx] = answer

        connectivityProvider.sendData(masterId!!, GameMessage(GameMessageType.ANSWER, answer))

        phase = QUESTION_ANSWERED
    }

    fun submitRoundAnswers() {
        expect(PLAYER, QUESTION_ANSWERED, "submit round answers")

        connectivityProvider.sendData(masterId!!, GameMessage(
            GameMessageType.SUBMIT_ROUND,
            playerName, // This can probably be removed
            rounds[currentRoundIdx].answers))

        phase = ROUND_ENDED
    }

    /**
     * 13. As a Master, I can see same screen as a player and select the correct answer
     */
    fun selectCorrectAnswer(answer: String) {
        expectRole(MASTER, "select correct answer")
        // TODO check phase

        rounds[currentRoundIdx].answers[currentQuestionIdx] = answer

        // phase is not changed!
    }

    /**
     * 18. As a Master, I can trigger end of the game
     */
    fun endGame() {
        expect(MASTER, ROUND_ENDED, "end game")

        connectedIds.forEach {
            connectivityProvider.sendData(it, GameMessage(GameMessageType.GAME_OVER))
        }

        phase = END
    }

    private fun expect(role: UserRole, phase: GamePhase, action: String = "") {
        expectRole(role, action)
        expectPhase(phase, action)
    }

    private fun expect(role: UserRole, phases: EnumSet<GamePhase>, action: String = "") {
        expectRole(role, action)
        expectPhase(phases, action)
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

    private fun expectPhase(expectedPhases: EnumSet<GamePhase>, action: String) {
        if (!expectedPhases.contains(phase)) {
            throw IllegalStateException("Invalid game phase ($phase) to proceed with $action")
        }
    }

    private fun anyOf(vararg phases: GamePhase) : EnumSet<GamePhase> {
        val set = EnumSet.noneOf(GamePhase::class.java)
        phases.forEach { set.add(it) }
        return set
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
    QUESTION_ANSWERED,
    END
}

data class GameMessage(
    val type: GameMessageType,
    val name: String? = null,
    val answers: List<String>? = null) : ProtocolMessage

enum class GameMessageType {
    PLAYER_JOIN,
    PLAYER_READY,
    ROUND_START,
    ROUND_END,
    QUESTION,
    ANSWER,
    SUBMIT_ROUND,
    GAME_OVER
}

class GameProtocol: ConnectivityProtocol<GameMessage> {
    companion object {
        val SEPARATOR = ';'
    }
    override fun serialize(data: GameMessage): String {
        var s = data.type.toString() + SEPARATOR + data.name
        data.answers?.forEach { s += SEPARATOR + it }
        return s
    }

    override fun deserialize(data: String): GameMessage {
        val elements = data.split(SEPARATOR)
        return when(val type = GameMessageType.valueOf(elements[0])) {
            GameMessageType.PLAYER_JOIN -> GameMessage(type, elements[1])
            GameMessageType.PLAYER_READY -> GameMessage(type, if (elements.size > 1) elements[1] else null)
            GameMessageType.ROUND_START -> GameMessage(type, elements[1])
            GameMessageType.ROUND_END -> GameMessage(type, elements[1])
            GameMessageType.QUESTION -> GameMessage(type, elements[1], elements.subList(2, elements.size))
            GameMessageType.ANSWER -> GameMessage(type, elements[1])
            GameMessageType.SUBMIT_ROUND -> GameMessage(type, elements[1], elements.subList(2, elements.size))

            else -> GameMessage(
                type = type,
                name = if (elements.size > 1) elements[1] else null,
                answers = if (elements.size > 2) elements.subList(2, elements.size) else null
            )
        }
    }
}

data class Round(
    val index: Int,
    val name: String?,
    val questions: MutableList<Question> = mutableListOf()
) {
    val answers = MutableList(questions.size) { "" }
}

data class Question(val index: Int, val text: String?, val answers: List<String>) {

}

data class Player(val name: String) {
    val answersPerRound: MutableList<List<String>> = mutableListOf()
    var ready: Boolean = false
    var answered: Boolean = false
}
