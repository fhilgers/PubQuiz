package at.aau.appdev.g7.pubquiz.domain.interfaces

interface ConnectivityProvider<T: ProtocolMessage> {
    var protocol: ConnectivityProtocol<T>

    /**
     * Advertise new session
     */
    fun advertise()

    /**
     * Search for advertised session and connect
     */
    fun connect()

    /**
     * send data to other side, i.e.:
     * - player sends data to the master;
     * - master sends data to all players.
     */
    @Throws(ProtocolException::class)
    fun sendData(data: T)

    /**
     * receive data from other side, i.e.:
     * - player receives data from the master;
     * - master receives data from a player.
     *
     * integrator is encouraged to signal errors using ProtocolException
     */
    var onReceiveData: (data: T) -> Unit
}

/**
 * Connectivity protocol wrapper.
 */
interface ConnectivityProtocol<T: ProtocolMessage> {
    // TODO discuss/define serialization type String vs bytes
    fun serialize(data: T): String

    fun deserialize(data: String): T
}

interface ProtocolMessage {

}

class ProtocolException(message: String): Exception(message) {}