package com.ahmetkupelikilinc.memorygame

import java.util.*
import kotlin.random.Random as KotlinRandom

data class DailyChallenge(
    val id: Int,
    val date: Date,
    val targetScore: Int,
    val maxMoves: Int,
    val timeLimit: Int,
    val specialEmojis: List<String>,
    var isCompleted: Boolean = false,
    var earnedStars: Int = 0
) {
    companion object {
        fun generateDailyChallenge(): DailyChallenge {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val random = KotlinRandom(dayOfYear)
            
            // Her gün için farklı zorluk seviyesi
            val targetScores = listOf(1000, 1500, 2000, 2500, 3000)
            val maxMoves = listOf(20, 25, 30, 35, 40)
            val timeLimits = listOf(120, 150, 180, 210, 240, 270, 300)
            
            val targetScore = targetScores[random.nextInt(targetScores.size)]
            val moves = maxMoves[random.nextInt(maxMoves.size)]
            val timeLimit = timeLimits[random.nextInt(timeLimits.size)]
            
            // Özel emoji seti
            val specialEmojis = listOf(
                "🌈", "🌞", "🌙", "⭐", "🌟", "🌍", "🚀", "🛸",
                "🎮", "🎲", "🎯", "🎪", "🎨", "🎭", "🎪", "🎢"
            ).shuffled(random).take(8)
            
            return DailyChallenge(
                id = dayOfYear,
                date = calendar.time,
                targetScore = targetScore,
                maxMoves = moves,
                timeLimit = timeLimit,
                specialEmojis = specialEmojis
            )
        }
    }
    
    fun calculateStars(score: Int, moves: Int, timeSpent: Int): Int {
        var stars = 0
        
        // Skor hedefine göre yıldız
        if (score >= targetScore) stars++
        if (score >= targetScore * 1.2) stars++
        
        // Hamle sayısına göre yıldız
        if (moves <= maxMoves) stars++
        
        // Süreye göre yıldız
        if (timeSpent <= timeLimit * 0.8) stars++
        if (timeSpent <= timeLimit * 0.6) stars++
        
        return stars
    }
} 