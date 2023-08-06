package com.example.vegasbluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vegasbluetooth.ble.BleScanApi
import com.example.vegasbluetooth.sound.SoundApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class VegasViewModel @Inject constructor(
    val bleScanApi: BleScanApi,
    val soundApi: SoundApi,
) : ViewModel() {

    var askToExitFromApp: Boolean = true

    var currentTheme = mutableStateOf(0)
    val keepScreenOn = mutableStateOf(false)
    var currentMode = mutableStateOf(0)
    var currentWakeupSound = mutableStateOf(0)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    val bleDeviceName = bleScanApi.bleDeviceName
    private var bleScanRequested = false


    fun startBleScan() {
        Log.d("zzz", "startBleScan")

        bleScanRequested = true

        viewModelScope.launch {
            bleScanApi.stopScan()
            bleScanApi.startScan()
        }
    }


    fun stopBleScan() {
        Log.d("zzz", "stopBleScan")

        bleScanRequested = false

        viewModelScope.launch {
            bleScanApi.stopScan()
        }
    }

    fun wakeUp() {
        Log.d("zzz", "Wake Up")
        bleScanApi.sendMessage(currentWakeupSound.value.toString())
    }

    fun setConnection() {
        Log.d("zzz", "viewModel.bluetoothDevice: ${viewModel.bleScanApi.bluetoothDevice}")
        viewModel.bleScanApi.bluetoothDevice?.let {
            bleScanApi.connectToDevice(it)
            isSetConnection.value = true
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun play(sound_id: Int) {
        when (sound_id) {
            0 -> soundApi.play(soundApi.catSound)
            1 -> soundApi.play(soundApi.chickenSound)
            2 -> soundApi.play(soundApi.cowSound)
            3 -> soundApi.play(soundApi.dogSound)
            4 -> soundApi.play(soundApi.duckSound)
            5 -> soundApi.play(soundApi.sheepSound)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    val isSetConnection = MutableStateFlow(false)
    val message = MutableStateFlow("")

    val isWaitToStartScan = MutableStateFlow(true)
    val isWaitToStopScan = MutableStateFlow(false)
    val isWaitToStartConnect = MutableStateFlow(false)
    val isWaitToStartWakeup = MutableStateFlow(false)

    val isScanning = bleScanApi.isScanning
    val isConnecting = bleScanApi.isConnecting

}