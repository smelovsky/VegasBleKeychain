package com.example.vegasbluetooth

sealed class BleState {
    object Disconnected : BleState()
    object StartConnect : BleState()
    object Connected : BleState()
    object Scanning : BleState()
    object ScanningStoped : BleState()
    object Sended : BleState()
}