package org.triangle.settings.system

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Реальное сканирование Bluetooth: Classic-обнаружение через `ACTION_FOUND`
 * и BLE через `BluetoothLeScanner`. Список содержит только те устройства,
 * которые нашла система; при отсутствии разрешений или выключенном адаптере
 * сканирование не запускается (UI честно объясняет причину).
 */
class BluetoothScanner(private val context: Context) {

    data class FoundDevice(
        val name: String,
        val address: String,
        val rssi: Int?,
        val isLe: Boolean,
        val isBonded: Boolean
    )

    private val adapter: BluetoothAdapter?
        get() = try {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        } catch (e: Exception) {
            null
        }

    private val handler = Handler(Looper.getMainLooper())
    private var receiver: BroadcastReceiver? = null
    private var leCallback: ScanCallback? = null
    private var onFound: ((FoundDevice) -> Unit)? = null
    private var onFinished: (() -> Unit)? = null
    private var isRunning = false

    fun isSupported(): Boolean = adapter != null

    fun isBluetoothEnabled(): Boolean = try {
        adapter?.isEnabled == true
    } catch (e: Exception) {
        false
    }

    fun hasScanPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED

    fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    fun isRunning(): Boolean = isRunning

    @SuppressLint("MissingPermission")
    fun start(onFound: (FoundDevice) -> Unit, onFinished: () -> Unit) {
        stop()
        val localAdapter = adapter
        if (localAdapter == null || !hasScanPermission() || !localAdapter.isEnabled) {
            onFinished()
            return
        }
        this.onFound = onFound
        this.onFinished = onFinished
        isRunning = true

        // Classic discovery
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        val classicReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        device?.let {
                            emit(it, if (rssi == Short.MIN_VALUE) null else rssi.toInt(), isLe = false)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> finish()
                }
            }
        }
        receiver = classicReceiver
        try {
            ContextCompat.registerReceiver(
                context,
                classicReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            localAdapter.startDiscovery()
        } catch (e: Exception) {
            android.util.Log.w("BluetoothScanner", "classic discovery failed", e)
        }

        // BLE scan
        try {
            val scanner = localAdapter.bluetoothLeScanner
            if (scanner != null) {
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        val device = result?.device ?: return
                        emit(device, result.rssi, isLe = true)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        android.util.Log.w("BluetoothScanner", "BLE scan failed: $errorCode")
                    }
                }
                leCallback = callback
                scanner.startScan(
                    null,
                    ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build(),
                    callback
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("BluetoothScanner", "BLE scan failed", e)
        }

        handler.postDelayed({ finish() }, SCAN_DURATION_MS)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        handler.removeCallbacksAndMessages(null)
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Уже отрегистрирован.
            }
        }
        receiver = null
        leCallback?.let {
            try {
                adapter?.bluetoothLeScanner?.stopScan(it)
            } catch (e: Exception) {
                // Адаптер мог выключиться.
            }
        }
        leCallback = null
        try {
            adapter?.cancelDiscovery()
        } catch (e: Exception) {
            // Ничего страшного: обнаружение уже остановлено.
        }
        isRunning = false
    }

    @SuppressLint("MissingPermission")
    private fun emit(device: BluetoothDevice, rssi: Int?, isLe: Boolean) {
        if (!isRunning) return
        val address = device.address ?: return
        val name = try {
            device.name
        } catch (e: SecurityException) {
            null
        }
        val bonded = try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (e: SecurityException) {
            false
        }
        onFound?.invoke(
            FoundDevice(
                name = name?.takeIf { it.isNotBlank() } ?: address,
                address = address,
                rssi = rssi,
                isLe = isLe,
                isBonded = bonded
            )
        )
    }

    private fun finish() {
        if (!isRunning) return
        stop()
        val callback = onFinished
        onFinished = null
        callback?.invoke()
    }

    @SuppressLint("MissingPermission")
    fun pair(address: String): Boolean {
        if (!hasConnectPermission()) return false
        return try {
            val device = adapter?.getRemoteDevice(address) ?: return false
            if (device.bondState == BluetoothDevice.BOND_BONDED) return true
            device.createBond()
        } catch (e: Exception) {
            android.util.Log.w("BluetoothScanner", "cannot pair $address", e)
            false
        }
    }

    companion object {
        const val SCAN_DURATION_MS = 12_000L
    }
}
