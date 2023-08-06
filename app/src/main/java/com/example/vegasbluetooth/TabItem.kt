package com.example.vegasbluetooth

import androidx.compose.runtime.Composable
import com.example.vegasbluetooth.screens.DevicesScreen
import com.example.vegasbluetooth.screens.HomeScreen
import com.example.vegasbluetooth.screens.MessageScreen
import com.example.vegasbluetooth.screens.SettingsScreen


data class ScreenParams(
    val permissionsGranted: Boolean,
    val INTERNET: Boolean,
    val ACCESS_NETWORK_STATE: Boolean,
    val WAKE_LOCK: Boolean,
    val isBleSupported: Boolean,
    val isBleEnabled: Boolean,
    val isBleAdvertisementsSupported: Boolean,
    val ACCESS_NOTIFICATION_POLICY: Boolean,
    val RECEIVE_BOOT_COMPLETED: Boolean,
    val ACCESS_FINE_LOCATION: Boolean,
    val ACCESS_COARSE_LOCATION: Boolean,
    val FOREGROUND_SERVICE: Boolean,
    val BLUETOOTH: Boolean,
    val BLUETOOTH_ADMIN: Boolean,
    val BLUETOOTH_SCAN: Boolean,
    val BLUETOOTH_CONNECT: Boolean,

)
typealias ComposableFun = @Composable (screenParams: ScreenParams) -> Unit

sealed class TabItem(var icon: Int, var title: Int, var screen: ComposableFun) {
    object Home : TabItem(R.drawable.ic_label, R.string.tab_name_home, { HomeScreen(
        it.permissionsGranted,
        it.INTERNET,
        it.ACCESS_NETWORK_STATE,
        it.WAKE_LOCK,
        it.isBleSupported,
        it.isBleEnabled,
        it.isBleAdvertisementsSupported,
        it.ACCESS_NOTIFICATION_POLICY,
        it.RECEIVE_BOOT_COMPLETED,
        it.ACCESS_FINE_LOCATION,
        it.ACCESS_COARSE_LOCATION,
        it.FOREGROUND_SERVICE,
        it.BLUETOOTH,
        it.BLUETOOTH_ADMIN,
        it.BLUETOOTH_SCAN,
        it.BLUETOOTH_CONNECT
    ) } )
    object Devices : TabItem(R.drawable.ic_label, R.string.tab_name_devices, { DevicesScreen() } )
    object Message : TabItem(R.drawable.ic_label, R.string.tab_name_message, { MessageScreen() } )
    object Settings : TabItem(R.drawable.ic_label, R.string.tab_name_settings, { SettingsScreen() } )
}