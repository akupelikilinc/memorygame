package com.ahmetkupelikilinc.memorygame

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
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
import java.util.Date

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
    private var maxCombo = 0
    private var isHintUsed = false
    private var perfectMatch = true
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    private var cardFlipSound: MediaPlayer? = null
    private var matchSuccessSound: MediaPlayer? = null
    private var levelCompleteSound: MediaPlayer? = null
    private var backgroundMusic: MediaPlayer? = null
    private var isSoundEnabled = false
    private var isMusicEnabled = false
    private var currentChallenge: DailyChallenge? = null
    private var startTime: Long = 0
    private lateinit var achievements: List<Achievement>
    private lateinit var timerText: TextView
    private lateinit var handler: Handler
    private var emojis: MutableList<String> = mutableListOf()
    private var isPaused = false
    private var isTimeMode = false
    private var remainingTime = 0
    private var initialTime = 0

    companion object {
        fun newIntent(context: Context, challenge: DailyChallenge): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra("challenge_id", challenge.id)
                putExtra("target_score", challenge.targetScore)
                putExtra("max_moves", challenge.maxMoves)
                putExtra("time_limit", challenge.timeLimit)
                putStringArrayListExtra("special_emojis", ArrayList(challenge.specialEmojis))
            }
        }
    }

    private val allThemes = mapOf(
        "animals" to listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🦁", "🐯", "🐮", "🐷"
        ),
        "food" to listOf(
            "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍒", "🥝", "🍍"
        ),
        "sea" to listOf(
            "🐋", "🐳", "🐟", "🐠", "🦈", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐬"
        ),
        "sports" to listOf(
            "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🏸", "⛳", "🎯", "🎮"
        ),
        "space" to listOf(
            "🌞", "🌙", "⭐", "🌟", "☄️", "🌍", "🚀", "🛸", "🌈", "⛅", "❄️", "🌪️"
        ),
        "flags" to listOf(
            "🇹🇷", "🇺🇸", "🇬🇧", "🇫🇷", "🇩🇪", "🇮🇹", "🇪🇸", "🇵🇹", "🇷🇺", "🇯🇵", "🇰🇷", "🇨🇳"
        ),
        "jobs" to listOf(
            "👨‍⚕️", "👨‍🏫", "👨‍🌾", "👨‍🍳", "👨‍🔧", "👨‍🏭", "👨‍💼", "👨‍🔬", "👨‍💻", "👨‍🎨", "👨‍✈️", "👨‍🚀"
        ),
        "vehicles" to listOf(
            "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚"
        ),
        "weather" to listOf(
            "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "💨"
        ),
        "sports_equipment" to listOf(
            "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓"
        )
    )
    
    private var currentTheme = "animals"
    
    private enum class Difficulty {
        EASY, MEDIUM, HARD
    }
    
    private var currentDifficulty = Difficulty.MEDIUM
    
    private fun getDifficultySettings(difficulty: Difficulty): Triple<Int, Int, Float> {
        return when(difficulty) {
            Difficulty.EASY -> Triple(90, 4, 1.5f)    // 90 seconds, 4x4 grid, 1.5x points
            Difficulty.MEDIUM -> Triple(60, 5, 2.0f)  // 60 seconds, 5x4 grid, 2x points
            Difficulty.HARD -> Triple(45, 6, 3.0f)    // 45 seconds, 6x4 grid, 3x points
        }
    }
    
    private fun showDifficultySelectionDialog() {
        val difficulties = mapOf(
            Difficulty.EASY to "Easy Mode ⭐",
            Difficulty.MEDIUM to "Medium Mode ⭐⭐",
            Difficulty.HARD to "Hard Mode ⭐⭐⭐"
        )
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Difficulty")
        
        val difficultyNames = difficulties.values.toTypedArray()
        val difficultyValues = difficulties.keys.toTypedArray()
        
        builder.setSingleChoiceItems(difficultyNames, difficultyValues.indexOf(currentDifficulty)) { dialog, which ->
            currentDifficulty = difficultyValues[which]
            resetGame()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    private fun getEmojisForLevel(level: Int): List<String> {
        val themeEmojis = allThemes[currentTheme] ?: allThemes["animals"]!!
        val pairsNeeded = when(level) {
            1, 2 -> 8     // 4x4 grid (16 cards)
            3, 4 -> 10    // 5x4 grid (20 cards)
            else -> 12    // 6x4 grid (24 cards)
        }
        return themeEmojis.take(pairsNeeded)
    }
    
    private fun showThemeSelectionDialog() {
        val themes = mapOf(
            "animals" to "Animals 🐶",
            "food" to "Food & Fruits 🍎",
            "sea" to "Sea Creatures 🐋",
            "sports" to "Sports 🏀",
            "space" to "Space 🚀",
            "flags" to "Flags 🏁",
            "jobs" to "Professions 👨‍💼",
            "vehicles" to "Vehicles 🚗",
            "weather" to "Weather ⛅",
            "sports_equipment" to "Sports Equipment ⚽"
        )
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Theme")
        
        val themeNames = themes.values.toTypedArray()
        val themeKeys = themes.keys.toTypedArray()
        
        builder.setSingleChoiceItems(themeNames, themeKeys.indexOf(currentTheme)) { dialog, which ->
            currentTheme = themeKeys[which]
            resetGame()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            gridLayout = findViewById(R.id.gridLayout)
            timerText = findViewById(R.id.timerTextView)
            handler = Handler(Looper.getMainLooper())
            
            loadHighScore()
            setupButtons()
            setupTimer()
            setupSounds()
            
            achievements = loadAchievements()
            
            if (intent.hasExtra("challenge_id")) {
                setupDailyChallenge()
            } else {
                emojis = allThemes[currentTheme]?.toMutableList() ?: emojis
            }
            
            startTime = System.currentTimeMillis()
            
            // Start the game
            setupGame(true)
            
            // Play/Pause button to pause
            findViewById<ImageButton>(R.id.playPauseButton).setImageResource(R.drawable.ic_pause)
            
            // Hint button to active
            findViewById<ImageButton>(R.id.hintButton).apply {
                visibility = View.VISIBLE
                isEnabled = true
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting the game", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupButtons() {
        try {
            findViewById<ImageButton>(R.id.playPauseButton).apply {
                setOnClickListener {
                    togglePausePlay()
                }
            }
            
            findViewById<ImageButton>(R.id.difficultyButton).apply {
                setOnClickListener {
                    showDifficultySelectionDialog()
                }
            }
            
            findViewById<ImageButton>(R.id.themeButton).apply {
                setOnClickListener {
                    showThemeSelectionDialog()
                }
            }

            findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.timeModeSwitch).apply {
                setOnCheckedChangeListener { _, isChecked ->
                    isTimeMode = isChecked
                    if (isGameActive) {
                        // If game is active, restart
                        resetGame()
                    }
                }
            }

            findViewById<ImageButton>(R.id.hintButton).apply {
                visibility = View.GONE // Hidden at the start
                setOnClickListener {
                    if (isGameActive && !isAnimating && !isPaused) {
                        showHint()
                    }
                }
            }

            findViewById<ImageButton>(R.id.achievementsButton).apply {
                setOnClickListener {
                    try {
                        startActivity(Intent(this@MainActivity, AchievementsActivity::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Achievements will be available soon!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            findViewById<ImageButton>(R.id.dailyChallengeButton).apply {
                setOnClickListener {
                    try {
                        startActivity(Intent(this@MainActivity, DailyChallengeActivity::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Daily Challenges will be available soon!", Toast.LENGTH_SHORT).show()
                    }
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
            Toast.makeText(this, "Error setting up buttons", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                if (isGameActive) {
                    if (isTimeMode) {
                        remainingTime--
                        if (remainingTime <= 0) {
                            handleTimeUp()
                            return
                        }
                    } else {
                        gameTime++
                    }
                    updateTimerText()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startTimer() {
        isGameActive = true
        if (isTimeMode) {
            remainingTime = getLevelTime(currentLevel)
            initialTime = remainingTime
        }
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        isGameActive = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerText() {
        if (isTimeMode) {
            val minutes = remainingTime / 60
            val seconds = remainingTime % 60
            val timerView = findViewById<TextView>(R.id.timerTextView)
            
            // Red color for last 10 seconds
            if (remainingTime <= 10) {
                timerView.setTextColor(getColor(android.R.color.holo_red_light))
            } else {
                timerView.setTextColor(getColor(android.R.color.white))
            }
            
            timerView.text = String.format("Time: %02d:%02d", minutes, seconds)
        } else {
            val minutes = gameTime / 60
            val seconds = gameTime % 60
            findViewById<TextView>(R.id.timerTextView).text = 
                String.format("Time: %02d:%02d", minutes, seconds)
        }
    }

    private fun showHint() {
        if (!isGameActive || isAnimating) return
        
        isAnimating = true
        
        // Score and time penalty
        score = maxOf(0, score - 50)
        if (isTimeMode) {
            val button = gridLayout.getChildAt(0)
            val location = IntArray(2)
            button.getLocationInWindow(location)
            addTimeBonus(-5, location[0].toFloat(), location[1].toFloat())
        }
        
        updateScoreText()
        
        // Show all cards
        cards.forEachIndexed { index, card ->
            if (!card.isMatched) {
                val button = gridLayout.getChildAt(index) as Button
                button.text = card.content
                button.setBackgroundResource(R.drawable.card_background_flipped)
            }
        }

        // Hide cards after 2 seconds
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
        
        // Points multiplier based on difficulty level
        val (_, _, scoreMultiplier) = getDifficultySettings(currentDifficulty)
        val matchPoints = (10 * comboCount * scoreMultiplier).toInt()
        
        score += matchPoints
        updateMovesText()
        updateScoreText()
        
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        
        playSound(matchSuccessSound)
        
        // Apply animation to matched cards
        val button1 = gridLayout.getChildAt(position1)
        val button2 = gridLayout.getChildAt(position2)
        
        val matchAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.card_match_animation)
        button1.startAnimation(matchAnimation)
        button2.startAnimation(matchAnimation)
        
        // Sparkle effect
        showSparkleEffect(button1)
        showSparkleEffect(button2)
        
        Handler(Looper.getMainLooper()).postDelayed({
            button1.visibility = View.INVISIBLE
            button2.visibility = View.INVISIBLE
            isAnimating = false
            
            if (cards.all { it.isMatched }) {
                playSound(levelCompleteSound)
                handleLevelComplete()
            }
        }, 500)
        
        showBonusAnimation(position1, matchPoints)
    }
    
    private fun showSparkleEffect(view: View) {
        repeat(5) { // 5 flashes
            val sparkle = View(this).apply {
                setBackgroundResource(R.drawable.sparkle_effect)
                alpha = 0f
                scaleX = 0f
                scaleY = 0f
            }
            
            val container = findViewById<ViewGroup>(android.R.id.content)
            container.addView(sparkle)
            
            // Random position around the card
            val location = IntArray(2)
            view.getLocationInWindow(location)
            sparkle.x = location[0] + (Math.random() * view.width).toFloat()
            sparkle.y = location[1] + (Math.random() * view.height).toFloat()
            
            // Sparkle animation
            sparkle.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .withEndAction {
                    sparkle.animate()
                        .alpha(0f)
                        .scaleX(0f)
                        .scaleY(0f)
                        .setDuration(200)
                        .withEndAction {
                            container.removeView(sparkle)
                        }
                        .start()
                }
                .start()
        }
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
        
        // Bonus text on top of the card
        val card = gridLayout.getChildAt(position)
        val location = IntArray(2)
        card.getLocationInWindow(location)
        bonusText.x = location[0].toFloat()
        bonusText.y = location[1].toFloat()
        
        (findViewById<View>(android.R.id.content) as ViewGroup).addView(bonusText)
        
        // Upward and downward animations
        bonusText.animate()
            .translationYBy(-100f)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                (findViewById<View>(android.R.id.content) as ViewGroup).removeView(bonusText)
            }
            .start()
    }

    private fun setupGame(autoStart: Boolean = false) {
        try {
            gridLayout.removeAllViews()
            
            moves = 0
            score = 0
            comboCount = 0
            maxCombo = 0
            isHintUsed = false
            perfectMatch = true
            firstSelectedCard = null
            isAnimating = false
            isPaused = false
            isGameActive = true
            
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            val headerHeight = resources.getDimensionPixelSize(R.dimen.header_height)
            val navHeight = resources.getDimensionPixelSize(R.dimen.nav_height)
            val availableHeight = screenHeight - headerHeight - navHeight
            
            val padding = (16 * displayMetrics.density).toInt()
            
            // Grid size based on difficulty level
            val (_, gridSize, _) = getDifficultySettings(currentDifficulty)
            val rows = gridSize
            val cols = 4
            
            gridLayout.rowCount = rows
            gridLayout.columnCount = cols
            
            val cardMargin = (4 * displayMetrics.density).toInt()
            val totalHorizontalMargins = cardMargin * (cols * 2)
            val totalVerticalMargins = cardMargin * (rows * 2)
            
            val availableWidth = screenWidth - (padding * 2)
            val availableGridHeight = availableHeight - (padding * 2)
            
            val cardWidth = (availableWidth - totalHorizontalMargins) / cols
            val cardHeight = (availableGridHeight - totalVerticalMargins) / rows
            val cardSize = minOf(cardWidth, cardHeight)
            
            val emojis = getEmojisForLevel(currentLevel)
            cards = (emojis + emojis).shuffled().mapIndexed { index, emoji ->
                MemoryCard(index, emoji)
            }
            
            cards.forEachIndexed { index, card ->
                val cardButton = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cardSize
                        height = cardSize
                        setMargins(cardMargin, cardMargin, cardMargin, cardMargin)
                    }
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.card_background)
                    textSize = cardSize / 4f
                    elevation = 4f
                    gravity = android.view.Gravity.CENTER
                    includeFontPadding = false
                    setPadding(0, 0, 0, 0)
                    isEnabled = true
                    isClickable = true
                    setOnClickListener {
                        updateGameWithFlip(index)
                    }
                }
                gridLayout.addView(cardButton)
            }
            
            // Show cards at the start of the level
            Handler(Looper.getMainLooper()).postDelayed({
                // Show all cards
                cards.forEachIndexed { index, card ->
                    val button = gridLayout.getChildAt(index) as Button
                    button.text = card.content
                    button.setBackgroundResource(R.drawable.card_background_flipped)
                }
                
                // Hide cards after 2 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    cards.forEachIndexed { index, card ->
                        val button = gridLayout.getChildAt(index) as Button
                        button.text = ""
                        button.setBackgroundResource(R.drawable.card_background)
                    }
                    
                    // Start the game after cards are hidden
                    if (autoStart) {
                        startTimer()
                        if (isMusicEnabled) {
                            startBackgroundMusic()
                        }
                    }
                }, 2000)
            }, 500)
            
            updateMovesText()
            updateScoreText()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up the game", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateGameWithFlip(position: Int) {
        if (isAnimating || !isGameActive || cards[position].isMatched || isPaused) return
        
        val button = gridLayout.getChildAt(position) as Button
        
        when {
            firstSelectedCard == null -> {
                firstSelectedCard = position
                cards[position].isFaceUp = true
                flipCard(button, true) {
                    button.text = cards[position].content
                }
            }
            firstSelectedCard != position -> {
                isAnimating = true
                cards[position].isFaceUp = true
                
                flipCard(button, true) {
                    button.text = cards[position].content
                    
                    if (cards[firstSelectedCard!!].content == cards[position].content) {
                        handleMatch(firstSelectedCard!!, position)
                    } else {
                        handleMismatch(firstSelectedCard!!, position)
                        perfectMatch = false
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
        try {
            stopTimer()
            currentLevel = 1
            moves = 0
            score = 0
            comboCount = 0
            gameTime = 0
            firstSelectedCard = null
            isAnimating = false
            isPaused = false
            
            // Hint button to show
            findViewById<ImageButton>(R.id.hintButton).apply {
                visibility = View.VISIBLE
                isEnabled = true
            }
            
            // Play/Pause button to pause
            findViewById<ImageButton>(R.id.playPauseButton).setImageResource(R.drawable.ic_pause)
            
            updateTimerText()
            updateMovesText()
            updateScoreText()
            
            // Restart the game
            isGameActive = true
            setupGame(true)
            startTimer()
            if (isMusicEnabled) {
                startBackgroundMusic()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error resetting the game", Toast.LENGTH_SHORT).show()
        }
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
        
        repeat(50) { // 50 confetti pieces
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
        if (isAnimating) return
        
        stopTimer()
        isAnimating = true
        
        // Show confetti effect
        showConfetti()
        
        // Level completion animation
        val container = findViewById<ViewGroup>(android.R.id.content)
        val levelCompleteText = TextView(this).apply {
            text = "Level Complete!"
            textSize = 36f
            setTextColor(getColor(android.R.color.white))
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
        }
        
        container.addView(levelCompleteText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        levelCompleteText.x = (container.width - levelCompleteText.width) / 2f
        levelCompleteText.y = (container.height - levelCompleteText.height) / 2f
        
        levelCompleteText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .withEndAction {
                Handler(Looper.getMainLooper()).postDelayed({
                    levelCompleteText.animate()
                        .alpha(0f)
                        .scaleX(0f)
                        .scaleY(0f)
                        .setDuration(500)
                        .withEndAction {
                            container.removeView(levelCompleteText)
                            
                            if (currentLevel < maxLevel) {
                                currentLevel++
                                val levelMessage = when(currentLevel) {
                                    1 -> "Level 1: Animals"
                                    2 -> "Level 2: Fruits & Food"
                                    3 -> "Level 3: Sea Creatures"
                                    4 -> "Level 4: Sports & Activities"
                                    else -> "Level 5: Space & Sky"
                                }
                                
                                val currentScore = score
                                
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (!isFinishing && !isDestroyed) {
                                        Toast.makeText(this, 
                                            "Congratulations! $levelMessage", 
                                            Toast.LENGTH_LONG
                                        ).show()
                                        
                                        setupGame(true)
                                        score = currentScore
                                        updateScoreText()
                                        
                                        isGameActive = true
                                        isPaused = false
                                        isAnimating = false
                                        
                                        gridLayout.alpha = 1.0f
                                        gridLayout.isEnabled = true
                                        for (i in 0 until gridLayout.childCount) {
                                            val card = gridLayout.getChildAt(i)
                                            if (!cards[i].isMatched) {
                                                card.isEnabled = true
                                                card.isClickable = true
                                            }
                                        }
                                        
                                        startTimer()
                                        if (isMusicEnabled) {
                                            startBackgroundMusic()
                                        }
                                        
                                        findViewById<ImageButton>(R.id.playPauseButton)?.setImageResource(R.drawable.ic_pause)
                                    }
                                }, 1000)
                            } else {
                                if (score > highScore) {
                                    highScore = score
                                    saveHighScore()
                                    updateScoreText()
                                    showHighScoreAnimation()
                                }
                                showGameCompleteDialog()
                            }
                        }
                        .start()
                }, 1000)
            }
            .start()
    }
    
    private fun showHighScoreAnimation() {
        val container = findViewById<ViewGroup>(android.R.id.content)
        val highScoreText = TextView(this).apply {
            text = "New High Score!\n$highScore"
            textSize = 36f
            setTextColor(ContextCompat.getColor(context, R.color.holo_gold_light))
            gravity = android.view.Gravity.CENTER
            alpha = 0f
        }
        
        container.addView(highScoreText)
        highScoreText.x = (container.width - highScoreText.width) / 2f
        highScoreText.y = (container.height - highScoreText.height) / 2f
        
        // High score animation
        val scaleX = ObjectAnimator.ofFloat(highScoreText, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(highScoreText, "scaleY", 0f, 1.2f, 1f)
        val alpha = ObjectAnimator.ofFloat(highScoreText, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1000
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        highScoreText.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .withEndAction {
                                container.removeView(highScoreText)
                            }
                            .start()
                    }, 2000)
                }
            })
            start()
        }
        
        // Extra sparkle effect
        Handler(Looper.getMainLooper()).postDelayed({
            repeat(20) {
                showSparkleEffect(highScoreText)
            }
        }, 500)
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
            
        stopTimer()
        stopBackgroundMusic()
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
            backgroundMusic = MediaPlayer.create(this, R.raw.background_music)
            backgroundMusic?.isLooping = true
            
            // Load sound settings
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            isSoundEnabled = prefs.getBoolean("sound_enabled", true)
            isMusicEnabled = prefs.getBoolean("music_enabled", true)
            
            // Update button icons
            updateSoundIcon()
            updateMusicIcon()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        updateSoundIcon()
        
        // Save setting
        getSharedPreferences("settings", MODE_PRIVATE).edit()
            .putBoolean("sound_enabled", isSoundEnabled)
            .apply()
    }
    
    private fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled
        updateMusicIcon()
        
        if (isMusicEnabled && isGameActive && !isPaused) {
            startBackgroundMusic()
        } else {
            stopBackgroundMusic()
        }
        
        // Save setting
        getSharedPreferences("settings", MODE_PRIVATE).edit()
            .putBoolean("music_enabled", isMusicEnabled)
            .apply()
    }
    
    private fun updateSoundIcon() {
        findViewById<ImageButton>(R.id.soundButton).setImageResource(
            if (isSoundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off
        )
    }
    
    private fun updateMusicIcon() {
        findViewById<ImageButton>(R.id.musicButton).setImageResource(
            if (isMusicEnabled) R.drawable.ic_music_on else R.drawable.ic_music_off
        )
    }
    
    private fun startBackgroundMusic() {
        try {
            if (isMusicEnabled && backgroundMusic?.isPlaying == false) {
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
            // Handle MediaPlayer release errors
        }
    }

    private fun setupDailyChallenge() {
        currentChallenge = DailyChallenge(
            id = intent.getIntExtra("challenge_id", 0),
            date = Date(),
            targetScore = intent.getIntExtra("target_score", 0),
            maxMoves = intent.getIntExtra("max_moves", 0),
            timeLimit = intent.getIntExtra("time_limit", 0),
            specialEmojis = intent.getStringArrayListExtra("special_emojis") ?: listOf()
        )
        
        // Use special emojis
        emojis = currentChallenge?.specialEmojis?.toMutableList() ?: emojis
    }
    
    private fun handleGameEnd() {
        if (currentChallenge != null) {
            val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val stars = currentChallenge?.calculateStars(score, moves, timeSpent) ?: 0
            
            // Result to DailyChallengeActivity
            setResult(RESULT_OK, Intent().apply {
                putExtra(DailyChallengeActivity.CHALLENGE_COMPLETED, true)
                putExtra(DailyChallengeActivity.STARS_EARNED, stars)
            })
        }
        
        // ... existing game end code ...
    }
    
    private fun handleCardClick(position: Int) {
        if (isAnimating || !isGameActive) return
        
        moves++
        // Daily Challenge move control
        if (currentChallenge != null && moves > currentChallenge?.maxMoves!!) {
            showMessage("Maximum moves reached!")
            handleGameEnd()
            return
        }
        
        updateGameWithFlip(position)
    }
    
    private fun updateTimer() {
        if (currentChallenge != null) {
            val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            if (timeSpent >= currentChallenge?.timeLimit!!) {
                showMessage("Time's up!")
                handleGameEnd()
                return
            }
            
            val timeLeft = currentChallenge?.timeLimit!! - timeSpent
            timerText.text = String.format("Time: %02d:%02d", timeLeft / 60, timeLeft % 60)
        }
        
        handler.postDelayed({ updateTimer() }, 1000)
    }

    private fun loadAchievements(): List<Achievement> {
        val savedAchievements = Achievement.createAchievements()
        
        // Load saved progress
        getSharedPreferences("achievements", MODE_PRIVATE).apply {
            savedAchievements.forEach { achievement ->
                achievement.progress = getInt("${achievement.id}_progress", 0)
                achievement.isUnlocked = getBoolean("${achievement.id}_unlocked", false)
            }
        }
        
        return savedAchievements
    }
    
    private fun saveAchievements() {
        getSharedPreferences("achievements", MODE_PRIVATE).edit().apply {
            achievements.forEach { achievement ->
                putInt("${achievement.id}_progress", achievement.progress)
                putBoolean("${achievement.id}_unlocked", achievement.isUnlocked)
            }
            apply()
        }
    }
    
    private fun checkAchievements() {
        var unlockedAchievements = 0
        
        achievements.forEach { achievement ->
            when(achievement.id) {
                // Score achievements
                "score_1000", "score_5000", "score_10000" -> {
                    achievement.updateProgress(score)
                }
                
                // Speed achievements
                "speed_level_30", "speed_level_20", "speed_level_15" -> {
                    if (gameTime <= achievement.requirement) {
                        achievement.updateProgress(1)
                    }
                }
                
                // Streak achievements - will be controlled in Daily Challenge
                
                // Special achievements
                "no_hint" -> {
                    // Hint not used
                    if (!isHintUsed && cards.all { it.isMatched }) {
                        achievement.updateProgress(1)
                    }
                }
                "perfect_match" -> {
                    // No mistakes
                    if (perfectMatch && cards.all { it.isMatched }) {
                        achievement.updateProgress(1)
                    }
                }
                "combo_master" -> {
                    achievement.updateProgress(maxCombo)
                }
            }
            
            // New unlocked achievement
            if (achievement.isUnlocked && !achievement.wasShown) {
                achievement.wasShown = true
                unlockedAchievements++
                score += achievement.rewardPoints
                showAchievementUnlocked(achievement)
            }
        }
        
        if (unlockedAchievements > 0) {
            saveAchievements()
            updateScoreText()
        }
    }
    
    private fun showAchievementUnlocked(achievement: Achievement) {
        val view = layoutInflater.inflate(R.layout.achievement_unlocked, null)
        view.findViewById<TextView>(R.id.achievementTitle).text = achievement.title
        view.findViewById<TextView>(R.id.achievementPoints).text = "+${achievement.rewardPoints}"
        
        val container = findViewById<ViewGroup>(android.R.id.content)
        container.addView(view)
        
        view.alpha = 0f
        view.translationY = 100f
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .withEndAction {
                Handler(Looper.getMainLooper()).postDelayed({
                    view.animate()
                        .alpha(0f)
                        .translationY(-100f)
                        .setDuration(500)
                        .withEndAction {
                            container.removeView(view)
                        }
                }, 2000)
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun togglePausePlay() {
        if (isAnimating) return
        
        isPaused = !isPaused
        val playPauseButton = findViewById<ImageButton>(R.id.playPauseButton)
        
        if (isPaused) {
            stopTimer()
            stopBackgroundMusic()
            playPauseButton.setImageResource(R.drawable.ic_play)
            
            gridLayout.alpha = 0.5f
            gridLayout.isEnabled = false
            for (i in 0 until gridLayout.childCount) {
                val card = gridLayout.getChildAt(i)
                card.isEnabled = false
                card.isClickable = false
            }
            
            findViewById<ImageButton>(R.id.hintButton)?.isEnabled = false
        } else {
            startTimer()
            if (isMusicEnabled) {
                startBackgroundMusic()
            }
            playPauseButton.setImageResource(R.drawable.ic_pause)
            
            gridLayout.alpha = 1.0f
            gridLayout.isEnabled = true
            for (i in 0 until gridLayout.childCount) {
                val card = gridLayout.getChildAt(i)
                if (!cards[i].isMatched) {
                    card.isEnabled = true
                    card.isClickable = true
                }
            }
            
            findViewById<ImageButton>(R.id.hintButton)?.isEnabled = true
            isGameActive = true
        }
    }

    private fun getLevelTime(level: Int): Int {
        val (baseTime, _, _) = getDifficultySettings(currentDifficulty)
        return when(level) {
            1 -> baseTime
            2 -> (baseTime * 0.9).toInt()
            3 -> (baseTime * 0.8).toInt()
            4 -> (baseTime * 0.7).toInt()
            else -> (baseTime * 0.6).toInt()
        }
    }

    private fun handleTimeUp() {
        stopTimer()
        isGameActive = false
        showTimeUpDialog()
    }

    private fun showTimeUpDialog() {
        val message = """
            ⏰ Time's Up!
            
            📊 Statistics:
            🎯 Score: $score
            🔄 Moves: $moves
            ⭐ Best Score: $highScore
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Game Over!")
            .setMessage(message)
            .setPositiveButton("Try Again") { _, _ -> resetGame() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun addTimeBonus(seconds: Int, x: Float, y: Float) {
        if (!isTimeMode) return
        
        remainingTime += seconds
        
        // Bonus/penalty animation
        val bonusText = TextView(this).apply {
            text = if (seconds > 0) "+$seconds" else "$seconds"
            textSize = 18f
            setTextColor(if (seconds > 0) 
                getColor(android.R.color.holo_green_light)
            else 
                getColor(android.R.color.holo_red_light)
            )
        }
        
        val container = findViewById<ViewGroup>(android.R.id.content)
        container.addView(bonusText)
        
        bonusText.x = x
        bonusText.y = y
        
        bonusText.animate()
            .translationY(y - 100)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                container.removeView(bonusText)
            }
            .start()
    }
}