package com.lunex.lunexcontrol

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException

class MainActivity : AppCompatActivity(), HomeFragment.OnDataSendToActivity, BluetoothController.Listener, BluetoothDataReceiver, AdvancedFragment.AdvancedFragmentInteractionListener {
    private var bluetoothService: BluetoothService? = null
    private lateinit var temperatureText: TextView
    private lateinit var humidityText: TextView
    private lateinit var sharedViewModel: SharedBluetoothViewModel
    private lateinit var logoImageView: ImageView
    private val updateValuesRunnable = Runnable {
        sharedViewModel.temperatureData.postValue("--")
        sharedViewModel.humidityData.postValue("--")
    }
    private lateinit var bluetoothConnectionReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize your BroadcastReceiver here
        bluetoothConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Handle the Bluetooth device connected event here
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        bottomNav.post {
            val navController = findNavController(R.id.nav_host_fragment)
            bottomNav.setupWithNavController(navController)
        }

        val intent = Intent(this, BluetoothService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Incorporando o layout personalizado à Toolbar
        val customToolbarLayout = layoutInflater.inflate(R.layout.toolbar_layout, null)
        toolbar.addView(customToolbarLayout)

        // Agora, você pode referenciar os TextViews de temperatura e umidade e atualizá-los quando necessário
        temperatureText = customToolbarLayout.findViewById(R.id.temperature_text)
        humidityText = customToolbarLayout.findViewById(R.id.humidity_text)
        logoImageView = customToolbarLayout.findViewById(R.id.logo)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        logoImageView.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lunex.com.br/"))
            startActivity(browserIntent)
        }

        sharedViewModel = ViewModelProvider(this)[SharedBluetoothViewModel::class.java]

        sharedViewModel.temperatureData.observe(this, Observer { temp ->
            temperatureText.text = "$temp ºC"
            resetUpdateValuesTimer()
        })

        sharedViewModel.humidityData.observe(this, Observer { humidity ->
            humidityText.text = "$humidity %"
            resetUpdateValuesTimer()
        })
    }

    fun onBtnToggleChanged(isChecked: Boolean) {
        val advancedFragment = supportFragmentManager.findFragmentByTag(AdvancedFragment::class.java.simpleName) as? AdvancedFragment
        advancedFragment?.setAdvancedToggleEnabled(!isChecked)
    }

    override fun onAdvancedModeToggled(isEnabled: Boolean) {
        Log.d("DebugMainActivity", "onAdvancedModeToggled called with: $isEnabled")
        val homeFragment = getHomeFragment()
        homeFragment?.setBtnToggleEnabled(!isEnabled)
    }

    private fun getHomeFragment(): HomeFragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragments = navHostFragment?.childFragmentManager?.fragments
        return fragments?.firstOrNull { it is HomeFragment } as? HomeFragment
    }

    override fun onReceiveTemperature(temp: String) {
        sharedViewModel.temperatureData.postValue(temp)
        resetUpdateValuesTimer()
    }

    override fun onReceiveHumidity(humidity: String) {
        sharedViewModel.humidityData.postValue(humidity)
        resetUpdateValuesTimer()
    }

    private fun resetUpdateValuesTimer() {
        handler.removeCallbacks(updateValuesRunnable)
        handler.postDelayed(updateValuesRunnable, 3000)  // 3 segundos de delay
    }

    private var connectedSockets: List<BluetoothSocket>? = null

    override fun onDestroy() {
        super.onDestroy()
        connectedSockets?.forEach {
            try {
                it.close()
                Log.d("BluetoothDebug", "BluetoothFragment onResume chamado")
            } catch (e: IOException) {
                // Trate o erro
            }
        }
    }

    override fun sendData(data: String) {
        // Aqui você pode implementar a lógica para lidar com os dados enviados.
        // Por exemplo, se você quer enviar este dado via Bluetooth ou qualquer outro método.
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }
    }

    override fun onReceive(message: String) {
        // Quando uma mensagem é recebida, você pode resetar o timer
        resetUpdateValuesTimer()
    }

    override fun onConnected(bluetoothConnected: String) {
        TODO("Not yet implemented")
    }

    override fun onDisconnected() {
        TODO("Not yet implemented")
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        registerReceiver(bluetoothConnectionReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothConnectionReceiver)
    }

}
