package com.example.vegasbluetooth.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.vegasbluetooth.MainActivity
import com.example.vegasbluetooth.R
import java.util.concurrent.TimeUnit

const val BT_ADVERTISING_FAILED_EXTRA_CODE = "bt_adv_failure_code"
const val ADVERTISING_TIMED_OUT = 6
const val BLE_NOTIFICATION_CHANNEL_ID = "bleChl"
const val FOREGROUND_NOTIFICATION_ID = 3
const val ADVERTISING_FAILED = "com.example.android.bluetoothadvertisements.advertising_failed"

val ScanFilterService_UUID: ParcelUuid = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb")


class AdvertiserService : Service() {

    private val TAG = "zzz"
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var handler: Handler? = null

    /**
     * Length of time to allow advertising before automatically shutting off. (10 minutes)
     */
    private val TIMEOUT: Long = TimeUnit.MILLISECONDS.convert(300, TimeUnit.SECONDS)

    override fun onBind(intent: Intent?): IBinder? {
        return null // no binding necessary. This Service will only be started
    }

    override fun onCreate() {
        running = true
        initialize()
        startAdvertising()
        setTimeout()
        super.onCreate()
    }

    override fun onDestroy() {
        running = false
        stopAdvertising()
        handler?.removeCallbacksAndMessages(null) // this is a generic way for removing tasks
        stopForeground(true)
        super.onDestroy()
    }

    private fun initialize() {
        if (bluetoothLeAdvertiser == null) {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter: BluetoothAdapter = manager.adapter
            bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        goForeground()
        Log.d(TAG, "Service: Starting Advertising")
        if (advertiseCallback == null) {
            val settings: AdvertiseSettings = buildAdvertiseSettings()
            val data: AdvertiseData = buildAdvertiseData()
            advertiseCallback = sampleAdvertiseCallback()
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() = bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        .also { bluetoothLeAdvertiser = null }

    private fun goForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val nBuilder = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val bleNotificationChannel = NotificationChannel(
                    BLE_NOTIFICATION_CHANNEL_ID, "BLE",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val nManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nManager.createNotificationChannel(bleNotificationChannel)
                Notification.Builder(this, BLE_NOTIFICATION_CHANNEL_ID)
            }
            else -> Notification.Builder(this)
        }

        val notification = nBuilder.setContentTitle(getString(R.string.bt_notif_title))
            .setContentText(getString(R.string.bt_notif_txt))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun buildAdvertiseSettings() = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        .setTimeout(0).build()

    private fun buildAdvertiseData() = AdvertiseData.Builder()
        .addServiceUuid(ScanFilterService_UUID).setIncludeDeviceName(true).build()

    private fun sampleAdvertiseCallback() = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d(TAG, "Advertising failed")
            broadcastFailureIntent(errorCode)
            stopSelf()
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising successfully started")
        }
    }

    private fun broadcastFailureIntent(errorCode: Int) {
        val failureIntent = Intent().setAction(ADVERTISING_FAILED).putExtra(
            BT_ADVERTISING_FAILED_EXTRA_CODE, errorCode
        )
        Log.d(TAG, "broadcastFailureIntent")
        sendBroadcast(failureIntent)
    }

    private fun setTimeout() {
        handler = Handler(Looper.myLooper()!!)
        val runnable = Runnable {
            Log.d(
                TAG,
                "run: AdvertiserService has reached timeout of $TIMEOUT milliseconds, stopping advertising."
            )
            Log.d(TAG, "broadcastFailureIntent")
            broadcastFailureIntent(ADVERTISING_TIMED_OUT)
        }
        handler?.postDelayed(runnable, TIMEOUT)
    }

    companion object {
        /**
         * A global variable to let AdvertiserFragment check if the Service is running without needing
         * to start or bind to it.
         * This is the best practice method as defined here:
         * https://groups.google.com/forum/#!topic/android-developers/jEvXMWgbgzE
         */
        var running: Boolean = false
    }

}