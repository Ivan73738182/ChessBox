package com.ivangames.chessbox

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LanGameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lan_game)

        val role = intent.getStringExtra("role") ?: "host"
        val hostIp = intent.getStringExtra("hostIp") ?: ""
        val statusText = findViewById<TextView>(R.id.lanStatus)

        if (role == "host") {
            statusText.text = "Создаю игру..."
            NetworkManager.startServer { ip ->
                runOnUiThread {
                    statusText.text = "Ваш IP: $ip\n\nОжидаю подключения клиента..."
                }
            }
        } else {
            statusText.text = "Подключаюсь к $hostIp..."
            NetworkManager.connectToServer(hostIp,
                onSuccess = {
                    runOnUiThread {
                        statusText.text = "Подключено!"
                        Toast.makeText(this, "Успешно подключено!", Toast.LENGTH_SHORT).show()
                    }
                },
                onFail = { error ->
                    runOnUiThread {
                        statusText.text = "Ошибка: $error"
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        NetworkManager.onConnected = {
            runOnUiThread {
                statusText.text = "Соединение установлено!\nСкоро здесь будет игра."
            }
        }

        NetworkManager.onDisconnected = {
            runOnUiThread {
                statusText.text = "Соединение потеряно"
            }
        }

        NetworkManager.onMessageReceived = { message ->
            runOnUiThread {
                Toast.makeText(this, "Получено: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkManager.disconnect()
    }
}
