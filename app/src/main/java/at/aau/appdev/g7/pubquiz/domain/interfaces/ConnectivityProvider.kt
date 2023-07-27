package at.aau.appdev.g7.pubquiz.domain.interfaces

interface ConnectivityProvider<T: ProtocolMessage> {
    var protocol: ConnectivityProtocol<T>

    /*
     * start advertising this endpoint to nearby devices
     */
    fun startAdvertising()

    /*
     * stop advertising this endpoint to nearby devices
     */
    fun stopAdvertising()

    /*
     * start discovering nearby devices and call
     *  - onEndpointFound when a new endpoint is found
     *  - onEndpointLost when an endpoint is lost
     */
    fun startDiscovery()

    /*
     * stop discovering nearby devices
     */
    fun stopDiscovery()

    /*
     * request connection to a discovered endpoint
     */
    fun requestConnection(endpointId: String)

    /*
     * accept a connection request from
     * onConnectionRequest callback
     */
    fun acceptConnection(endpointId: String)

    /*
     * reject a connection request from
     * onConnectionRequest callback
     */
    fun rejectConnection(endpointId: String)

    /*
     * disconnect from a connected endpoint
     */
    fun disconnect(endpointId: String)

    /*
     * onEndpointFound is called when discovering
     * nearby devices and a new endpoint is found
     */
    var onEndpointFound: (endpointId: String) -> Unit

    /*
     * onEndpointLost is called when discovering
     * nearby devices and an endpoint is lost
     */
    var onEndpointLost: (endpointId: String) -> Unit

    /*
     * onConnectionRequest is called on the advertising
     * endpoint when a discovering endpoint requests a connection
     */
    var onConnectionRequest: (endpointId: String) -> Unit

    /*
     * onConnected is called on both sides when a connection
     * is established
     */
    var onConnected: (endpointId: String) -> Unit

    /*
     * onDisconnected is called on both sides when a connection
     * is lost
     */
    var onDisconnected: (endpointId: String) -> Unit

    /**
     * send data to other side, i.e.:
     * - player sends data to the master;
     * - master sends data to all players.
     */
    @Throws(ProtocolException::class)
    fun sendData(endpointId: String, data: T)

    /**
     * receive data from other side, i.e.:
     * - player receives data from the master;
     * - master receives data from a player.
     *
     * integrator is encouraged to signal errors using ProtocolException
     */
    var onReceiveData: (endpointId: String, data: T) -> Unit
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