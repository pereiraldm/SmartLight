package com.lunex.lunexcontrol


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData

class BluetoothManagerApp private constructor() {

    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var bluetoothController: BluetoothController
    private lateinit var applicationContext: Context
    private var connectionListener: ConnectionListener? = null
    private var realDeviceName: String? = null

    enum class ConnectionState {
        CONNECTED, DISCONNECTED,
    }

    val connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    private val connectedDeviceName = MutableLiveData<String?>()

    interface ConnectionListener {
        fun onBluetoothConnected()
        fun onBluetoothDisconnected()
    }
    fun initializeBluetooth(context: Context) {
        if (!::bluetoothController.isInitialized) {
            this.applicationContext = context.applicationContext
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            btAdapter = bluetoothManager.adapter ?: return
            bluetoothController = BluetoothController(btAdapter)
        }
    }

    fun setConnectionListener(listener: ConnectionListener?) {
        this.connectionListener = listener
    }

    fun isConnected(): Boolean {
        return connectionState.value == ConnectionState.CONNECTED
    }

    fun connect(deviceAddress: String, listener: DeviceListFragment) {
        bluetoothController.connect(deviceAddress, listener)
        if (DEBUG)   Log.d("DebugLunex", "connect chamado do BluetoothManagerApp")
    }

    fun disconnect() {
        bluetoothController.disconnect()
        if (DEBUG)   Log.d("DebugLunex", "disconnect chamado do BluetoothManagerApp")
    }

    fun sendMessage(message: String) {
        if (DEBUG)   Log.d("DebugBluetoothManagerAp", "sendMessage called with message: $message")
        bluetoothController.sendMessage(message)
    }

    fun setConnectedDeviceName(deviceName: String?) {
        connectedDeviceName.postValue(deviceName)
    }

    private fun setRealDeviceName(deviceName: String?) {
        realDeviceName = deviceName
    }

    fun getConnectedDeviceAddress(): String? {
        if (!::bluetoothController.isInitialized) {
            return null
        }
        return bluetoothController.getConnectedDeviceAddress()
    }

    fun handleConnection(deviceName: String) {
        connectionState.postValue(ConnectionState.CONNECTED)
        if (DEBUG)    Log.w("BluetoothManagerApp", "connectionState: $connectionState ")
        setConnectedDeviceName(deviceName)
        if (DEBUG)   Log.w("BluetoothManagerApp", "setConnectedDeviceName(device.name): $deviceName ")
        setRealDeviceName(deviceName)
        if (DEBUG)    Log.w("BluetoothManagerApp", "setRealDeviceName(device.name): $deviceName ")
        if (DEBUG)    Log.d("DebugLunex", "handleConnection chamado do BluetoothManagerApp")
    }

    fun handleDisconnection() {
        connectionState.postValue(ConnectionState.DISCONNECTED)
        setConnectedDeviceName(null)
        setRealDeviceName(null)
        if (DEBUG)    Log.d("DebugLunex", "handleDisconnection chamado do BluetoothManagerApp")
    }

    companion object {
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
        private var instance: BluetoothManagerApp? = null

        fun getInstance(): BluetoothManagerApp {
            if (instance == null) {
                synchronized(BluetoothManagerApp::class.java) {
                    if (instance == null) {
                        instance = BluetoothManagerApp()
                    }
                }
            }
            return instance!!
        }
    }
}
