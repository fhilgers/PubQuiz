package at.aau.appdev.g7.pubquiz.demo

import android.util.Log
import at.aau.appdev.g7.pubquiz.domain.GameMessage
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


    override fun sendData(data: GameMessage) {
        TODO("Not yet implemented")
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
}