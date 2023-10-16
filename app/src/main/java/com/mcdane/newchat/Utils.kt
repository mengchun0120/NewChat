package com.mcdane.newchat

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel

private const val TAG = "Utils"

val AsynchronousServerSocketChannel.port: Int
    get() {
        val address = this.localAddress as? InetSocketAddress ?: throw RuntimeException("Wrong address")
        return address.port
    }

val AsynchronousServerSocketChannel.address: InetAddress
    get() {
        val addr = this.localAddress as? InetSocketAddress ?: throw RuntimeException("Wrong address")
        return addr.address
    }

fun ByteBuffer.decode(size: Int): String =
    array().decodeToString(arrayOffset(), arrayOffset() + size)

fun ByteBuffer.put(s: String) {
    clear()
    put(s.toByteArray())
    flip()
}

fun findLocalIPv4(context: Context): InetAddress? =
    context.getSystemService(ConnectivityManager::class.java).run {
        getLinkProperties(activeNetwork)
    }?.linkAddresses?.find { it.address.address.size == 4 }?.address

fun validateIP(ipStr: String): Boolean {
    var partCount = 0

    try {
        for (part in ipStr.splitToSequence(".")) {
            if (part.toInt() !in 0..255) {
                return false
            }
            ++partCount
        }
    } catch (e: Exception){
        Log.e(TAG, "validateIP failed: $e")
        return false
    }

    return partCount == 4
}