package com.example.vegasbluetooth.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.vegasbluetooth.App
import com.example.vegasbluetooth.BleState
import com.example.vegasbluetooth.Message
import com.example.vegasbluetooth.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.LinkedList
import java.util.UUID


class BleScanImpl(val context: Context) : BleScanApi {

    val SERVICE_UUID: UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb")
    val MESSAGE_UUID: UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")
    val CONFIRM_UUID: UUID = UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928")

    val supported_devices = arrayOf("realme C21-Y", "realme C21-Y")

    private var gatt: BluetoothGatt? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    private var app: App? = null
    private lateinit var bluetoothManager: BluetoothManager
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData = buildAdvertiseData()

    private val _messages = MutableLiveData<Message>()
    override val messages = _messages as LiveData<Message>

    private val _connectionRequest = MutableLiveData<BluetoothDevice>()
    override val connectionRequest = _connectionRequest as LiveData<BluetoothDevice>

    private var gattServer: BluetoothGattServer? = null
    private var gattServerCallback: BluetoothGattServerCallback? = null

    private var gattClient: BluetoothGatt? = null
    private var gattClientCallback: BluetoothGattCallback? = null

    private val _bleState = MutableLiveData<BleState>()
    override val bleState = _bleState as LiveData<BleState>

    override fun startServer(app: App) {
        Log.d("zzz", "<startServer>")
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!adapter.isEnabled) {
            // prompt the user to enable bluetooth
        } else {
            setupGattServer(app)
            startAdvertisement()
        }
    }

    fun stopServer() {
        stopAdvertising()
    }

    /**
     * The questions of how to obtain a device's own MAC address comes up a lot. The answer is
     * you cannot; it would be a security breach. Only system apps can get that permission.
     * Otherwise apps might use that address to fingerprint a device (e.g. for advertising, etc.)
     * A user can find their own MAC address through Settings, but apps cannot find it.
     * This method, which some might be tempted to use, returns a default value,
     * usually 02:00:00:00:00:00
     */
    override fun getYourDeviceAddress(): String = bluetoothManager.adapter.address


    override fun connectToDevice(device: BluetoothDevice) {
        Log.d("zzz", "<connectToChatDevice>")
        gattClientCallback = GattClientCallback()
        gattClient = device.connectGatt(
            app, false,
            gattClientCallback
        )
        isScanning.value = false
        isConnecting.value = true
        _bleState.postValue(BleState.StartConnect)
    }

    override fun sendMessage(message: String): Boolean {

        Log.d("zzz", "<sendMessage>")

        messageCharacteristic?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val messageBytes = message.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes
            Log.d("zzz", "messageBytes: ${messageBytes}")
            Log.d("zzz", "gatt: ${gatt}")
            gatt?.let {
                val success = it.writeCharacteristic(messageCharacteristic)
                Log.d("zzz", "onServicesDiscovered: message send: $success")
                if (success) {
                    viewModel.messageResponse.value = "success"
                    //_bleState.postValue(BleState.Sended)
                } else {
                    viewModel.messageResponse.value = "fail"
                }
            } ?: run {
                viewModel.messageResponse.value = "no connection"
                Log.d("zzz", "sendMessage: no gatt connection to send a message with")
            }
        }
        return false
    }

    /**
     * Function to setup a local GATT server.
     * This requires setting up the available services and characteristics that other devices
     * can read and modify.
     */
    private fun setupGattServer(app: App) {

        Log.d("zzz", "<setupGattServer>")

        gattServerCallback = GattServerCallback()

        gattServer = bluetoothManager.openGattServer(
            app,
            gattServerCallback
        ).apply {
            addService(setupGattService())
        }
    }

    /**
     * Function to create the GATT Server with the required characteristics and descriptors
     */
    private fun setupGattService(): BluetoothGattService {
        Log.d("zzz", "<setupGattService>")

        // Setup gatt service
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        // need to ensure that the property is writable and has the write permission
        val messageCharacteristic = BluetoothGattCharacteristic(
            MESSAGE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(messageCharacteristic)
        val confirmCharacteristic = BluetoothGattCharacteristic(
            CONFIRM_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(confirmCharacteristic)

        return service
    }

    /**
     * Start advertising this device so other BLE devices can see it and connect
     */
    private fun startAdvertisement() {

        advertiser = adapter.bluetoothLeAdvertiser
        Log.d("zzz", "<startAdvertisement> ${advertiser}")

        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()

            advertiser?.startAdvertising(
                advertiseSettings,
                advertiseData,
                advertiseCallback
            )
        }
    }

    /**
     * Stops BLE Advertising.
     */
    private fun stopAdvertising() {
        Log.d("zzz", "<stopAdvertising> ${advertiser}")
        advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private fun buildAdvertiseData(): AdvertiseData {
        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         * This limit is outlined in section 2.3.1.1 of this document:
         * https://inst.eecs.berkeley.edu/~ee290c/sp18/note/BLE_Vol6.pdf
         *
         * This limit includes everything put into AdvertiseData including UUIDs, device info, &
         * arbitrary service or manufacturer data.
         * Attempting to send packets over this limit will result in a failure with error code
         * AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         * onStartFailure() method of an AdvertiseCallback implementation.
         */
        val dataBuilder = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
        return dataBuilder.build()
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private fun buildAdvertiseSettings(): AdvertiseSettings {
        Log.d("zzz", "<buildAdvertiseSettings>")
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTimeout(0)
            .build()
    }

    /**
     * Custom callback for the Gatt Server this device implements
     */
    inner class GattServerCallback : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)

            Log.d("zzz", "<GattServerCallback: onConnectionStateChange>")

            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.d(
                "zzz",
                "onConnectionStateChange: Server $device ${device.name} success: $isSuccess connected: $isConnected"
            )
            if (isSuccess && isConnected) {
                Log.d("zzz", "bluetoothDevice: ${device}")
                bluetoothDevice = device
            } else {
                _bleState.postValue(BleState.Disconnected)
                isScanning.value = false
                isConnecting.value = false
                bluetoothDevice = null
                bleDeviceName.value = ""
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)

            Log.d("zzz", "<onCharacteristicWriteRequest")

            if (characteristic.uuid == MESSAGE_UUID) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                val message = value?.toString(Charsets.UTF_8)
                Log.d("zzz", "onCharacteristicWriteRequest: Have message: \"$message\"")

                message?.let {
                    viewModel.message.value = message
                    _messages.postValue(Message.RemoteMessage(it))
                }
            }
        }
    }

    inner private class GattClientCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            Log.d("zzz", "<GattClientCallback: onConnectionStateChange")

            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.d("zzz", "onConnectionStateChange: Client $gatt  success: $isSuccess connected: $isConnected")
            // try to send a message to the other device as a test
            if (isSuccess && isConnected) {
                // discover services
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(discoveredGatt, status)

            Log.d("zzz", "<onServicesDiscovered>")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("zzz", "onServicesDiscovered: Have gatt $discoveredGatt")
                gatt = discoveredGatt
                val service = discoveredGatt.getService(SERVICE_UUID)
                messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)

                _bleState.postValue(BleState.Connected)
                isConnecting.value = false

            }
        }
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class DeviceAdvertiseCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            Log.d("zzz", "<DeviceAdvertiseCallback: onStartFailure>")

            // Send error state to display
            val errorMessage = "Advertise failed with error: $errorCode"
            Log.d("zzz", "Advertising failed")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d("zzz", "<DeviceAdvertiseCallback: onStartSuccess>")
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager?
        bluetoothManager?.adapter
    }

    private val bleScanner: BluetoothLeScanner? by lazy { bluetoothAdapter?.bluetoothLeScanner }

    override val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled ?: false

    override val bleDeviceName = MutableStateFlow("")
    override var bluetoothDevice: BluetoothDevice? = null

    override val isScanning = MutableStateFlow(false)
    override val isConnecting = MutableStateFlow(false)

    @SuppressLint("MissingPermission")
    override fun startScan() {
        Log.d("zzz", "startScan")
        if (isBluetoothEnabled) {
            try {
                isScanning.value = true
                isConnecting.value = false
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                bleScanner?.startScan(emptyList(), settings, scanCallback)

                _bleState.postValue(BleState.Scanning)

                Log.d("zzz", "scan started")
            } catch (e: Exception) {
                Log.e("zzz", e.stackTraceToString())
                isScanning.value = false
                _bleState.postValue(BleState.ScanningStoped)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        Log.d("zzz", "stopScan")
        if (isBluetoothEnabled) {
            try {
                bleScanner?.stopScan(scanCallback)
                Log.d("zzz", "scan stopped")
            } catch (e: Exception) {
                Log.e("zzz", e.stackTraceToString())
            }
        }

        isScanning.value = false
        _bleState.postValue(BleState.ScanningStoped)
    }

    override fun close() {
        Log.d("zzz", "close")
        stopScan()
    }


    private val scanCallback = object : ScanCallback() {

        override fun onBatchScanResults(results: MutableList<ScanResult?>?) {
            Log.d("zzz", "onBatchScanResults")
            results?.forEach { result ->
                result?.let { handleScanResult(it) }
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.d("zzz", "onScanResults")
            result?.let { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("zzz", "onScanFailed: errorCode=$errorCode")
            isScanning.value = false
        }
    }


    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {


        if (result.scanRecord != null) {
            if (result.scanRecord!!.deviceName != null) {

                Log.d("zzz", "${result.scanRecord!!.deviceName}")
                Log.d("zzz", "${result.scanRecord?.serviceUuids?.get(0)}")

                val bleDeviceInfo = BleDeviceInfo(
                    timestamp = result.timestampNanos.toString(),
                    bluetoothDevice = result.device,
                    name = result.scanRecord?.deviceName ?: "",
                    address = result.device.address,
                    serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid }
                        ?: emptyList(),
                    manufacturerSpecificData = result.scanRecord?.manufacturerSpecificData?.hexStrings(
                        ""
                    ) ?: emptyList(),
                    advertiseFlags = result.scanRecord?.advertiseFlags ?: 0,
                    rssi = result.rssi,
                )

                if (result.device.name in supported_devices) {
                    bleDeviceName.value = result.device.name
                    Log.d ("zzz", "device: ${bleDeviceName.value}")
                    bluetoothDevice = result.device
                    stopScan()
                }
            }
        }
    }

    private fun ByteArray.hexString(separator: String = " "): String {
        return joinToString(separator) { it.toUByte().toString(16).padStart(2, '0').uppercase() }
    }

    private fun SparseArray<ByteArray>.hexStrings(separator: String = " "): List<String> {
        val list = LinkedList<String>()
        this.forEach { _, value -> list.add(value.hexString(separator)) }
        return list
    }


}