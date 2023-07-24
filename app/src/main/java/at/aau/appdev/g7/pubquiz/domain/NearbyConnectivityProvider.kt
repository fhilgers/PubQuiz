package at.aau.appdev.g7.pubquiz.domain

import android.content.Context
import android.nfc.Tag
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
import com.google.android.gms.nearby.connection.ConnectionType
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.util.UUID

const val TAG = "NearbyConnectivityProvider"
const val SERVICE_ID = "at.aau.appdev.g7.pubquiz"
val STRATEGY = Strategy.P2P_STAR

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


class NearbyConnectivityProvider<T: ProtocolMessage>(val context: Context): ConnectivityProvider<T> {

    override lateinit var protocol: ConnectivityProtocol<T>
    override lateinit var onReceiveData: (data: T) -> Unit

    private val providerID = UUID.randomUUID().toString()
    private val endpoints = mutableSetOf<String>()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(TAG, "onPayloadReceived: $endpointId, $payload")
            val data = payload.asBytes()?.let { protocol.deserialize(String(it)) }
            if (data != null) {
                onReceiveData(data)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG, "onPayloadTransferUpdate: $endpointId, $update")
            // Bytes are sent as a single chunk, so you'll receive a SUCCESS update immediately
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Log.d(TAG, "onPayloadTransferUpdate: success")
                }
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    Log.d(TAG, "onPayloadTransferUpdate: in progress")
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.d(TAG, "onPayloadTransferUpdate: failure")
                }
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "onConnectionInitiated: $endpointId, $connectionInfo")
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "onConnectionResult: $endpointId, $result")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpoints.add(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // The connection was rejected by one or both sides.
                    Log.d(TAG, "onConnectionResult: rejected")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    // The connection broke before it was able to be accepted.
                    Log.d(TAG, "onConnectionResult: error")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "onDisconnected: $endpointId")
            endpoints.remove(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound: $endpointId, $info")
            Nearby.getConnectionsClient(context).requestConnection(
                providerID,
                endpointId,
                connectionLifecycleCallback
            ).addOnSuccessListener { _ ->
                Log.d(TAG, "requestConnection: success")
            }.addOnFailureListener { e ->
                Log.e(TAG, "requestConnection: ${e.message}", e)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "onEndpointLost: $endpointId")
        }
    }

    override fun connect() {

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

        Nearby.getConnectionsClient(context).startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener { _ ->
            Log.d(TAG, "startDiscovery: success")
        }.addOnFailureListener { e ->
            Log.e(TAG, "startDiscovery: ${e.message}", e)
        }
    }

    override fun sendData(data: T) {
        endpoints.forEach { endpointId ->
            Nearby.getConnectionsClient(context).sendPayload(endpointId, Payload.fromBytes(protocol.serialize(data).toByteArray()))
        }
    }

    override fun advertise() {

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()

        Nearby.getConnectionsClient(context).startAdvertising(
            providerID,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener { _ ->
            Log.d(TAG, "startAdvertising: success")
        }.addOnFailureListener { e ->
            Log.e(TAG, "startAdvertising: ${e.message}", e)
        }
    }
}