package at.aau.appdev.g7.pubquiz.domain.interfaces

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ConnectionProvider<M: ProtocolMessage> {
    var protocol: ConnectivityProtocol<M>

    val messages: SharedFlow<M>

    suspend fun send(message: M)

    suspend fun close()
}

interface ConnectivityProvider<M: ProtocolMessage> {
    var protocol: ConnectivityProtocol<M>

    val discoveredEndpoints: StateFlow<Set<String>>
    val initiatedConnections: StateFlow<Set<String>>

    suspend fun advertise()
    suspend fun discover()

    suspend fun connect(endpointId: String): ConnectionProvider<M>
    suspend fun accept(endpointId: String): ConnectionProvider<M>
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