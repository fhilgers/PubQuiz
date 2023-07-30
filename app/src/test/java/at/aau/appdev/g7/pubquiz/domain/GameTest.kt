package at.aau.appdev.g7.pubquiz.domain

import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectionProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class TestConnection(
    private val senderName: String,
    private val receiverName: String,
    override val messages: SharedFlow<GameMessage>,
    private val other: MutableSharedFlow<GameMessage>,
    override var protocol: ConnectivityProtocol<GameMessage> = GameProtocol()
) : ConnectionProvider<GameMessage> {

    override suspend fun send(message: GameMessage) {
        println("Sending $message from $senderName to $receiverName")
        other.emit(message)
        println("Sent $message from $senderName to $receiverName")
    }

    override suspend fun close() {
        println("Closing connection from $senderName to $receiverName")
    }
}

class TestConnectivityProvider(
    private val name: String,
) : ConnectivityProvider<GameMessage> {
    companion object {
        val server = MutableStateFlow<TestConnectivityProvider?>(null)

        val acceptedConnectionsFlow = MutableSharedFlow<Pair<String, TestConnection>>()
    }

    override lateinit var protocol: ConnectivityProtocol<GameMessage>

    private val _discoverEndpoints = MutableStateFlow<Set<String>>(setOf())
    override val discoveredEndpoints = _discoverEndpoints

    private val _initiatedConnections = MutableStateFlow<Set<String>>(setOf())
    override val initiatedConnections = _initiatedConnections

    override suspend fun advertise() {
        try {
            server.value = this

            awaitCancellation()
        } finally {
            server.value = null
        }
    }

    override suspend fun discover() {
        server.collect { c ->
            when (c) {
                null -> _discoverEndpoints.update { setOf() }
                else -> _discoverEndpoints.update { setOf(c.name) }
            }
        }
    }

    override suspend fun connect(endpointId: String): ConnectionProvider<GameMessage> =
        coroutineScope {
            if (!_discoverEndpoints.value.contains(endpointId)) {
                throw RuntimeException("Endpoint $endpointId not found")
            }

            server.value?.let { s ->
                assertEquals(endpointId, s.name)

                val connection = async {
                    acceptedConnectionsFlow.first { it.first == name }.second
                }

                s._initiatedConnections.update { it + name }

                connection.await()
            } ?: throw RuntimeException("Server not found")
        }

    override suspend fun accept(endpointId: String): ConnectionProvider<GameMessage> {
        if (!_initiatedConnections.value.contains(endpointId)) {
            throw RuntimeException("Endpoint $endpointId not found")
        }

        val messages = MutableSharedFlow<GameMessage>()
        val other = MutableSharedFlow<GameMessage>()

        val connection = TestConnection(name, endpointId, messages, other)
        val otherConnection = TestConnection(endpointId, name, other, messages)

        acceptedConnectionsFlow.emit(endpointId to otherConnection)

        return connection
    }
}

class TestDataProvider : DataProvider {
    override fun saveGameConfiguration(gameConfiguration: GameConfiguration) {

    }

    override fun deleteGameConfiguration(gameConfiguration: GameConfiguration) {

    }

    override suspend fun getGameConfigurations(): List<GameConfiguration> {
        return listOf()
    }

}

class GameTest {
    lateinit var master: Game
    lateinit var players: MutableList<Game>

    @Before
    fun setUp() {
        master = Game(UserRole.MASTER, TestConnectivityProvider("master"), TestDataProvider())
        master.onPlayerJoined = { p -> println("Master: player $p joined") }
        master.onPlayerLeft = { p -> println("Master: player $p left") }
        master.onPlayerReady = { p -> println("Master: player $p ready") }
        master.onPlayerAnswer = { p -> println("Master: player $p answered") }
        master.onPlayerSubmitRound = { p -> println("Master: player $p submit round") }

        players = mutableListOf(
            Game(UserRole.PLAYER, TestConnectivityProvider("Team0"), TestDataProvider()),
            Game(UserRole.PLAYER, TestConnectivityProvider("Team1"), TestDataProvider()),
        )
        players.forEachIndexed { i, p ->
            p.onGameStarting = { println("Player $i: on game starting") }
            p.onNewRoundStart = { println("Player $i: on new round start") }
            p.onNewQuestion = { println("Player $i: on new question") }
            p.onRoundEnd = { println("Player $i: on round end") }
            p.onGameOver = { println("Player $i: on game over") }
        }
    }

    @Test
    fun simpleSimulation() = runBlocking {
        // 1
        master.setupGame(2, 3, 4, 15)
        assertEquals(2, master.roundNames.size)

        master.createGame()

        // 4
        //master.onPlayerJoined = { p -> println("master: $p joined") }

        //3
        players.forEach { p -> p.searchGame() }


        val connectJob = launch {
            players.forEach { p ->
                p.endpoints.first { it.contains("master") }.let {
                    launch {
                        p.connectToGame("master")
                    }
                }
            }
        }



        val acceptJob = launch {
            master.connectivityProvider.initiatedConnections.first {
                it.contains("Team0") && it.contains(
                    "Team1"
                )
            }.let {
                launch {
                    master.acceptConnection("Team0")
                }
                launch {
                    master.acceptConnection("Team1")
                }
            }
        }

        joinAll(connectJob, acceptJob)


        // 3a
        players.forEachIndexed { i, p -> p.joinGameAs("Team$i") }

        //5
        master.startGame()

        // 6
        players.forEach { p -> p.readyPlayer() }


        delay(1000)

        // 7
        assertTrue(master.players.all { it.value.ready })

        while (master.hasNextRound) {
            //8
            master.startNextRound()
            assertEquals(3, master.currentRound.questions.size)
            assertEquals("Question 1", master.currentRound.questions[0].text)
            assertEquals("Question 2", master.currentRound.questions[1].text)
            assertEquals("Question 3", master.currentRound.questions[2].text)

            //9
            assertTrue(players.all { it.phase == GamePhase.ROUND_STARTED })

            while (master.hasNextQuestion) {
                // 10
                master.startNextQuestion()

                //13
                master.selectCorrectAnswer(master.currentQuestion.answers.random())

                // 11
                assertTrue(players.all { it.phase == GamePhase.QUESTION_ACTIVE })
                assertEquals(GamePhase.QUESTION_ACTIVE, master.phase)

                // 12
                players.forEach { p -> p.answerQuestion(p.currentQuestion.answers.random()) }

                // TODO master.players.forEach { i,p -> p }
            }

            // 14
            master.endRound()

            // 16
            players.forEach { p -> p.submitRoundAnswers() }
        }

        // 17

        // 18
        master.endGame()
    }
}