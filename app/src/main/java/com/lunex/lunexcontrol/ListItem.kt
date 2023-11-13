package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

@SuppressLint("MissingPermission")
data class ListItem(
    val device: BluetoothDevice,
    val isChecked: Boolean,
    var customName: String = device.name
)