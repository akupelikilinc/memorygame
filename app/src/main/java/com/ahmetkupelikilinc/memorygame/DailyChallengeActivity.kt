package com.ahmetkupelikilinc.memorygame

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DailyChallengeActivity : AppCompatActivity() {
    private lateinit var challenge: DailyChallenge
    private var currentStreak = 0
    
    companion object {
        const val CHALLENGE_COMPLETED = "challenge_completed"
        const val STARS_EARNED = "stars_earned"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_challenge)
        
        // Günlük görevi oluştur
        challenge = DailyChallenge.generateDailyChallenge()
        
        // Streak'i yükle
        currentStreak = loadStreak()
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        // Hedefleri göster
        findViewById<TextView>(R.id.targetScoreValue).text = challenge.targetScore.toString()
        findViewById<TextView>(R.id.maxMovesValue).text = challenge.maxMoves.toString()
        findViewById<TextView>(R.id.timeLimitValue).text = formatTime(challenge.timeLimit)
        
        // Streak'i göster
        findViewById<TextView>(R.id.streakText).text = "Current Streak: $currentStreak"
        
        // Yıldızları sıfırla
        resetStars()
    }
    
    private fun setupListeners() {
        findViewById<Button>(R.id.startChallengeButton).setOnClickListener {
            startChallenge()
        }
    }
    
    private fun startChallenge() {
        // Ana oyun aktivitesini başlat
        val intent = MainActivity.newIntent(this, challenge)
        startActivity(intent)
    }
    
    private fun resetStars() {
        val stars = listOf(
            findViewById<ImageView>(R.id.star1),
            findViewById<ImageView>(R.id.star2),
            findViewById<ImageView>(R.id.star3),
            findViewById<ImageView>(R.id.star4),
            findViewById<ImageView>(R.id.star5)
        )
        
        stars.forEach { it.setImageResource(R.drawable.ic_star_empty) }
    }
    
    private fun updateStars(count: Int) {
        val stars = listOf(
            findViewById<ImageView>(R.id.star1),
            findViewById<ImageView>(R.id.star2),
            findViewById<ImageView>(R.id.star3),
            findViewById<ImageView>(R.id.star4),
            findViewById<ImageView>(R.id.star5)
        )
        
        stars.forEachIndexed { index, star ->
            star.setImageResource(
                if (index < count) R.drawable.ic_star_filled
                else R.drawable.ic_star_empty
            )
        }
    }
    
    private fun loadStreak(): Int {
        return getSharedPreferences("daily_challenge", MODE_PRIVATE)
            .getInt("current_streak", 0)
    }
    
    private fun saveStreak(streak: Int) {
        getSharedPreferences("daily_challenge", MODE_PRIVATE)
            .edit()
            .putInt("current_streak", streak)
            .apply()
    }
    
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
} 