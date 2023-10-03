package com.mcdane.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import kotlinx.serialization.Serializable

@Serializable
data class ChatRecord(
    val name: String,
    val time: Date,
    val local: Boolean,
    val msg: String,
)

class ChatViewModel: ViewModel() {
    private val _records = ArrayList<ChatRecord>()

    val numberOfRecords: Int
        get() = _records.size

    fun addAll(records: Iterable<ChatRecord>) {
        _records.addAll(records)
    }

    fun add(record: ChatRecord) {
        _records.add(record)
    }

    operator fun get(index: Int): ChatRecord = _records[index]

    fun run(isServer: Boolean) {

    }
}