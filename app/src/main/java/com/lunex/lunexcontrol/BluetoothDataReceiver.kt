package com.lunex.lunexcontrol

interface BluetoothDataReceiver {
    fun onReceiveTemperature(temp: String)
    fun onReceiveHumidity(humidity: String)
}
