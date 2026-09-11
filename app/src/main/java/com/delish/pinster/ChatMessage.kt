package com.delish.pinster

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFailed: Boolean = false,
    val isUserImage: Boolean = false,
    val userImagePath: String? = null
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
