package com.example.vegasbluetooth.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vegasbluetooth.MainActivity
import com.example.vegasbluetooth.R
import com.example.vegasbluetooth.viewModel

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MessageScreen() {

    val message by viewModel.message.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {

        if (message != "") {
            Text(text = "${stringResource(R.string.wake_up)}",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp))
        }

    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainActivity.BottomBarMessage() {

    val message by viewModel.message.collectAsState()

    if (message != "") {
        androidx.compose.material3.Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.message.value = ""
            }

        ) {
            androidx.compose.material3.Text(text = stringResource(R.string.clear))
        }
    }

}