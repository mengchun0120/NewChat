package com.mcdane.newchat

import android.net.InetAddresses
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class ChatChannel(
    val isServer: Boolean,
    val nsdManager: NsdManager,
    private val coroutineScope: CoroutineScope,
) {
    interface Listener {
        fun onConnected()

        fun onFailure()

        fun onMsgReceived(record: ChatRecord)
    }

    private val _running = AtomicBoolean(false)
    private var _listener: Listener? = null
    private var _job: Job? = null
    private var _serverChannel: AsynchronousServerSocketChannel? = null
    private var _socketChannel: AsynchronousSocketChannel? = null
    private val _acceptChannel = Channel<AsynchronousSocketChannel>()
    private val _receiveChannel = Channel<ChatRecord>()
    private val _receiveBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)

    private val registrationListener = object: NsdManager.RegistrationListener {
        override fun onServiceRegistered(service: NsdServiceInfo?) {
            service?.let{
                Log.i(TAG, "Service registered $it")
                waitForClient()
            }
        }

        override fun onRegistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
            TODO("Not yet implemented")
        }

        override fun onServiceUnregistered(service: NsdServiceInfo?) {
            TODO("Not yet implemented")
        }

        override fun onUnregistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
            TODO("Not yet implemented")
        }
    }

    private val resolveListener = object: NsdManager.ResolveListener {
        override fun onResolveFailed(service: NsdServiceInfo?, errorCode: Int) {
            TODO("Not yet implemented")
        }

        override fun onServiceResolved(p0: NsdServiceInfo?) {
            TODO("Not yet implemented")
        }
    }

    private val discoveryListener = object: NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            TODO("Not yet implemented")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            TODO("Not yet implemented")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            TODO("Not yet implemented")
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            TODO("Not yet implemented")
        }

        override fun onServiceFound(service: NsdServiceInfo?) {
            TODO("Not yet implemented")
        }

        override fun onServiceLost(service: NsdServiceInfo?) {
            TODO("Not yet implemented")
        }

    }

    private val _acceptHandler = object: java.nio.channels.CompletionHandler<AsynchronousSocketChannel, Unit> {
        override fun completed(result: AsynchronousSocketChannel?, attachment: Unit?) {
            _serverChannel?.let { server ->
                if (server.isOpen) {
                    server.accept<Unit>(null, this)
                }
            }

            if (result != null && result.isOpen) {
                coroutineScope.launch(Dispatchers.IO) {
                    _acceptChannel.send(result)
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            Log.e(TAG, "Failed to listen to client: $exc")
        }
    }

    private val _receiveHandler = object: java.nio.channels.CompletionHandler<Int, Unit> {
        override fun completed(result: Int?, attachment: Unit?) {

        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            Log.e(TAG, "Failed to receive message: $exc")
        }

    }


    private fun startAsServer() {
        _serverChannel = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(0))

        val service = NsdServiceInfo().apply {
            serviceType = SERVICE_TYPE
            serviceName = SERVICE_NAME
            port = _serverChannel!!.port
        }

        Log.i(TAG, "Registering service $service")

        nsdManager.registerService(service, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun waitForClient() {
        _job = coroutineScope.launch(Dispatchers.IO) {
            _serverChannel?.accept<Unit>(null, _acceptHandler)
            _socketChannel = _acceptChannel.receive()
            _listener?.onConnected()
            waitForMessage()
        }
    }

    private fun waitForMessage() {
        coroutineScope.launch(Dispatchers.IO) {
            _running.set(true)
            while (_running.get()) {
                _receiveBuffer.clear()
                _socketChannel?.read(_receiveBuffer, Unit, _receiveHandler)
                _listener?.onMsgReceived(_receiveChannel.receive())
            }
        }
    }

    companion object {
        const val TAG = "ChatChannel"
        const val SERVICE_TYPE = "MyChatService"
        const val SERVICE_NAME = "_chat._tcp"
        const val BUFFER_CAPACITY = 2000
    }
}