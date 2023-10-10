package com.mcdane.newchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var _viewModel: ChatViewModel
    private lateinit var _statusText: TextView
    private lateinit var _cancelButton: Button
    private lateinit var _disconnectButton: Button
    private lateinit var _runAsServerButton: Button
    private lateinit var _runAsClientButton: Button
    private lateinit var _msgList: RecyclerView
    private lateinit var _msgEdit: EditText
    private lateinit var _sendButton: Button

    private val channelListener = object: ChatChannel.Listener {
        override fun onConnected() {

        }

        override fun onStartServerFailure(errorMsg: String) {

        }

        override fun onConnectToServerFailure(errorMsg: String) {

        }


        override fun onMsgReceived(errorMsg: String) {

        }

        override fun onReceiveFailure(errorMsg: String) {

        }

        override fun onMsgSent(errorMsg: String) {

        }

        override fun onSendFailure(errorMsg: String) {

        }
    }

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
        _statusText = findViewById(R.id.status)

        _cancelButton = findViewById(R.id.cancel_button)
        _cancelButton.visibility = View.GONE
        _cancelButton.setOnClickListener{ onCancelClicked() }

        _disconnectButton = findViewById(R.id.disconnect_button)
        _disconnectButton.visibility = View.GONE
        _disconnectButton.setOnClickListener{ onDisconnectClicked() }

        _runAsServerButton = findViewById(R.id.run_as_server_button)
        _runAsServerButton.visibility = View.VISIBLE
        _runAsServerButton.setOnClickListener{ onRunAsServerClicked() }

        _runAsClientButton = findViewById(R.id.run_as_client_button)
        _runAsClientButton.visibility = View.VISIBLE
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

    }

    private fun onDisconnectClicked() {

    }

    private fun onRunAsServerClicked() {
        _statusText.setText(R.string.waiting_for_client)
        _runAsServerButton.visibility = View.GONE
        _runAsClientButton.visibility = View.GONE
        _disconnectButton.visibility = View.GONE
        _cancelButton.visibility = View.VISIBLE

    }

    private fun onRunAsClientClicked() {

    }

    private fun onSendClicked() {

    }

    companion object {
        const val TAG = "MainActivity"
    }
}