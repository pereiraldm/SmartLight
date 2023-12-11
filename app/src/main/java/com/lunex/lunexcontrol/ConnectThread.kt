package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.util.*


class ConnectThread(
    mDevice: BluetoothDevice, // Campo adicionado para manter uma referência ao dispositivo Bluetooth
    private val listener: BluetoothController.Listener,
) : Thread() {
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var mSocket: BluetoothSocket? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        try {
            if (DEBUG)   Log.d("DebugLunex", "log antes do chamando mSocket = mDevice")
            mSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()))
            if (DEBUG)    Log.d("DebugLunex", "log depois do chamando mSocket = mDevice")
        } catch (_: IOException) {
            // Trate o erro conforme necessário
            if (DEBUG)    Log.d("DebugLunex", "log do chamando catch (_: IOException)")
        } catch (_: SecurityException) {
            // Trate o erro conforme necessário
            if (DEBUG)   Log.d("DebugLunex", "log do chamando catch (_: SecurityException)")
        }
    }

    override fun run() {
        if (DEBUG)   Log.d("DebugLunex", "chamando run() no ConnecteThread")
        try {
            if (DEBUG)    Log.d("DebugLunex", "log antes do chamando mSocket?.connect()")
            mSocket?.connect()
            if (DEBUG)   Log.d("DebugLunex", "log depois do chamando mSocket?.connect()")
            readMessage()
            if (DEBUG)   Log.d("DebugLunex", "log imediatamente antes de listener.onConnected(BluetoothController.BLUETOOTH_CONNECTED) no ConnectThread ")
            listener.onConnected(BluetoothController.BLUETOOTH_CONNECTED)
            if (DEBUG)   Log.d("DebugLunex", "chamando listener.onConnected em run() no ConnecteThread")
            listener.onReceive(BluetoothController.BLUETOOTH_CONNECTED)
        } catch (e: IOException) {
            if (DEBUG)   Log.e("DebugLunex", "IOException em ConnectThread: ${e.message}")
            listener.onDisconnected()
            listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
        } catch (e: SecurityException) {
            if (DEBUG)  Log.e("DebugLunex", "SecurityException em ConnectThread: ${e.message}")
        }
    }


    private val disconnectRunnable = Runnable {
    }

    private fun readMessage() {
        val buffer = ByteArray(256)
        while (true) {
            try {
                val length = mSocket?.inputStream?.read(buffer)
                val message = String(buffer, 0, length ?: 0)

                // Reset the timer every time a message is received
                handler.removeCallbacks(disconnectRunnable)
                handler.postDelayed(disconnectRunnable, 4000) // 5 seconds delay
                when {
                    message.contains("H:") && message.contains("C:") -> {
                        // Processar mensagem do estado dos LEDs
                        val hotValue = message.substringAfter("H:").substringBefore(";")
                        val coldValue = message.substringAfter("C:")
                           val whiteHotValue = hotValue.toInt()
                           val whiteColdValue = coldValue.toInt()
                            if (DEBUG)    Log.d("testedebug", "Dados recebidos em readMessage() do ConnectThread, hotValue: $hotValue e coldValue: $coldValue")
                            listener.onReceiveHotState(whiteHotValue)
                            listener.onReceiveColdState(whiteColdValue)
                    }
                    message.contains("T:") && message.contains("U:") -> {
                        // Processar mensagem de temperatura e umidade
                        val temp = message.substringAfter("T:").substringBefore("º")
                        val humidity = message.substringAfter("U:").substringBefore("%")
                        listener.onReceiveTemperature(temp)
                        listener.onReceiveHumidity(humidity)
                    }
                    else -> {
                        // Processar outras mensagens
                        listener.onReceive(message)
                    }
                }
            } catch (_: IOException) {
                // Tratar exceção, se necessário
            }
        }
    }


    fun sendMessage(message: String){
        try {
            mSocket?.outputStream?.write(message.toByteArray())
        } catch (_: IOException){

        }
    }

    @SuppressLint("MissingPermission")
    fun closeConnection() {
        if (DEBUG)  Log.d("DebugLunex", "Tentando fechar a conexão")
        try {
            mSocket?.let { socket ->
                if (socket.isConnected) {
                    socket.close()
                    if (DEBUG)   Log.d("DebugLunex", "Socket fechado com sucesso")
                } else {
                    if (DEBUG)    Log.d("DebugLunex", "Socket já estava desconectado")
                }
            } ?:
            if (DEBUG) Log.d("DebugLunex", "mSocket é nulo") else TODO()
        } catch (e: IOException) {
            if (DEBUG)    Log.e("DebugLunex", "Erro ao fechar socket", e)
        } finally {
            listener.onDisconnected()
            BluetoothManagerApp.getInstance().handleDisconnection()
            if (DEBUG)    Log.d("DebugLunex", "Listener notificado sobre a desconexão")
        }
    }
    companion object {
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }

}