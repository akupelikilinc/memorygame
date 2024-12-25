package com.ahmetkupelikilinc.memorygame

data class MemoryCard(
    val id: Int,
    val content: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)