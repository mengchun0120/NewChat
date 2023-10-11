package com.mcdane.newchat

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.channels.CompletionHandler

class ChatChannel(
    private val _channel: AsynchronousSocketChannel,
    private val _coroutineScope: CoroutineScope,
) {
    interface Listener {
        fun onMsgReceived(errorMsg: String)

        fun onReceiveFailure(errorMsg: String)

        fun onMsgSent(errorMsg: String)

        fun onSendFailure(errorMsg: String)
    }

    private val _running = AtomicBoolean(false)
    private lateinit var _listener: Listener
    private var _job: Job? = null
    private val _receiveChannel = Channel<String>()
    private val _receiveBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
    private val _sendBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
    private val _sendPending = AtomicBoolean(false)

    val isOpen: Boolean
        get() = _channel.isOpen ?: false

    val sendPending: Boolean
        get() = _sendPending.get()


    private val _receiveHandler = object: CompletionHandler<Int, Unit> {
        override fun completed(result: Int?, attachment: Unit?) {
            _coroutineScope.launch(Dispatchers.IO) {
                if (result != null && result > 0) {
                    _receiveChannel.send(_receiveBuffer.decode(result))
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            val msg = "Failed to receive message: $exc"
            Log.e(TAG, msg)
            _listener.onReceiveFailure(msg)
        }
    }

    private val _sendHandler = object: CompletionHandler<Int, Unit> {
        override fun completed(result: Int?, attachment: Unit?) {
            if (result != null && result > 0) {
                val msg = _sendBuffer.decode(result)
                Log.i(TAG, "Sent message: $msg")
                _listener.onMsgSent(msg)
            }
            _sendPending.set(false)
        }

        override fun failed(exc: Throwable?, attachment: Unit?) {
            _sendPending.set(false)
            val msg = "Failed to send message"
            Log.e(TAG, msg)
            _listener.onSendFailure(msg)
        }
    }


    fun send(msg: String): Boolean {
        if (_sendPending.get()) {
            Log.w(TAG, "Trying to send while writ is still pending")
            return false
        }

        _coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Sending msg: $msg")
            _sendPending.set(true)
            _sendBuffer.put(msg)
            _channel.write(_sendBuffer, Unit, _sendHandler)
        }

        return true
    }

    fun setListener(listener: Listener) {
        _listener = listener
    }

    fun start() {
        _job = _coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Starting receive loop")
            _running.set(true)
            while (_running.get()) {
                _receiveBuffer.clear()
                _channel.read(_receiveBuffer, Unit, _receiveHandler)
                val msg = _receiveChannel.receive()
                Log.i(TAG, "Received $msg")
                _listener.onMsgReceived(msg)
            }
        }
    }

    companion object {
        const val TAG = "ChatChannel"
        const val BUFFER_CAPACITY = 2000
    }
}