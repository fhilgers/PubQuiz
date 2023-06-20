package at.aau.appdev.g7.pubquiz.domain.interfaces

interface ConnectivityProvider {
    val protocol: ConnectivityProtocol

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
    fun sendData(data: ProtocolMessage)

    /**
     * receive data from other side, i.e.:
     * - player receives data from the master;
     * - master receives data from a player.
     */
    val onReceiveData: (data: ProtocolMessage) -> Unit
}

/**
 * Connectivity protocol wrapper.
 */
interface ConnectivityProtocol {
    // TODO discuss/define serialization type String vs bytes
    fun serialize(data: ProtocolMessage): String

    fun deserialize(data: String): ProtocolMessage
}

interface ProtocolMessage {

}