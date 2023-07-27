package at.aau.appdev.g7.pubquiz.demo

import android.util.Log
import at.aau.appdev.g7.pubquiz.domain.GameMessage
import at.aau.appdev.g7.pubquiz.domain.GameMessageType.*
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider

import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

/*
class MasterDemoConnectivitySimulator(
    val tickMs: Long = 1000L
) : ConnectivityProvider<GameMessage> {
    companion object {
        const val TAG = "MasterDemo"
        val PLAYERS = listOf(
            "Team Rocket",
            "Team Star",
            "Team Magma",
            "Team Flare",
        )
    }

    override lateinit var protocol: ConnectivityProtocol<GameMessage>
    override lateinit var onReceiveData: (data: GameMessage) -> Unit

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
            onReceiveData(data)
        } catch (e: Exception) {
            Log.e(TAG, "onReceiveData: ${e.message}", e)
        }
    }

    override fun advertise() {
        Log.i(TAG, "advertise")
        for (player in PLAYERS) {
            schedule(Random.nextInt(2, 2 + PLAYERS.size)) {
                simulateData(GameMessage(PLAYER_JOIN, player))
            }
        }
    }

    override fun connect() {
        Log.i(TAG, "connect")
    }

    private var answers: MutableList<List<String>> = mutableListOf()
    override fun sendData(data: GameMessage) {
        Log.i(TAG, "sendData: $data")
        when(data.type) {
            PLAYER_JOIN -> {}
            PLAYER_READY -> {
                for (player in PLAYERS) {
                    schedule(Random.nextInt(1, PLAYERS.size)) {
                        simulateData(GameMessage(PLAYER_READY, player))
                    }
                }
            }
            ROUND_START -> {
                answers = mutableListOf()
            }
            ROUND_END -> {
                for (player in PLAYERS) {
                    schedule(Random.nextInt(5, 5 + 2*PLAYERS.size)) {
                        simulateData(GameMessage(SUBMIT_ROUND, player, answers.map { it.random() }))
                    }
                }
            }
            QUESTION -> {
                answers.add(data.answers!!)
                for (player in PLAYERS) {
                    schedule(Random.nextInt(5, 5 + 2*PLAYERS.size)) {
                        simulateData(GameMessage(ANSWER, player))
                    }
                }
            }
            ANSWER -> {}
            SUBMIT_ROUND -> {}
            GAME_OVER -> {}
        }
    }
}

 */
