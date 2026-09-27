package com.ivangames.chessbox

import android.os.Bundle
import android.widget.TextView
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

        statusText.text = "Ход: белые"
    }
}
