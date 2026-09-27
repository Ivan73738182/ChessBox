package com.ivangames.chessbox

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        findViewById<Button>(R.id.btnTwoPlayers).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("mode", "two_players")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnVsComputer).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("mode", "vs_computer")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnLan).setOnClickListener {
            val intent = Intent(this, LanActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
