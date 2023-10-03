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
    private lateinit var viewModel: ChatViewModel
    private lateinit var statusText: TextView
    private lateinit var cancelButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var runAsServerButton: Button
    private lateinit var runAsClientButton: Button
    private lateinit var msgList: RecyclerView
    private lateinit var msgEdit: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initModel()
        initUI()
    }

    private fun initModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        val records = listOf(
            ChatRecord("John", Date(), true, "Hello this is me"),
            ChatRecord("Willow", Date(), false, "I am here. Come get me"),
            ChatRecord("John", Date(), true, "Sorry, I cannot go there"),
            ChatRecord("Willow", Date(), false, "I will go to your place"),
            ChatRecord("Willow", Date(), false, "No. I will send you a message"),
            ChatRecord("Willow", Date(), false, "OK. I sent you a message"),
            ChatRecord("John", Date(), true, "Fine. I got it"),
        )
        viewModel.addAll(records)
    }

    private fun initUI() {
        statusText = findViewById(R.id.status)

        cancelButton = findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener{ onCancelClicked() }

        disconnectButton = findViewById(R.id.disconnect_button)
        disconnectButton.setOnClickListener{ onDisconnectClicked() }

        runAsServerButton = findViewById(R.id.run_as_server_button)
        runAsServerButton.setOnClickListener{ onRunAsServerClicked() }

        runAsClientButton = findViewById(R.id.run_as_client_button)
        runAsClientButton.setOnClickListener{ onRunAsClientClicked() }

        msgList = findViewById(R.id.chat_records)
        msgList.adapter = ChatViewAdapter(viewModel)
        msgList.layoutManager = LinearLayoutManager(this)

        msgEdit = findViewById(R.id.msg_edit)

        sendButton = findViewById(R.id.send_button)
        sendButton.setOnClickListener{ onSendClicked() }
    }

    private fun onCancelClicked() {

    }

    private fun onDisconnectClicked() {

    }

    private fun onRunAsServerClicked() {
        statusText.setText(R.string.waiting_for_client)
        runAsServerButton.visibility = View.GONE
        runAsClientButton.visibility = View.GONE
        disconnectButton.visibility = View.GONE
        cancelButton.visibility = View.VISIBLE
        viewModel.run(true)
    }

    private fun onRunAsClientClicked() {

    }

    private fun onSendClicked() {

    }
}