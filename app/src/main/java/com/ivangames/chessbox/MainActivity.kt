package com.ivangames.chessbox

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var chessBoard: ChessBoardView
    private lateinit var statusText: TextView
    private lateinit var winOverlay: FrameLayout
    private lateinit var winText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chessBoard = findViewById(R.id.chessBoard)
        statusText = findViewById(R.id.statusText)
        winOverlay = findViewById(R.id.winOverlay)
        winText = findViewById(R.id.winText)

        val mode = intent.getStringExtra("mode") ?: "two_players"
        chessBoard.vsComputer = (mode == "vs_computer")

        statusText.text = if (chessBoard.vsComputer) "Вы играете белыми" else "Ход: белые"

        chessBoard.onTurnChanged = { isWhiteTurn ->
            if (chessBoard.vsComputer) {
                statusText.text = if (isWhiteTurn) "Ваш ход" else "Компьютер думает..."
            } else {
                statusText.text = if (isWhiteTurn) "Ход: белые" else "Ход: чёрные"
            }
        }

        chessBoard.onCheck = {
            Toast.makeText(this, "⚠️ ШАХ!", Toast.LENGTH_SHORT).show()
        }

        chessBoard.onCheckmate = { whiteLost ->
            val winner = if (whiteLost) "Чёрные" else "Белые"
            winText.text = "🏆 МАТ!\n\nПобедили $winner!"
            winOverlay.visibility = View.VISIBLE
        }

        chessBoard.onStalemate = {
            winText.text = "🤝 ПАТ!\n\nНичья!"
            winOverlay.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.undoBtn).setOnClickListener {
            chessBoard.undoMove()
        }

        findViewById<Button>(R.id.resetBtn).setOnClickListener {
            chessBoard.resetGame()
            winOverlay.visibility = View.GONE
            statusText.text = if (chessBoard.vsComputer) "Ваш ход" else "Ход: белые"
        }

        findViewById<Button>(R.id.newGameBtn).setOnClickListener {
            finish()  // вернуться в меню
        }

        findViewById<Button>(R.id.newGameBtn2).setOnClickListener {
            finish()  // вернуться в меню
        }
    }
}
