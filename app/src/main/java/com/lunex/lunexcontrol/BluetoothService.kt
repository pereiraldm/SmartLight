package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.util.UUID

class BluetoothService : Service() {
    private var isConnected = false
    private val binder = LocalBinder()
    private val btManager = BluetoothManagerApp.getInstance()

    private val bluetoothAdapter: BluetoothAdapter?
        get() {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            return bluetoothManager?.adapter
        }

    private fun createNotification(): Notification {
        val channelId = "BluetoothServiceChannel"
        val channelName = "Bluetooth Connection Service"
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Crie o canal de notificação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)  // Esta linha foi modificada
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bluetooth Connection")
            .setContentText("Maintaining Bluetooth connection...")
            .setSmallIcon(R.drawable.ic_bluetooth)  // Substitua pelo ícone que você deseja usar
            .setContentIntent(pendingIntent)
            .build()
    }

    // Inicialize as variáveis que você precisa aqui, como o BluetoothAdapter, etc.
    private val connectedSockets = mutableListOf<BluetoothSocket>()

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID padrão para dispositivos Bluetooth

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        val deviceName = intent?.getStringExtra("device_name")
        if (deviceName != null) {
            val device = bluetoothAdapter?.bondedDevices?.find { it.name == deviceName }
            if (device != null) {
                connectToDevice(device)
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        val socket: BluetoothSocket? = try {
            device.createRfcommSocketToServiceRecord(uuid)
        } catch (e: IOException) {
            if (DEBUG) Log.e("BluetoothDebug", "Erro ao criar o socket RFComm", e)
            null
        }

        socket?.let {
            try {
                it.connect()
                connectedSockets.add(it)
                isConnected = true
                if (DEBUG) Log.d("BluetoothDebug", "Conexão estabelecida com sucesso com ${device.name}")
                btManager.setConnectedDeviceName(device.name)
            } catch (e: IOException) {
                if (DEBUG) Log.e("BluetoothDebug", "Erro ao conectar ao dispositivo ${device.name}", e)
                btManager.setConnectedDeviceName(null)
                try {
                    it.close()
                } catch (e2: IOException) {
                    if (DEBUG) Log.e("BluetoothDebug", "Erro ao fechar o socket", e2)
                }
            }
        }
    }


    private fun disconnectAllDevices() {
        for (socket in connectedSockets) {
            try {
                socket.close()
                isConnected = false
                btManager.setConnectedDeviceName(null)
            } catch (e: IOException) {
                if (DEBUG) Log.e("BluetoothDebug", "Erro ao fechar o socket", e)
            }
        }
        connectedSockets.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectAllDevices()
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }
}

