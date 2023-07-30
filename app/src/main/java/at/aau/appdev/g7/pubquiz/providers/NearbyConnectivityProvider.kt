package at.aau.appdev.g7.pubquiz.providers

import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectionProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolMessage
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val basePermissions = listOf(
    android.Manifest.permission.ACCESS_WIFI_STATE,
    android.Manifest.permission.CHANGE_WIFI_STATE,
    android.Manifest.permission.BLUETOOTH,
    android.Manifest.permission.BLUETOOTH_ADMIN,
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION
)

@RequiresApi(Build.VERSION_CODES.S)
private val api31Permissions = listOf(
    android.Manifest.permission.BLUETOOTH_ADVERTISE,
    android.Manifest.permission.BLUETOOTH_CONNECT,
    android.Manifest.permission.BLUETOOTH_SCAN
)


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private val api32Permissions = listOf(
    android.Manifest.permission.NEARBY_WIFI_DEVICES
)

val nearbyProviderPermissions = basePermissions +
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) api31Permissions else listOf()) +
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) api32Permissions else listOf())

private const val SERVICE_ID = "at.aau.appdev.g7.pubquiz"
private const val TAG = "NearbyConnectivity"
private val STRATEGY = Strategy.P2P_STAR

class NearbyConnection<M: ProtocolMessage>(
    override var protocol: ConnectivityProtocol<M>,
    override val messages: SharedFlow<M>,
    private val scope: CoroutineScope,
    private val connectionClient: ConnectionsClient,
    private val connected: StateFlow<Boolean>,
    private val endpointId: String
) : ConnectionProvider<M> {

    override suspend fun send(message: M) {
        connected.first { it }

        connectionClient.sendPayload(endpointId, Payload.fromBytes(protocol.serialize(message).toByteArray()))
    }

    override suspend fun close() {
        connectionClient.disconnectFromEndpoint(endpointId)

        scope.cancel()
    }

}

class NearbyConnectivityProvider<M: ProtocolMessage>(context: Context) : ConnectivityProvider<M> {

    override lateinit var protocol: ConnectivityProtocol<M>

    private val _discoveredEndpoints = MutableStateFlow<Set<String>>(emptySet())
    override val discoveredEndpoints: StateFlow<Set<String>> = _discoveredEndpoints.asStateFlow()

    private val _initiatedConnections = MutableStateFlow<Set<String>>(emptySet())
    override val initiatedConnections: StateFlow<Set<String>> = _initiatedConnections.asStateFlow()

    private val _connections = MutableStateFlow<Set<String>>(emptySet())

    private val connectionsClient = Nearby.getConnectionsClient(context)

    companion object {
        private val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        private val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()
    }

    override suspend fun discover() {
        try {
            suspendCoroutine { continuation ->
                val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        Log.d(TAG, "onEndpointFound: $endpointId")
                        _discoveredEndpoints.value = _discoveredEndpoints.value + endpointId
                    }

                    override fun onEndpointLost(endpointId: String) {
                        Log.d(TAG, "onEndpointLost: $endpointId")
                        _discoveredEndpoints.value = _discoveredEndpoints.value - endpointId
                    }
                }

                connectionsClient.startDiscovery(
                    SERVICE_ID,
                    endpointDiscoveryCallback,
                    discoveryOptions
                )
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener {
                        continuation.resumeWithException(it)
                    }
            }

            awaitCancellation()
        } finally {
            connectionsClient.stopDiscovery()
        }
    }

    override suspend fun connect(endpointId: String): NearbyConnection<M> {
        if (!discoveredEndpoints.value.contains(endpointId)) {
            throw IllegalArgumentException("Endpoint $endpointId not found")
        }

        val connected = MutableStateFlow(false)
        val messages = MutableSharedFlow<M>()
        val scope = CoroutineScope(Dispatchers.IO)

        val payloadCallback = object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                Log.d(TAG, "onPayloadReceived: $endpointId")
                val message = protocol.deserialize(String(payload.asBytes()!!))

                scope.launch {
                    messages.emit(message)
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                Log.d(TAG, "onPayloadTransferUpdate: $endpointId")
            }
        }

        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                Log.d(TAG, "onConnectionInitiated: $endpointId")

                scope.launch {
                    suspendCoroutine {continuation ->
                        connectionsClient.acceptConnection(endpointId, payloadCallback)
                            .addOnSuccessListener {
                                continuation.resume(Unit)
                            }
                            .addOnFailureListener {
                                continuation.resumeWithException(it)
                            }
                    }
                }
            }

            override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
                Log.d(TAG, "onConnectionResult: $endpointId")
                when (resolution.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        connected.value = true
                    }
                    else -> {
                        connected.value = false
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                Log.d(TAG, "onDisconnected: $endpointId")
                connected.value = false
            }
        }

        suspendCoroutine {continuation ->
            connectionsClient.requestConnection(
                "Client",
                endpointId,
                connectionLifecycleCallback)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }

        connected.first { it }

        return NearbyConnection(
            protocol,
            messages.asSharedFlow(),
            scope,
            connectionsClient,
            connected.asStateFlow(),
            endpointId
        )
    }

    override suspend fun advertise() {
        try {
            suspendCoroutine { continuation ->
                val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                        Log.d(TAG, "onConnectionInitiated: $endpointId")
                        _initiatedConnections.value = _initiatedConnections.value + endpointId
                    }

                    override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
                        Log.d(TAG, "onConnectionResult: $endpointId")
                        _initiatedConnections.value = _initiatedConnections.value - endpointId

                        when (resolution.status.statusCode) {
                            ConnectionsStatusCodes.STATUS_OK -> {
                                _connections.value = _connections.value + endpointId
                            }
                            else -> {
                                _connections.value = _connections.value - endpointId
                            }
                        }
                    }

                    override fun onDisconnected(endpointId: String) {
                        Log.d(TAG, "onDisconnected: $endpointId")

                        _connections.value = _connections.value - endpointId
                    }
                }

                connectionsClient.startAdvertising(
                    "Server",
                    SERVICE_ID,
                    connectionLifecycleCallback,
                    advertisingOptions
                )
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener {
                        continuation.resumeWithException(it)
                    }
            }

            awaitCancellation()
        } finally {
            connectionsClient.stopDiscovery()
        }
    }

    override suspend fun accept(endpointId: String): NearbyConnection<M> {
        if (!initiatedConnections.value.contains(endpointId)) {
            throw IllegalArgumentException("Endpoint $endpointId not found")
        }

        val connected = MutableStateFlow(false)
        val messages = MutableSharedFlow<M>()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            _connections.onEach {
                connected.value = it.contains(endpointId)
            }.collect()
        }

        val payloadCallback = object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                Log.d(TAG, "onPayloadReceived: $endpointId")
                val message = protocol.deserialize(String(payload.asBytes()!!))

                scope.launch {
                    messages.emit(message)
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                Log.d(TAG, "onPayloadTransferUpdate: $endpointId")
            }
        }

        suspendCoroutine {continuation ->
            connectionsClient.acceptConnection(
                endpointId,
                payloadCallback)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }

        connected.first { it }

        return NearbyConnection(
            protocol,
            messages.asSharedFlow(),
            scope,
            connectionsClient,
            connected.asStateFlow(),
            endpointId
        )
    }
}
