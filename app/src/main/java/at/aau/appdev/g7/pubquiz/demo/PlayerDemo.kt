package at.aau.appdev.g7.pubquiz.demo

import android.util.Log
import at.aau.appdev.g7.pubquiz.domain.GameMessage
import at.aau.appdev.g7.pubquiz.domain.GameMessageType.*
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectionProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.util.Timer
import java.util.TimerTask

class PlayerDemoConnectivitySimulator(
    val tickMs: Long = 1000L
) : ConnectivityProvider<GameMessage> {
    companion object {
        const val TAG = "PlayerDemo"
    }

    override lateinit var protocol: ConnectivityProtocol<GameMessage>

    private val _initiatedConnections = MutableStateFlow(setOf<String>())
    override val initiatedConnections = _initiatedConnections

    private val _discoveredEndpoints = MutableStateFlow(setOf<String>())
    override val discoveredEndpoints = _discoveredEndpoints

    var messages: MutableSharedFlow<GameMessage> = MutableSharedFlow()

    override suspend fun advertise() {
        throw RuntimeException("Player should not call advertise()")
    }

    override suspend fun discover() {
        Log.i(TAG, "discover")
        _discoveredEndpoints.emit(setOf("master"))
    }

    override suspend fun connect(endpointId: String): ConnectionProvider<GameMessage> {
        Log.i(TAG, "connect")

        return Connection(this)
    }

    override suspend fun accept(endpointId: String): ConnectionProvider<GameMessage> {
        Log.i(TAG, "accept")
        TODO("Player does not accept")
    }


    var roundIdx = 0
    var roundsNumber = 2
    var questionIdx = 0
    var questionsNumber = 3

    class Connection(
        private val simulator: PlayerDemoConnectivitySimulator
    ): ConnectionProvider<GameMessage> {


        override lateinit var protocol: ConnectivityProtocol<GameMessage>
        override val messages = simulator.messages

        override suspend fun send(message: GameMessage) {
            when (message.type) {
                PLAYER_JOIN -> {
                    simulator.schedule(2) { simulator.simulateData(GameMessage(PLAYER_READY)) }
                }

                PLAYER_READY -> {
                    simulator.roundIdx = 1
                    simulator.schedule(2) {
                        simulator.simulateData(
                            GameMessage(
                                ROUND_START,
                                "Round ${simulator.roundIdx}"
                            )
                        )
                    }
                    simulator.questionIdx = 1
                    simulator.schedule(3) {
                        simulator.simulateData(
                            GameMessage(
                                QUESTION,
                                "Question ${simulator.questionIdx}",
                                listOf("A", "B", "C", "D")
                            )
                        )
                    }

                }

                ROUND_START -> {}
                ROUND_END -> {}
                QUESTION -> {}
                ANSWER -> {
                    simulator.questionIdx++
                    if (simulator.questionIdx > simulator.questionsNumber) {
                        simulator.questionIdx = 1
                        simulator.schedule(3) { simulator.simulateData(GameMessage(ROUND_END)) }
                    } else {
                        simulator.schedule(2) {
                            simulator.simulateData(
                                GameMessage(
                                    QUESTION,
                                    "Question ${simulator.questionIdx}",
                                    listOf("A", "B", "C", "D")
                                )
                            )
                        }
                    }
                }

                SUBMIT_ROUND -> {
                    simulator.roundIdx++
                    if (simulator.roundIdx > simulator.roundsNumber) {
                        simulator.schedule(3) { simulator.simulateData(GameMessage(GAME_OVER)) }
                    } else {
                        simulator.schedule(3) {
                            simulator.simulateData(
                                GameMessage(
                                    ROUND_START,
                                    "Round ${simulator.roundIdx}"
                                )
                            )
                        }
                        simulator.questionIdx = 1
                        simulator.schedule(4) {
                            simulator.simulateData(
                                GameMessage(
                                    QUESTION,
                                    "Question ${simulator.questionIdx}",
                                    listOf("A", "B", "C", "D")
                                )
                            )
                        }
                    }
                }

                GAME_OVER -> {}
            }
        }
        override suspend fun close() {
            Log.i(TAG, "close")
        }


    }

    private val timer = Timer()
    private fun schedule(ticks: Int = 1, action: () -> Unit) : Long {
        val delay = ticks * tickMs
        timer.schedule(object : TimerTask() {
            override fun run() {
                action()
            }
        }, delay)
        return delay
    }

    private fun simulateData(data: GameMessage) {
        Log.d(TAG, "simulateData: $data")
        try {
            runBlocking {
                messages.emit(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onReceiveData: ${e.message}", e)
        }
    }
}