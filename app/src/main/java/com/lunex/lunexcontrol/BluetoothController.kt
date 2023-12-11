package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.os.Looper
import android.util.Log

class BluetoothController(private val adapter: BluetoothAdapter) {
    private var connectThread: ConnectThread? = null
    private var connectedDeviceAddress: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val disconnectRunnable = Runnable {
        if (DEBUG)  Log.d("BluetoothDebug", "disconnectRunnable executed")
    }

    @Volatile private var isConnected: Boolean = false
    @Volatile private var isDisconnecting: Boolean = false


    @SuppressLint("MissingPermission")
    fun connect(mac: String, listener: Listener) {
        if (adapter.isEnabled && mac.isNotEmpty()) {
            val device = adapter.getRemoteDevice(mac)
            if (DEBUG)  Log.d("BluetoothDebug", "Trying to connect to device: $device ($mac)")
            connectedDeviceAddress = device.address
            synchronized(this) {
                if (!isConnected) {
                    isConnected = true
                    // Notifique o listener sobre a conexão
                }
            }
            // Aqui você cria a ConnectThread passando o dispositivo e o listener
            connectThread = ConnectThread(device, object : Listener {
                override fun onReceive(message: String) {
                    handler.removeCallbacks(disconnectRunnable)
                    handler.postDelayed(disconnectRunnable, 4000) // 3 segundos de atraso
                    listener.onReceive(message)
                }

                override fun onConnected(bluetoothConnected: String) {
                    isConnected = true
                    // Quando conectado, notifica o sistema de gerenciamento do Bluetooth
                    BluetoothManagerApp.getInstance().handleConnection(device.name)
                    listener.onConnected(bluetoothConnected)
                    if (DEBUG) Log.d("DebugLunex", "chamando listener.onConnected em onConnected() no BluetoothController")
                }

                override fun onDisconnected() {
                    isConnected = false
                    // Quando desconectado, limpa o endereço e notifica o sistema de gerenciamento
                    connectedDeviceAddress = null
                    BluetoothManagerApp.getInstance().handleDisconnection()
                    listener.onDisconnected()
                    if (DEBUG)  Log.d("DebugLunex", "chamando listener.onDisconnected em onDisconnected() no BluetoothController")
                }

                // Estas funções são chamadas quando a temperatura e a umidade são recebidas
                override fun onReceiveTemperature(temp: String) {
                    listener.onReceiveTemperature(temp)
                }

                override fun onReceiveHumidity(humidity: String) {
                    listener.onReceiveHumidity(humidity)
                }

                override fun onReceiveHotState(whiteHotValue: Int) {
                    if (DEBUG)    Log.d("testedebug", "log no onReceiveHotState do BluetoothController")
                    listener.onReceiveHotState(whiteHotValue)
                }

                override fun onReceiveColdState(whiteColdValue: Int) {
                    if (DEBUG)    Log.d("testedebug", "log no onReceiveColdState do BluetoothController")
                    listener.onReceiveColdState(whiteColdValue)
                }
            })
            connectThread?.start()
        }
    }

    // Envia uma mensagem pelo socket de conexão Bluetooth
    fun sendMessage(message: String){
        connectThread?.sendMessage(message)
    }

    fun disconnect() {
        synchronized(this) {
            if (isDisconnecting) {
                if (DEBUG) Log.d("DebugLunex", "Desconexão já em andamento")
                return
            }
            isDisconnecting = true
        }

        if (DEBUG) Log.d("DebugLunex", "disconnect() no BluetoothController")

        try {
            connectThread?.closeConnection()
            if (DEBUG) Log.d("DebugLunex", "Tentando fechar a conexão")
        } catch (e: Exception) {
            if (DEBUG) Log.d("DebugLunex", "Erro ao fechar a conexão: ${e.message}")
        } finally {
            synchronized(this) {
                if (isConnected) {
                    isConnected = false
                    // Notifique o listener sobre a desconexão
                }
                isDisconnecting = false
            }
            Log.d("DebugLunex", "Desconexão concluída")
        }
    }

    // Retorna o endereço do dispositivo conectado, se houver
    fun getConnectedDeviceAddress(): String? {
        return connectedDeviceAddress
    }

    companion object{
        const val BLUETOOTH_CONNECTED = "Bluetooth conectado"
        const val BLUETOOTH_NO_CONNECTED = "Bluetooth desconectado"
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }

    interface Listener{
        fun onReceive(message: String)
        fun onConnected(bluetoothConnected: String)
        fun onDisconnected()
        fun onReceiveTemperature(temp: String)
        fun onReceiveHumidity(humidity: String)
        fun onReceiveHotState(whiteHotValue: Int)
        fun onReceiveColdState(whiteColdValue: Int)
    }
}
