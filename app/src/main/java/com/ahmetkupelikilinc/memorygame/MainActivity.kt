package com.ahmetkupelikilinc.memorygame

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var cards: List<MemoryCard>
    private var firstSelectedCard: Int? = null
    private var moves = 0
    private var score = 0
    private var highScore = 0
    private var currentLevel = 1
    private var maxLevel = 5
    private var gameTime = 0
    private var isGameActive = false
    private lateinit var gridLayout: GridLayout
    private var isAnimating = false
    private var comboCount = 0
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    private var cardFlipSound: MediaPlayer? = null
    private var matchSuccessSound: MediaPlayer? = null
    private var levelCompleteSound: MediaPlayer? = null
    private var backgroundMusic: MediaPlayer? = null
    private var isSoundEnabled = false
    private var isMusicEnabled = false

    private val allEmojis = listOf(
        // Seviye 1 - Hayvanlar
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🦁", "🐯", "🐮", "🐷",
        // Seviye 2 - Meyveler ve Yiyecekler
        "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍒", "🥝", "🍍",
        // Seviye 3 - Deniz Canlıları
        "🐋", "🐳", "🐟", "🐠", "🦈", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐬",
        // Seviye 4 - Spor ve Aktiviteler
        "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🏸", "⛳", "🎯", "🎮",
        // Seviye 5 - Uzay ve Gökyüzü
        "🌞", "🌙", "⭐", "🌟", "☄️", "🌍", "🚀", "🛸", "🌈", "⛅", "❄️", "🌪️"
    )

    private fun getEmojisForLevel(level: Int): List<String> {
        val startIndex = when(level) {
            1 -> 0     // Animals
            2 -> 12    // Fruits & Food
            3 -> 24    // Sea Creatures
            4 -> 36    // Sports
            else -> 48 // Space
        }
        
        val pairsNeeded = when(level) {
            1, 2 -> 8     // 4x4 grid (16 cards)
            3, 4 -> 10    // 5x4 grid (20 cards)
            else -> 12    // 6x4 grid (24 cards)
        }
        
        return allEmojis.subList(startIndex, startIndex + pairsNeeded)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            // Hint butonunu başlangıçta gizle
            findViewById<Button>(R.id.hintButton).visibility = View.GONE

        gridLayout = findViewById(R.id.gridLayout)
        loadHighScore()
            setupButtons()
            setupTimer()
            setupSounds()
            
            // Oyunu başlat
            isGameActive = true
        setupGame()
            startTimer()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting the game", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupButtons() {
        try {
            findViewById<Button>(R.id.restartButton).apply {
                setBackgroundResource(R.drawable.button_background)
                setTextColor(android.graphics.Color.WHITE)
                elevation = 4f
                textSize = 16f
                setPadding(48, 24, 48, 24)
                text = "Restart"
                setOnClickListener {
                    resetGame()
                }
            }

            findViewById<Button>(R.id.hintButton).apply {
                setBackgroundResource(R.drawable.button_background)
                setTextColor(android.graphics.Color.WHITE)
                elevation = 4f
                setOnClickListener {
                    showHint()
                }
            }

            findViewById<ImageButton>(R.id.soundButton).apply {
                setOnClickListener {
                    toggleSound()
                }
            }

            findViewById<ImageButton>(R.id.musicButton).apply {
                setOnClickListener {
                    toggleMusic()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    gameTime++
                    updateTimerText()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startTimer() {
        isGameActive = true
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        isGameActive = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerText() {
        val minutes = gameTime / 60
        val seconds = gameTime % 60
        findViewById<TextView>(R.id.timerTextView).text = 
            String.format("Time: %02d:%02d", minutes, seconds)
    }

    private fun showHint() {
        // Oyun başlamadan önce ipucu çalışmasın
        if (!isGameActive || isAnimating) return
        
        isAnimating = true
        
        // Her ipucu kullanımında 50 puan düş
        score = maxOf(0, score - 50)
        updateScoreText()
        
        // Tüm kartları göster
        cards.forEachIndexed { index, card ->
            if (!card.isMatched) {
                val button = gridLayout.getChildAt(index) as Button
                button.text = card.content
                button.setBackgroundResource(R.drawable.card_background_flipped)
            }
        }

        // 2 saniye sonra kartları kapat
        Handler(Looper.getMainLooper()).postDelayed({
            cards.forEachIndexed { index, card ->
                if (!card.isMatched) {
                    val button = gridLayout.getChildAt(index) as Button
                    button.text = ""
                    button.setBackgroundResource(R.drawable.card_background)
                }
            }
            isAnimating = false
        }, 2000)
    }

    private fun handleMatch(position1: Int, position2: Int) {
        moves++
        comboCount++
        val matchPoints = 10 * comboCount
        score += matchPoints
        updateMovesText()
        updateScoreText()

            cards[position1].isMatched = true
            cards[position2].isMatched = true
        
        playSound(matchSuccessSound)
        
        Handler(Looper.getMainLooper()).postDelayed({
            gridLayout.getChildAt(position1).visibility = View.INVISIBLE
            gridLayout.getChildAt(position2).visibility = View.INVISIBLE
            isAnimating = false
            
            if (cards.all { it.isMatched }) {
                playSound(levelCompleteSound)
                handleLevelComplete()
            }
        }, 1000)

        showBonusAnimation(position1, matchPoints)
    }

    private fun handleMismatch(position1: Int, position2: Int) {
        moves++
        comboCount = 0
        updateMovesText()
        updateScoreText()
        
        Handler(Looper.getMainLooper()).postDelayed({
            cards[position1].isFaceUp = false
            cards[position2].isFaceUp = false
            flipCard(gridLayout.getChildAt(position1) as Button, false)
            flipCard(gridLayout.getChildAt(position2) as Button, false)
            isAnimating = false
        }, 1000)
    }

    private fun showBonusAnimation(position: Int, points: Int) {
        val bonusText = TextView(this).apply {
            text = "+$points"
            textSize = 18f
            setTextColor(getColor(android.R.color.holo_green_light))
        }
        
        val params = GridLayout.LayoutParams()
        params.width = GridLayout.LayoutParams.WRAP_CONTENT
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        bonusText.layoutParams = params
        
        // Bonus yazısını kartın üzerine ekle
        val card = gridLayout.getChildAt(position)
        val location = IntArray(2)
        card.getLocationInWindow(location)
        bonusText.x = location[0].toFloat()
        bonusText.y = location[1].toFloat()
        
        (findViewById<View>(android.R.id.content) as ViewGroup).addView(bonusText)
        
        // Yukarı kayma ve solma animasyonu
        bonusText.animate()
            .translationYBy(-100f)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                (findViewById<View>(android.R.id.content) as ViewGroup).removeView(bonusText)
            }
            .start()
    }

    private fun setupGame(autoStart: Boolean = true) {
        try {
            gridLayout.removeAllViews()
            
            if (autoStart) {
                isGameActive = true
                gameTime = 0
                updateTimerText()
                startTimer()
            }
            
            firstSelectedCard = null
            isAnimating = false
            
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            val uiHeight = (200 * displayMetrics.density).toInt()
            val availableHeight = screenHeight - uiHeight
            
            val padding = (16 * displayMetrics.density).toInt()
            
            val rows = when(currentLevel) {
                1, 2 -> 4     // 16 cards
                3, 4 -> 5     // 20 cards
                else -> 6     // 24 cards
            }
            
            val cols = 4
            maxLevel = 5
            
            gridLayout.rowCount = rows
            gridLayout.columnCount = cols
            
            val cardMargin = (4 * displayMetrics.density).toInt()
            val totalHorizontalMargins = cardMargin * (cols * 2)
            val totalVerticalMargins = cardMargin * (rows * 2)
            
            val availableWidth = screenWidth - (padding * 2) - totalHorizontalMargins
            val availableGridHeight = availableHeight - (padding * 2) - totalVerticalMargins
            
            val maxCardWidth = availableWidth / cols
            val maxCardHeight = availableGridHeight / rows
            val baseCardSize = minOf(maxCardWidth, maxCardHeight)
            
            val maxCardSize = (120 * displayMetrics.density).toInt()
            val cardSize = minOf(baseCardSize, maxCardSize)
            
            val emojiTextSize = when {
                cardSize <= 60 -> cardSize / 2.5f
                cardSize <= 100 -> cardSize / 3f
                else -> cardSize / 4f
            }

            cards = getEmojisForLevel(currentLevel).let { emojis ->
                (emojis + emojis).shuffled().mapIndexed { index, emoji ->
                    MemoryCard(index, emoji)
                }
            }

            cards.forEachIndexed { index, card ->
                val cardButton = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cardSize
                        height = cardSize
                        setMargins(cardMargin, cardMargin, cardMargin, cardMargin)
                    }
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.card_background)
                    textSize = emojiTextSize
                    elevation = 4f
                    setOnClickListener {
                        updateGameWithFlip(index)
                    }
                }
                gridLayout.addView(cardButton)
            }

            if (autoStart) {
                Handler(Looper.getMainLooper()).postDelayed({
                    findViewById<Button>(R.id.hintButton)?.visibility = View.VISIBLE
                }, 1000)
            }
            
            // Update level display
            updateScoreText()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up the game", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateGameWithFlip(position: Int) {
        if (isAnimating || cards[position].isMatched) return

        val button = gridLayout.getChildAt(position) as Button

        when {
            // İlk kart seçildiğinde
            firstSelectedCard == null -> {
                firstSelectedCard = position
                cards[position].isFaceUp = true
                flipCard(button, true) {
                    button.text = cards[position].content
                }
            }
            // İkinci kart seçildiğinde
            firstSelectedCard != position -> {
                isAnimating = true
                cards[position].isFaceUp = true
                
                flipCard(button, true) {
                    button.text = cards[position].content
                    
                    // Eşleşme kontrolü
                    if (cards[firstSelectedCard!!].content == cards[position].content) {
                        handleMatch(firstSelectedCard!!, position)
                    } else {
                        handleMismatch(firstSelectedCard!!, position)
                    }
                    firstSelectedCard = null
                }
            }
        }
    }
    
    private fun flipCard(button: Button, isFaceUp: Boolean, onComplete: () -> Unit = {}) {
        if (isFaceUp) {
            playSound(cardFlipSound)
        }
        val rotation = ObjectAnimator.ofFloat(button, "rotationY", 
            if (isFaceUp) 0f else 180f, 
            if (isFaceUp) 180f else 0f
        ).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        rotation.addUpdateListener { animation ->
            if (animation.animatedFraction >= 0.5f) {
                if (isFaceUp) {
                    button.setBackgroundResource(R.drawable.card_background_flipped)
                } else {
                    button.setBackgroundResource(R.drawable.card_background)
                    button.text = ""
                }
            }
        }

        AnimatorSet().apply {
            play(rotation)
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            onComplete()
        }, 300)
    }

    private fun resetGame() {
        stopTimer()
        currentLevel = 1
        moves = 0
        score = 0
        comboCount = 0
        gameTime = 0
        firstSelectedCard = null
        isAnimating = false
        
        // Hint butonunu gizle
        findViewById<Button>(R.id.hintButton).visibility = View.GONE
        
        updateTimerText()
        updateMovesText()
        updateScoreText()
        
        // Oyunu yeniden başlat
        isGameActive = true
        setupGame()
    }

    private fun updateMovesText() {
        findViewById<TextView>(R.id.movesTextView).text = "Moves: $moves"
    }

    private fun updateScoreText() {
        findViewById<TextView>(R.id.scoreTextView).text = "Score: $score"
        findViewById<TextView>(R.id.highScoreTextView).text = "Best: $highScore"
        findViewById<TextView>(R.id.levelTextView).text = "Level: $currentLevel"
    }

    private fun saveHighScore() {
        getSharedPreferences("game", MODE_PRIVATE).edit().apply {
            putInt("high_score", highScore)
            apply()
        }
    }

    private fun loadHighScore() {
        highScore = getSharedPreferences("game", MODE_PRIVATE).getInt("high_score", 0)
        updateScoreText()
    }

    private fun showConfetti() {
        val container = findViewById<ViewGroup>(android.R.id.content)
        val width = container.width
        val height = container.height
        
        repeat(50) { // 50 konfeti parçası
            val confetti = View(this).apply {
                setBackgroundResource(R.drawable.confetti)
                val size = (20..40).random()
                layoutParams = ViewGroup.LayoutParams(size, size)
                x = (-50..width + 50).random().toFloat()
                y = -50f
                rotation = (-45..45).random().toFloat()
            }
            
            container.addView(confetti)
            
            val fallDuration = (2000L..3000L).random()
            val swayDuration = (2000L..3000L).random()
            
            val fallAnimator = ObjectAnimator.ofFloat(confetti, "translationY", -50f, height + 50f).apply {
                duration = fallDuration
                interpolator = AccelerateInterpolator()
            }
            
            val swayAnimator = ObjectAnimator.ofFloat(confetti, "translationX", 
                confetti.x - 100f, confetti.x + 100f).apply {
                duration = swayDuration
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            val rotateAnimator = ObjectAnimator.ofFloat(confetti, "rotation",
                confetti.rotation, confetti.rotation + (-720..720).random().toFloat()).apply {
                duration = fallDuration
                interpolator = LinearInterpolator()
            }
            
            AnimatorSet().apply {
                playTogether(fallAnimator, swayAnimator, rotateAnimator)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        container.removeView(confetti)
                    }
                })
                start()
            }
        }
    }

    private fun handleLevelComplete() {
        stopTimer()
        isAnimating = true
        
        showConfetti()
        
        if (currentLevel < maxLevel) {
            currentLevel++
            val levelMessage = when(currentLevel) {
                1 -> "Level 1: Animals"
                2 -> "Level 2: Fruits & Food"
                3 -> "Level 3: Sea Creatures"
                4 -> "Level 4: Sports & Activities"
                else -> "Level 5: Space & Sky"
            }
            
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, 
                    "Congratulations! $levelMessage", 
                    Toast.LENGTH_LONG
                ).show()
                setupGame()
                isAnimating = false
            }, 2500)
        } else {
            if (score > highScore) {
                highScore = score
                saveHighScore()
                updateScoreText()
            }
            showGameCompleteDialog()
        }
    }
    
    private fun showGameCompleteDialog() {
        val message = """
            🎉 Congratulations! You've completed all levels!
            
            📊 Statistics:
            ⏱️ Total Time: ${formatTime(gameTime)}
            🎯 Total Score: $score
            🔄 Total Moves: $moves
            ⭐ Best Score: $highScore
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Game Complete!")
            .setMessage(message)
            .setPositiveButton("Play Again") { _, _ -> resetGame() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
    
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun setupSounds() {
        try {
            cardFlipSound = MediaPlayer.create(this, R.raw.card_flip)
            matchSuccessSound = MediaPlayer.create(this, R.raw.match_success)
            levelCompleteSound = MediaPlayer.create(this, R.raw.level_complete)
            backgroundMusic = MediaPlayer.create(this, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
            }
            
            // Ses dosyaları başarıyla yüklendiyse sesi aç
            isSoundEnabled = true
            isMusicEnabled = true
            
            // UI'ı güncelle
            findViewById<ImageButton>(R.id.soundButton).setImageResource(
                if (isSoundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off
            )
            findViewById<ImageButton>(R.id.musicButton).setImageResource(
                if (isMusicEnabled) R.drawable.ic_music_on else R.drawable.ic_music_off
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isSoundEnabled = false
            isMusicEnabled = false
            findViewById<ImageButton>(R.id.soundButton).setImageResource(R.drawable.ic_sound_off)
            findViewById<ImageButton>(R.id.musicButton).setImageResource(R.drawable.ic_music_off)
        }
    }
    
    private fun startBackgroundMusic() {
        try {
            if (isMusicEnabled && backgroundMusic?.isPlaying != true) {
                backgroundMusic?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopBackgroundMusic() {
        try {
            if (backgroundMusic?.isPlaying == true) {
                backgroundMusic?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        findViewById<ImageButton>(R.id.soundButton).setImageResource(
            if (isSoundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off
        )
    }
    
    private fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled
        findViewById<ImageButton>(R.id.musicButton).setImageResource(
            if (isMusicEnabled) R.drawable.ic_music_on else R.drawable.ic_music_off
        )
        if (isMusicEnabled) {
            startBackgroundMusic()
        } else {
            stopBackgroundMusic()
        }
    }
    
    private fun playSound(sound: MediaPlayer?) {
        try {
            if (isSoundEnabled && sound?.isPlaying == false) {
                sound.seekTo(0)
                sound.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundMusic()
        stopTimer()
    }
    
    override fun onResume() {
        super.onResume()
        if (isMusicEnabled) {
            startBackgroundMusic()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        try {
            cardFlipSound?.release()
            matchSuccessSound?.release()
            levelCompleteSound?.release()
            backgroundMusic?.release()
        } catch (e: Exception) {
            // MediaPlayer release hatalarını yönet
        }
    }
}