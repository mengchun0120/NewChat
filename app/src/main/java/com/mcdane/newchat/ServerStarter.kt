package com.mcdane.newchat

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.atomic.AtomicBoolean

class ServerStarter(
    private val _nsdManager: NsdManager,
    private val _coroutineScope: CoroutineScope,
    private val _listener: Listener,
) {
    interface Listener {
        fun onConnected(channel: ChatChannel)

        fun onFailure(errorMsg: String)
    }

    private var _serverChannel: AsynchronousServerSocketChannel? = null
    private var _serviceRegistered = AtomicBoolean(false)
    private var _clientConnected = AtomicBoolean(false)

    private val registrationListener = object: NsdManager.RegistrationListener {
        override fun onServiceRegistered(service: NsdServiceInfo?) {
            service?.let{
                Log.i(TAG, "Service registered $it")
                waitForClient()
            }
        }

        override fun onRegistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
            val msg = "Failed to register service $service. errorCode=$errorCode"
            Log.e(TAG, msg)
            _listener.onFailure(msg)
        }

        override fun onServiceUnregistered(service: NsdServiceInfo?) {
            Log.i(TAG, "Service $service unregistered")
        }

        override fun onUnregistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
            val msg = "Failed to unregister service $service"
            Log.e(TAG, msg)
        }
    }

    private val _acceptHandler = object: CompletionHandler<AsynchronousSocketChannel, Unit> {
        override fun completed(result: AsynchronousSocketChannel?, attachment: Unit?) {
            if (result != null && result.isOpen) {
                _listener.onConnected(ChatChannel(result, _coroutineScope))
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            val msg = "Failed to listen to client: $exc"
            Log.e(TAG, msg)
            _listener.onFailure(msg)
        }
    }

    fun start() {
        _serverChannel = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))

        val service = NsdServiceInfo().apply {
            serviceType = SERVICE_TYPE
            serviceName = SERVICE_NAME
            port = _serverChannel!!.port
        }

        Log.i(TAG, "Registering service $service")

        _nsdManager.registerService(service, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun close() {
        _serverChannel?.close()
    }

    private fun waitForClient() {
        _coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Waiting for client")
            _serverChannel?.accept<Unit>(null, _acceptHandler)
        }
    }

    companion object {
        const val TAG = "ServerStarter"
        const val SERVICE_TYPE = "MyChatService"
        const val SERVICE_NAME = "_chat._tcp"
    }
}