package com.mcdane.newchat

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel

val AsynchronousServerSocketChannel.port: Int
    get() {
        val address = this.localAddress as? InetSocketAddress ?: throw RuntimeException("Wrong address")
        return address.port
    }