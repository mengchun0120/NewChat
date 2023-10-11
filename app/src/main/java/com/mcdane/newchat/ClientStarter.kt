package com.mcdane.newchat

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class ClientStarter(
    private val _nsdManager: NsdManager,
    private val _coroutineScope: CoroutineScope,
    private val _listener: Listener,
) {
    
    interface Listener {
        fun onConnected(channel: ChatChannel)

        fun onFailure(errorMsg: String)
    }

    private val resolveListener = object: NsdManager.ResolveListener {
        override fun onServiceResolved(service: NsdServiceInfo?) {
            service?.apply {
                connectToServer(InetSocketAddress(host, port))
            }
        }

        override fun onResolveFailed(service: NsdServiceInfo?, errorCode: Int) {
            val msg = "Failed to resolve service $service. errorCode=$errorCode"
            Log.e(TAG, msg)
            _listener.onFailure(msg)
        }
    }

    private val discoveryListener = object: NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String?) {
            Log.i(TAG, "Starting to discover service $serviceType")
        }

        override fun onServiceFound(service: NsdServiceInfo?) {
            service?.apply {
                if (!serviceType.startsWith(ServerStarter.SERVICE_TYPE)) {
                    Log.e(TAG, "Unknown service $service")
                    return
                }

                if (serviceName.contains(ServerStarter.SERVICE_NAME)) {
                    Log.i(TAG, "Service discovered successfully")
                    _nsdManager.resolveService(service, resolveListener)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo?) {
            val msg = "Service $service lost"
            Log.e(TAG, msg)
            _listener.onFailure(msg)
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            val msg = "Failed to discover service $serviceType. errorCode=$errorCode"
            Log.e(TAG, msg)
            _listener.onFailure(msg)
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Log.i(TAG, "Stopped searching for service $serviceType")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            val msg = "Failed to stop service $serviceType. errorCode=$errorCode"
            Log.e(TAG, msg)
        }
    }

    private val _connectHandler = object: CompletionHandler<Void, AsynchronousSocketChannel> {
        override fun completed(result: Void?, attachment: AsynchronousSocketChannel?) {
            if (attachment != null) {
                _listener.onConnected(ChatChannel(attachment, _coroutineScope))
            }
        }

        override fun failed(exc: Throwable?, attachment: AsynchronousSocketChannel?) {
            val msg = "Failed to connect to server: $exc"
            Log.e(TAG, msg)
            _listener.onFailure(msg)
        }
    }

    private fun connectToServer(address: InetSocketAddress) {
        _coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Connecting to $address")
            val socketChannel = AsynchronousSocketChannel.open()
            socketChannel.connect(address, socketChannel, _connectHandler)
        }
    }

    fun start() {
        _nsdManager.discoverServices(
            ServerStarter.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    companion object {
        const val TAG = "ClientStarter"
    }
}