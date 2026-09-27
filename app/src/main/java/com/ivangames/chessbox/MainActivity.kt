package com.ivangames.chessbox

import android.app.AlertDialog
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

        // Спрашиваем режим при запуске
        showModeDialog()

        chessBoard.onTurnChanged = { isWhiteTurn ->
            statusText.text = if (isWhiteTurn) "Ход: белые" else "Ход: чёрные"
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
            statusText.text = "Ход: белые"
            winOverlay.visibility = View.GONE
        }

        findViewById<Button>(R.id.newGameBtn).setOnClickListener {
            chessBoard.resetGame()
            statusText.text = "Ход: белые"
            winOverlay.visibility = View.GONE
            showModeDialog()
        }
    }

    private fun showModeDialog() {
        val options = arrayOf("👥 Играть вдвоём", "🤖 Против компьютера")
        AlertDialog.Builder(this)
            .setTitle("Режим игры")
            .setItems(options) { _, which ->
                if (which == 0) {
                    chessBoard.vsComputer = false
                    statusText.text = "Ход: белые"
                } else {
                    chessBoard.vsComputer = true
                    statusText.text = "Ход: белые"
                }
                chessBoard.resetGame()
            }
            .setCancelable(false)
            .show()
    }
}
