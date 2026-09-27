package com.ivangames.chessbox

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lan)

        val btnCreate = findViewById<Button>(R.id.btnCreateGame)
        val btnJoin = findViewById<Button>(R.id.btnJoinGame)
        val joinPanel = findViewById<LinearLayout>(R.id.joinPanel)
        val ipInput = findViewById<EditText>(R.id.ipInput)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnCreate.setOnClickListener {
            val intent = Intent(this, LanGameActivity::class.java)
            intent.putExtra("role", "host")
            startActivity(intent)
        }

        btnJoin.setOnClickListener {
            if (joinPanel.visibility == View.VISIBLE) {
                joinPanel.visibility = View.GONE
            } else {
                joinPanel.visibility = View.VISIBLE
            }
        }

        btnConnect.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "Введите IP адрес", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, LanGameActivity::class.java)
            intent.putExtra("role", "client")
            intent.putExtra("hostIp", ip)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
