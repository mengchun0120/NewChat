package com.mcdane.newchat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class ServerStarter(
    private val _coroutineScope: CoroutineScope,
    private val _listener: ConnectListener,
) {
    private var _serverChannel: AsynchronousServerSocketChannel? = null

     private val _acceptHandler = object: CompletionHandler<AsynchronousSocketChannel, Unit> {
        override fun completed(result: AsynchronousSocketChannel?, attachment: Unit?) {
            if (result != null && result.isOpen) {
                Log.i(TAG, "Connected to client ${result.remoteAddress}")
                _listener.onConnected(ChatChannel(result, _coroutineScope))
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            val msg = "Failed to listen to client: $exc"
            Log.e(TAG, msg)
            _listener.onConnectFailure(msg)
        }
    }

    fun start(context: Context, ip: InetAddress): Boolean =
        try {
            _coroutineScope.launch(Dispatchers.IO) {
                val address = InetSocketAddress(ip, PORT)
                _serverChannel = AsynchronousServerSocketChannel.open().bind(address)
                Log.i(TAG, "Waiting for client")
                _serverChannel?.accept<Unit>(null, _acceptHandler)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server: $e")
            false
        }


    fun close() {
        _serverChannel?.close()
    }

    private fun waitForClient() {

    }

    companion object {
        const val TAG = "ServerStarter"
        const val PORT = 13999
    }
}