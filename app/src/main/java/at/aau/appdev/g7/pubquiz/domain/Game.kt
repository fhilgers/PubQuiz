package at.aau.appdev.g7.pubquiz.domain

import android.os.Parcelable
import android.util.Log
import at.aau.appdev.g7.pubquiz.domain.UserRole.*
import at.aau.appdev.g7.pubquiz.domain.GamePhase.*
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectionProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolException
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import java.util.Collections
import java.util.EnumSet

class Game(
    val userRole: UserRole,
    val connectivityProvider: ConnectivityProvider<GameMessage>,
    val dataProvider: DataProvider
) {
    private lateinit var rounds: MutableList<Round>
    var phase = INIT
        private set

    private var connectionScope = CoroutineScope(Dispatchers.IO)

    lateinit var playerName: String

    val players = Collections.synchronizedMap(mutableMapOf<String, Player>())

    val endpoints = connectivityProvider.discoveredEndpoints

    var currentRoundIdx = -1
        private set
    var currentQuestionIdx = -1
        private set

    val numberOfRounds: Int
        get() = rounds.size
    val roundNames: List<String>
        get() = rounds.map { it.name }

    val currentRound: Round
        get() = rounds[currentRoundIdx]
    val currentQuestion: Question
        get() = rounds[currentRoundIdx].questions[currentQuestionIdx]

    val currentRoundAnswers: List<PlayerRoundScoreRecord>
        get() {
            val roundAnswers = rounds[currentRoundIdx].answers
            return players.values
                .filter { player -> player.answersPerRound.size > currentRoundIdx }
                .map { player ->
                    val answers = player.answersPerRound[currentRoundIdx]
                    val score = answers.zip(roundAnswers).count { it.first == it.second }
                    PlayerRoundScoreRecord(player.name, answers, score)
                }
                .sortedByDescending { it.roundScore }
        }

    val hasNextRound: Boolean
        get() = currentRoundIdx < rounds.size - 1
    val hasNextQuestion: Boolean
        get() = currentQuestionIdx < currentRound.questions.size - 1

    // Master UI events
    var onConnectionRequestFlow: Flow<String>
    var onPlayerJoined: (player: String) -> Unit = {}
    var onPlayerLeft: (player: String) -> Unit = {}
    var onPlayerReady: (player: String) -> Unit = {}
    var onPlayerAnswer: (player: String) -> Unit = {}
    var onPlayerSubmitRound: (player: String) -> Unit = {}
    var onNavigateRounds: (roundIndex: Int) -> Unit = {}
    var onNavigateQuestions: (questionIndex: Int) -> Unit = {}
    var onNavigateRoundEnd: () -> Unit = {}
    var onNavigateStart: () -> Unit = {}
    var onNavigateGameOver: (rounds: List<Round>, players: List<Player>) -> Unit = { _, _ -> }

    // Player UI events
    var onGameStarting: () -> Unit = {}
    var onNewRoundStart: () -> Unit = {}
    var onNewQuestion: () -> Unit = {}
    var onRoundEnd: () -> Unit = {}
    var onGameOver: () -> Unit = {}

    val connections = mutableMapOf<String, ConnectionProvider<GameMessage>>()

    init {
        connectivityProvider.protocol = GameProtocol()

        onConnectionRequestFlow = flow {
            connectivityProvider.initiatedConnections
                .onEach { playerIds ->
                    playerIds.minus(connections.keys).forEach {playerId ->
                        Log.d("MYCUSTOMLOG", "New connection: $playerId")
                        emit(playerId)
                    }
                }.collect()
        }

    }

    private fun identifyPlayer(message: GameMessage): Player {
        return players[message.name] ?: throw ProtocolException("Invalid player: ${message.name}")
    }

    private fun onReceiveData(message: GameMessage) {
        when (message.type) {
            GameMessageType.PLAYER_JOIN -> {
                expect(MASTER, CREATED, "on player joined")
                val name = message.name ?: throw ProtocolException("Player name is required")
                if (players.containsKey(name)) {
                    throw ProtocolException("Player name is already used")
                }
                players[name] = Player(name)
                onPlayerJoined(name)
            }

            GameMessageType.PLAYER_READY -> {
                when (userRole) {
                    PLAYER -> {
                        onGameStarting.invoke()
                    }

                    MASTER -> {
                        val player = identifyPlayer(message)
                        player.ready = true
                        onPlayerReady(player.name)
                        if (players.all { it.value.ready }) {
                            phase = READY
                        }
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
                rounds.add(currentRoundIdx, Round(currentRoundIdx, message.name!!))
                onNewRoundStart.invoke()
            }

            GameMessageType.ROUND_END -> {
                _timer.value = currentRound.questions.first().time
                onRoundEnd.invoke()
            }

            GameMessageType.QUESTION -> {
                // TODO expect(PLAYER, )
                phase = QUESTION_ACTIVE
                currentQuestionIdx++
                currentRound.questions.add(
                    Question(
                        currentQuestionIdx,
                        message.name!!,
                        message.answers!!,
                        message.time!!
                    )
                )
                _timer.value = message.time
                currentRound.answers.add("")
                onNewQuestion.invoke()
            }

            GameMessageType.ANSWER -> {
                expect(MASTER, QUESTION_ACTIVE, "question answer")
                identifyPlayer(message).answered = true
                if (players.all { it.value.answered }) {
                    phase = QUESTION_ANSWERED
                }
                onPlayerAnswer.invoke(message.name!!)
            }

            GameMessageType.SUBMIT_ROUND -> {
                expectRole(MASTER, "submit round")
                identifyPlayer(message).answersPerRound.add(message.answers!!)
                onPlayerSubmitRound.invoke(message.name!!)
            }

            GameMessageType.GAME_OVER -> {
                phase = END
                onGameOver.invoke()
            }

            GameMessageType.TIMER_STARTED -> {
                expectRole(PLAYER, "timer started")
                expectTimer(TimerState.ENDED, "timer started")

                timerJob = CoroutineScope(Dispatchers.Default).launch {
                    for (i in message.time!! downTo 0) {
                        _timer.emit(i)

                        delay(1000)
                    }
                }

                _timerState.value = TimerState.STARTED
            }

            GameMessageType.TIMER_PAUSED -> {
                expectRole(PLAYER, "timer paused")
                expectTimer(TimerState.STARTED, "timer paused")

                timerJob?.cancel()

                _timerState.value = TimerState.PAUSED
                _timer.value = message.time!!
            }

            GameMessageType.TIMER_RESUMED -> {
                expectRole(PLAYER, "timer resumed")
                expectTimer(TimerState.PAUSED, "timer resumed")

                timerJob = CoroutineScope(Dispatchers.Default).launch {
                    for (i in message.time!! downTo 0) {
                        _timer.emit(i)

                        delay(1000)
                    }
                }

                _timerState.value = TimerState.STARTED
            }

            GameMessageType.TIMER_ENDED -> {
                expectRole(PLAYER, "timer ended")
                expectTimer(anyOf(TimerState.STARTED, TimerState.PAUSED), "timer ended")

                timerJob?.cancel()

                _timerState.value = TimerState.ENDED
            }
        }
    }

    /**
     * 0. When I close a lobby I want to be able to host a new one
     */
    fun reset() {
        phase = INIT
        rounds = mutableListOf()
        currentRoundIdx = -1
        currentQuestionIdx = -1
        players.clear()
        connections.clear()
        connectionScope.cancel()
    }

    /**
     * 1. As a Master, I can set up a new pub quiz session
     */
    fun setupGame(numberOfRounds: Int, questionsPerRound: Int, answersPerQuestion: Int, timePerQuestion: Int) {
        expectRole(MASTER, "setup game")
        expectPhase(INIT, "setup game")

        rounds = (1..numberOfRounds).map { r ->
            val questions = (1..questionsPerRound).map { q ->
                val answers = 'A'.rangeTo('A'.plus(answersPerQuestion - 1))
                    .map { a -> "$a" }.toList()
                Question(q, "Question $q", answers, timePerQuestion)
            }.toMutableList()
            Round(r, "Round $r", questions)
        }.toMutableList()
        phase = SETUP
    }

    fun setupGame(configuration: GameConfiguration) {
        setupGame(configuration.numberOfRounds, configuration.numberOfQuestions, configuration.numberOfAnswers, configuration.timePerQuestion)
    }

    // TODO add overloaded setup() for import use case as soon as it will be needed

    fun createGame() {
        expectRole(MASTER, "create game")
        expectPhase(SETUP, "create game")

        // TODO advertise game, wait for players
        connectionScope.launch {
            connectivityProvider.advertise()
        }

        phase = CREATED
    }

    fun startGame() {
        expectRole(MASTER, "start game")
        expectPhase(CREATED, "start game")

        // TODO start game, notify players, wait for readiness
        broadcast(GameMessage(GameMessageType.PLAYER_READY))

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

        connectionScope.launch {
            connectivityProvider.discover()
        }

        phase = CREATED
    }

    suspend fun connectToGame(serverId: String) {
        expectRole(PLAYER, "connect to game")
        expectPhase(CREATED, "connect to game")


        val connection = connectivityProvider.connect(serverId)

        connectionScope.launch {
            connection.messages.collect { message ->
                onReceiveData(message)
            }
        }

        connections[serverId] = connection

        // TODO define phase
    }

    suspend fun acceptConnection(clientId: String) {
        expectRole(MASTER, "accept connection")
        expectPhase(CREATED, "accept connection")


        val connection = connectivityProvider.accept(clientId)

        connectionScope.launch {
            connection.messages.collect { message ->
                onReceiveData(message)
            }
        }

        connections[clientId] = connection

        // TODO define phase
    }

    /**
     * 3a
     */
    fun joinGameAs(name: String) {
        expectRole(PLAYER, "join game")
        expectPhase(CREATED, "join game")

        playerName = name
        sendToMaster(GameMessage(GameMessageType.PLAYER_JOIN, playerName))

        phase = STARTING
    }

    fun readyPlayer() {
        expect(PLAYER, STARTING, "ready player")

        sendToMaster(GameMessage(GameMessageType.PLAYER_READY, playerName))

        phase = READY
    }

    fun startNextRound() {
        expect(MASTER, anyOf(READY, ROUND_ENDED), "start round")

        currentRoundIdx++
        currentQuestionIdx = -1

        val currentRound = rounds[currentRoundIdx]

        val roundName = "Round ${currentRound.index}"
        broadcast(GameMessage(GameMessageType.ROUND_START, roundName))

        phase = ROUND_STARTED
    }

    fun endRound() {
        expectRole(MASTER, "end round")
        // TODO check phase

        _timer.value = rounds[currentRoundIdx].questions[currentQuestionIdx].time

        broadcast(GameMessage(GameMessageType.ROUND_END))

        phase = ROUND_ENDED

        startTimer()
    }

    fun startNextQuestion() {
        expect(MASTER, anyOf(ROUND_STARTED, QUESTION_ANSWERED), "start next question")

        currentQuestionIdx++

        players.forEach { it.value.answered = false }

        _timer.value = rounds[currentRoundIdx].questions[currentQuestionIdx].time
    }

    /**
     * 12. As a Player, I can select the answer for current question
     * @param answer Answer to the target question
     * @param questionIndex Index of the target question. If not specified, current question is assumed.
     */
    fun answerQuestion(answer: String, questionIndex: Int = currentQuestionIdx) {
        expect(PLAYER, anyOf(QUESTION_ACTIVE, QUESTION_ANSWERED), "answer question")

        if (questionIndex < 0 || questionIndex > currentQuestionIdx)
            throw IndexOutOfBoundsException("Invalid question index: $questionIndex")

        val answers = rounds[currentRoundIdx].answers
        val isNewAnswer = questionIndex == currentQuestionIdx && answers[questionIndex].isEmpty()

        answers[questionIndex] = answer

        // update phase and notify master only if the answer is new
        if (isNewAnswer) {
            sendToMaster(GameMessage(GameMessageType.ANSWER, playerName))

            phase = QUESTION_ANSWERED
        }
    }

    fun submitRoundAnswers() {
        expect(PLAYER, QUESTION_ANSWERED, "submit round answers")

        sendToMaster(
            GameMessage(
                GameMessageType.SUBMIT_ROUND,
                playerName,
                rounds[currentRoundIdx].answers
            )
        )

        phase = ROUND_ENDED
    }

    /**
     * 13. As a Master, I can see same screen as a player and select the correct answer
     */
    fun selectCorrectAnswer(answer: String) {
        expectRole(MASTER, "select correct answer")
        // TODO check phase

        rounds[currentRoundIdx].answers[currentQuestionIdx] = answer

        // phase is changed!
        val question = currentQuestion
        broadcast(GameMessage(GameMessageType.QUESTION, question.text, question.answers, question.time))

        phase = QUESTION_ACTIVE

        startTimer()
    }

    private var _timer = MutableStateFlow(-1)
    var timer = _timer.asStateFlow()
    private var timerJob: Job? = null
    private var _timerState = MutableStateFlow(TimerState.ENDED)
    var timerState = _timerState.asStateFlow()

    enum class TimerState {
        STARTED, PAUSED, ENDED
    }


    fun startTimer() {
        expectRole(MASTER, "start timer")
        expectTimer(TimerState.ENDED, "start timer")

        val question = currentQuestion

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                for (i in question.time downTo 0) {
                    // blocks until paused == false
                    timerState.first { it == TimerState.STARTED }

                    _timer.emit(i)
                    delay(1000)
                }
            } finally {
                _timerState.value = TimerState.ENDED

                broadcast(GameMessage(GameMessageType.TIMER_ENDED, "", null, timer.value))

                if (phase == ROUND_ENDED) {
                    if (currentRoundIdx == rounds.size - 1) {
                        endGame()
                        onNavigateGameOver(rounds, players.values.toList())
                    } else {
                        onNavigateRounds(currentRoundIdx + 1)
                    }
                } else {
                    if (currentQuestionIdx == currentRound.questions.size - 1) {
                        endRound()
                        onNavigateRoundEnd()
                    } else {
                        onNavigateQuestions(currentQuestionIdx + 1)
                    }
                }
            }
        }


        broadcast(GameMessage(GameMessageType.TIMER_STARTED, "", null, timer.value))

        _timerState.value = TimerState.STARTED
    }

    fun pauseTimer() {
        expectRole(MASTER,"pause timer")
        expectTimer(TimerState.STARTED, "pause timer")

        _timerState.value = TimerState.PAUSED

        broadcast(GameMessage(GameMessageType.TIMER_PAUSED, "", null, timer.value))
    }

    fun resumeTimer() {
        expectRole(MASTER,"resume timer")
        expectTimer(TimerState.PAUSED, "resume timer")


        broadcast(GameMessage(GameMessageType.TIMER_RESUMED, "", null, timer.value))

        _timerState.value = TimerState.STARTED
    }

    fun skipTimer() {
        expectRole(MASTER, "skip timer")
        expectTimer(anyOf(TimerState.STARTED, TimerState.PAUSED), "skip timer")

        timerJob?.cancel()

        _timerState.value = TimerState.ENDED
    }

    /**
     * 18. As a Master, I can trigger end of the game
     */
    fun endGame() {
        expect(MASTER, ROUND_ENDED, "end game")
        broadcast(GameMessage(GameMessageType.GAME_OVER))
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

    private fun expectTimer(expectedState: TimerState, action: String) {
        if (expectedState != timerState.value) {
            throw IllegalStateException("Invalid timer state (${timerState.value}) to $action")
        }
    }

    private fun expectTimer(expectedStates: EnumSet<TimerState>, action: String) {
        if (!expectedStates.contains(timerState.value)) {
            throw IllegalStateException("Invalid timer state (${timerState.value}) to $action")
        }
    }

    private fun expectPhase(expectedPhases: EnumSet<GamePhase>, action: String) {
        if (!expectedPhases.contains(phase)) {
            throw IllegalStateException("Invalid game phase ($phase) to proceed with $action")
        }
    }

    private inline fun <reified T: Enum<T>> anyOf(vararg phases: T): EnumSet<T> {
        val set = EnumSet.noneOf(T::class.java)
        phases.forEach { set.add(it) }
        return set
    }

    private fun broadcast(message: GameMessage) {
        expectRole(MASTER, "broadcast")
        runBlocking {
            connections.forEach {
                it.value.send(message)
            }
        }
    }

    private fun sendToMaster(message: GameMessage) {
        expectRole(PLAYER, "send to master")
        assert(connections.size == 1)

        runBlocking {
            connections.values.first().send(message)
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
    QUESTION_ANSWERED,
    END
}

@Parcelize
data class GameMessage(
    val type: GameMessageType,
    val name: String? = null,
    val answers: List<String>? = null,
    val time: Int? = null
) : ProtocolMessage, Parcelable

enum class GameMessageType {
    PLAYER_JOIN,
    PLAYER_READY,
    ROUND_START,
    ROUND_END,
    QUESTION,
    ANSWER,
    SUBMIT_ROUND,
    GAME_OVER,
    TIMER_STARTED,
    TIMER_PAUSED,
    TIMER_RESUMED,
    TIMER_ENDED,
}

class GameProtocol : ConnectivityProtocol<GameMessage> {
    companion object {
        val SEPARATOR = ';'
    }

    override fun serialize(data: GameMessage): String {
        var s = data.type.toString() + SEPARATOR + data.name + SEPARATOR + data.time.toString()
        data.answers?.forEach { s += SEPARATOR + it }
        return s
    }

    override fun deserialize(data: String): GameMessage {
        val elements = data.split(SEPARATOR)
        return when (val type = GameMessageType.valueOf(elements[0])) {
            GameMessageType.PLAYER_JOIN -> GameMessage(type, elements[1])
            GameMessageType.PLAYER_READY -> GameMessage(
                type,
                if (elements.size > 1) elements[1] else null
            )

            GameMessageType.ROUND_START -> GameMessage(type, elements[1])
            GameMessageType.ROUND_END -> GameMessage(type, elements[1])
            GameMessageType.QUESTION -> GameMessage(
                type,
                elements[1],
                elements.subList(3, elements.size),
                elements[2].toInt()
            )

            GameMessageType.ANSWER -> GameMessage(type, elements[1])
            GameMessageType.SUBMIT_ROUND -> GameMessage(
                type,
                elements[1],
                elements.subList(3, elements.size)
            )

            GameMessageType.TIMER_STARTED -> GameMessage(type, null, null, elements[2].toInt())
            GameMessageType.TIMER_ENDED -> GameMessage(type, null, null, elements[2].toInt())
            GameMessageType.TIMER_PAUSED -> GameMessage(type, null, null, elements[2].toInt())
            GameMessageType.TIMER_RESUMED -> GameMessage(type, null, null, elements[2].toInt())

            else -> GameMessage(
                type = type,
                name = if (elements.size > 1) elements[1] else null,
                answers = if (elements.size > 3) elements.subList(3, elements.size) else null,
                time = null
            )
        }
    }
}

@Parcelize
data class Round(
    val index: Int,
    val name: String,
    val questions: MutableList<Question> = mutableListOf(),
    val answers: MutableList<String> = MutableList(questions.size) { "" }
): Parcelable

@Parcelize
data class Question(
    val index: Int,
    val text: String,
    val answers: List<String>,
    val time: Int
): Parcelable

@Parcelize
data class Player(
    val name: String,
    val answersPerRound: MutableList<List<String>> = mutableListOf(),
    var ready: Boolean = false,
    var answered: Boolean = false
): Parcelable

@Parcelize
data class PlayerRoundScoreRecord(
    val player: String,
    val roundAnswers: List<String>,
    val roundScore: Int
): Parcelable