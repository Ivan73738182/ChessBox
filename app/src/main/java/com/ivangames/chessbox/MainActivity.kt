package com.ivangames.chessbox

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var chessBoard: ChessBoardView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chessBoard = findViewById(R.id.chessBoard)
        statusText = findViewById(R.id.statusText)

        chessBoard.onTurnChanged = { isWhiteTurn ->
            statusText.text = if (isWhiteTurn) "Ход: белые" else "Ход: чёрные"
        }

        chessBoard.onCheck = {
            Toast.makeText(this, "⚠️ ШАХ!", Toast.LENGTH_SHORT).show()
        }

        chessBoard.onCheckmate = { whiteLost ->
            val winner = if (whiteLost) "Чёрные" else "Белые"
            Toast.makeText(this, "🏆 МАТ! Победили $winner!", Toast.LENGTH_LONG).show()
            statusText.text = "🏆 Победили $winner!"
        }

        findViewById<Button>(R.id.undoBtn).setOnClickListener {
            chessBoard.undoMove()
        }

        findViewById<Button>(R.id.resetBtn).setOnClickListener {
            chessBoard.resetGame()
            statusText.text = "Ход: белые"
        }

        statusText.text = "Ход: белые"
    }
}
