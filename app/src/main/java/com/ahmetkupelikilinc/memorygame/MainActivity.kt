package com.ahmetkupelikilinc.memorygame

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var cards: List<MemoryCard>
    private var indexOfSingleSelectedCard: Int? = null
    private var moves = 0
    private var highScore = 0
    private lateinit var gridLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.gridLayout)
        val restartButton = findViewById<Button>(R.id.restartButton)

        setupGame()

        restartButton.setOnClickListener {
            moves = 0
            updateMovesText()
            setupGame()
        }
    }

    private fun setupGame() {
        val images = listOf(
            R.drawable.ic_image1,
            R.drawable.ic_image2,
            R.drawable.ic_image3,
            R.drawable.ic_image4,
            R.drawable.ic_image5,
            R.drawable.ic_image6,
            R.drawable.ic_image7,
            R.drawable.ic_image8
        )

        val randomizedImages = (images + images).shuffled()
        cards = randomizedImages.map { MemoryCard(it) }

        gridLayout.removeAllViews()
        cards.forEachIndexed { index, card ->
            val cardButton = Button(this)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = 0
            params.columnSpec = GridLayout.spec(index % 4, 1f)
            params.rowSpec = GridLayout.spec(index / 4, 1f)
            cardButton.layoutParams = params

            cardButton.setOnClickListener {
                updateGameWithFlip(index)
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
            moves++
            updateMovesText()
            checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        updateViewsFromModel()
    }

    private fun checkForMatch(position1: Int, position2: Int) {
        if (cards[position1].imageId == cards[position2].imageId) {
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
                updateScoreText()
            }
            Toast.makeText(this, "Tebrikler! Oyunu ${moves} hamlede bitirdiniz!", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateMovesText() {
        findViewById<TextView>(R.id.movesTextView).text = "Hamle Sayısı: $moves"
    }

    private fun updateScoreText() {
        findViewById<TextView>(R.id.scoreTextView).text = "En Yüksek Skor: $highScore"
    }

    private fun updateViewsFromModel() {
        cards.forEachIndexed { index, card ->
            val button = gridLayout.getChildAt(index) as Button
            if (card.isMatched) {
                button.visibility = View.INVISIBLE
            } else {
                button.visibility = View.VISIBLE
                if (card.isFaceUp) {
                    button.setBackgroundResource(card.imageId)
                } else {
                    button.setBackgroundResource(R.drawable.ic_card_back)
                }
            }
        }
    }
}