package com.mcdane.newchat

import android.net.nsd.NsdManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.InetAddress
import java.util.*

class MainActivity : AppCompatActivity(), ConnectListener, ChatChannel.Listener {
    private lateinit var _viewModel: ChatViewModel
    private var _localIP: InetAddress? = null
    private lateinit var _ipText: TextView
    private lateinit var _statusText: TextView
    private lateinit var _cancelButton: Button
    private lateinit var _disconnectButton: Button
    private lateinit var _runAsServerButton: Button
    private lateinit var _runAsClientButton: Button
    private lateinit var _msgList: RecyclerView
    private lateinit var _msgEdit: EditText
    private lateinit var _sendButton: Button
    private var _serverStarter: ServerStarter? = null
    private var _clientStarter: ClientStarter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initModel()
        initUI()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initModel() {
        _viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    private fun initUI() {
        _localIP = findLocalIPv4(this)

        _ipText = findViewById(R.id.ip)
        _ipText.text = _localIP?.hostAddress ?: "Unknown Address"

        _statusText = findViewById(R.id.status)

        _cancelButton = findViewById(R.id.cancel_button)
        _cancelButton.visibility = View.GONE
        _cancelButton.setOnClickListener{ onCancelClicked() }

        _disconnectButton = findViewById(R.id.disconnect_button)
        _disconnectButton.visibility = View.GONE
        _disconnectButton.setOnClickListener{ onDisconnectClicked() }

        _runAsServerButton = findViewById(R.id.run_as_server_button)
        _runAsServerButton.visibility = View.VISIBLE
        _runAsServerButton.isEnabled = _localIP != null
        _runAsServerButton.setOnClickListener{ onRunAsServerClicked() }

        _runAsClientButton = findViewById(R.id.run_as_client_button)
        _runAsClientButton.visibility = View.VISIBLE
        _runAsClientButton.isEnabled = _localIP != null
        _runAsClientButton.setOnClickListener{ onRunAsClientClicked() }

        _msgList = findViewById(R.id.chat_records)
        _msgList.adapter = ChatViewAdapter(_viewModel)
        _msgList.layoutManager = LinearLayoutManager(this)

        _msgEdit = findViewById(R.id.msg_edit)
        _msgEdit.isEnabled = false

        _sendButton = findViewById(R.id.send_button)
        _sendButton.isEnabled = false
        _sendButton.setOnClickListener{ onSendClicked() }
    }

    private fun onCancelClicked() {
        TODO("Not yet implemented")
    }

    private fun onDisconnectClicked() {
        TODO("Not yet implemented")
    }

    private fun onRunAsServerClicked() {
        _serverStarter = ServerStarter(_viewModel.viewModelScope, this)
        if (_serverStarter!!.start(this, _localIP!!)) {
            _statusText.setText(R.string.waiting_for_client)
            _runAsServerButton.visibility = View.GONE
            _runAsClientButton.visibility = View.GONE
            _disconnectButton.visibility = View.GONE
            _cancelButton.visibility = View.VISIBLE
        } else {
            _statusText.setText(R.string.failed_to_start_server)
        }
    }

    private fun onRunAsClientClicked() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.enter_server_ip)

            val view = layoutInflater.inflate(R.layout.get_server_ip, null)
            setView(view)

            val ipText = view.findViewById<EditText>(R.id.server_ip)

            setPositiveButton(R.string.ok) { dialog, _ ->
                startClient(ipText.text.toString())
                dialog.dismiss()
            }

            setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }

        }.show()
    }

    private fun startClient(ipStr: String) {
        val ip = ipStr.trim()
        if (!validateIP(ip)) {
            Toast.makeText(this, R.string.invalid_ip, Toast.LENGTH_SHORT).show()
            return
        }

        _clientStarter = ClientStarter(_viewModel.viewModelScope, this)
        if (_clientStarter!!.start(InetAddress.getByName(ip))) {
            _statusText.setText(R.string.connecting_to_server)
            _runAsServerButton.visibility = View.GONE
            _runAsClientButton.visibility = View.GONE
            _disconnectButton.visibility = View.GONE
            _cancelButton.visibility = View.VISIBLE
        } else {
            _statusText.setText(R.string.failed_to_start_client)
        }
    }

    private fun onSendClicked() {

    }

    override fun onConnected(channel: ChatChannel) {
        channel.setListener(this)
        _viewModel.channel = channel
        channel.start()

        runOnUiThread {
            _msgEdit.isEnabled = true
            _sendButton.isEnabled = true
            _cancelButton.visibility = View.GONE
            _disconnectButton.visibility = View.VISIBLE
        }
    }

    override fun onConnectFailure(errorMsg: String) {
        runOnUiThread {
            _statusText.text = errorMsg
            _runAsServerButton.visibility = View.VISIBLE
            _runAsClientButton.visibility = View.VISIBLE
            _disconnectButton.visibility = View.GONE
            _cancelButton.visibility = View.GONE
        }
    }

    override fun onMsgReceived(msg: String) {
        TODO("Not yet implemented")
    }

    override fun onReceiveFailure(errorMsg: String) {
        TODO("Not yet implemented")
    }

    override fun onMsgSent(msg: String) {
        TODO("Not yet implemented")
    }

    override fun onSendFailure(errorMsg: String) {
        TODO("Not yet implemented")
    }

    private fun validateIP(ipStr: String): Boolean =
        try {
            var partCount = 0
            for (part in ipStr.splitToSequence(".")) {
                if (part.toInt() !in 0..255) {
                    throw RuntimeException("Invalid IP")
                }
                ++partCount
            }
            if (partCount != 4) {
                throw RuntimeException("Invalid IP")
            }
            true
        } catch (e: Exception) {
            Log.i(TAG, "Invalid IP")
            Toast.makeText(this, R.string.invalid_ip, Toast.LENGTH_SHORT).show()
            false
        }

    companion object {
        const val TAG = "MainActivity"
    }

}