package com.ahmetkupelikilinc.memorygame

data class MemoryCard(
    val imageId: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)