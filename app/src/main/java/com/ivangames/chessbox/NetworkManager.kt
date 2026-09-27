package com.ivangames.chessbox

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object NetworkManager {

    private const val TAG = "NetworkManager"
    private const val PORT = 8888

    var isHost = false
        private set

    var isConnected = false
        private set

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    // Колбэк: пришли данные от другого игрока
    var onMessageReceived: ((String) -> Unit)? = null

    // Колбэк: подключились / отключились
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    // ============ ХОСТ ============

    fun startServer(onReady: (String) -> Unit) {
        isHost = true
        thread {
            try {
                serverSocket = ServerSocket(PORT)
                val ip = getLocalIpAddress()
                Log.d(TAG, "Сервер запущен на $ip:$PORT")
                onReady(ip)

                val socket = serverSocket!!.accept()
                Log.d(TAG, "Клиент подключился: ${socket.inetAddress}")
                clientSocket = socket
                setupStreams(socket)
                isConnected = true
                onConnected?.invoke()
                listenForMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сервера: ${e.message}")
            }
        }
    }

    // ============ КЛИЕНТ ============

    fun connectToServer(ip: String, onSuccess: () -> Unit, onFail: (String) -> Unit) {
        isHost = false
        thread {
            try {
                val socket = Socket(ip, PORT)
                Log.d(TAG, "Подключились к $ip:$PORT")
                clientSocket = socket
                setupStreams(socket)
                isConnected = true
                onSuccess()
                onConnected?.invoke()
                listenForMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка подключения: ${e.message}")
                onFail("Не удалось подключиться: ${e.message}")
            }
        }
    }

    // ============ ОБЩЕЕ ============

    private fun setupStreams(socket: Socket) {
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        writer = PrintWriter(socket.getOutputStream(), true)
    }

    private fun listenForMessages() {
        thread {
            try {
                while (isConnected) {
                    val line = reader?.readLine() ?: break
                    onMessageReceived?.invoke(line)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка чтения: ${e.message}")
            } finally {
                isConnected = false
                onDisconnected?.invoke()
            }
        }
    }

    fun sendMessage(message: String) {
        if (!isConnected) return
        thread {
            try {
                writer?.println(message)
                Log.d(TAG, "Отправлено: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки: ${e.message}")
            }
        }
    }

    fun disconnect() {
        isConnected = false
        try { clientSocket?.close() } catch (e: Exception) {}
        try { serverSocket?.close() } catch (e: Exception) {}
        clientSocket = null
        serverSocket = null
        reader = null
        writer = null
    }

    // Получить IP-адрес устройства в локальной сети
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения IP: ${e.message}")
        }
        return "Не удалось определить IP"
    }
}
