package com.ahmetkupelikilinc.memorygame

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var cards: List<MemoryCard>
    private var indexOfSingleSelectedCard: Int? = null
    private var moves = 0
    private var highScore = 0
    private lateinit var gridLayout: GridLayout
    private val emojis = listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.gridLayout)
        loadHighScore()
        setupGame()

        findViewById<Button>(R.id.restartButton).setOnClickListener {
            resetGame()
        }
    }

    private fun setupGame() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val cardSize = (screenWidth - 64) / 4 // 16dp padding on each side

        val randomizedEmojis = (emojis + emojis).shuffled()
        cards = randomizedEmojis.mapIndexed { index, emoji ->
            MemoryCard(index, emoji)
        }

        gridLayout.removeAllViews()
        cards.forEachIndexed { index, card ->
            val cardButton = Button(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cardSize
                    height = cardSize
                    setMargins(4, 4, 4, 4)
                }
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.card_background)
                setOnClickListener {
                    updateGameWithFlip(index)
                }
            }
            gridLayout.addView(cardButton)
        }
        updateViewsFromModel()
    }

    private fun updateGameWithFlip(position: Int) {
        if (indexOfSingleSelectedCard == null) {
            indexOfSingleSelectedCard = position
            cards[position].isFaceUp = true
        } else {
            checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        updateViewsFromModel()
    }

    private fun checkForMatch(position1: Int, position2: Int) {
        moves++
        updateMovesText()

        if (cards[position1].content == cards[position2].content) {
            cards[position1].isMatched = true
            cards[position2].isMatched = true
            checkGameEnd()
        } else {
            cards[position1].isFaceUp = false
            cards[position2].isFaceUp = false
        }
    }

    private fun checkGameEnd() {
        if (cards.all { it.isMatched }) {
            if (moves < highScore || highScore == 0) {
                highScore = moves
                saveHighScore()
                updateScoreText()
            }
            Toast.makeText(this, "Tebrikler! Oyunu ${moves} hamlede bitirdiniz!", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetGame() {
        moves = 0
        updateMovesText()
        cards.forEach {
            it.isFaceUp = false
            it.isMatched = false
        }
        cards = cards.shuffled()
        updateViewsFromModel()
    }

    private fun updateViewsFromModel() {
        cards.forEachIndexed { index, card ->
            val button = gridLayout.getChildAt(index) as Button
            if (card.isMatched) {
                button.visibility = View.INVISIBLE
            } else {
                button.visibility = View.VISIBLE
                if (card.isFaceUp) {
                    button.text = card.content
                } else {
                    button.text = ""
                    button.setBackgroundResource(R.drawable.card_background)
                }
            }
        }
    }

    private fun updateMovesText() {
        findViewById<TextView>(R.id.movesTextView).text = "Hamle Sayısı: $moves"
    }

    private fun updateScoreText() {
        findViewById<TextView>(R.id.scoreTextView).text = "En Yüksek Skor: $highScore"
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
}