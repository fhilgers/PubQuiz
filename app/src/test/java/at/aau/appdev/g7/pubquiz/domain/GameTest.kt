package at.aau.appdev.g7.pubquiz.domain

import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.util.UUID

class TestConnectivityProvider() : ConnectivityProvider<GameMessage> {

    private val providerId = UUID.randomUUID().toString()

    private val connectedIds = mutableSetOf<String>()

    override fun hashCode(): Int {
        return providerId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TestConnectivityProvider) return false
        return providerId == other.providerId
    }

    companion object {
        val servers = mutableSetOf<TestConnectivityProvider>()
        val clients = mutableSetOf<TestConnectivityProvider>()

        fun endpointFoundCallback(endpointId: String) {
            clients.forEach {
                it.onEndpointFound(endpointId)
            }
        }
        fun endpointLostCallback(endpointId: String) {
            clients.forEach {
                it.onEndpointLost(endpointId)
            }
        }
    }

    override lateinit var protocol: ConnectivityProtocol<GameMessage>

    override fun startAdvertising() {
        servers.add(this)

        endpointFoundCallback(providerId)
    }

    override fun stopAdvertising() {
        servers.remove(this)

        endpointLostCallback(providerId)
    }

    override fun startDiscovery() {
        clients.add(this)

        servers.forEach {
            onEndpointFound(it.providerId)
        }
    }

    override fun stopDiscovery() {
        clients.remove(this)
    }

    override fun requestConnection(endpointId: String) {

        servers.find { it.providerId == endpointId }?.let {
            it.onConnectionRequest(providerId)
        }
    }

    override fun acceptConnection(endpointId: String) {
        connectedIds.add(endpointId)

        onConnected(endpointId)

        clients.find { it.providerId == endpointId }?.let {
            it.onConnected(providerId)
        }
    }

    override fun rejectConnection(endpointId: String) {
        // ignore
    }

    override fun disconnect(endpointId: String) {
        connectedIds.remove(endpointId)

        onDisconnected(endpointId)

        clients.find { it.providerId == endpointId }?.let {
            it.onDisconnected(providerId)
        }
    }

    override fun sendData(endpointId: String, data: GameMessage) {

        (clients + servers).find { it.providerId == endpointId }?.let {
            it.onReceiveData(providerId, data)
        }
    }

    override lateinit var onEndpointFound: (endpointId: String) -> Unit
    override lateinit var onEndpointLost: (endpointId: String) -> Unit
    override lateinit var onConnectionRequest: (endpointId: String) -> Unit
    override lateinit var onConnected: (endpointId: String) -> Unit
    override lateinit var onDisconnected: (endpointId: String) -> Unit
    override lateinit var onReceiveData: (endpointId: String, data: GameMessage) -> Unit

}

class TestDataProvider: DataProvider

class GameTest {
    lateinit var master: Game
    lateinit var players: MutableList<Game>

    @Before
    fun setUp() {
        master = Game(UserRole.MASTER, TestConnectivityProvider(), TestDataProvider())

        var joined = 0
        master.onPlayerJoined = { p ->
            joined++
            println("Master: player $p joined")

            if (joined == players.size) {

                // 5
                master.startGame()
            }
        }

        master.onPlayerLeft = { p ->
            println("Master: player $p left")
        }

        var ready = 0
        master.onPlayerReady = {pl ->
            ready++
            println("Master: player $pl ready")

            if (ready == players.size) {

                while(master.hasNextRound) {
                    //8
                    master.startNextRound()
                    assertEquals(3, master.currentRound.questions.size)
                    assertEquals("Question 1", master.currentRound.questions[0].text)
                    assertEquals("Question 2", master.currentRound.questions[1].text)
                    assertEquals("Question 3", master.currentRound.questions[2].text)

                    //9
                    assertTrue(players.all { it.phase == GamePhase.ROUND_STARTED })

                    while(master.hasNextQuestion) {
                        // 10
                        master.startNextQuestion()

                        // 11
                        assertTrue(players.all { it.phase == GamePhase.QUESTION_ACTIVE })
                        assertEquals(GamePhase.QUESTION_ACTIVE, master.phase)

                        // 12
                        players.forEach { p -> p.answerQuestion(p.currentQuestion.answers.random()) }

                        //13
                        master.selectCorrectAnswer(master.currentQuestion.answers.random())
                        // TODO master.players.forEach { i,p -> p }
                    }

                    // 14
                    master.endRound()

                    // 16
                    players.forEach { p -> p.submitRoundAnswers() }
                }

                // 18
                master.endGame()
            }
        }
        master.onPlayerAnswer = { p -> println("Master: player $p answered")}
        master.onPlayerSubmitRound = { p -> println("Master: player $p submit round")}

        players = mutableListOf(
            Game(UserRole.PLAYER, TestConnectivityProvider(), TestDataProvider()),
            Game(UserRole.PLAYER, TestConnectivityProvider(), TestDataProvider()),
        )
        players.forEachIndexed { i,p ->
            p.onGameStarting = {
                println("Player $i: on game starting")

                // 6
                p.readyPlayer()
            }
            p.onNewRoundStart = { println("Player $i: on new round start")}
            p.onNewQuestion = { println("Player $i: on new question")}
            p.onRoundEnd = { println("Player $i: on round end")}
            p.onGameOver = { println("Player $i: on game over")}

            var changed = false
            p.onEndpointChange = {
                println("Player $i: on endpoint change")

                if (!changed) {
                    changed = true

                    assert(it.size == 1)
                    val masterId = it.first()

                    // 3a
                    p.joinGameAs(masterId, "Team$i")
                } else {
                    fail()
                }
            }

            p.onGameConnected = { println("Player $i: on game connected")}
        }
    }

    @Test
    fun simpleSimulation() {
        // 1
        master.setupGame(2, 3, 4)
        assertEquals(2, master.roundNames.size)

        master.createGame()

        // 4
        //master.onPlayerJoined = { p -> println("master: $p joined") }

        //3
        players.forEach { p -> p.searchGame() }
    }
}