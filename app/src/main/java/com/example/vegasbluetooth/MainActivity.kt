package com.example.vegasbluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vegasbluetooth.screens.BottomBarDevices
import com.example.vegasbluetooth.screens.BottomBarHome
import com.example.vegasbluetooth.screens.BottomBarSettings
import com.example.vegasbluetooth.ui.theme.VegasBluetoothTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vegasbluetooth.screens.BottomBarMessage
import java.io.IOException

val tabs_server = listOf(
    TabItem.Home,
    TabItem.Message,
    TabItem.Settings,
)

val tabs_client = listOf(
    TabItem.Home,
    TabItem.Devices,
    TabItem.Settings,
)

val tabs_init = listOf(
    TabItem.Home,
)

sealed class AppFunction(var run: () -> Unit) {

    object putPreferences : AppFunction( {} )
}

val basePermissions = arrayOf(
    Manifest.permission.INTERNET,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.ACCESS_NOTIFICATION_POLICY,
    Manifest.permission.RECEIVE_BOOT_COMPLETED,
    Manifest.permission.FOREGROUND_SERVICE,
    Manifest.permission.WAKE_LOCK,
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

val bluetoothPermissions = arrayOf(
    Manifest.permission.BLUETOOTH_ADVERTISE,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
)

lateinit var viewModel: VegasViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    lateinit var INTERNET: MutableState<Boolean>
    lateinit var ACCESS_NETWORK_STATE: MutableState<Boolean>
    lateinit var WAKE_LOCK: MutableState<Boolean>
    lateinit var ACCESS_NOTIFICATION_POLICY: MutableState<Boolean>
    lateinit var RECEIVE_BOOT_COMPLETED: MutableState<Boolean>
    lateinit var ACCESS_FINE_LOCATION: MutableState<Boolean>
    lateinit var ACCESS_COARSE_LOCATION: MutableState<Boolean>
    lateinit var FOREGROUND_SERVICE: MutableState<Boolean>
    lateinit var BLUETOOTH: MutableState<Boolean>
    lateinit var BLUETOOTH_ADMIN: MutableState<Boolean>
    lateinit var BLUETOOTH_SCAN: MutableState<Boolean>
    lateinit var BLUETOOTH_CONNECT: MutableState<Boolean>

    lateinit var prefs: SharedPreferences
    val APP_PREFERENCES_THEME = "theme"
    val APP_PREFERENCES_ASK_TO_EXIT_FROM_APP = "ask_to_exit_from_app"
    val APP_PREFERENCES_KEEP_SCREEN_ON = "keep_screen_on"
    val APP_PREFERENCES_MODE = "mode"
    val APP_PREFERENCES_WAKEUP_SOUND = "wakeup_sound"

    lateinit var permissionsGranted: MutableState<Boolean>
    lateinit var theme: MutableState<Boolean>
    lateinit var isModeClient: MutableState<Boolean>
    lateinit var isBleSupported: MutableState<Boolean>
    lateinit var isBleEnabled: MutableState<Boolean>
    lateinit var isBleAdvertisementsSupported: MutableState<Boolean>

    private lateinit var btAdvertisingFailureReceiver: BroadcastReceiver

    var isAppInited: Boolean = false
    var isFistStart: Boolean = true


    val BT_ADVERTISING_FAILED_EXTRA_CODE = "bt_adv_failure_code"
    val INVALID_CODE = -1
    val ADVERTISING_TIMED_OUT = 6


    //private val vm: VegasViewModel by viewModels()

    @OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //viewModel = vm

        btAdvertisingFailureReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                Log.d("zzz", "btAdvertisingFailureReceiver")

                val errorCode = intent?.getIntExtra(BT_ADVERTISING_FAILED_EXTRA_CODE, INVALID_CODE)

                var errMsg = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> getString(R.string.already_started)
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> getString(R.string.data_too_large)
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> getString(R.string.not_supported)
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> getString(R.string.inter_err)
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers."
                    ADVERTISING_TIMED_OUT -> "Timed out."
                    else -> "Error unknown."
                }
                errMsg = "Start advertising failed: $errMsg"
                Toast.makeText(applicationContext, errMsg, Toast.LENGTH_LONG).show()
            }
        }

        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        AppFunction.putPreferences.run = ::putPreferences

        setContent {

            viewModel = hiltViewModel()

            getPreferences()

            INTERNET = remember { mutableStateOf(false) }
            ACCESS_NETWORK_STATE = remember { mutableStateOf(false) }
            WAKE_LOCK = remember { mutableStateOf(false) }
            ACCESS_NOTIFICATION_POLICY = remember { mutableStateOf(false) }
            RECEIVE_BOOT_COMPLETED = remember { mutableStateOf(false) }
            ACCESS_FINE_LOCATION = remember { mutableStateOf(false) }
            ACCESS_COARSE_LOCATION = remember { mutableStateOf(false) }
            FOREGROUND_SERVICE = remember { mutableStateOf(false) }
            BLUETOOTH = remember { mutableStateOf(false) }
            BLUETOOTH_ADMIN = remember { mutableStateOf(false) }
            BLUETOOTH_SCAN = remember { mutableStateOf(false) }
            BLUETOOTH_CONNECT = remember { mutableStateOf(false) }

            permissionsGranted = remember { mutableStateOf(hasAllPermissions()) }
            theme = remember { mutableStateOf(viewModel.currentTheme.value == 1) }
            isModeClient = remember { mutableStateOf(viewModel.currentMode.value == 0) }
            isBleSupported = remember { mutableStateOf(isBleSupported()) }
            isBleEnabled = remember { mutableStateOf(isBleEnabled()) }
            isBleAdvertisementsSupported = remember { mutableStateOf(isBleAdvertisementsSupported()) }

            if (viewModel.keepScreenOn.value) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            isAppInited = true

            if (isFistStart) {
                if (permissionsGranted.value) {

                    viewModel.bleScanApi.bleState.observe(this, bleConnectionObserver)
                    viewModel.bleScanApi.messages.observe(this, messageObserver)

                    viewModel.bleScanApi.startServer(application as App)

                    isFistStart = false
                }
            }



            VegasBluetoothTheme(theme.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var pagerState: PagerState = rememberPagerState(0)

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                backgroundColor = MaterialTheme.colorScheme.background,
                                title = {
                                    Row() {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                        )
                                    }

                                },
                                modifier = Modifier.height(30.dp),
                                actions = {
                                    IconButton(onClick = {
                                        exitFromApp()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.ExitToApp,
                                            contentDescription = "Exit",
                                        )
                                    }
                                },
                            )
                        },
                        bottomBar = {
                            BottomAppBar(
                                modifier = Modifier.height(60.dp),
                                backgroundColor = MaterialTheme.colorScheme.background,
                            )
                            {
                                if (isModeClient.value) {
                                    when (tabs_client[pagerState.currentPage]) {
                                        TabItem.Home -> BottomBarHome()
                                        TabItem.Devices -> BottomBarDevices()
                                        TabItem.Settings -> BottomBarSettings()
                                        else -> {}
                                    }
                                } else {
                                    when (tabs_server[pagerState.currentPage]) {
                                        TabItem.Home -> BottomBarHome()
                                        TabItem.Message -> BottomBarMessage()
                                        TabItem.Settings -> BottomBarSettings()
                                        else -> {}
                                    }
                                }

                            }
                        }
                    ) {
                            padding ->
                        Column(modifier = Modifier
                            .padding(padding)
                            //.navigationBarsWithImePadding(),
                        ) {
                            Tabs(tabs =
                                    if (permissionsGranted.value) {
                                        if (isModeClient.value) tabs_client else tabs_server
                                    } else tabs_init,
                                pagerState = pagerState)
                            TabsContent(
                                tabs = if (permissionsGranted.value) {
                                    if (isModeClient.value) tabs_client else tabs_server
                                } else tabs_init,
                                pagerState = pagerState,
                                permissionsGranted.value,
                                INTERNET.value,
                                ACCESS_NETWORK_STATE.value,
                                WAKE_LOCK.value,
                                isBleSupported.value,
                                isBleEnabled.value,
                                isBleAdvertisementsSupported.value,
                                ACCESS_NOTIFICATION_POLICY.value,
                                RECEIVE_BOOT_COMPLETED.value,
                                ACCESS_FINE_LOCATION.value,
                                ACCESS_COARSE_LOCATION.value,
                                FOREGROUND_SERVICE.value,
                                BLUETOOTH.value,
                                BLUETOOTH_ADMIN.value,
                                BLUETOOTH_SCAN.value,
                                BLUETOOTH_CONNECT.value,
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun Tabs(tabs: List<TabItem>, pagerState: PagerState) {
        val scope = rememberCoroutineScope()
        ScrollableTabRow(
            backgroundColor = MaterialTheme.colorScheme.background,
            selectedTabIndex = pagerState.currentPage,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    text = { Text(stringResource(tab.title)) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                )
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun TabsContent(
        tabs: List<TabItem>, pagerState: PagerState,
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
        HorizontalPager(state = pagerState, count = tabs.size) { page ->

            var screenParams: ScreenParams = ScreenParams(
                permissionsGranted,
                INTERNET,
                ACCESS_NETWORK_STATE,
                WAKE_LOCK,
                isBleSupported,
                isBleEnabled,
                isBleAdvertisementsSupported,
                ACCESS_NOTIFICATION_POLICY,
                RECEIVE_BOOT_COMPLETED,
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
                FOREGROUND_SERVICE,
                BLUETOOTH,
                BLUETOOTH_ADMIN,
                BLUETOOTH_SCAN,
                BLUETOOTH_CONNECT,
            )

            tabs[page].screen(screenParams)

        }
    }

    override fun onBackPressed() {

        if (viewModel.askToExitFromApp) {

            val alertDialog = android.app.AlertDialog.Builder(this)

            alertDialog.apply {
                setIcon(R.drawable.vegas_02)
                setTitle(getApplicationContext().getResources().getString(R.string.app_name))
                setMessage(getApplicationContext().getResources().getString(R.string.do_you_really_want_to_close_the_application))
                setPositiveButton(getApplicationContext().getResources().getString(R.string.yes))
                { _: DialogInterface?, _: Int -> exitFromApp() }
                setNegativeButton(getApplicationContext().getResources().getString(R.string.no))
                { _, _ -> }

            }.create().show()
        }
        else {
            exitFromApp()
        }

    }

    fun exitFromApp() {
        onBackPressedDispatcher.onBackPressed()
    }

    fun isBleSupported() : Boolean {

        var isSupported = true

        val bluetoothAdapter =
            (getSystemService(ComponentActivity.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (bluetoothAdapter == null) {
            Log.d("zzz", "Bluetooth is not supported on this hardware platform")
            isSupported = false
        }

        return isSupported
    }

    fun isBleAdvertisementsSupported() : Boolean {

        var isSupported = false

        val bluetoothAdapter =
            (getSystemService(ComponentActivity.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled && bluetoothAdapter.isMultipleAdvertisementSupported) {
                isSupported = true
            } else {
                Log.d("zzz", "Bluetooth Advertisements are not supported")

            }
        }
        return isSupported
    }

    fun isBleEnabled() : Boolean {

        var isEnabled = true

        val bluetoothAdapter =
            (getSystemService(ComponentActivity.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled) {
                Log.d("zzz", "Bluetooth is OFF, user should turn it ON")
                isEnabled = false
            }
        }

        return isEnabled
    }

    fun requestBleEnable() {
        startActivityForResult(
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 105)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == 105) {
            Log.d("zzz", "onActivityResult: REQUEST_ENABLE_BT")
            isBleEnabled.value = isBleEnabled()
            isBleAdvertisementsSupported.value = isBleAdvertisementsSupported()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun hasAllPermissions(): Boolean{
        var result = true

        if (!hasBasePermissions()) {
            result = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasBluetoothPermissions()) {
                result = false
            }
        }

        return result
    }

    fun hasBasePermissions(): Boolean{
        var result = true
        basePermissions.forEach {

            val permission = ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            if ( !permission)
            {
                Log.d("zzz","PERMISSION DENIED ${it}")
                result = false
            } else {
                //Log.d("zzz","PERMISSION GRANTED ${it}")
            }
            when (it) {
                Manifest.permission.INTERNET -> INTERNET.value = permission
                Manifest.permission.ACCESS_NETWORK_STATE -> ACCESS_NETWORK_STATE.value = permission
                Manifest.permission.WAKE_LOCK -> WAKE_LOCK.value = permission
                Manifest.permission.ACCESS_NOTIFICATION_POLICY -> ACCESS_NOTIFICATION_POLICY.value = permission
                Manifest.permission.RECEIVE_BOOT_COMPLETED -> RECEIVE_BOOT_COMPLETED.value = permission
                Manifest.permission.FOREGROUND_SERVICE -> FOREGROUND_SERVICE.value = permission
                Manifest.permission.BLUETOOTH -> BLUETOOTH.value = permission
                Manifest.permission.BLUETOOTH_ADMIN -> BLUETOOTH_ADMIN.value = permission
                Manifest.permission.ACCESS_FINE_LOCATION -> ACCESS_FINE_LOCATION.value = permission
                Manifest.permission.ACCESS_COARSE_LOCATION -> ACCESS_COARSE_LOCATION.value = permission
            }
        }
        return result
    }

    fun requestBasePermissions() {
        Log.d("zzz","requestPermissions")
        ActivityCompat.requestPermissions(this, basePermissions,101)
    }

    fun hasBluetoothPermissions(): Boolean{
        var result = true

        Log.d("zzz","bluetoothPermissions")

        bluetoothPermissions.forEach {

            val permission = ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            if ( !permission)
            {
                Log.d("zzz","PERMISSION DENIED ${it}")
                result = false
            } else {
                //Log.d("zzz","PERMISSION GRANTED ${it}")
            }
            when (it) {
                Manifest.permission.BLUETOOTH_SCAN -> BLUETOOTH_SCAN.value = permission
                Manifest.permission.BLUETOOTH_CONNECT -> BLUETOOTH_CONNECT.value = permission
            }
        }

        return result
    }

    fun requestBluetoothPermissions() {
        Log.d("zzz","requestBluetoothPermissions")
        ActivityCompat.requestPermissions(this, bluetoothPermissions,101)
    }


    fun putPreferences() {
        val editor = prefs.edit()
        editor.putInt(APP_PREFERENCES_THEME, viewModel.currentTheme.value).apply()
        editor.putBoolean(APP_PREFERENCES_ASK_TO_EXIT_FROM_APP, viewModel.askToExitFromApp).apply()
        editor.putBoolean(APP_PREFERENCES_KEEP_SCREEN_ON, viewModel.keepScreenOn.value).apply()
        editor.putInt(APP_PREFERENCES_MODE, viewModel.currentMode.value).apply()
        editor.putInt(APP_PREFERENCES_WAKEUP_SOUND, viewModel.currentWakeupSound.value).apply()

        isModeClient.value = (viewModel.currentMode.value == 0)
        theme.value = (viewModel.currentTheme.value == 1)
    }

    fun getPreferences() {
        if(prefs.contains(APP_PREFERENCES_THEME)){
            viewModel.currentTheme.value = prefs.getInt(APP_PREFERENCES_THEME, 0)
        }

        if(prefs.contains(APP_PREFERENCES_ASK_TO_EXIT_FROM_APP)){
            viewModel.askToExitFromApp = prefs.getBoolean(APP_PREFERENCES_ASK_TO_EXIT_FROM_APP, true)
        }

        if(prefs.contains(APP_PREFERENCES_KEEP_SCREEN_ON)){
            viewModel.keepScreenOn.value = prefs.getBoolean(APP_PREFERENCES_KEEP_SCREEN_ON, true)
        }

        if(prefs.contains(APP_PREFERENCES_MODE)){
            viewModel.currentMode.value = prefs.getInt(APP_PREFERENCES_MODE, 0)
        }

        if(prefs.contains(APP_PREFERENCES_WAKEUP_SOUND)){
            viewModel.currentWakeupSound.value = prefs.getInt(APP_PREFERENCES_WAKEUP_SOUND, 0)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (isAppInited) {
            permissionsGranted.value = hasAllPermissions()
        }
    }

    private val bleConnectionObserver = Observer<BleState> { state ->
        when(state) {
            is BleState.Scanning -> {
                Log.d("zzz", "BleState.Scanning")
                viewModel.isWaitToStartScan.value = false
                viewModel.isWaitToStopScan.value = true
                viewModel.isWaitToStartConnect.value = false
                viewModel.isWaitToStartWakeup.value = false
            }
            is BleState.ScanningStoped -> {
                Log.d("zzz", "BleState.ScanningStoped")
                viewModel.isWaitToStartScan.value = viewModel.bleDeviceName.value == ""
                viewModel.isWaitToStopScan.value = viewModel.bleDeviceName.value == ""
                viewModel.isWaitToStartConnect.value = viewModel.bleDeviceName.value != ""
                viewModel.isWaitToStartWakeup.value = false
            }
            is BleState.Connected -> {
                Log.d("zzz", "BleState.Connected")
                viewModel.isWaitToStartScan.value = false
                viewModel.isWaitToStopScan.value = false
                viewModel.isWaitToStartConnect.value = false
                viewModel.isWaitToStartWakeup.value = true
            }
            is BleState.Disconnected -> {
                Log.d("zzz", "BleState.Disconnected")
                viewModel.isWaitToStartScan.value = true
                viewModel.isWaitToStopScan.value = false
                viewModel.isWaitToStartConnect.value = false
                viewModel.isWaitToStartWakeup.value = false
                viewModel.messageResponse.value = ""
            }
            is BleState.StartConnect -> {
                Log.d("zzz", "BleState.StartConnect")
                viewModel.isWaitToStartScan.value = false
                viewModel.isWaitToStopScan.value = false
                viewModel.isWaitToStartConnect.value = false
                viewModel.isWaitToStartWakeup.value = false
            }
            is BleState.Sended -> {
                Log.d("zzz", "BleState.Sended")
            }
        }

    }

    private val messageObserver = Observer<Message> { message ->
        Log.d("zzz", "Have message ${message.text}")
        val sound_id = message.text.toInt()
        viewModel.play(sound_id)
    }


}
