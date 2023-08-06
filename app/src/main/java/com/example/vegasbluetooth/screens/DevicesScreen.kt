package com.example.vegasbluetooth.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.vegasbluetooth.MainActivity
import com.example.vegasbluetooth.R
import com.example.vegasbluetooth.viewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DevicesScreen() {

    val isScanning by viewModel.isScanning.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()

    val bleDeviceName by viewModel.bleDeviceName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {

        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
            )

            Text(
                text = stringResource(id = R.string.scanning),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally),
            )
        } else if (isConnecting){
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
            )

            Text(
                text = stringResource(id = R.string.connecting),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }

        Text(text = bleDeviceName,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.primary
        )

    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainActivity.BottomBarDevices() {

    val isWaitToStartScan by viewModel.isWaitToStartScan.collectAsState()
    val isWaitToStopScan by viewModel.isWaitToStopScan.collectAsState()
    val isWaitToStartConnect by viewModel.isWaitToStartConnect.collectAsState()
    val isWaitToStartWakeup by viewModel.isWaitToStartWakeup.collectAsState()

    if (isWaitToStartScan || isWaitToStopScan || isWaitToStartConnect || isWaitToStartWakeup) {
        androidx.compose.material3.Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                when {
                    isWaitToStartScan -> viewModel.startBleScan()
                    isWaitToStopScan -> viewModel.stopBleScan()
                    isWaitToStartConnect -> viewModel.setConnection()
                    else -> viewModel.wakeUp()
                }
            }

        ) {
            androidx.compose.material3.Text(text =
            when {
                isWaitToStartScan -> stringResource(R.string.scan)
                isWaitToStopScan -> stringResource(R.string.stop)
                isWaitToStartConnect -> stringResource(R.string.set_connection)
                else -> stringResource(R.string.wake_up)
            }
            )
        }
    } else {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.please_wait),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

    }

}