package com.example.vegasbluetooth.ble

import android.bluetooth.BluetoothDevice
import java.util.UUID

data class BleDeviceInfo(
    val bluetoothDevice: BluetoothDevice,
    val name: String,
    val address: String,
    val serviceUuids: List<UUID>,
    val manufacturerSpecificData: List<String>,
    val advertiseFlags: Int,
    val rssi: Int,
    val timestamp: String,
)