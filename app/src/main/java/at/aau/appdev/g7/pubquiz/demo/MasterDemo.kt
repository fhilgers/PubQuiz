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
import kotlin.random.Random

private const val TAG = "MasterDemo"

class MasterDemoConnectivitySimulator(
    val tickMs: Long = 1000L
) : ConnectivityProvider<GameMessage> {
    companion object {

        val PLAYERS = listOf(
            "Team Rocket",
            "Team Star",
            "Team Magma",
            "Team Flare",
        )
    }

    private val _initiatedConnections = MutableStateFlow(setOf<String>())
    override val initiatedConnections = _initiatedConnections

    private val _discoveredEndpoints = MutableStateFlow(setOf<String>())
    override val discoveredEndpoints = _discoveredEndpoints

    override lateinit var protocol: ConnectivityProtocol<GameMessage>

    private val timer = Timer()
    fun schedule(ticks: Int = 1, action: () -> Unit) : Long {
        val delay = ticks * tickMs
        timer.schedule(object : TimerTask() {
            override fun run() {
                action()
            }
        }, delay)
        return delay
    }

    fun simulateData(data: GameMessage) {
        Log.d(TAG, "simulateData: $data")
        try {
            runBlocking {
                messages.emit(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onReceiveData: ${e.message}", e)
        }
    }

    override suspend fun advertise() {
        Log.i(TAG, "advertise")

        // Only give one dummy connection which schedules the simulation messages
        _initiatedConnections.emit(setOf("player"))
    }

    override suspend fun discover() {
        Log.i(TAG, "discover")
        TODO("Server does not call discover")
    }

    override suspend fun connect(endpointId: String): ConnectionProvider<GameMessage> {
        Log.i(TAG, "connect")
        TODO("Server does not call connect")
    }

    override suspend fun accept(endpointId: String): ConnectionProvider<GameMessage> {
        Log.i(TAG, "accept")

        _initiatedConnections.emit(setOf())

        for (player in PLAYERS) {
            schedule(Random.nextInt(2, 2 + PLAYERS.size)) {
                simulateData(GameMessage(PLAYER_JOIN, player))
            }
        }

        return Connection(this)
    }

    var answers: MutableList<List<String>> = mutableListOf()
    var messages: MutableSharedFlow<GameMessage> = MutableSharedFlow()

    private class Connection(
        private val simulator: MasterDemoConnectivitySimulator,
    ): ConnectionProvider<GameMessage> {
        override lateinit var protocol: ConnectivityProtocol<GameMessage>
        override val messages = simulator.messages

        override suspend fun send(message: GameMessage) {
            Log.i(TAG, "send: $message")

            when(message.type) {
                PLAYER_JOIN -> {}
                PLAYER_READY -> {
                    for (player in PLAYERS) {
                        simulator.schedule(Random.nextInt(1, PLAYERS.size)) {
                            simulator.simulateData(GameMessage(PLAYER_READY, player))
                        }
                    }
                }

                ROUND_START -> {
                    simulator.answers = mutableListOf()
                }

                ROUND_END -> {
                    for (player in PLAYERS) {
                        simulator.schedule(
                            Random.nextInt(
                                5,
                                5 + 2 * PLAYERS.size
                            )
                        ) {
                            simulator.simulateData(
                                GameMessage(
                                    SUBMIT_ROUND,
                                    player,
                                    simulator.answers.map { it.random() })
                            )
                        }
                    }
                }

                QUESTION -> {
                    simulator.answers.add(message.answers!!)
                    for (player in PLAYERS) {
                        simulator.schedule(
                            Random.nextInt(
                                5,
                                5 + 2 * PLAYERS.size
                            )
                        ) {
                            simulator.simulateData(GameMessage(ANSWER, player))
                        }
                    }
                }

                ANSWER -> {}
                SUBMIT_ROUND -> {}
                GAME_OVER -> {}
                TIMER_STARTED -> {}
                TIMER_PAUSED -> {}
                TIMER_RESUMED -> {}
                TIMER_ENDED -> {}
            }
        }

        override suspend fun close() {
            Log.i(TAG, "close")
        }
    }
}