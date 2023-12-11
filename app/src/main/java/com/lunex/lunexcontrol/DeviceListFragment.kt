package com.lunex.lunexcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.lunex.lunexcontrol.databinding.FragmentBluetoothBinding
import com.lunex.lunexcontrol.databinding.FragmentBluetoothBinding.*
import kotlin.concurrent.*

class DeviceListFragment : Fragment(), ItemAdapter.Listener, BluetoothController.Listener,
    BluetoothManagerApp.ConnectionListener {

    override var isConnected: Boolean = BluetoothManagerApp.getInstance().isConnected()
    private var preferences: SharedPreferences? = null
    private lateinit var itemAdapter: ItemAdapter
    private var bAdapter: BluetoothAdapter? = null
    private lateinit var binding: FragmentBluetoothBinding
    private lateinit var btLauncher: ActivityResultLauncher<Intent>
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var btAdapter: BluetoothAdapter
    private var isBound = false
    private var bluetoothService: BluetoothService? = null
    private lateinit var sharedViewModel: SharedBluetoothViewModel
    private var bottomNavigationView: BottomNavigationView? = null
    private var progressBar: ProgressBar? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isConnected = arguments?.getBoolean("isConnected") ?: false
        binding = inflate(inflater, container, false)
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btManager = BluetoothManagerApp.getInstance() // Pega a instância do BluetoothManagerApp
        btManager.initializeBluetooth(requireContext())

        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        activity?.registerReceiver(disconnectReceiver, filter)

        bottomNavigationView = activity?.findViewById(R.id.bottomNavigationView)
        progressBar = activity?.findViewById(R.id.progressBar)

        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = bluetoothManager.adapter ?: return

        preferences = activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        binding.imBluetoothOn.setOnClickListener {
            btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        binding.connect.setOnClickListener {
            val pref = activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
            val mac = pref?.getString(BluetoothConstants.MAC, "") ?: ""
            if (isConnected) { // Se já estiver conectado, desconecta
                btManager.disconnect()
                binding.connect.isEnabled = false
                bottomNavigationView?.menu?.forEach { item ->
                    item.isEnabled = false
                }
                progressBar?.visibility = View.VISIBLE
                binding.connect.postDelayed({
                    binding.connect.isEnabled = true
                    bottomNavigationView?.menu?.forEach { item ->
                        item.isEnabled = true
                    }
                    progressBar?.visibility = View.GONE
                }, 2500)
            } else { // Se não estiver conectado, tenta conectar
                btManager.connect(mac, this@DeviceListFragment)
                binding.connect.isEnabled = false
                bottomNavigationView?.menu?.forEach { item ->
                    item.isEnabled = false
                }
                progressBar?.visibility = View.VISIBLE
                binding.connect.postDelayed({
                    binding.connect.isEnabled = true
                    bottomNavigationView?.menu?.forEach { item ->
                        item.isEnabled = true
                    }
                    progressBar?.visibility = View.GONE
                }, 2500)
            }
        }
        checkPermissions()
        initRcViews()
        registerBtLauncher()
        initBtAdapter()
        bluetoothState()
        updateConnectButtonState()
        // Estabeleça o fragmento como o listener para atualizações de conexão
        BluetoothManagerApp.getInstance().setConnectionListener(this)

        BluetoothManagerApp.getInstance().connectionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                BluetoothManagerApp.ConnectionState.CONNECTED -> {
                    isConnected = true
                    updateConnectButtonState()
                }

                BluetoothManagerApp.ConnectionState.DISCONNECTED -> {
                    isConnected = false
                    updateConnectButtonState()
                }

                else -> {}
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onReceive(message: String) {
        activity?.runOnUiThread {
            when(message){
                BluetoothController.BLUETOOTH_CONNECTED -> {
                }
                BluetoothController.BLUETOOTH_NO_CONNECTED -> {
                }
                else -> {
                    Log.v(TAG, message)
                    // binding.tvMessage.text = ("Temperatura: $message")
                }
            }
        }
    }

    override fun onConnected(bluetoothConnected: String) {
        if (DEBUG)  Log.d("DebugLunex", "onConnected do DeviceListFragment chamado")
    }

    override fun onDisconnected() {
        if (DEBUG)  Log.d("DebugLunex", "onDisconnected do DeviceListFragment chamado")
    }

    private fun initRcViews() = with(binding){
        rcViewPaired.layoutManager = LinearLayoutManager(requireContext())
        itemAdapter = ItemAdapter(this@DeviceListFragment, false)
        rcViewPaired.adapter = itemAdapter
    }

    private fun getPairedDevices(){
        try {
            val list = ArrayList<ListItem>()
            val deviceList = bAdapter?.bondedDevices as Set<BluetoothDevice>
            deviceList.forEach{
                list.add(
                    ListItem(
                        it,
                        preferences?.getString(BluetoothConstants.MAC, "") == it.address
                    )
                )
            }
            itemAdapter.submitList(list)
        } catch (_: SecurityException){

        }

    }

    private fun initBtAdapter(){
        val bManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bAdapter = bManager.adapter
    }

    private fun bluetoothState(){
        if (bAdapter?.isEnabled == true){
            binding.imBluetoothOn.setImageResource(R.drawable.ic_bluetooth_on)
            getPairedDevices()
        }
        else {
            binding.imBluetoothOn.setImageResource(R.drawable.ic_bluetooth_off)
        }
    }

    private fun registerBtLauncher(){
        btLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
            if (it.resultCode == Activity.RESULT_OK){
                binding.imBluetoothOn.setImageResource(R.drawable.ic_bluetooth_on)
                getPairedDevices()
                Snackbar.make(binding.root, "Bluetooth está ligado!", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, "Bluetooth está desligado!", Snackbar.LENGTH_LONG).show()
                binding.imBluetoothOn.setImageResource(R.drawable.ic_bluetooth_off)
            }
        }
    }

    private fun checkPermissions(){
        if(!checkBtPermissions()){
            registerPermissionListener()
            launchBtPermissions()
        }
    }

    private fun launchBtPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            pLauncher.launch(arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        } else {
            pLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }
    }

    private fun registerPermissionListener(){
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ){

        }
    }

    private fun saveMac(mac: String){
        val editor = preferences?.edit()
        editor?.putString(BluetoothConstants.MAC, mac)
        editor?.apply()
    }

    override fun onClick(device: ListItem) {
        saveMac(device.device.address)
    }


    override fun onDestroy() {
        super.onDestroy()
        BluetoothManagerApp.getInstance().setConnectionListener(null)
        activity?.unregisterReceiver(disconnectReceiver)
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

    override fun onBluetoothConnected() {
        isConnected = BluetoothManagerApp.getInstance().isConnected()
        if (DEBUG)  Log.d("DebugLunex", "estado do isConnected: $isConnected")
        updateConnectButtonState()
    }

    override fun onBluetoothDisconnected() {
        isConnected = BluetoothManagerApp.getInstance().isConnected()
        if (DEBUG)  Log.d("DebugLunex", "estado do isConnected: $isConnected")
        updateConnectButtonState()
    }

    private val disconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if (DEBUG)   Log.d("DebugLunex", "Receptor de desconexão chamado com ação: ${intent.action}")
                   BluetoothManagerApp.getInstance().handleDisconnection()
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.name?.let { BluetoothManagerApp.getInstance().handleConnection(it) }
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        // Você tem permissão para acessar o nome do dispositivo
                        device?.name
                    } else {
                        // Você não tem permissão para acessar o nome do dispositivo
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity())[SharedBluetoothViewModel::class.java]
    }

    override fun onReceiveTemperature(temp: String) {
        sharedViewModel.temperatureData.postValue(temp)
    }

    override fun onReceiveHumidity(humidity: String) {
        sharedViewModel.humidityData.postValue(humidity)
    }

    override fun onReceiveHotState(whiteHotValue: Int) {
        if (DEBUG) Log.d("testedebug", "Antes do sharedViewModel.hotValue.postValue(whiteHotValue). whiteHotValue: $whiteHotValue em DeviceListFragment")
        sharedViewModel.hotValue.postValue(whiteHotValue)
        if (DEBUG) Log.d("testedebug", "Depois do sharedViewModel.hotValue.postValue(whiteHotValue). whiteHotValue: $whiteHotValue em DeviceListFragment")
    }

    override fun onReceiveColdState(whiteColdValue: Int) {
        if (DEBUG) Log.d("testedebug", "Antes do sharedViewModel.coldValue.postValue(whiteColdValue). whiteColdValue: $whiteColdValue em DeviceListFragment")
        sharedViewModel.coldValue.postValue(whiteColdValue)
        if (DEBUG) Log.d("testedebug", "Depois do sharedViewModel.coldValue.postValue(whiteColdValue). whiteColdValue: $whiteColdValue em DeviceListFragment")
    }

    private fun updateConnectButtonState() {
        activity?.runOnUiThread {
            if (isConnected) {
                if (DEBUG)  Log.d("DebugLunex", "estado do isConnected: $isConnected no updateConnectButtonState()")
                binding.connect.text = "Desconectar"
                binding.connect.backgroundTintList = AppCompatResources
                    .getColorStateList(requireContext(), R.color.red)
            } else {
                if (DEBUG)  Log.d("DebugLunex", "estado do isConnected: $isConnected no updateConnectButtonState()")
                binding.connect.text = "Conectar"
                binding.connect.backgroundTintList = AppCompatResources
                    .getColorStateList(requireContext(), R.color.green)
            }
        }
    }

    companion object {
        const val DEBUG = false // Mude para 'true' durante o desenvolvimento, 'false' para produção
    }
}