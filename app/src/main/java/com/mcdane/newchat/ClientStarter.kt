package com.mcdane.newchat

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class ClientStarter(
    private val _coroutineScope: CoroutineScope,
    private val _listener: ConnectListener,
) {
    private var _socketChannel: AsynchronousSocketChannel? = null

    private val _connectHandler = object: CompletionHandler<Void, Unit> {
        override fun completed(result: Void?, attachment: Unit?) {
            _listener.onConnected(ChatChannel(_socketChannel!!, _coroutineScope))
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            val msg = "Failed to connect to server: $exc"
            Log.e(TAG, msg)
            _listener.onConnectFailure(msg)
        }
    }

    fun start(address: InetAddress): Boolean =
        try {
            _coroutineScope.launch(Dispatchers.IO) {
                val socketAddress = InetSocketAddress(address, ServerStarter.PORT)
                Log.i(TAG, "Connecting to $socketAddress")
                _socketChannel = AsynchronousSocketChannel.open().apply {
                    connect(socketAddress, Unit, _connectHandler)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start: $e")
            false
        }

    companion object {
        const val TAG = "ClientStarter"
    }
}