package at.aau.appdev.g7.pubquiz.demo

import android.util.Log
import at.aau.appdev.g7.pubquiz.domain.GameMessage
import at.aau.appdev.g7.pubquiz.domain.GameMessageType.*
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import java.util.Timer
import java.util.TimerTask

class PlayerDemoConnectivitySimulator(
    val tickMs: Long = 1000L
) : ConnectivityProvider<GameMessage> {
    companion object {
        const val TAG = "PlayerDemo"
    }

    override lateinit var protocol: ConnectivityProtocol<GameMessage>
    override lateinit var onReceiveData: (data: GameMessage) -> Unit

    override fun advertise() {
        throw RuntimeException("Player should not call advertise()")
    }

    override fun connect() {
        Log.i(TAG, "connect")
    }


    var roundIdx = 0
    var roundsNumber = 2
    var questionIdx = 0
    var questionsNumber = 3
    override fun sendData(data: GameMessage) {
        Log.i(TAG, "sendData: $data")
        when(data.type) {
            PLAYER_JOIN -> {
                schedule(2) { simulateData(GameMessage(PLAYER_READY)) }
            }
            PLAYER_READY -> {
                roundIdx = 1
                schedule(2) { simulateData(GameMessage(ROUND_START, "Round $roundIdx")) }
                questionIdx = 1
                schedule(3) { simulateData(GameMessage(QUESTION, "Question $questionIdx", listOf("A", "B", "C", "D"))) }

            }
            ROUND_START -> {}
            ROUND_END -> {}
            QUESTION -> {}
            ANSWER -> {
                questionIdx++
                if (questionIdx > questionsNumber) {
                    questionIdx = 1
                    schedule(3) { simulateData(GameMessage(ROUND_END)) }
                } else {
                    schedule(2) {
                        simulateData(
                            GameMessage(
                                QUESTION,
                                "Question questionIdx",
                                listOf("A", "B", "C", "D")
                            )
                        )
                    }
                }
            }
            SUBMIT_ROUND -> {
                roundIdx++
                if (roundIdx > roundsNumber) {
                    schedule(3) { simulateData(GameMessage(GAME_OVER)) }
                } else {
                    schedule(3) { simulateData(GameMessage(ROUND_START, "Round $roundIdx")) }
                    questionIdx = 1
                    schedule(4) { simulateData(GameMessage(QUESTION, "Question $questionIdx", listOf("A", "B", "C", "D"))) }
                }
            }
            GAME_OVER -> {}
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
            onReceiveData(data)
        } catch (e: Exception) {
            Log.e(TAG, "onReceiveData: ${e.message}", e)
        }
    }
}