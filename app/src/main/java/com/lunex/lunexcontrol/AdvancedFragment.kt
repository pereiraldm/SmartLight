package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class AdvancedFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var seekBarValue: TextView
    lateinit var advancedToggleButton: ToggleButton
    private lateinit var connected_devices_advanced: TextView
    private lateinit var lampYellow: ImageView
    private lateinit var lampWhite: ImageView
    private var isResuming = false
    private lateinit var sharedViewModel: SharedBluetoothViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_advanced, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seekBar = view.findViewById(R.id.seekBar)
        seekBarValue = view.findViewById(R.id.seekBarValue)
        advancedToggleButton = view.findViewById(R.id.advancedToggleButton)
        connected_devices_advanced = view.findViewById(R.id.connected_devices_advanced)
        lampYellow = view.findViewById(R.id.lamp_yellow)
        lampWhite = view.findViewById(R.id.lamp_white)

        val sharedPref = activity?.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPref?.getBoolean("AdvancedToggleState", false) ?: false
        val savedSeekBarValue = sharedPref?.getInt("SeekBarValue", 0) ?: 0
        seekBar.progress = savedSeekBarValue
        advancedToggleButton.setOnCheckedChangeListener(null)  // Temporariamente remover o listener
        advancedToggleButton.isChecked = false
        seekBar.isEnabled = false
        advancedToggleButton.setOnCheckedChangeListener { _, isChecked ->
            (activity as? MainActivity)?.onBtnToggleChanged(isChecked)
            if (isChecked) {
                BluetoothManagerApp.getInstance().sendMessage("avancado")
                desabilitar(advancedToggleButton)
                seekBar.isEnabled = false
                advancedToggleButton.postDelayed({
                    habilitar(advancedToggleButton)
                    val adjustedValue = getAdjustedSeekBarValue()
                    BluetoothManagerApp.getInstance().sendMessage(adjustedValue.toString())
                    seekBar.isEnabled = true
                }, 1000)
            } else {
                BluetoothManagerApp.getInstance().sendMessage("desligarAvancado")
                desabilitar(advancedToggleButton)
                seekBar.isEnabled = false
                advancedToggleButton.postDelayed({
                    habilitar(advancedToggleButton)
                }, 1000)
            }
            val sharedPref = activity?.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            sharedPref?.edit()?.putBoolean("AdvancedToggleState", isChecked)?.apply()
        }

        seekBar.max = 6500 - 3000  // Ajuste o máximo do SeekBar para 3500 (6500 - 3000)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val adjustedProgress = progress + 3000  // Ajuste o valor para o intervalo 3000-6500
                seekBarValue.text = "Valor: $adjustedProgress (3000 - 6500)"


                lampYellow.setImageResource(R.drawable.ic_quente)
                lampWhite.setImageResource(R.drawable.ic_frio)
                val alphaValue = (progress / 3500.0 * 255).toInt()

                lampYellow.imageAlpha = 255 - alphaValue
                if (DEBUG) Log.d(
                    "testedebug",
                    "lampYellow 255 - alphaValue: ${255 - alphaValue}")
                lampWhite.imageAlpha = alphaValue
                if (DEBUG) Log.d(
                    "testedebug",
                    "lampWhite alphaValue: $alphaValue"
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Nada a fazer aqui
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (!isResuming) {
                    val progress = seekBar?.progress ?: return
                    val adjustedProgress = (progress / 3500.0 * 1024).toInt()  // Converta o valor para o intervalo 0-1024
                    BluetoothManagerApp.getInstance().sendMessage(adjustedProgress.toString())
                    // Salvar o estado do SeekBar
                    val sharedPref = activity?.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    sharedPref?.edit()?.putInt("SeekBarValue", progress)?.apply()
                }
            }
        })
        getCustomDeviceName()
        // Adicione o observador para o estado de conexão Bluetooth
        BluetoothManagerApp.getInstance().connectionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                BluetoothManagerApp.ConnectionState.CONNECTED -> {
                    updateConnectedDeviceName()
                    btConnected()
                }

                BluetoothManagerApp.ConnectionState.DISCONNECTED -> {
                    setDeviceName()
                    btDisconnected()
                }

                else -> {}
            }
        }
        sharedViewModel = ViewModelProvider(requireActivity())[SharedBluetoothViewModel::class.java]
        if (!advancedToggleButton.isChecked) {
            sharedViewModel.hotValue.observe(viewLifecycleOwner) { whiteHotValue ->
                 updateHotState(whiteHotValue)
            }
            sharedViewModel.coldValue.observe(viewLifecycleOwner) { whiteColdValue ->
                   updateColdState(whiteColdValue)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val sharedPref = activity?.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPref?.edit()?.remove("AdvancedToggleState")?.apply()
    }

    fun setAdvancedToggleEnabled(isEnabled: Boolean) {
        advancedToggleButton.isEnabled = isEnabled
    }

    @SuppressLint("SetTextI18n")
    private fun getCustomDeviceName(): String {
        val defaultAddress = BluetoothManagerApp.getInstance().getConnectedDeviceAddress()
        val sharedPref = activity?.getSharedPreferences("DeviceNames", Context.MODE_PRIVATE)
        return sharedPref?.getString(defaultAddress, "-") ?: "-"
    }

    @SuppressLint("SetTextI18n")
    private fun setDeviceName() {
        connected_devices_advanced.text = "NENHUM DISPOSITIVO CONECTADO"
    }

    @SuppressLint("SetTextI18n")
    private fun updateConnectedDeviceName() {
        val sharedPref = activity?.getSharedPreferences("DeviceNames", Context.MODE_PRIVATE)
        sharedPref?.getString("ESP32test", "NOT_FOUND")
        val deviceName = getCustomDeviceName()

        connected_devices_advanced.text = "DISPOSITIVO CONECTADO: $deviceName"
    }

    private fun btConnected() {
        advancedToggleButton.isChecked = false
        seekBar.isEnabled = false
        advancedToggleButton.isVisible = true
        seekBar.isVisible = true
        lampWhite.isVisible = true
        lampYellow.isVisible = true
    }

    private fun btDisconnected() {
        advancedToggleButton.isVisible = false
        advancedToggleButton.isChecked = false
        seekBar.isEnabled = false
        seekBar.isVisible = false
        lampWhite.isVisible = false
        lampYellow.isVisible = false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun desabilitar (toggleButton: ToggleButton){
        toggleButton.isEnabled = false
        context?.let { toggleButton.setTextColor(it.getColor(R.color.azul_desa)) }
        context?.let { toggleButton.setBackgroundColor(it.getColor(R.color.laranja_desa)) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun habilitar (toggleButton: ToggleButton){
        toggleButton.isEnabled = true
        context?.let { toggleButton.setTextColor(it.getColor(R.color.azul_padrao)) }
        context?.let { toggleButton.setBackgroundColor(it.getColor(R.color.laranja_padrao)) }
    }

    private fun getAdjustedSeekBarValue(): Int {
        val progress = seekBar.progress
        return (progress / 3500.0 * 1024).toInt()
    }


    private fun updateHotState(whiteHotValue: Int) {
        if (whiteHotValue == 0) {
            lampYellow.setImageResource(R.drawable.ic_desligado)
        } else {
            val alphaValue = mapValueToAlpha(whiteHotValue)
            lampYellow.setImageResource(R.drawable.ic_quente)
            lampYellow.imageAlpha = alphaValue
        }
    }

    private fun updateColdState(whiteColdValue: Int) {
        if (whiteColdValue == 0) {
            lampWhite.setImageResource(R.drawable.ic_desligado)
        } else {
            val alphaValue = mapValueToAlpha(whiteColdValue)
            lampWhite.setImageResource(R.drawable.ic_frio)
            lampWhite.imageAlpha = alphaValue
        }
    }

    private fun mapValueToAlpha(value: Int): Int {
        // Mapeia o valor para um intervalo de transparência (por exemplo, de 0 a 255)
        return (value / 1024.0 * 255).toInt()
    }

    companion object {
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }
}

