package com.example.vegasbluetooth.screens

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.vegasbluetooth.R
import com.example.vegasbluetooth.MainActivity
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    permissionsGranted: Boolean,
    INTERNET: Boolean,
    ACCESS_NETWORK_STATE: Boolean,
    WAKE_LOCK: Boolean,
    isBleSupported: Boolean,
    isBleEnabled: Boolean,
    isBleAdvertisementsSupported: Boolean,
    ACCESS_NOTIFICATION_POLICY: Boolean,
    RECEIVE_BOOT_COMPLETED: Boolean,
    ACCESS_FINE_LOCATION: Boolean,
    ACCESS_COARSE_LOCATION: Boolean,
    FOREGROUND_SERVICE: Boolean,
    BLUETOOTH: Boolean,
    BLUETOOTH_ADMIN: Boolean,
    BLUETOOTH_SCAN: Boolean,
    BLUETOOTH_CONNECT: Boolean,
) {
    Column() {
        Image(
            painterResource(R.drawable.vegas_01),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }

    if (!permissionsGranted) {
        Column() {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "INTERNET",
                color = if (INTERNET) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "ACCESS_NETWORK_STATE",
                color = if (ACCESS_NETWORK_STATE) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "WAKE_LOCK",
                color = if (WAKE_LOCK) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "ACCESS_NOTIFICATION_POLICY",
                color = if (ACCESS_NOTIFICATION_POLICY) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "RECEIVE_BOOT_COMPLETED",
                color = if (RECEIVE_BOOT_COMPLETED) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "ACCESS_FINE_LOCATION",
                color = if (ACCESS_FINE_LOCATION) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "ACCESS_COARSE_LOCATION",
                color = if (ACCESS_COARSE_LOCATION) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "FOREGROUND_SERVICE",
                color = if (FOREGROUND_SERVICE) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "BLUETOOTH",
                color = if (BLUETOOTH) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Text(
                text = "BLUETOOTH_ADMIN",
                color = if (BLUETOOTH_ADMIN) Color.Green else Color.Red,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    text = "BLUETOOTH_SCAN",
                    color = if (BLUETOOTH_SCAN) Color.Green else Color.Red,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Text(
                    text = "BLUETOOTH_CONNECT",
                    color = if (BLUETOOTH_CONNECT) Color.Green else Color.Red,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }
    }

    //if (isBleSupported) { }

}



@Composable
fun MainActivity.BottomBarHome() {

    if (!permissionsGranted.value) {
        androidx.compose.material3.Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!hasBasePermissions()) {
                    requestBasePermissions()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!hasBluetoothPermissions()) requestBluetoothPermissions()

                }
            }
        ) {
            androidx.compose.material3.Text(text = stringResource(R.string.permissions))
        }
    }

    if (isBleSupported.value) {
        if (!isBleEnabled.value) {
            androidx.compose.material3.Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    requestBleEnable()
                }
            ) {
                androidx.compose.material3.Text(text = stringResource(R.string.enable_bluetooth))
            }
        } else if (!isBleAdvertisementsSupported.value) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.ble_advertisements_is_not_supported))
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.all_permissions_granted) )
                }
            }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.ble_is_not_supported))
        }
    }
}
