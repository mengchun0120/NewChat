package com.mcdane.newchat

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatViewAdapter(private val model: ChatViewModel): RecyclerView.Adapter<ChatViewAdapter.ChatViewHolder>() {

    companion object {
        const val LOCAL_VIEW = 0
        const val REMOTE_VIEW = 1
    }

    class ChatViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.name)
        val msgText: TextView = view.findViewById(R.id.msg)
        val timeText: TextView = view.findViewById(R.id.time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val layout = if (viewType == LOCAL_VIEW) {
            R.layout.local_chat_record
        } else {
            R.layout.remote_chat_record
        }
        val view = inflater.inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = model.numberOfRecords

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        with(holder) {
            nameText.text = model[position].name
            msgText.text = model[position].msg
            timeText.text = model[position].time.toString()
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (model[position].local) LOCAL_VIEW else REMOTE_VIEW

}