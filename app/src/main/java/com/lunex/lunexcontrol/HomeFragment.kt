package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.lunex.lunexcontrol.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    // Referências para os botões e lâmpadas
    private lateinit var btnToggle: AppCompatButton
    private lateinit var btnFrio: Button
    private lateinit var btnNeutro: Button
    private lateinit var btnQuente: Button
    private lateinit var lampWhite: ImageView
    private lateinit var lampYellow: ImageView
    private lateinit var nomeTextView: TextView
    private lateinit var bluetoothStatusIcon: ImageView
    private var bluetoothService: BluetoothService? = null
    private var isBound = false
    private val btManager = BluetoothManagerApp.getInstance()
    private lateinit var binding: FragmentHomeBinding
    private lateinit var sharedViewModel: SharedBluetoothViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        activity?.registerReceiver(bluetoothStateReceiver, filter)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                binding.bluetoothStatusIcon.setImageResource(R.drawable.ic_bluetooth_on)
            } else {
                binding.bluetoothStatusIcon.setImageResource(R.drawable.ic_bluetooth_off)
            }
        } else {
            Toast.makeText(context, "Este dispositivo não suporta Bluetooth!", Toast.LENGTH_SHORT).show()
        }

        // Inicializar as referências
        btnToggle = view.findViewById(R.id.btn_toggle)
        btnFrio = view.findViewById(R.id.btn_frio)
        btnNeutro = view.findViewById(R.id.btn_neutro)
        btnQuente = view.findViewById(R.id.btn_quente)
        lampWhite = view.findViewById(R.id.lamp_white)
        lampYellow = view.findViewById(R.id.lamp_yellow)
        nomeTextView = view.findViewById(R.id.connected_devices)

        btnToggle.setOnClickListener{
            btManager.sendMessage("00")
            desabilitar(btnQuente)
            desabilitar(btnFrio)
            desabilitar(btnToggle)
            desabilitar(btnNeutro)
            btnToggle.postDelayed({
                habilitar(btnQuente)
                habilitar(btnFrio)
                habilitar(btnToggle)
                habilitar(btnNeutro)
            }, 1000)
        }
        // Listener para o botão Frio
        btnFrio.setOnClickListener {
            btManager.sendMessage("01")
            desabilitar(btnQuente)
            desabilitar(btnFrio)
            desabilitar(btnToggle)
            desabilitar(btnNeutro)
            btnFrio.postDelayed({
                habilitar(btnQuente)
                habilitar(btnFrio)
                habilitar(btnToggle)
                habilitar(btnNeutro)
            }, 1000) // Atraso de 1 segundos em milissegundos
        }

        // Listener para o botão Neutro
        btnNeutro.setOnClickListener {
            btManager.sendMessage("11")
            desabilitar(btnQuente)
            desabilitar(btnFrio)
            desabilitar(btnToggle)
            desabilitar(btnNeutro)
            btnNeutro.postDelayed({
                habilitar(btnQuente)
                habilitar(btnFrio)
                habilitar(btnToggle)
                habilitar(btnNeutro)
            }, 1000) // Atraso de 1 segundos em milissegundos
        }

        // Listener para o botão Quente
        btnQuente.setOnClickListener {
            btManager.sendMessage("10")
            desabilitar(btnQuente)
            desabilitar(btnFrio)
            desabilitar(btnToggle)
            desabilitar(btnNeutro)
            btnQuente.postDelayed({
                habilitar(btnQuente)
                habilitar(btnFrio)
                habilitar(btnToggle)
                habilitar(btnNeutro)
            }, 1000) // Atraso de 1 segundos em milissegundos
        }

        bluetoothStatusIcon = view.findViewById(R.id.bluetoothStatusIcon)

        bluetoothStatusIcon.setOnClickListener {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled) {
                    Toast.makeText(context, "Bluetooth está ligado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Bluetooth está desligado!", Toast.LENGTH_SHORT).show()
                }
            }   else {
                Toast.makeText(context, "Este dispositivo não suporta Bluetooth!", Toast.LENGTH_SHORT).show()
            }
        }

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
        sharedViewModel.hotValue.observe(viewLifecycleOwner) { whiteHotValue ->
            updateHotState(whiteHotValue)
        }
        sharedViewModel.coldValue.observe(viewLifecycleOwner) { whiteColdValue ->
            updateColdState(whiteColdValue)
        }
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

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent?.action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        binding.bluetoothStatusIcon.setImageResource(R.drawable.ic_bluetooth_off)
                    }

                    BluetoothAdapter.STATE_ON -> {
                        binding.bluetoothStatusIcon.setImageResource(R.drawable.ic_bluetooth_on)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unregisterReceiver(bluetoothStateReceiver)
    }

    override fun onResume() {
        super.onResume()
        val advancedFragment = activity?.supportFragmentManager?.findFragmentByTag(AdvancedFragment::class.java.simpleName) as? AdvancedFragment
        val isAdvancedToggleChecked = advancedFragment?.advancedToggleButton?.isChecked ?: false
        setBtnToggleEnabled(!isAdvancedToggleChecked)
    }

    override fun onStart() {
        super.onStart()
        Intent(activity, BluetoothService::class.java).also { intent ->
            activity?.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            activity?.unbindService(serviceConnection)
            isBound = false
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCustomDeviceName(): String {
        val defaultAddress = BluetoothManagerApp.getInstance().getConnectedDeviceAddress()

        // Adicionando mensagem de log para monitorar o valor de defaultAddress
        if (DEBUG) Log.d("CustomDeviceNameDebug", "Valor de defaultAddress: $defaultAddress")

        val sharedPref = activity?.getSharedPreferences("DeviceNames", Context.MODE_PRIVATE)
        val customName = sharedPref?.getString(defaultAddress, "-") ?: "-"

        // Adicionando mensagem de log para monitorar o valor final de customName
        if (DEBUG) Log.d("CustomDeviceNameDebug", "Nome do dispositivo customizado: $customName")

        return customName
    }

    @SuppressLint("SetTextI18n")
    private fun setDeviceName() {
        nomeTextView.text = "NENHUM DISPOSITIVO CONECTADO"
    }

    @SuppressLint("SetTextI18n")
    private fun updateConnectedDeviceName() {
        val sharedPref = activity?.getSharedPreferences("DeviceNames", Context.MODE_PRIVATE)
        sharedPref?.getString("ESP32test", "NOT_FOUND")
        val deviceName = getCustomDeviceName()
        nomeTextView.text = "DISPOSITIVO CONECTADO: $deviceName"
        // Adicione esta linha para exibir uma mensagem de log
    }

    private fun setBtnToggleEnabled(isEnabled: Boolean) {
        if (DEBUG) Log.d("DebugHomeFragment", "setBtnToggleEnabled called with: $isEnabled")
        btnToggle.isEnabled = isEnabled
        btnFrio.isEnabled = isEnabled
        btnNeutro.isEnabled = isEnabled
        btnQuente.isEnabled = isEnabled
    }

    private fun btConnected() {
        if (DEBUG) Log.d("DebugHomeFragment", "btConnected called")
        btnToggle.isVisible = true
        btnQuente.isVisible = true
        btnNeutro.isVisible = true
        btnFrio.isVisible = true
        lampWhite.isVisible = true
        lampYellow.isVisible = true
    }

    private fun btDisconnected() {
        if (DEBUG) Log.d("DebugHomeFragment", "btDisconnected called")
        btnToggle.isVisible = false
        btnQuente.isVisible = false
        btnNeutro.isVisible = false
        btnFrio.isVisible = false
        lampWhite.isVisible = false
        lampYellow.isVisible = false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun desabilitar (button: Button){
        button.isEnabled = false
        context?.let { button.setTextColor(it.getColor(R.color.azul_desa)) }
        context?.let { button.setBackgroundColor(it.getColor(R.color.laranja_desa)) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun habilitar (button: Button){
        button.isEnabled = true
        context?.let { button.setTextColor(it.getColor(R.color.azul_padrao)) }
        context?.let { button.setBackgroundColor(it.getColor(R.color.laranja_padrao)) }
    }

    companion object {
        const val DEBUG = true // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }
}