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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException

class MainActivity : AppCompatActivity() {
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
        sharedViewModel.temperatureData.observe(this) { temp ->
            temperatureText.text = "$temp ºC"
            resetUpdateValuesTimer()
        }

        sharedViewModel.humidityData.observe(this) { humidity ->
            humidityText.text = "$humidity %"
            resetUpdateValuesTimer()
        }
        showWelcomeDialog()
    }

    fun onBtnToggleChanged(isChecked: Boolean) {
        val advancedFragment = supportFragmentManager.findFragmentByTag(AdvancedFragment::class.java.simpleName) as? AdvancedFragment
        advancedFragment?.setAdvancedToggleEnabled(!isChecked)
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
                if (DEBUG) Log.d("BluetoothDebug", "BluetoothFragment onResume chamado")
            } catch (e: IOException) {
                // Trate o erro
            }
        }
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

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        registerReceiver(bluetoothConnectionReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(bluetoothConnectionReceiver)
    }

    private fun showWelcomeDialog() {
        val sharedPref = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val showDialog = sharedPref.getBoolean("ShowWelcomeDialog", true)

        if (showDialog) {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Bem-vindo!")
            alertDialog.setMessage("Assista ao vídeo de manual de uso.")
            alertDialog.setNegativeButton("Ok") { dialog, which ->
                // Abrir o link do vídeo
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/IJ76DUCN6bk"))
                startActivity(intent)
            }
            alertDialog.setPositiveButton("Não mostrar novamente") { _, _ ->
                sharedPref.edit().putBoolean("ShowWelcomeDialog", false).apply()
            }
            alertDialog.show()
        }
    }

    companion object {
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }
}
