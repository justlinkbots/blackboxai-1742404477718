package com.example.filetransfer.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.example.filetransfer.data.Server
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class ServerDiscoveryManager(context: Context) {
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_http._tcp."
    private val discoveredServers = ConcurrentHashMap<String, Server>()
    
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.e("Failed to start discovery: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.e("Failed to stop discovery: $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) {
            Timber.d("Service discovery started")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Timber.d("Service discovery stopped")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Timber.d("Service found: ${serviceInfo.serviceName}")
            nsdManager.resolveService(serviceInfo, createResolveListener())
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Timber.d("Service lost: ${serviceInfo.serviceName}")
            discoveredServers.remove(serviceInfo.serviceName)
            updateServersList()
        }
    }

    private fun createResolveListener() = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Timber.e("Failed to resolve service: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Timber.d("Service resolved: ${serviceInfo.serviceName}")
            val server = Server(
                id = serviceInfo.serviceName,
                name = serviceInfo.serviceName,
                address = serviceInfo.host.hostAddress ?: "",
                port = serviceInfo.port
            )
            discoveredServers[serviceInfo.serviceName] = server
            updateServersList()
            
            // Verify server availability
            verifyServerAvailability(server)
        }
    }

    private fun updateServersList() {
        _servers.value = discoveredServers.values.toList()
    }

    private fun verifyServerAvailability(server: Server) {
        // Create API client and check server status
        val apiClient = FileTransferApiClient.create(server.fullAddress)
        apiClient.checkStatus(
            onSuccess = {
                discoveredServers[server.id]?.isAvailable = true
                updateServersList()
            },
            onError = {
                discoveredServers[server.id]?.isAvailable = false
                updateServersList()
            }
        )
    }

    fun startDiscovery() {
        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start discovery")
        }
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop discovery")
        }
    }
}