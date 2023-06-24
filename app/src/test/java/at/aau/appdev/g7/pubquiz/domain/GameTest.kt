package at.aau.appdev.g7.pubquiz.domain

import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.DataProvider
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

class TestConnection: ConnectivityProvider<GameMessage> {
    companion object {
        lateinit var server: TestConnection
        val clients = mutableListOf<TestConnection>()
    }

    override lateinit var protocol: ConnectivityProtocol<GameMessage>
    override lateinit var onReceiveData: (data: GameMessage) -> Unit
    var isServer = false

    override fun advertise() {
        server = this
        isServer = true
    }

    override fun connect() {
        clients.add(this)
    }

    override fun sendData(data: GameMessage) {
        val s = protocol.serialize(data)
        if (isServer) {
            println("TestConnection: server broadcasting message: $s")
            clients.forEach { c ->
                val m = protocol.deserialize(s)
                c.onReceiveData(m)
            }
        } else {
            println("TestConnection: client sending message: $s")
            val m = protocol.deserialize(s)
            server.onReceiveData(m)
        }
    }
}

class TestDataProvider: DataProvider

class GameTest {
    lateinit var master: Game
    lateinit var players: MutableList<Game>

    @Before
    fun setUp() {
        master = Game(UserRole.MASTER, TestConnection(), TestDataProvider())
        master.onPlayerJoined = { p -> println("Master: player $p joined")}
        master.onPlayerLeft = { p -> println("Master: player $p left")}
        master.onPlayerReady = { p -> println("Master: player $p ready")}
        master.onPlayerAnswer = { p -> println("Master: player $p answered")}
        master.onPlayerSubmitRound = { p -> println("Master: player $p submit round")}

        players = mutableListOf(
            Game(UserRole.PLAYER, TestConnection(), TestDataProvider()),
            Game(UserRole.PLAYER, TestConnection(), TestDataProvider()),
        )
        players.forEachIndexed { i,p ->
            p.onGameStarting = { println("Player $i: on game starting")}
            p.onNewRoundStart = { println("Player $i: on new round start")}
            p.onNewQuestion = { println("Player $i: on new question")}
            p.onRoundEnd = { println("Player $i: on round end")}
            p.onGameOver = { println("Player $i: on game over")}
        }
    }

    @Test
    fun simpleSimulation() {
        // 1
        master.setupGame(2, 3, 4)
        master.createGame()

        // 4
        //master.onPlayerJoined = { p -> println("master: $p joined") }

        //3
        players.forEach { p -> p.searchGame() }
        // 3a
        players.forEachIndexed { i,p -> p.joinGameAs("Team$i") }

        //5
        master.startGame()

        // 6
        players.forEach { p -> p.readyPlayer() }

        // 7
        assertTrue(master.players.all { it.value.ready })

        //8
        master.startNextRound()

        //9
        assertTrue(players.all { it.phase == GamePhase.ROUND_STARTED })

        // 10
        master.startNextQuestion()

        // 11
        assertTrue(players.all { it.phase == GamePhase.QUESTION_ACTIVE })

        // 12
        players.forEach { p -> p.answerQuestion(p.currentQuestion.answers.random()) }

        //13
        master.selectCorrectAnswer(master.currentQuestion.answers.random())
        // TODO master.players.forEach { i,p -> p }

        // 14
        master.endRound()

        // 16
        players.forEach { p -> p.submitRoundAnswers() }

        // 17

        // 18
        master.endGame()
    }
}