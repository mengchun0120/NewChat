package com.mcdane.newchat

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.channels.CompletionHandler

class ChatChannel(
    val isServer: Boolean,
    val nsdManager: NsdManager,
    private val coroutineScope: CoroutineScope,
) {
    interface Listener {
        fun onConnected()

        fun onFailure(errorMsg: String)

        fun onMsgReceived(msg: String)

        fun onMsgSent(msg: String)
    }

    private val _running = AtomicBoolean(false)
    private var _listener: Listener? = null
    private val _job: Job? = null
    private var _serverChannel: AsynchronousServerSocketChannel? = null
    private var _socketChannel: AsynchronousSocketChannel? = null
    private val _connectChannel = Channel<Unit>()
    private val _receiveChannel = Channel<String>()
    private val _receiveBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
    private val _sendBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
    private val _sendChannel = Channel<String>()
    private val _sendPending = AtomicBoolean(false)

    val isOpen: Boolean
        get() = _socketChannel?.isOpen ?: false

    val sendPending: Boolean
        get() = _sendPending.get()

    private val registrationListener = object: NsdManager.RegistrationListener {
        override fun onServiceRegistered(service: NsdServiceInfo?) {
            service?.let{
                Log.i(TAG, "Service registered $it")
                waitForClient()
            }
        }

        override fun onRegistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
            onFailure("Failed to register service $service. errorCode=$errorCode")
        }

        override fun onServiceUnregistered(service: NsdServiceInfo?) {
            Log.i(TAG, "Service $service unregistered")
        }

        override fun onUnregistrationFailed(service: NsdServiceInfo?, errorCode: Int) {
            onFailure("Failed to unregister service $service")
        }
    }

    private val resolveListener = object: NsdManager.ResolveListener {
        override fun onServiceResolved(service: NsdServiceInfo?) {
            service?.apply {
                connectToServer(InetSocketAddress(host, port))
            }
        }

        override fun onResolveFailed(service: NsdServiceInfo?, errorCode: Int) {
            onFailure("Failed to resolve service $service. errorCode=$errorCode")
        }
    }

    private val discoveryListener = object: NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String?) {
            Log.i(TAG, "Starting to discover service $serviceType")
        }

        override fun onServiceFound(service: NsdServiceInfo?) {
            service?.apply {
                if (!serviceType.startsWith(SERVICE_TYPE)) {
                    Log.e(TAG, "Unknown service $service")
                    return
                }

                if (serviceName.contains(SERVICE_NAME)) {
                    Log.i(TAG, "Service discovered successfully")
                    nsdManager.resolveService(service, resolveListener)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo?) {
            onFailure("Service $service lost")
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            onFailure("Failed to discover service $serviceType. errorCode=$errorCode")
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Log.i(TAG, "Stopped searching for service $serviceType")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            onFailure("Failed to stop service $serviceType. errorCode=$errorCode")
        }
    }

    private val _acceptHandler = object: CompletionHandler<AsynchronousSocketChannel, Unit> {
        override fun completed(result: AsynchronousSocketChannel?, attachment: Unit?) {
            if (result != null && result.isOpen) {
                _socketChannel = result
                coroutineScope.launch(Dispatchers.IO) {
                    _connectChannel.send(Unit)
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            Log.e(TAG, "Failed to listen to client: $exc")
        }
    }

    private val _connectHandler = object: CompletionHandler<Void, Unit> {
        override fun completed(result: Void?, attachment: Unit?) {
            coroutineScope.launch(Dispatchers.IO) {
                _connectChannel.send(Unit)
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            onFailure("Failed to connect to server: $exc")
        }
    }

    private val _receiveHandler = object: CompletionHandler<Int, Unit> {
        override fun completed(result: Int?, attachment: Unit?) {
            coroutineScope.launch(Dispatchers.IO) {
                if (result != null && result > 0) {
                    _receiveChannel.send(_receiveBuffer.decode(result))
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            onFailure("Failed to receive message: $exc")
        }
    }

    private val _sendHandler = object: CompletionHandler<Int, Unit> {
        override fun completed(result: Int?, attachment: Unit?) {
            if (result != null && result > 0) {
                val msg = _sendBuffer.decode(result)
                Log.i(TAG, "Sent message: $msg")
                _listener?.onMsgSent(msg)
            }
            _sendPending.set(false)
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            _sendPending.set(false)
            onFailure("Failed to send message")
        }

    }

    fun start() {
        if (isServer) {
            startAsServer()
        } else {
            startAsClient()
        }
    }

    fun send(msg: String): Boolean {
        if (_sendPending.get()) {
            Log.w(TAG, "Trying to send while writ is still pending")
            return false
        }

        coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Sending msg: $msg")
            _sendPending.set(true)
            _sendBuffer.put(msg)
            _socketChannel?.write(_sendBuffer, Unit, _sendHandler)
        }

        return true
    }

    fun setListener(listener: Listener) {
        _listener = listener
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

    private fun startAsClient() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun waitForClient() {
        coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Waiting for client")
            _serverChannel?.accept<Unit>(null, _acceptHandler)
            _connectChannel.receive()
            _listener?.onConnected()
            Log.i(TAG, "Connected to client")
            startReceiveLoop()
        }
    }

    private fun connectToServer(address: InetSocketAddress) {
        coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Connecting to $address")
            _socketChannel = AsynchronousSocketChannel.open()
            _socketChannel?.connect(address, Unit, _connectHandler)
            _connectChannel.receive()
            _listener?.onConnected()
            Log.i(TAG, "Connected to server")
            startReceiveLoop()
        }
    }

    private fun startReceiveLoop() {
        coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Starting receive loop")
            while (_running.get()) {
                _receiveBuffer.clear()
                _socketChannel?.read(_receiveBuffer, Unit, _receiveHandler)
                val msg = _receiveChannel.receive()
                Log.i(TAG, "Received $msg")
                _listener?.onMsgReceived(msg)
            }
        }
    }

    private fun onFailure(msg: String) {
        Log.e(TAG, msg)
        _listener?.onFailure(msg)
    }

    companion object {
        const val TAG = "ChatChannel"
        const val SERVICE_TYPE = "MyChatService"
        const val SERVICE_NAME = "_chat._tcp"
        const val BUFFER_CAPACITY = 2000
    }
}