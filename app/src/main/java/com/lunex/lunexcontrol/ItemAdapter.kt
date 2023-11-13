package com.lunex.lunexcontrol

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lunex.lunexcontrol.databinding.BluetoothDeviceItemBinding

class ItemAdapter(private val listener: Listener, private val adapterType: Boolean) :
    ListAdapter<ListItem, ItemAdapter.MyHolder>(Comparator()) {
    private var oldCheckBox: CheckBox? = null

    class MyHolder(
        view: View, private val adapter: ItemAdapter,
        private val listener: Listener,
        private val adapterType: Boolean
    ) : RecyclerView.ViewHolder(view) {
        private val b = BluetoothDeviceItemBinding.bind(view)
        private var item1: ListItem? = null

        init {
            b.checkBox.setOnClickListener {
                item1?.let { it1 -> listener.onClick(it1) }
                adapter.selectCheckBox(it as CheckBox)
            }
            itemView.setOnClickListener {
                if(adapterType){
                    try {
                        item1?.device?.createBond()
                    } catch (_: SecurityException){}
                } else {
                    item1?.let { it1 -> listener.onClick(it1) }
                    adapter.selectCheckBox(b.checkBox)
                }
            }
            b.renameIcon.setOnClickListener {
                showRenameDialog(item1)
            }
        }

        @SuppressLint("MissingPermission")
        private fun showRenameDialog(item: ListItem?) {
            val context = itemView.context
            val sharedPref = context.getSharedPreferences("DeviceNames", Context.MODE_PRIVATE)
            // Testando o SharedPreferences aqui
            sharedPref?.edit()?.putString("TestKey", "TestValue")?.apply()
            Log.d("SharedPreferencesTest", "Attempted to save TestValue for TestKey")
            val savedValue = sharedPref?.getString("TestKey", "NOT_FOUND")
            Log.d("SharedPreferencesTest", "Reading back TestKey: $savedValue")

            val editText = EditText(context)
            Log.d("CustomNameRetrieve", "Trying to retrieve custom name for address: ${item?.device?.address}")
            val customName = sharedPref.getString(item?.device?.address, item?.device?.name)
            Log.d("CustomNameResult", "Retrieved custom name: $customName")
            editText.setText(customName)

            AlertDialog.Builder(context)
                .setTitle("Renomear Dispositivo")
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    item?.customName = editText.text.toString()
                    Log.d("CustomNameSave", "Saving custom name: ${item?.customName} for address: ${item?.device?.address}")
                    with(sharedPref.edit()) {
                        putString(item?.device?.address, item?.customName)
                        apply()
                    }
                    b.name.text = item?.customName
                    adapter.notifyItemChanged(adapterPosition)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        @SuppressLint("MissingPermission")
        fun bind(item: ListItem) = with(b) {
            checkBox.visibility = if (adapterType) View.GONE else View.VISIBLE
            item1 = item
            val sharedPref = itemView.context.getSharedPreferences("DeviceNames", Context.MODE_PRIVATE)
            val customName = sharedPref.getString(item.device.address, item.device.name)
            name.text = customName
            if (item.isChecked) adapter.selectCheckBox(checkBox)
        }

    }


    class Comparator : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.device.address == newItem.device.address
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.customName == newItem.customName &&
                    oldItem.isChecked == newItem.isChecked
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.bluetooth_device_item, parent, false)
        return MyHolder(view, this, listener, adapterType)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem.device.address == newItem.device.address && oldItem.customName == newItem.customName
    }

    fun selectCheckBox(checkBox: CheckBox) {
        oldCheckBox?.isChecked = false
        oldCheckBox = checkBox
        oldCheckBox?.isChecked = true
    }

    interface Listener {
        val isConnected: Boolean

        fun onClick(device: ListItem)
    }
}