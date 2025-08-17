package com.example.letsgetweddi.data

data class Conversation(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val lastText: String = "",
    val lastTs: Long = 0
)
