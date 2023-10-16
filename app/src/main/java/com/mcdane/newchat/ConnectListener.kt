package com.mcdane.newchat

interface ConnectListener {
    fun onConnected(channel: ChatChannel)

    fun onConnectFailure(errorMsg: String)
}