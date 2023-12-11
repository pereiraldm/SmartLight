package com.lunex.lunexcontrol

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedBluetoothViewModel : ViewModel() {
    val temperatureData: MutableLiveData<String> = MutableLiveData()
    val humidityData: MutableLiveData<String> = MutableLiveData()
    val hotValue: MutableLiveData<Int> = MutableLiveData()
    val coldValue: MutableLiveData<Int> = MutableLiveData()
}

