package com.example.vegasbluetooth.ble

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import com.example.vegasbluetooth.App
import com.example.vegasbluetooth.BleState
import com.example.vegasbluetooth.Message
import kotlinx.coroutines.flow.StateFlow

interface BleScanApi : AutoCloseable {

    val isBluetoothEnabled: Boolean

    val bleDeviceName: StateFlow<String>
    var bluetoothDevice: BluetoothDevice?

    val isScanning: StateFlow<Boolean>
    val isConnecting: StateFlow<Boolean>
    val connectionRequest: LiveData<BluetoothDevice>
    val messages: LiveData<Message>
    val bleState: LiveData<BleState>

    fun startScan()
    fun stopScan()
    fun startServer(app: App)
    fun getYourDeviceAddress(): String
    fun sendMessage(message: String): Boolean
    fun connectToDevice(device: BluetoothDevice)

}
