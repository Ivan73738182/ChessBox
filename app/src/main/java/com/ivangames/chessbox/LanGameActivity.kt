package com.ivangames.chessbox

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LanGameActivity : AppCompatActivity() {

    private lateinit var chessBoard: ChessBoardView
    private lateinit var statusText: TextView
    private var isHost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lan_game)

        chessBoard = findViewById(R.id.lanChessBoard)
        statusText = findViewById(R.id.lanStatus)

        val role = intent.getStringExtra("role") ?: "host"
        val hostIp = intent.getStringExtra("hostIp") ?: ""
        isHost = (role == "host")

        // Настраиваем доску: кто играет каким цветом
        chessBoard.myTurnIsWhite = isHost
        chessBoard.networkMode = true
        chessBoard.vsComputer = false

        statusText.text = if (isHost) "Вы играете белыми" else "Вы играете чёрными"

        // Когда игрок сделал ход — отправляем по сети
        chessBoard.onMoveMade = { fromRow, fromCol, toRow, toCol ->
            val message = "$fromRow,$fromCol,$toRow,$toCol"
            NetworkManager.sendMessage(message)
        }

        // Когда пришло сообщение — делаем ход на нашей доске
        NetworkManager.onMessageReceived = { message ->
            runOnUiThread {
                val parts = message.split(",")
                if (parts.size == 4) {
                    try {
                        val fromRow = parts[0].toInt()
                        val fromCol = parts[1].toInt()
                        val toRow = parts[2].toInt()
                        val toCol = parts[3].toInt()
                        chessBoard.makeMoveFromNetwork(fromRow, fromCol, toRow, toCol)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Ошибка хода: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Подключение / запуск сервера
        if (isHost) {
            statusText.text = "Запускаю сервер..."
            NetworkManager.startServer { ip ->
                runOnUiThread {
                    statusText.text = "Ваш IP: $ip\nОжидаю соперника..."
                }
            }
        } else {
            statusText.text = "Подключаюсь к $hostIp..."
            NetworkManager.connectToServer(hostIp,
                onSuccess = {
                    runOnUiThread { statusText.text = "Вы играете чёрными" }
                },
                onFail = { error ->
                    runOnUiThread { statusText.text = "Ошибка: $error" }
                }
            )
        }

        NetworkManager.onConnected = {
            runOnUiThread {
                statusText.text = if (isHost) "Вы играете белыми — ваш ход" else "Вы играете чёрными — ждём ход белых"
            }
        }

        NetworkManager.onDisconnected = {
            runOnUiThread {
                statusText.text = "Соединение потеряно"
                Toast.makeText(this, "Соперник отключился", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { NetworkManager.disconnect() } catch (e: Exception) {}
    }
}

