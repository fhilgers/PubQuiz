package at.aau.appdev.g7.pubquiz.providers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProtocol
import at.aau.appdev.g7.pubquiz.domain.interfaces.ConnectivityProvider
import at.aau.appdev.g7.pubquiz.domain.interfaces.ProtocolMessage
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.util.UUID



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


class NearbyConnectivityProvider<T: ProtocolMessage>(context: Context): ConnectivityProvider<T> {

    override lateinit var protocol: ConnectivityProtocol<T>

    override lateinit var onEndpointFound: (endpointId: String) -> Unit
    override lateinit var onEndpointLost: (endpointId: String) -> Unit

    override lateinit var onConnectionRequest: (endpointId: String) -> Unit
    override lateinit var onConnected: (endpointId: String) -> Unit
    override lateinit var onDisconnected: (endpointId: String) -> Unit

    override lateinit var onReceiveData: (endpointId: String, data: T) -> Unit

    companion object {
        const val SERVICE_ID = "at.aau.appdev.g7.pubquiz"
        private val STRATEGY = Strategy.P2P_STAR

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val providerID = UUID.randomUUID().toString()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            onConnectionRequest(endpointId)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    onConnected(endpointId)
                }
                else -> {}
            }
        }

        override fun onDisconnected(endpointId: String) {
            onDisconnected(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            onEndpointFound(endpointId)
        }

        override fun onEndpointLost(endpointId: String) {
            onEndpointLost(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val data = protocol.deserialize(String(payload.asBytes()!!))
            onReceiveData(endpointId, data)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        }
    }

    override fun startAdvertising() {
        connectionsClient.startAdvertising(
            providerID,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        )
    }

    override fun stopAdvertising() {
        connectionsClient.stopAdvertising()
    }

    override fun startDiscovery() {
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        )
    }

    override fun stopDiscovery() {
        connectionsClient.stopDiscovery()
    }

    override fun requestConnection(endpointId: String) {
        connectionsClient.requestConnection(
            providerID,
            endpointId,
            connectionLifecycleCallback
        )
    }

    override fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    override fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
    }

    override fun disconnect(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
    }

    override fun sendData(endpointId: String, data: T) {
        val payload = Payload.fromBytes(protocol.serialize(data).toByteArray())
        connectionsClient.sendPayload(endpointId, payload)
    }
}