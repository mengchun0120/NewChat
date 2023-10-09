package com.mcdane.newchat

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel

val AsynchronousServerSocketChannel.port: Int
    get() {
        val address = this.localAddress as? InetSocketAddress ?: throw RuntimeException("Wrong address")
        return address.port
    }

fun ByteBuffer.decode(size: Int): String =
    array().decodeToString(arrayOffset(), arrayOffset() + size)

fun ByteBuffer.put(s: String) {
    clear()
    put(s.toByteArray())
    flip()
}