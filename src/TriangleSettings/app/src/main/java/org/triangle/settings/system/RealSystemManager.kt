package org.triangle.settings.system

import android.app.UiModeManager
import android.app.AppOpsManager
import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.format.Formatter

/** Имя appops-операции «разрешить уведомления» (AppOpsManager.OPSTR_POST_NOTIFICATION). */
private const val OP_POST_NOTIFICATION = "android:post_notification"

data class RealAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val icon: android.graphics.drawable.Drawable? = null
)

data class RealWifiItem(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val isSecure: Boolean,
    val isConnected: Boolean
)

data class RealSimInfo(
    val subId: Int,
    val simSlotIndex: Int,
    val carrierName: String,
    val displayName: String,
    val number: String,
    val isDataRoaming: Boolean
)

data class RealBtDeviceItem(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val majorClass: String,
    val batteryLevel: Int = -1
)

data class RealVpnProfileItem(
    val name: String,
    val type: String,
    val server: String,
    val isConnected: Boolean = false
)

/**
 * Полнофункциональный менеджер служб Android 15:
 * Сеть, Wi-Fi сканер, SIM-карты, Точка доступа, Звук, Экран, Батарея, Приложения.
 */
class RealSystemManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    private val a11yManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
    private val sensorPrivacyManager = try {
        context.getSystemService("sensor_privacy")
    } catch (e: Exception) { null }

    // ========================================
    // СЕТЬ И ИНТЕРНЕТ (Полный набор параметров)
    // ========================================

    fun isAirplaneModeOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        } catch (e: Exception) { false }
    }

    fun setAirplaneMode(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (enable) 1 else 0)
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply { putExtra("state", enable) }
            context.sendBroadcast(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isDataRoamingOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DATA_ROAMING, 0) == 1
        } catch (e: Exception) { false }
    }

    fun setDataRoaming(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.DATA_ROAMING, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isMobileDataOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setMobileData(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "mobile_data", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isDataSaverOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "data_saver_mode", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setDataSaver(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "data_saver_mode", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getPrivateDnsMode(): String {
        return try {
            val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode") ?: "opportunistic"
            val specifier = Settings.Global.getString(context.contentResolver, "private_dns_specifier") ?: ""
            when (mode) {
                "off" -> "Отключен"
                "hostname" -> if (specifier.isNotEmpty()) specifier else "Пользовательский"
                else -> "Автоматически"
            }
        } catch (e: Exception) { "Автоматически" }
    }

    fun getPrivateDnsRawMode(): String {
        return try {
            Settings.Global.getString(context.contentResolver, "private_dns_mode") ?: "opportunistic"
        } catch (e: Exception) { "opportunistic" }
    }

    fun getPrivateDnsHostname(): String {
        return try {
            Settings.Global.getString(context.contentResolver, "private_dns_specifier") ?: ""
        } catch (e: Exception) { "" }
    }

    fun setPrivateDns(mode: String, hostname: String = "") {
        try {
            Settings.Global.putString(context.contentResolver, "private_dns_mode", mode)
            if (mode == "hostname") {
                Settings.Global.putString(context.contentResolver, "private_dns_specifier", hostname)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Wi-Fi
    fun isWifiOn(): Boolean {
        return try { wifiManager?.isWifiEnabled == true } catch (e: Exception) { false }
    }

    fun setWifi(enable: Boolean) {
        try {
            @Suppress("DEPRECATION")
            wifiManager?.setWifiEnabled(enable)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getRealWifiSummary(): Pair<Boolean, String> {
        return try {
            val isEnabled = wifiManager?.isWifiEnabled == true
            if (!isEnabled) return Pair(false, "Отключено")

            val activeNetwork = connManager?.activeNetwork
            val caps = connManager?.getNetworkCapabilities(activeNetwork)

            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                @Suppress("DEPRECATION")
                val info = wifiManager?.connectionInfo
                val rawSsid = info?.ssid ?: ""
                val cleanSsid = if (rawSsid.startsWith("\"") && rawSsid.endsWith("\"")) {
                    rawSsid.substring(1, rawSsid.length - 1)
                } else rawSsid
                val finalSsid = if (cleanSsid.isNotEmpty() && cleanSsid != "<unknown ssid>") cleanSsid else "Подключено"
                Pair(true, finalSsid)
            } else {
                Pair(true, "Включен (поиск сетей)")
            }
        } catch (e: Exception) { Pair(false, "Недоступно") }
    }

    fun getRealWifiScanList(): List<RealWifiItem> {
        return try {
            try {
                @Suppress("DEPRECATION")
                wifiManager?.startScan()
            } catch (e: Exception) {}

            @Suppress("DEPRECATION")
            val currentInfo = wifiManager?.connectionInfo
            val currentBssid = currentInfo?.bssid ?: ""

            @Suppress("DEPRECATION")
            val scanResults: List<ScanResult> = wifiManager?.scanResults ?: emptyList()
            android.util.Log.i("TriangleSettings", "getRealWifiScanList raw scanResults size: ")

            val seen = mutableSetOf<String>()
            val result = mutableListOf<RealWifiItem>()

            for (scan in scanResults) {
                @Suppress("DEPRECATION")
                val ssid = scan.SSID ?: ""
                if (ssid.isBlank() || seen.contains(ssid)) continue
                seen.add(ssid)

                val isConnected = scan.BSSID == currentBssid
                val isSecure = scan.capabilities.contains("WPA") || scan.capabilities.contains("WEP") || scan.capabilities.contains("EAP")

                result.add(
                    RealWifiItem(
                        ssid = ssid,
                        bssid = scan.BSSID,
                        level = scan.level,
                        isSecure = isSecure,
                        isConnected = isConnected
                    )
                )
            }

            android.util.Log.i("TriangleSettings", "getRealWifiScanList processed items: ")
            result.sortedWith(compareByDescending<RealWifiItem> { it.isConnected }.thenByDescending { it.level })
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "getRealWifiScanList error: ", e)
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    fun connectToWifi(ssid: String, password: String? = null): Pair<Boolean, String> {
        val cleanSsid = ssid.replace("\"", "").trim()
        val secType = if (password.isNullOrEmpty()) "open" else "wpa2"
        android.util.Log.i("TriangleSettings", "connectToWifi start for: $cleanSsid, secType: $secType")

        // 1. WifiManager API
        try {
            val conf = android.net.wifi.WifiConfiguration().apply {
                SSID = "\"$cleanSsid\""
                if (password.isNullOrEmpty()) {
                    allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                } else {
                    preSharedKey = "\"$password\""
                    allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK)
                }
            }

            var netId = -1
            val existingList = wifiManager?.configuredNetworks
            val existing = existingList?.firstOrNull { it.SSID != null && it.SSID.replace("\"", "") == cleanSsid }

            if (existing != null) {
                conf.networkId = existing.networkId
                val updateRes = wifiManager?.updateNetwork(conf) ?: -1
                netId = if (updateRes != -1) updateRes else existing.networkId
            } else {
                netId = wifiManager?.addNetwork(conf) ?: -1
            }

            if (netId != -1) {
                wifiManager?.disconnect()
                val enabled = wifiManager?.enableNetwork(netId, true) ?: false
                val reconnected = wifiManager?.reconnect() ?: false
                android.util.Log.i("TriangleSettings", "WifiManager connect netId: $netId, enabled: $enabled, reconnected: $reconnected")
                if (enabled) {
                    return Pair(true, "Подключение к $cleanSsid...")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("TriangleSettings", "WifiManager standard connect warning: ${e.message}")
        }

        // 2. WifiNetworkSuggestion API (Android 10+)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val suggestionBuilder = android.net.wifi.WifiNetworkSuggestion.Builder()
                    .setSsid(cleanSsid)
                if (!password.isNullOrEmpty()) {
                    suggestionBuilder.setWpa2Passphrase(password)
                }
                suggestionBuilder.setIsInitialAutojoinEnabled(true)
                val suggestion = suggestionBuilder.build()
                val status = wifiManager?.addNetworkSuggestions(listOf(suggestion))
                android.util.Log.i("TriangleSettings", "addNetworkSuggestions status: $status")
                if (status == android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    return Pair(true, "Сеть сохранена. Ожидание авторизации...")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "Suggestion error: ${e.message}")
        }

        // 3. Fallback: cmd -w wifi connect-network
        try {
            val cmd = if (password.isNullOrEmpty()) {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType)
            } else {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType, password)
            }
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)
            android.util.Log.i("TriangleSettings", "cmd wifi output: $output, exitCode: ${proc.exitValue()}")
            if (proc.exitValue() == 0 || !output.contains("Exception", ignoreCase = true)) {
                return Pair(true, "Команда подключения отправлена")
            } else {
                return Pair(false, output.trim().ifEmpty { "Ошибка подключения к сети" })
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "cmd wifi error: ${e.message}")
        }

        return Pair(false, "Не удалось настроить профиль сети")
    }

    // SIM
    fun getRealSimCards(): List<RealSimInfo> {
        val result = mutableListOf<RealSimInfo>()
        try {
            val subs: List<SubscriptionInfo>? = subManager?.activeSubscriptionInfoList
            if (!subs.isNullOrEmpty()) {
                for (sub in subs) {
                    result.add(
                        RealSimInfo(
                            subId = sub.subscriptionId,
                            simSlotIndex = sub.simSlotIndex,
                            carrierName = sub.carrierName?.toString() ?: "Оператор",
                            displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                            number = sub.number ?: "",
                            isDataRoaming = sub.dataRoaming == SubscriptionManager.DATA_ROAMING_ENABLE
                        )
                    )
                }
            } else {
                val opName = telephonyManager?.networkOperatorName
                if (!opName.isNullOrEmpty()) {
                    result.add(
                        RealSimInfo(
                            subId = 1,
                            simSlotIndex = 0,
                            carrierName = opName,
                            displayName = opName,
                            number = "",
                            isDataRoaming = isDataRoamingOn()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getPreferredNetworkType(): String {
        return try {
            val mode = Settings.Global.getInt(context.contentResolver, "preferred_network_mode", 9)
            when (mode) {
                26, 28, 31, 33 -> "5G / 4G / 3G / 2G (Авто)"
                9, 10, 11, 12 -> "4G (LTE) / 3G / 2G"
                0, 1, 2 -> "3G / 2G"
                1 -> "Только 2G"
                else -> "4G (LTE) предпочтительно"
            }
        } catch (e: Exception) { "4G (LTE) предпочтительно" }
    }

    fun cyclePreferredNetworkType(): String {
        return try {
            val current = Settings.Global.getInt(context.contentResolver, "preferred_network_mode", 9)
            val next = when (current) {
                9 -> 26 // переключить на 5G
                26 -> 0 // переключить на 3G
                0 -> 1  // переключить на 2G
                else -> 9 // обратно на 4G
            }
            Settings.Global.putInt(context.contentResolver, "preferred_network_mode", next)
            getPreferredNetworkType()
        } catch (e: Exception) { getPreferredNetworkType() }
    }

    // Точка доступа и модем
    fun isWifiHotspotOn(): Boolean {
        return try {
            val method = wifiManager?.javaClass?.getDeclaredMethod("isWifiApEnabled")
            (method?.invoke(wifiManager) as? Boolean) == true
        } catch (e: Exception) { false }
    }

    fun isUsbTetheringOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "usb_tethering_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setUsbTethering(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "usb_tethering_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isBluetoothTetheringOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "bluetooth_tethering_on", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setBluetoothTethering(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "bluetooth_tethering_on", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ========================================
    // VPN (Виртуальные частные сети)
    // ========================================

    fun isVpnConnected(): Boolean {
        return try {
            val activeNet = connManager?.activeNetwork ?: return false
            val caps = connManager?.getNetworkCapabilities(activeNet) ?: return false
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) { false }
    }

    fun getAlwaysOnVpnPackage(): String? {
        return try {
            val pkg = Settings.Secure.getString(context.contentResolver, "always_on_vpn_app")
            if (pkg.isNullOrEmpty() || pkg == "null") null else pkg
        } catch (e: Exception) { null }
    }

    fun isAlwaysOnVpnLockdown(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "always_on_vpn_lockdown", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setAlwaysOnVpn(pkg: String?, lockdown: Boolean) {
        try {
            if (pkg != null) {
                Settings.Secure.putString(context.contentResolver, "always_on_vpn_app", pkg)
                Settings.Secure.putInt(context.contentResolver, "always_on_vpn_lockdown", if (lockdown) 1 else 0)
            } else {
                Settings.Secure.putString(context.contentResolver, "always_on_vpn_app", null)
                Settings.Secure.putInt(context.contentResolver, "always_on_vpn_lockdown", 0)
            }
        } catch (e: Exception) { e.printStackTrace() }
        if (pkg != null) {
            executeRootCommand("settings put secure always_on_vpn_app $pkg; settings put secure always_on_vpn_lockdown ${if (lockdown) 1 else 0}")
        } else {
            executeRootCommand("settings delete secure always_on_vpn_app; settings put secure always_on_vpn_lockdown 0")
        }
    }

    fun getInstalledVpnApps(): List<RealAppItem> {
        val result = mutableListOf<RealAppItem>()
        val pm = context.packageManager
        val intent = Intent("android.net.VpnService")
        val services = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        val seen = mutableSetOf<String>()

        for (resolveInfo in services) {
            val sInfo = resolveInfo.serviceInfo ?: continue
            val pkg = sInfo.packageName
            if (seen.contains(pkg)) continue
            seen.add(pkg)

            val appInfo = sInfo.applicationInfo ?: continue
            val label = appInfo.loadLabel(pm)?.toString() ?: pkg
            val icon = appInfo.loadIcon(pm)
            val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            result.add(
                RealAppItem(
                    packageName = pkg,
                    appName = label,
                    isSystemApp = isSys,
                    icon = icon
                )
            )
        }
        return result
    }

    fun getSavedVpnProfiles(): List<RealVpnProfileItem> {
        val profiles = mutableListOf<RealVpnProfileItem>()
        try {
            val prefs = context.getSharedPreferences("triangle_vpn_profiles", Context.MODE_PRIVATE)
            val count = prefs.getInt("count", 0)
            for (i in 0 until count) {
                val name = prefs.getString("name_$i", "") ?: ""
                val type = prefs.getString("type_$i", "IKEv2/IPSec PSK") ?: "IKEv2/IPSec PSK"
                val server = prefs.getString("server_$i", "") ?: ""
                if (name.isNotEmpty() && server.isNotEmpty()) {
                    // Профили только сохранены этим приложением; реальный статус туннеля
                    // показывает верхний блок «Состояние VPN» (isVpnConnected).
                    profiles.add(RealVpnProfileItem(name = name, type = type, server = server))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return profiles
    }

    fun saveVpnProfile(name: String, type: String, server: String) {
        try {
            val prefs = context.getSharedPreferences("triangle_vpn_profiles", Context.MODE_PRIVATE)
            val count = prefs.getInt("count", 0)
            prefs.edit().apply {
                putString("name_$count", name)
                putString("type_$count", type)
                putString("server_$count", server)
                putInt("count", count + 1)
                apply()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun deleteVpnProfile(index: Int) {
        try {
            val prefs = context.getSharedPreferences("triangle_vpn_profiles", Context.MODE_PRIVATE)
            val current = getSavedVpnProfiles().toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                val editor = prefs.edit().clear()
                editor.putInt("count", current.size)
                current.forEachIndexed { i, p ->
                    editor.putString("name_$i", p.name)
                    editor.putString("type_$i", p.type)
                    editor.putString("server_$i", p.server)
                }
                editor.apply()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ========================================
    // BLUETOOTH И ПОДКЛЮЧЕННЫЕ УСТРОЙСТВА
    // ========================================

    fun isBluetoothOn(): Boolean {
        return try {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        } catch (e: Exception) { false }
    }

    fun setBluetooth(enable: Boolean) {
        try {
            @Suppress("DEPRECATION")
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
            @Suppress("DEPRECATION")
            if (enable) adapter.enable() else adapter.disable()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getBluetoothDeviceName(): String {
        return try {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()?.name ?: Build.MODEL
        } catch (e: Exception) { Build.MODEL }
    }

    fun getBondedBluetoothDevices(): List<RealBtDeviceItem> {
        val result = mutableListOf<RealBtDeviceItem>()
        try {
            @Suppress("DEPRECATION")
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            val bonded = adapter.bondedDevices ?: return emptyList()

            for (dev in bonded) {
                val devClass = dev.bluetoothClass?.majorDeviceClass ?: 0
                val classLabel = when (devClass) {
                    android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> "Аудиоустройство / Наушники"
                    android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> "Компьютер"
                    android.bluetooth.BluetoothClass.Device.Major.PHONE -> "Смартфон"
                    android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL -> "Клавиатура / Мышь"
                    android.bluetooth.BluetoothClass.Device.Major.WEARABLE -> "Умные часы / Браслет"
                    else -> "Bluetooth аксессуар"
                }

                var isConnected = false
                try {
                    val method = dev.javaClass.getMethod("isConnected")
                    isConnected = (method.invoke(dev) as? Boolean) == true
                } catch (e: Exception) { isConnected = false }

                var battery = -1
                try {
                    val method = dev.javaClass.getMethod("getBatteryLevel")
                    battery = (method.invoke(dev) as? Int) ?: -1
                } catch (e: Exception) { battery = -1 }

                result.add(
                    RealBtDeviceItem(
                        name = dev.name ?: dev.address,
                        address = dev.address,
                        isConnected = isConnected,
                        majorClass = classLabel,
                        batteryLevel = battery
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.sortedByDescending { it.isConnected }
    }

    fun isNfcAvailable(): Boolean {
        return try {
            val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(context)
            nfcAdapter != null
        } catch (e: Exception) { false }
    }

    fun isNfcOn(): Boolean {
        return try {
            val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(context)
            nfcAdapter?.isEnabled == true
        } catch (e: Exception) { false }
    }

    fun isQuickShareOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "nearby_sharing_slice", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setQuickShare(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "nearby_sharing_slice", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Реальная проверка доступности приёма трансляции: смотрим маршруты MediaRouter
     * и отбираем только нелокальные (удалённые дисплеи / cast-приёмники).
     */
    fun isCastAvailable(): Boolean {
        return try {
            val router = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? android.media.MediaRouter
                ?: return false
            for (i in 0 until router.routeCount) {
                val route = router.getRouteAt(i) ?: continue
                if (route.playbackType != android.media.MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    // ========================================
    // ЭКРАН И ОТОБРАЖЕНИЕ
    // ========================================

    fun isAutoRotateOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        } catch (e: Exception) { false }
    }

    fun setAutoRotate(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isAutoBrightnessOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Exception) { false }
    }

    fun setAutoBrightness(enable: Boolean) {
        try {
            val mode = if (enable) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getScreenBrightnessPercent(): Int {
        return try {
            val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            (brightness * 100 / 255)
        } catch (e: Exception) { 50 }
    }

    fun setScreenBrightnessPercent(percent: Int) {
        try {
            val raw = (percent.coerceIn(0, 100) * 255 / 100)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, raw)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isPeakRefreshRate120(): Boolean {
        return try {
            val rate = Settings.System.getFloat(context.contentResolver, "peak_refresh_rate", 120f)
            rate >= 119f
        } catch (e: Exception) { true }
    }

    fun setPeakRefreshRate(enable120Hz: Boolean) {
        try {
            val targetRate = if (enable120Hz) 120f else 60f
            Settings.System.putFloat(context.contentResolver, "peak_refresh_rate", targetRate)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isDarkThemeOn(): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun setDarkTheme(enable: Boolean) {
        try {
            uiModeManager?.nightMode = if (enable) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isNightDisplayOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "night_display_activated", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setNightDisplay(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "night_display_activated", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getScreenTimeoutSummary(): String {
        return try {
            val ms = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
            when (ms) {
                15000 -> "15 секунд"
                30000 -> "30 секунд"
                60000 -> "1 минута"
                120000 -> "2 минуты"
                300000 -> "5 минут"
                600000 -> "10 минут"
                else -> "${ms / 1000} сек."
            }
        } catch (e: Exception) { "1 минута" }
    }

    fun getScreenTimeoutMs(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
        } catch (e: Exception) { 60000 }
    }

    fun setScreenTimeoutMs(ms: Int) {
        try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Дополнительные параметры экрана AOSP 15
    fun isTapToWakeOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "double_tap_to_wake", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setTapToWake(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "double_tap_to_wake", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isLiftToWakeOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "wake_gesture_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setLiftToWake(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "wake_gesture_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getColorMode(): String {
        return try {
            val mode = Settings.System.getInt(context.contentResolver, "display_color_mode", 0)
            when (mode) {
                0 -> "Естественные цвета (sRGB)"
                1 -> "Яркие цвета (DCI-P3)"
                2 -> "Насыщенные"
                else -> "Автоматически"
            }
        } catch (e: Exception) { "Естественные цвета (sRGB)" }
    }

    fun cycleColorMode(): String {
        return try {
            val current = Settings.System.getInt(context.contentResolver, "display_color_mode", 0)
            val next = when (current) {
                0 -> 1
                1 -> 2
                else -> 0
            }
            Settings.System.putInt(context.contentResolver, "display_color_mode", next)
            getColorMode()
        } catch (e: Exception) { getColorMode() }
    }

    fun getScreenResolutionSummary(): String {
        val dm = context.resources.displayMetrics
        return "${dm.widthPixels} × ${dm.heightPixels} (${dm.densityDpi} dpi)"
    }

    // ========================================
    // ЗВУК И ТАКТИЛЬНЫЙ ОТКЛИК
    // ========================================

    fun isLockSoundOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, "lockscreen_sounds_enabled", 1) == 1 } catch (e: Exception) { true }
    }

    fun setLockSound(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, "lockscreen_sounds_enabled", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isHapticFeedbackOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, "haptic_feedback_enabled", 1) == 1 } catch (e: Exception) { true }
    }

    fun setHapticFeedback(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, "haptic_feedback_enabled", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isDialpadSoundOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1 } catch (e: Exception) { false }
    }

    fun setDialpadSound(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isChargingSoundOn(): Boolean {
        return try { Settings.Global.getInt(context.contentResolver, "charging_sounds_enabled", 1) == 1 } catch (e: Exception) { true }
    }

    fun setChargingSound(enable: Boolean) {
        try { Settings.Global.putInt(context.contentResolver, "charging_sounds_enabled", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun getMediaVolumePercent(): Int {
        return try {
            val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            (cur * 100 / max)
        } catch (e: Exception) { 75 }
    }

    fun setMediaVolumePercent(pct: Int) {
        try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val vol = (pct.coerceIn(0, 100) * max / 100)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getRingVolumePercent(): Int {
        return try {
            val cur = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 10
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 15
            (cur * 100 / max)
        } catch (e: Exception) { 100 }
    }

    fun setRingVolumePercent(pct: Int) {
        try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 15
            val vol = (pct.coerceIn(0, 100) * max / 100)
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, vol, 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getAlarmVolumePercent(): Int {
        return try {
            val cur = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 5
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7
            (cur * 100 / max)
        } catch (e: Exception) { 80 }
    }

    fun setAlarmVolumePercent(pct: Int) {
        try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7
            val vol = (pct.coerceIn(0, 100) * max / 100)
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getNotificationVolumePercent(): Int {
        return try {
            val cur = audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 8
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 15
            (cur * 100 / max)
        } catch (e: Exception) { 70 }
    }

    fun setNotificationVolumePercent(pct: Int) {
        try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 15
            val vol = (pct.coerceIn(0, 100) * max / 100)
            audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, vol, 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getCallVolumePercent(): Int {
        return try {
            val cur = audioManager?.getStreamVolume(AudioManager.STREAM_VOICE_CALL) ?: 4
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) ?: 7
            (cur * 100 / max)
        } catch (e: Exception) { 60 }
    }

    fun setCallVolumePercent(pct: Int) {
        try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) ?: 7
            val vol = (pct.coerceIn(0, 100) * max / 100)
            audioManager?.setStreamVolume(AudioManager.STREAM_VOICE_CALL, vol, 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Режим звонка: 2 - Normal (Звук), 1 - Vibrate (Вибрация), 0 - Silent (Без звука)
    fun getRingerMode(): Int {
        return try {
            audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        } catch (e: Exception) { AudioManager.RINGER_MODE_NORMAL }
    }

    fun setRingerMode(mode: Int) {
        try {
            audioManager?.ringerMode = mode
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Имена и выбор мелодий через RingtoneManager
    fun getRingtoneTitle(type: Int): String {
        return try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, type)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: "По умолчанию"
            } else {
                "Без звука"
            }
        } catch (e: Exception) { "По умолчанию" }
    }

    data class RingtoneItem(val title: String, val uriString: String)

    fun getAvailableRingtones(type: Int): List<RingtoneItem> {
        val list = mutableListOf<RingtoneItem>()
        list.add(RingtoneItem("Без звука", ""))
        try {
            val rm = RingtoneManager(context)
            rm.setType(type)
            val cursor = rm.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = rm.getRingtoneUri(cursor.position).toString()
                list.add(RingtoneItem(title, uri))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    fun getActualRingtoneUri(type: Int): String {
        return try {
            RingtoneManager.getActualDefaultRingtoneUri(context, type)?.toString() ?: ""
        } catch (e: Exception) { "" }
    }

    fun setActualRingtone(type: Int, uriString: String) {
        try {
            val uri = if (uriString.isEmpty()) null else android.net.Uri.parse(uriString)
            RingtoneManager.setActualDefaultRingtoneUri(context, type, uri)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Режим «Не беспокоить» (Zen Mode)
    fun isZenModeOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "zen_mode", 0) != 0
        } catch (e: Exception) { false }
    }

    fun setZenMode(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "zen_mode", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Реальная политика «Не беспокоить» из NotificationManager: какие
     * категории уведомлений пробиваются и от каких отправителей.
     */
    data class DndPolicy(
        val priorityCategories: Int,
        val callSenders: Int,
        val messageSenders: Int
    )

    fun isDndPolicyAccessGranted(): Boolean = try {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .isNotificationPolicyAccessGranted
    } catch (e: Exception) {
        false
    }

    fun getDndPolicy(): DndPolicy? {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            val policy = nm.javaClass.getMethod("getNotificationPolicy").invoke(nm) ?: return null
            val cls = policy.javaClass
            DndPolicy(
                priorityCategories = cls.getField("priorityCategories").getInt(policy),
                callSenders = cls.getField("priorityCallSenders").getInt(policy),
                messageSenders = cls.getField("priorityMessageSenders").getInt(policy)
            )
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "cannot read DND policy", e)
            null
        }
    }

    /** true, если система применила политику и вернула те же значения. */
    fun setDndPolicy(categories: Int, callSenders: Int, messageSenders: Int): Boolean {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            val policyClass = Class.forName("android.app.NotificationManager\$Policy")
            val policy = policyClass
                .getConstructor(
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                .newInstance(categories, callSenders, messageSenders)
            nm.javaClass.getMethod("setNotificationPolicy", policyClass).invoke(nm, policy)
            val applied = getDndPolicy()
            applied != null &&
                applied.priorityCategories == categories &&
                applied.callSenders == callSenders &&
                applied.messageSenders == messageSenders
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "cannot change DND policy", e)
            false
        }
    }

    // Дополнительные параметры тактильного отклика и системных звуков
    fun isTouchSoundOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 1 } catch (e: Exception) { true }
    }

    fun setTouchSound(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isRingVibrationOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, "vibrate_when_ringing", 1) == 1 } catch (e: Exception) { true }
    }

    fun setRingVibration(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, "vibrate_when_ringing", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isNotificationVibrationOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, "notification_vibration_intensity", 1) != 0 } catch (e: Exception) { true }
    }

    fun setNotificationVibration(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, "notification_vibration_intensity", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isAlarmVibrationOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, "alarm_vibration_intensity", 1) != 0 } catch (e: Exception) { true }
    }

    fun setAlarmVibration(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, "alarm_vibration_intensity", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isVibrateOnTapOn(): Boolean {
        return try { Settings.System.getInt(context.contentResolver, "haptic_feedback_enabled", 1) == 1 } catch (e: Exception) { true }
    }

    fun setVibrateOnTap(enable: Boolean) {
        try { Settings.System.putInt(context.contentResolver, "haptic_feedback_enabled", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    // Пространственный звук (Spatial Audio)
    fun isSpatialAudioOn(): Boolean {
        return try { Settings.Secure.getInt(context.contentResolver, "spatial_audio", 0) == 1 } catch (e: Exception) { false }
    }

    fun setSpatialAudio(enable: Boolean) {
        try { Settings.Secure.putInt(context.contentResolver, "spatial_audio", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    // Автоматические субтитры (Live Caption)
    fun isLiveCaptionOn(): Boolean {
        return try { Settings.Secure.getInt(context.contentResolver, "odi_captions_volume_ui_enabled", 0) == 1 } catch (e: Exception) { false }
    }

    fun setLiveCaption(enable: Boolean) {
        try { Settings.Secure.putInt(context.contentResolver, "odi_captions_volume_ui_enabled", if (enable) 1 else 0) } catch (e: Exception) { e.printStackTrace() }
    }

    fun isHapticVibrationAvailable(): Boolean {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        return vibrator?.hasVibrator() == true
    }

    // ========================================
    // УВЕДОМЛЕНИЯ И КОНФИДЕНЦИАЛЬНОСТЬ
    // ========================================

    // История уведомлений
    fun isNotificationHistoryOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "notification_history_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setNotificationHistory(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "notification_history_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Всплывающие пузыри (Bubbles)
    fun isNotificationBubblesOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "notification_bubbles", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setNotificationBubbles(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "notification_bubbles", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Уведомления на экране блокировки:
    // 0 = не показывать вовсе
    // 1 = скрывать конфиденциальное содержимое (Hide sensitive)
    // 2 = показывать полностью
    fun getLockscreenNotificationMode(): Int {
        return try {
            val show = Settings.Secure.getInt(context.contentResolver, "lock_screen_show_notifications", 1) == 1
            if (!show) {
                0
            } else {
                val allowPrivate = Settings.Secure.getInt(context.contentResolver, "lock_screen_allow_private_notifications", 1) == 1
                if (allowPrivate) 2 else 1
            }
        } catch (e: Exception) { 2 }
    }

    fun setLockscreenNotificationMode(mode: Int) {
        try {
            when (mode) {
                0 -> {
                    Settings.Secure.putInt(context.contentResolver, "lock_screen_show_notifications", 0)
                }
                1 -> {
                    Settings.Secure.putInt(context.contentResolver, "lock_screen_show_notifications", 1)
                    Settings.Secure.putInt(context.contentResolver, "lock_screen_allow_private_notifications", 0)
                }
                2 -> {
                    Settings.Secure.putInt(context.contentResolver, "lock_screen_show_notifications", 1)
                    Settings.Secure.putInt(context.contentResolver, "lock_screen_allow_private_notifications", 1)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Мигание вспышки при уведомлении
    fun isFlashNotificationOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "camera_flash_notification", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setFlashNotification(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "camera_flash_notification", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Откладывание уведомлений (Snooze)
    fun isNotificationSnoozeOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "show_notification_snooze", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setNotificationSnooze(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "show_notification_snooze", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Доступ к уведомлениям (NotificationListenerService)
    private fun notificationListenerComponent(): android.content.ComponentName =
        android.content.ComponentName(
            context,
            org.triangle.settings.notification.NotificationLogService::class.java
        )

    /**
     * Доступ к уведомлениям включён? Источник правды — список
     * enabled_notification_listeners: он меняется сразу, тогда как служба
     * подключается с задержкой.
     */
    fun isNotificationListenerEnabled(): Boolean {
        return try {
            val current = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            val flat = notificationListenerComponent().flattenToString()
            current.split(":").any { it.equals(flat, ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    /** Включает/выключает наш слушатель в системном списке доступа. */
    fun setNotificationListenerEnabled(enable: Boolean): Boolean {
        return try {
            val flat = notificationListenerComponent().flattenToString()
            val current = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            val parts = current.split(":")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (enable) {
                if (parts.none { it.equals(flat, ignoreCase = true) }) parts.add(flat)
            } else {
                parts.removeAll { it.equals(flat, ignoreCase = true) }
            }
            Settings.Secure.putString(
                context.contentResolver,
                "enabled_notification_listeners",
                parts.joinToString(":")
            )
            isNotificationListenerEnabled()
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "cannot change listener access", e)
            false
        }
    }

    /** Служба действительно подключена к системе прямо сейчас. */
    fun isNotificationListenerConnected(): Boolean =
        org.triangle.settings.notification.NotificationLogService.connected

    /**
     * Может ли приложение менять appops-режимы. MANAGE_APP_OPS_MODES — привилегированное
     * разрешение: на обычной установке оно не выдаётся, поэтому переключать
     * уведомления чужих приложений можно только после интеграции в прошивку.
     */
    fun canManageAppOps(): Boolean = try {
        context.checkSelfPermission("android.permission.MANAGE_APP_OPS_MODES") ==
            PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    /**
     * Разрешены ли уведомления конкретному приложению (appops POST_NOTIFICATION),
     * а не нашему собственному процессу.
     */
    fun isAppNotificationsAllowed(packageName: String): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val uid = context.packageManager.getPackageUid(packageName, 0)
            appOps.unsafeCheckOpNoThrow(
                OP_POST_NOTIFICATION,
                uid,
                packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            true
        }
    }

    // Список приложений с разрешением на уведомления
    data class AppNotificationStatus(
        val appName: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?,
        val areNotificationsEnabled: Boolean
    )

    fun getAppsNotificationList(): List<AppNotificationStatus> {
        val list = mutableListOf<AppNotificationStatus>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in packages) {
                // Исключаем чисто внутренние системные библиотеки без UI
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasLauncher = pm.getLaunchIntentForPackage(app.packageName) != null
                if (isSystem && !hasLauncher) continue

                val appName = pm.getApplicationLabel(app).toString()
                val icon = pm.getApplicationIcon(app)
                val enabled = isAppNotificationsAllowed(app.packageName)
                list.add(AppNotificationStatus(appName, app.packageName, icon, enabled))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list.sortedBy { it.appName.lowercase() }
    }

    // ========================================
    // БАТАРЕЯ И ЭНЕРГОПОТРЕБЛЕНИЕ
    // ========================================

    fun isPowerSaveMode(): Boolean {
        return try { powerManager?.isPowerSaveMode == true } catch (e: Exception) { false }
    }

    fun setPowerSaveMode(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "low_power", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getBatteryPercentLevel(): Int {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val status = context.registerReceiver(null, filter)
            val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 77
            val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            (level * 100 / scale.toFloat()).toInt()
        } catch (e: Exception) { 77 }
    }

    fun getRealBatteryStats(): Pair<Int, Boolean> {
        val info = getDetailedBatteryInfo()
        return Pair(info.percentage, info.isCharging)
    }

    data class BatteryFullInfo(
        val percentage: Int,
        val isCharging: Boolean,
        val statusText: String,
        val health: String,
        val technology: String,
        val temperatureCelsius: Float,
        val voltageMv: Int,
        val plugType: String
    )

    fun getDetailedBatteryInfo(): BatteryFullInfo {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 77
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val pct = (level * 100 / scale.toFloat()).toInt()

            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val statusText = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Идет зарядка"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Разряжается"
                BatteryManager.BATTERY_STATUS_FULL -> "Полностью заряжен"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Подключен, не заряжается"
                else -> "Неизвестно"
            }

            val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD) ?: BatteryManager.BATTERY_HEALTH_GOOD
            val healthText = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Отличное"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Перегрев"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Неисправен"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Перенапряжение"
                BatteryManager.BATTERY_HEALTH_COLD -> "Низкая температура"
                else -> "Нормальное"
            }

            val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
            val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
            val tempC = tempRaw / 10f
            val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100) ?: 4100

            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val plugText = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Сетевое ЗУ (AC)"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB порт"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Беспроводная зарядка"
                else -> "Работа от аккумулятора"
            }

            BatteryFullInfo(
                percentage = pct,
                isCharging = isCharging,
                statusText = statusText,
                health = healthText,
                technology = tech,
                temperatureCelsius = tempC,
                voltageMv = voltage,
                plugType = plugText
            )
        } catch (e: Exception) {
            BatteryFullInfo(
                percentage = 77,
                isCharging = false,
                statusText = "Разряжается",
                health = "Отличное",
                technology = "Li-ion",
                temperatureCelsius = 28.5f,
                voltageMv = 4050,
                plugType = "Работа от аккумулятора"
            )
        }
    }

    // Процент заряда в строке состояния
    fun isBatteryPercentageOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "status_bar_show_battery_percent", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setBatteryPercentage(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "status_bar_show_battery_percent", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Адаптивное энергопотребление (Adaptive Battery)
    fun isAdaptiveBatteryOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "adaptive_battery_management_enabled", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setAdaptiveBattery(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "adaptive_battery_management_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Защита аккумулятора (лимит 80%)
    fun isBatteryProtectOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "charge_limit_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setBatteryProtect(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "charge_limit_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Кастомный расчет потребления заряда приложениями
    data class AppBatteryUsage(
        val appName: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?,
        val percentage: Float,
        val foregroundTimeMinutes: Long,
        val consumedMah: Double
    )

    /**
     * Достаёт BatteryUsageStats из скрытого системного сервиса batterystats.
     *
     * Оба перегруженных метода — SystemApi. Сначала пробуем версию без
     * аргументов (внутри она сама подставляет BatteryUsageStatsQuery.DEFAULT),
     * затем — явный запрос. Манифест объявляет android:usesNonSdkApi, поэтому
     * платформенно подписанному приложению hidden API доступны.
     */
    private fun invokeBatteryUsageStats(service: Any): Any? {
        try {
            return service.javaClass.getMethod("getBatteryUsageStats").invoke(service)
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "getBatteryUsageStats() failed, trying query overload", e)
        }
        return try {
            val queryClass = Class.forName("android.os.BatteryUsageStatsQuery")
            val query = queryClass.getField("DEFAULT").get(null)
            service.javaClass.getMethod("getBatteryUsageStats", queryClass).invoke(service, query)
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "getBatteryUsageStats(query) failed", e)
            null
        }
    }

    /**
     * Реальная статистика по UID из системного BatteryUsageStats.
     *
     * Требуется BATTERY_STATS; приложение подписано платформенным ключом,
     * поэтому разрешение выдаётся при установке. Если системный API
     * недоступен, возвращаем пустой список: выдуманные проценты запрещены.
     */
    fun getAppsBatteryUsageList(): List<AppBatteryUsage> {
        val list = mutableListOf<AppBatteryUsage>()
        var totalPower = 0.0
        try {
            val service = context.getSystemService("batterystats") ?: return emptyList()
            val stats = invokeBatteryUsageStats(service) ?: return emptyList()

            val consumers = stats.javaClass.getMethod("getUidBatteryConsumers")
                .invoke(stats) as? List<*> ?: return emptyList()
            // Знаменатель для долей — сумма расхода по всем UID-потребителям,
            // то есть тот же набор данных, что и в списке.

            val pm = context.packageManager
            for (consumer in consumers) {
                if (consumer == null) continue
                try {
                    val uid = consumer.javaClass.getMethod("getUid").invoke(consumer) as? Int ?: continue
                    val powerMah = (consumer.javaClass.getMethod("getConsumedPower").invoke(consumer) as? Double)
                        ?: continue
                    if (powerMah <= 0.0 || uid < 0) continue
                    totalPower += powerMah

                    val packages = pm.getPackagesForUid(uid) ?: continue
                    val packageName = packages.firstOrNull { pkg ->
                        try {
                            val ai = pm.getApplicationInfo(pkg, 0)
                            (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                pm.getLaunchIntentForPackage(pkg) != null
                        } catch (e: Exception) {
                            false
                        }
                    } ?: packages.firstOrNull() ?: continue

                    val appInfo = try {
                        pm.getApplicationInfo(packageName, 0)
                    } catch (e: Exception) {
                        continue
                    }
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        null
                    }
                    val existing = list.indexOfFirst { it.packageName == packageName }
                    if (existing >= 0) {
                        val old = list[existing]
                        list[existing] = old.copy(
                            consumedMah = old.consumedMah + powerMah
                        )
                    } else {
                        list.add(
                            AppBatteryUsage(
                                appName = appName,
                                packageName = packageName,
                                icon = icon,
                                percentage = 0f,
                                foregroundTimeMinutes = 0L,
                                consumedMah = powerMah
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Один битый UID не должен ломать весь список.
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "battery usage stats unavailable", e)
            return emptyList()
        }

        return list
            .filter { it.consumedMah >= 0.01 }
            .map {
                it.copy(
                    percentage = if (totalPower > 0) {
                        ((it.consumedMah / totalPower) * 100.0).toFloat()
                    } else {
                        -1f // ёмкость/общий расход неизвестны — процент не показываем
                    }
                )
            }
            .sortedByDescending { it.consumedMah }
            .take(25)
    }

    // ========================================
    // ДЛЯ РАЗРАБОТЧИКОВ (AOSP Development)
    // ========================================

    fun isDevelopmentSettingsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) { true }
    }

    fun setDevelopmentSettingsEnabled(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isUsbDebuggingOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) { false }
    }

    fun setUsbDebugging(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isWirelessDebuggingOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setWirelessDebugging(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isStayAwakeOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0
        } catch (e: Exception) { false }
    }

    fun setStayAwake(enable: Boolean) {
        try {
            val flag = if (enable) (1 or 2 or 4) else 0
            Settings.Global.putInt(context.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, flag)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isShowTouchesOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "show_touches", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setShowTouches(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "show_touches", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isPointerLocationOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "pointer_location", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setPointerLocation(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "pointer_location", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isMobileDataAlwaysOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "mobile_data_always_on", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setMobileDataAlwaysOn(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "mobile_data_always_on", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getSystemProp(key: String, def: String = ""): String {
        return try {
            val propClass = Class.forName("android.os.SystemProperties")
            val getMethod = propClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, key, def) as String
        } catch (e: Exception) { def }
    }

    private fun setSystemProp(key: String, value: String) {
        try {
            val propClass = Class.forName("android.os.SystemProperties")
            val setMethod = propClass.getMethod("set", String::class.java, String::class.java)
            setMethod.invoke(null, key, value)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isShowRefreshRateOn(): Boolean {
        return try {
            val prop = getSystemProp("debug.hwui.show_fps", "false")
            prop == "true" || Settings.System.getInt(context.contentResolver, "show_refresh_rate", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setShowRefreshRate(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "show_refresh_rate", if (enable) 1 else 0)
            setSystemProp("debug.hwui.show_fps", if (enable) "true" else "false")
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isForceRtlOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "debug.force_rtl", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setForceRtl(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "debug.force_rtl", if (enable) 1 else 0)
            setSystemProp("debug.force_rtl", if (enable) "1" else "0")
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isAlwaysFinishActivitiesOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 1
        } catch (e: Exception) { false }
    }

    fun setAlwaysFinishActivities(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isWifiVerboseLoggingOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "wifi_verbose_logging_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setWifiVerboseLogging(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "wifi_verbose_logging_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getWindowAnimationScale(): Float {
        return try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
        } catch (e: Exception) { 1f }
    }

    fun setWindowAnimationScale(scale: Float) {
        try {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, scale)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getTransitionAnimationScale(): Float {
        return try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        } catch (e: Exception) { 1f }
    }

    fun setTransitionAnimationScale(scale: Float) {
        try {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, scale)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getAnimatorDurationScale(): Float {
        return try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (e: Exception) { 1f }
    }

    fun setAnimatorDurationScale(scale: Float) {
        try {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setAllAnimationScales(scale: Float) {
        setWindowAnimationScale(scale)
        setTransitionAnimationScale(scale)
        setAnimatorDurationScale(scale)
    }

    fun cycleAnimationScale(): String {
        return try {
            val current = getWindowAnimationScale()
            val next = when {
                current <= 0.05f -> 0.5f
                current in 0.4f..0.6f -> 1.0f
                current in 0.9f..1.1f -> 1.5f
                current in 1.4f..1.6f -> 2.0f
                else -> 0.0f
            }
            setAllAnimationScales(next)
            if (next == 0f) "Анимации отключены" else "${next}x"
        } catch (e: Exception) { "1x" }
    }

    fun formatScaleText(scale: Float): String {
        return if (scale == 0f) "Без анимации" else "${scale}x"
    }

    fun getSmallestWidthDp(): Int {
        return try {
            context.resources.configuration.smallestScreenWidthDp
        } catch (e: Exception) { 392 }
    }

    fun isLayoutBoundsOn(): Boolean {
        return getSystemProp("debug.layout", "false") == "true"
    }

    fun setLayoutBounds(enable: Boolean) {
        try {
            setSystemProp("debug.layout", if (enable) "true" else "false")
            pokeSystemProperties()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isForceDarkOn(): Boolean {
        return getSystemProp("debug.hwui.force_dark", "false") == "true"
    }

    fun setForceDark(enable: Boolean) {
        try {
            setSystemProp("debug.hwui.force_dark", if (enable) "true" else "false")
            pokeSystemProperties()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isForce4xMsaaOn(): Boolean {
        return getSystemProp("debug.egl.force_msaa", "false") == "true"
    }

    fun setForce4xMsaa(enable: Boolean) {
        try {
            setSystemProp("debug.egl.force_msaa", if (enable) "true" else "false")
            pokeSystemProperties()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isDisableOverlaysOn(): Boolean {
        return getSystemProp("debug.sf.disable_hw_overlays", "0") == "1"
    }

    fun setDisableOverlays(enable: Boolean) {
        try {
            setSystemProp("debug.sf.disable_hw_overlays", if (enable) "1" else "0")
            pokeSystemProperties()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isStrictModeVisualOn(): Boolean {
        return getSystemProp("persist.sys.strictmode.visual", "0") == "1"
    }

    fun setStrictModeVisual(enable: Boolean) {
        try {
            setSystemProp("persist.sys.strictmode.visual", if (enable) "1" else "0")
            pokeSystemProperties()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isShowSurfaceUpdatesOn(): Boolean {
        return getSystemProp("debug.sf.showupdates", "0") == "1"
    }

    fun setShowSurfaceUpdates(enable: Boolean) {
        try {
            setSystemProp("debug.sf.showupdates", if (enable) "1" else "0")
            pokeSystemProperties()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isForceResizableActivitiesOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "force_resizable_activities", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setForceResizableActivities(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "force_resizable_activities", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isEnableFreeformSupportOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0) == 1
        } catch (e: Exception) { false }
    }

    // --- Дополнительные опции разработчика: ADB без авторизации, Логирование, Проверка подписи ---

    fun isInsecureAdbOn(): Boolean {
        return getSystemProp("ro.adb.secure", "1") == "0"
    }

    fun setInsecureAdb(enable: Boolean) {
        val target = if (enable) "0" else "1"
        executeRootCommand("resetprop ro.adb.secure $target; setprop ro.adb.secure $target")
    }

    fun isHighSystemLoggingOn(): Boolean {
        val size = getSystemProp("persist.logd.size", "")
        val printk = try {
            val file = java.io.File("/proc/sys/kernel/printk")
            if (file.exists()) file.readText().trim() else ""
        } catch (e: Exception) { "" }
        return size == "16M" || size == "16777216" || size == "8M" || printk.startsWith("8") || printk.startsWith("7")
    }

    fun setHighSystemLogging(enable: Boolean) {
        if (enable) {
            val cmds = """
                setprop persist.logd.size 16M
                setprop persist.logd.size.main 16M
                setprop persist.logd.size.system 16M
                setprop persist.logd.size.crash 16M
                setprop persist.logd.size.kernel 16M
                logcat -G 16M
                dmesg -n 8
                sysctl -w kernel.printk="8 4 1 4"
                echo "8 4 1 4" > /proc/sys/kernel/printk
                sysctl -w kernel.dmesg_restrict=0
                setprop log.tag V
                setprop persist.log.tag V
            """.trimIndent().replace("\n", "; ")
            executeRootCommand(cmds)
        } else {
            val cmds = """
                setprop persist.logd.size 256K
                setprop persist.logd.size.main 256K
                setprop persist.logd.size.system 256K
                setprop persist.logd.size.crash 256K
                setprop persist.logd.size.kernel 256K
                logcat -G 256K
                dmesg -n 4
                sysctl -w kernel.printk="4 4 1 4"
                echo "4 4 1 4" > /proc/sys/kernel/printk
                setprop persist.log.tag ""
            """.trimIndent().replace("\n", "; ")
            executeRootCommand(cmds)
        }
    }

    fun getDeviceIpAddress(): String {
        // 1. Попытка через WifiManager / ConnectionInfo
        try {
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ipStr = android.text.format.Formatter.formatIpAddress(ipInt)
                if (ipStr.isNotEmpty() && ipStr != "0.0.0.0" && !ipStr.startsWith("127.")) {
                    return ipStr
                }
            }
        } catch (e: Exception) { /* continue */ }

        // 2. Попытка через ConnectivityManager / LinkProperties
        try {
            val activeNet = connManager?.activeNetwork
            val linkProps = connManager?.getLinkProperties(activeNet)
            val linkAddresses = linkProps?.linkAddresses
            if (linkAddresses != null) {
                for (la in linkAddresses) {
                    val addr = la.address
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) { /* continue */ }

        // 3. Попытка через NetworkInterface (без проверки isUp, так как в Android 11+ isUp может требовать доп. привилегий)
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            var fallbackIp = ""
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty() && !host.startsWith("127.")) {
                            if (iface.name.contains("wlan")) {
                                return host
                            }
                            if (fallbackIp.isEmpty()) fallbackIp = host
                        }
                    }
                }
            }
            if (fallbackIp.isNotEmpty()) return fallbackIp
        } catch (e: Exception) { /* continue */ }

        // 4. Попытка через root/ip route
        try {
            val suPaths = listOf("su", "/product/bin/su", "/system/bin/su")
            for (su in suPaths) {
                try {
                    val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", "ip route get 1.1.1.1 | grep src | awk '{print \$7}'"))
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(proc.inputStream))
                    val line = reader.readLine()?.trim() ?: ""
                    proc.waitFor()
                    if (line.isNotEmpty() && !line.startsWith("127.")) {
                        return line
                    }
                } catch (e: Exception) { /* next */ }
            }
        } catch (e: Exception) { /* continue */ }

        return "127.0.0.1"
    }

    fun getWirelessAdbPort(): Int {
        val p1 = getSystemProp("service.adb.tcp.port", "")
        val p2 = getSystemProp("persist.adb.tcp.port", "")
        val p3 = getSystemProp("service.adb.tls.port", "")
        return p1.toIntOrNull() ?: p2.toIntOrNull() ?: p3.toIntOrNull() ?: 5555
    }

    fun restartAdbTcpPort(port: Int = 5555) {
        executeRootCommand("setprop persist.adb.tcp.port $port; setprop service.adb.tcp.port $port; stop adbd; start adbd")
    }

    fun ensureWirelessAdbActive(port: Int = 5555) {
        val current = getWirelessAdbPort()
        if (current == 0 || current == -1) {
            restartAdbTcpPort(port)
        }
    }

    fun isDisablePackageVerifierOn(): Boolean {
        return try {
            val v1 = Settings.Global.getInt(context.contentResolver, "verifier_verify_adb_installs", 1) == 0
            val v2 = Settings.Global.getInt(context.contentResolver, "package_verifier_enable", 1) == 0
            v1 || v2
        } catch (e: Exception) { false }
    }

    fun setDisablePackageVerifier(disable: Boolean) {
        try {
            val value = if (disable) 0 else 1
            Settings.Global.putInt(context.contentResolver, "verifier_verify_adb_installs", value)
            Settings.Global.putInt(context.contentResolver, "package_verifier_enable", value)
            Settings.Global.putInt(context.contentResolver, "disable_package_verifier", if (disable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
        executeRootCommand("settings put global verifier_verify_adb_installs ${if (disable) 0 else 1}; settings put global package_verifier_enable ${if (disable) 0 else 1}; settings put global disable_package_verifier ${if (disable) 1 else 0}")
    }

    fun setEnableFreeformSupport(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "enable_freeform_support", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isForceAllowOnExternalOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "force_allow_on_external", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setForceAllowOnExternal(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "force_allow_on_external", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isShowDevicesWithoutNamesOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "bluetooth_show_devices_without_names", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setShowDevicesWithoutNames(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "bluetooth_show_devices_without_names", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isTetheringHardwareAccelerationOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "tether_offload_disabled", 0) == 0
        } catch (e: Exception) { true }
    }

    fun setTetheringHardwareAcceleration(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "tether_offload_disabled", if (enable) 0 else 1)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getBackgroundProcessLimit(): Int {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE)
            val getMethod = am.javaClass.getMethod("getProcessLimit")
            getMethod.invoke(am) as Int
        } catch (e: Exception) { -1 }
    }

    fun setBackgroundProcessLimit(limit: Int) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE)
            val setMethod = am.javaClass.getMethod("setProcessLimit", Int::class.javaPrimitiveType)
            setMethod.invoke(am, limit)
        } catch (e: Exception) {
            try {
                Settings.Global.putInt(context.contentResolver, "app_process_limit", limit)
            } catch (ex: Exception) { ex.printStackTrace() }
        }
    }

    fun getBackgroundProcessLimitName(limit: Int): String {
        return when (limit) {
            -1 -> "Стандартный предел"
            0 -> "Без фоновых процессов"
            1 -> "Не более 1 процесса"
            2 -> "Не более 2 процессов"
            3 -> "Не более 3 процессов"
            4 -> "Не более 4 процессов"
            else -> "Стандартный ($limit)"
        }
    }

    private fun pokeSystemProperties() {
        try {
            val propClass = Class.forName("android.os.SystemProperties")
            val poke = propClass.getMethod("reportSyspropChanged")
            poke.invoke(null)
        } catch (e: Exception) {
            try {
                val am = Class.forName("android.app.ActivityManager")
                val poke = am.getMethod("pokeProperties")
                poke.invoke(null)
            } catch (ex: Exception) { /* ignore */ }
        }
    }

    // ========================================
    // СПЕЦИАЛЬНЫЕ ВОЗМОЖНОСТИ
    // ========================================

    fun isHighTextContrastOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setHighTextContrast(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "high_text_contrast_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isColorInversionOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "accessibility_display_inversion_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setColorInversion(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "accessibility_display_inversion_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isColorCorrectionOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "accessibility_display_daltonizer_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setColorCorrection(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isMagnificationOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "accessibility_display_magnification_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setMagnification(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "accessibility_display_magnification_enabled", if (enable) 1 else 0)
            Settings.Secure.putInt(context.contentResolver, "accessibility_display_magnification_navbar_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isLargePointerOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "accessibility_large_pointer_icon", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setLargePointer(enable: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "accessibility_large_pointer_icon", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isAudioMonoOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "master_mono", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setAudioMono(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "master_mono", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    data class AccessibilityServiceItem(
        val componentName: String,
        val label: String,
        val packageName: String,
        val isEnabled: Boolean,
        val icon: android.graphics.drawable.Drawable?
    )

    fun getInstalledAccessibilityServices(): List<AccessibilityServiceItem> {
        return try {
            val pm = context.packageManager
            val installed = a11yManager?.getInstalledAccessibilityServiceList() ?: emptyList()
            val enabledRaw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val enabledSet = enabledRaw.split(":").map { it.trim() }.toSet()

            installed.map { info ->
                val si = info.resolveInfo.serviceInfo
                val comp = "${si.packageName}/${si.name}"
                AccessibilityServiceItem(
                    componentName = comp,
                    label = info.resolveInfo.loadLabel(pm)?.toString() ?: si.name,
                    packageName = si.packageName,
                    isEnabled = enabledSet.contains(comp),
                    icon = try { info.resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
                )
            }.sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toggleAccessibilityService(componentName: String, enable: Boolean) {
        try {
            val enabledRaw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            val list = enabledRaw.split(":").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (enable) {
                if (!list.contains(componentName)) list.add(componentName)
            } else {
                list.remove(componentName)
            }
            val newString = list.joinToString(":")
            Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newString)
            Settings.Secure.putInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, if (list.isNotEmpty()) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========================================
    // МЕСТОПОЛОЖЕНИЕ И ПРИЛОЖЕНИЯ
    // ========================================

    fun isLocationOn(): Boolean {
        return try { locManager?.isLocationEnabled == true } catch (e: Exception) { false }
    }

    fun setLocation(enable: Boolean) {
        try {
            @Suppress("DEPRECATION")
            Settings.Secure.putInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                if (enable) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isWifiScanningOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "wifi_scan_always_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setWifiScanning(enable: Boolean) {
        try {
            Settings.Global.putInt(
                context.contentResolver,
                "wifi_scan_always_enabled",
                if (enable) 1 else 0
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isBluetoothScanningOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "ble_scan_always_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setBluetoothScanning(enable: Boolean) {
        try {
            Settings.Global.putInt(
                context.contentResolver,
                "ble_scan_always_enabled",
                if (enable) 1 else 0
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    data class LocationAppItem(
        val packageName: String,
        val appName: String,
        val icon: android.graphics.drawable.Drawable?
    )

    fun getLocationAppsList(): List<LocationAppItem> {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            val result = mutableListOf<LocationAppItem>()
            for (pkg in packages) {
                val perms = pkg.requestedPermissions ?: continue
                val hasLoc = perms.contains(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                             perms.contains(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                if (hasLoc) {
                    val appInfo = pkg.applicationInfo ?: continue
                    result.add(
                        LocationAppItem(
                            packageName = pkg.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                        )
                    )
                }
            }
            result.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getInstalledAppsList(showSystem: Boolean): List<RealAppItem> {
        return try {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val filtered = if (showSystem) {
                installed
            } else {
                installed.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            }
            filtered.map { info ->
                RealAppItem(
                    packageName = info.packageName,
                    appName = pm.getApplicationLabel(info).toString(),
                    isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = try { pm.getApplicationIcon(info) } catch (e: Exception) { null }
                )
            }.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class RealAppDetailInfo(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val isSystemApp: Boolean,
        val icon: android.graphics.drawable.Drawable?,
        val appSizeFormatted: String,
        val dataSizeFormatted: String,
        val cacheSizeFormatted: String,
        val totalSizeFormatted: String,
        val appSizeBytes: Long = 0L,
        val dataSizeBytes: Long = 0L,
        val cacheSizeBytes: Long = 0L,
        val totalSizeBytes: Long = 0L,
        val targetSdk: Int,
        val minSdk: Int,
        val installTimeFormatted: String,
        val lastUpdateTimeFormatted: String,
        val isNotificationsEnabled: Boolean,
        val sourceDir: String,
        val uid: Int
    )

    fun getAppDetailInfo(pkg: String): RealAppDetailInfo? {
        val pm = context.packageManager
        return try {
            val packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS)
            val appInfo = packageInfo.applicationInfo ?: return null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            var appBytes = 0L
            var dataBytes = 0L
            var cacheBytes = 0L

            try {
                val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? android.app.usage.StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
                if (statsManager != null && storageManager != null) {
                    val uuid = storageManager.getUuidForPath(java.io.File(appInfo.sourceDir))
                    val stats = statsManager.queryStatsForPackage(uuid, pkg, android.os.Process.myUserHandle())
                    appBytes = stats.appBytes
                    dataBytes = stats.dataBytes
                    cacheBytes = stats.cacheBytes
                }
            } catch (e: Exception) {
                try {
                    val file = java.io.File(appInfo.sourceDir)
                    if (file.exists()) appBytes = file.length()
                } catch (ex: Exception) { /* ignore */ }
            }

            val totalBytes = appBytes + dataBytes + cacheBytes

            val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("ru"))
            val installTime = dateFormat.format(java.util.Date(packageInfo.firstInstallTime))
            val updateTime = dateFormat.format(java.util.Date(packageInfo.lastUpdateTime))

            val notifs = isAppNotificationsAllowed(pkg)

            val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val minSdk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else 21

            RealAppDetailInfo(
                packageName = pkg,
                appName = pm.getApplicationLabel(appInfo).toString(),
                versionName = packageInfo.versionName ?: "1.0",
                versionCode = vCode,
                isSystemApp = isSystem,
                icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                appSizeFormatted = android.text.format.Formatter.formatFileSize(context, appBytes),
                dataSizeFormatted = android.text.format.Formatter.formatFileSize(context, dataBytes),
                cacheSizeFormatted = android.text.format.Formatter.formatFileSize(context, cacheBytes),
                totalSizeFormatted = android.text.format.Formatter.formatFileSize(context, totalBytes),
                appSizeBytes = appBytes,
                dataSizeBytes = dataBytes,
                cacheSizeBytes = cacheBytes,
                totalSizeBytes = totalBytes,
                targetSdk = appInfo.targetSdkVersion,
                minSdk = minSdk,
                installTimeFormatted = installTime,
                lastUpdateTimeFormatted = updateTime,
                isNotificationsEnabled = notifs,
                sourceDir = appInfo.sourceDir ?: "",
                uid = appInfo.uid
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun executeRootCommand(cmd: String): Boolean {
        val suPaths = listOf("su", "/system_ext/bin/su", "/product/bin/su", "/system/bin/su", "/system/xbin/su", "/data/adb/magisk/magisk", "/data/adb/ksu/bin/su")
        for (su in suPaths) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", cmd))
                val exited = proc.waitFor()
                if (exited == 0) return true
            } catch (e: Exception) { /* try next path */ }
        }
        return false
    }

    fun forceStopPackage(pkg: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val method = am?.javaClass?.getMethod("forceStopPackage", String::class.java)
            method?.invoke(am, pkg)
            true
        } catch (e: Exception) {
            executeRootCommand("am force-stop $pkg")
        }
    }

    fun clearPackageData(pkg: String): Boolean {
        if (executeRootCommand("pm clear $pkg")) return true
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val clearMethod = am?.javaClass?.getMethod("clearApplicationUserData", String::class.java, Class.forName("android.content.pm.IPackageDataObserver"))
            clearMethod?.invoke(am, pkg, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearPackageCache(pkg: String): Boolean {
        val cmd = "rm -rf /data/data/$pkg/cache/* /data/data/$pkg/code_cache/* /data/user_de/0/$pkg/cache/* /sdcard/Android/data/$pkg/cache/*"
        if (executeRootCommand(cmd)) return true
        return try {
            val pm = context.packageManager
            val method = pm.javaClass.getMethod("deleteApplicationCacheFiles", String::class.java, Class.forName("android.content.pm.IPackageDataObserver"))
            method.invoke(pm, pkg, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun uninstallPackage(pkg: String): Boolean {
        // Пытаемся через Root мгновенно удалить
        if (executeRootCommand("pm uninstall $pkg")) return true

        // Системный интент если root не сработал
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchPackage(pkg: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) { false }
    }

    fun uninstallPackageIntent(pkg: String): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:$pkg")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // ========================================
    // MONET / MATERIAL YOU СИСТЕМНЫЕ ЦВЕТА
    // ========================================

    fun isDynamicColorEnabled(): Boolean {
        return try {
            val prefs = context.getSharedPreferences("triangle_settings_prefs", Context.MODE_PRIVATE)
            prefs.getBoolean("enable_monet_theme", true)
        } catch (e: Exception) { true }
    }

    fun setDynamicColorEnabled(enable: Boolean) {
        try {
            val prefs = context.getSharedPreferences("triangle_settings_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("enable_monet_theme", enable).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getMonetColorPalette(): String {
        return try {
            val raw = Settings.Secure.getString(context.contentResolver, "theme_customization_overlay_packages") ?: ""
            raw
        } catch (e: Exception) { "" }
    }

    fun setMonetPalette(colorHex: String, style: String = "TONAL_SPOT"): Boolean {
        val json = "{\"android.theme.customization.system_palette\":\"" + colorHex + "\",\"android.theme.customization.color_source\":\"preset\",\"android.theme.customization.theme_style\":\"" + style + "\"}"
        val settingsOk = try {
            Settings.Secure.putString(context.contentResolver, "theme_customization_overlay_packages", json)
            true
        } catch (e: Exception) { false }
        if (!settingsOk) {
            val escaped = json.replace("\"", "\\\"")
            executeRootCommand("settings put secure theme_customization_overlay_packages \"$escaped\"")
        }
        return true
    }

    data class AppPermissionItem(
        val permission: String,
        val label: String,
        val isGranted: Boolean,
        val group: String
    )

    fun getAppPermissions(pkg: String): List<AppPermissionItem> {
        val result = mutableListOf<AppPermissionItem>()
        val pm = context.packageManager
        try {
            val pkgInfo = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
            val requested = pkgInfo.requestedPermissions ?: return emptyList()
            val flags = pkgInfo.requestedPermissionsFlags ?: IntArray(requested.size)

            val importantPermissions = mapOf(
                "android.permission.CAMERA" to ("Камера" to "Аппаратные датчики"),
                "android.permission.RECORD_AUDIO" to ("Микрофон" to "Аппаратные датчики"),
                "android.permission.ACCESS_FINE_LOCATION" to ("Точное местоположение" to "Местоположение"),
                "android.permission.ACCESS_COARSE_LOCATION" to ("Приблизительное местоположение" to "Местоположение"),
                "android.permission.POST_NOTIFICATIONS" to ("Уведомления" to "Уведомления"),
                "android.permission.READ_CONTACTS" to ("Контакты" to "Персональные данные"),
                "android.permission.WRITE_CONTACTS" to ("Изменение контактов" to "Персональные данные"),
                "android.permission.READ_CALL_LOG" to ("Журнал вызовов" to "Персональные данные"),
                "android.permission.READ_PHONE_STATE" to ("Телефон и звонки" to "Телефон"),
                "android.permission.CALL_PHONE" to ("Прямой вызов" to "Телефон"),
                "android.permission.READ_CALENDAR" to ("Календарь" to "Персональные данные"),
                "android.permission.WRITE_CALENDAR" to ("Изменение календаря" to "Персональные данные"),
                "android.permission.READ_MEDIA_IMAGES" to ("Фото и изображения" to "Хранилище"),
                "android.permission.READ_MEDIA_VIDEO" to ("Видеозаписи" to "Хранилище"),
                "android.permission.READ_MEDIA_AUDIO" to ("Музыка и аудио" to "Хранилище"),
                "android.permission.READ_EXTERNAL_STORAGE" to ("Чтение файлов хранилища" to "Хранилище"),
                "android.permission.WRITE_EXTERNAL_STORAGE" to ("Запись в хранилище" to "Хранилище"),
                "android.permission.BLUETOOTH_CONNECT" to ("Подключение к устройствам Bluetooth" to "Устройства рядом"),
                "android.permission.BLUETOOTH_SCAN" to ("Поиск устройств Bluetooth" to "Устройства рядом"),
                "android.permission.NEARBY_WIFI_DEVICES" to ("Устройства Wi-Fi рядом" to "Устройства рядом"),
                "android.permission.BODY_SENSORS" to ("Нательные датчики" to "Аппаратные датчики"),
                "android.permission.REQUEST_INSTALL_PACKAGES" to ("Установка неизвестных приложений" to "Специальный доступ"),
                "android.permission.SYSTEM_ALERT_WINDOW" to ("Поверх других окон" to "Специальный доступ"),
                "android.permission.MANAGE_EXTERNAL_STORAGE" to ("Доступ ко всем файлам" to "Специальный доступ")
            )

            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            val uid = try { pm.getPackageUid(pkg, 0) } catch (e: Exception) { -1 }

            for (i in requested.indices) {
                val p = requested[i]
                if (importantPermissions.containsKey(p)) {
                    val (label, group) = importantPermissions[p]!!
                    val isGranted = when (p) {
                        "android.permission.REQUEST_INSTALL_PACKAGES" -> {
                            if (appOpsManager != null && uid != -1) {
                                appOpsManager.unsafeCheckOpNoThrow("android:request_install_packages", uid, pkg) == android.app.AppOpsManager.MODE_ALLOWED
                            } else {
                                (flags[i] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                            }
                        }
                        "android.permission.SYSTEM_ALERT_WINDOW" -> {
                            Settings.canDrawOverlays(context)
                        }
                        "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                            android.os.Environment.isExternalStorageManager()
                        }
                        else -> {
                            (flags[i] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                        }
                    }
                    result.add(AppPermissionItem(permission = p, label = label, isGranted = isGranted, group = group))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return result.distinctBy { it.permission }
    }

    fun setAppPermission(pkg: String, perm: String, grant: Boolean): Boolean {
        // Специальная обработка AppOp разрешений
        when (perm) {
            "android.permission.REQUEST_INSTALL_PACKAGES" -> {
                val op = if (grant) "allow" else "deny"
                if (executeRootCommand("appops set $pkg REQUEST_INSTALL_PACKAGES $op")) return true
            }
            "android.permission.SYSTEM_ALERT_WINDOW" -> {
                val op = if (grant) "allow" else "deny"
                if (executeRootCommand("appops set $pkg SYSTEM_ALERT_WINDOW $op")) return true
            }
            "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                val op = if (grant) "allow" else "deny"
                if (executeRootCommand("appops set $pkg MANAGE_EXTERNAL_STORAGE $op")) return true
            }
        }

        val cmd = if (grant) "pm grant $pkg $perm" else "pm revoke $pkg $perm"
        if (executeRootCommand(cmd)) return true

        return try {
            val pm = context.packageManager
            val method = if (grant) {
                pm.javaClass.getMethod("grantRuntimePermission", String::class.java, String::class.java, android.os.UserHandle::class.java)
            } else {
                pm.javaClass.getMethod("revokeRuntimePermission", String::class.java, String::class.java, android.os.UserHandle::class.java)
            }
            method.invoke(pm, pkg, perm, android.os.Process.myUserHandle())
            true
        } catch (e: Exception) { false }
    }

    data class AppNotificationChannelItem(
        val id: String,
        val name: String,
        val importance: Int,
        val isEnabled: Boolean
    )

    fun getAppNotificationChannels(pkg: String): List<AppNotificationChannelItem> {
        val result = mutableListOf<AppNotificationChannelItem>()
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val uid = context.packageManager.getPackageUid(pkg, 0)
            val method = nm?.javaClass?.getMethod("getNotificationChannelsForPackage", String::class.java, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            val list = method?.invoke(nm, pkg, uid, false) as? List<*>
            if (list != null) {
                for (item in list) {
                    if (item is android.app.NotificationChannel) {
                        result.add(
                            AppNotificationChannelItem(
                                id = item.id,
                                name = item.name?.toString() ?: item.id,
                                importance = item.importance,
                                isEnabled = item.importance != android.app.NotificationManager.IMPORTANCE_NONE
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) { /* reflection or su fallback */ }
        return result
    }

    fun setAppNotificationsEnabled(pkg: String, enable: Boolean): Boolean {
        val targetMode = if (enable) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_IGNORED
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val uid = context.packageManager.getPackageUid(pkg, 0)
            // setMode(String, int, String, int) доступен только системным
            // приложениям (MANAGE_APP_OPS_MODES) и скрыт из публичного SDK.
            val setMode = AppOpsManager::class.java.getMethod(
                "setMode",
                String::class.java,
                Int::class.javaPrimitiveType,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            setMode.invoke(appOps, OP_POST_NOTIFICATION, uid, pkg, targetMode)
            isAppNotificationsAllowed(pkg) == enable
        } catch (e: Exception) {
            android.util.Log.w("RealSystemManager", "cannot change notifications for $pkg", e)
            false
        }
    }

    fun getDefaultAppsSummary(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pm = context.packageManager
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://google.com"))
            val browserResolve = pm.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            result["Браузер"] = browserResolve?.loadLabel(pm)?.toString() ?: "Не выбран"
        } catch (e: Exception) { result["Браузер"] = "Не выбран" }

        try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val dialResolve = pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
            result["Телефон"] = dialResolve?.loadLabel(pm)?.toString() ?: "Телефон (AOSP)"
        } catch (e: Exception) { result["Телефон"] = "Телефон (AOSP)" }

        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
            val smsResolve = pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            result["SMS-сообщения"] = smsResolve?.loadLabel(pm)?.toString() ?: "Сообщения (AOSP)"
        } catch (e: Exception) { result["SMS-сообщения"] = "Сообщения (AOSP)" }

        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val homeResolve = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            result["Главный экран"] = homeResolve?.loadLabel(pm)?.toString() ?: "Triangle Launcher"
        } catch (e: Exception) { result["Главный экран"] = "Triangle Launcher" }

        return result
    }

    data class DefaultAppCandidate(val label: String, val packageName: String)

    fun getCandidatesForDefaultRole(role: String): List<DefaultAppCandidate> {
        val list = mutableListOf<DefaultAppCandidate>()
        val pm = context.packageManager
        try {
            val intent = when (role) {
                "Браузер" -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://google.com"))
                "Телефон" -> Intent(Intent.ACTION_DIAL)
                "SMS-сообщения" -> Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
                "Главный экран" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                else -> null
            }
            if (intent != null) {
                val resolves = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                for (r in resolves) {
                    val label = r.loadLabel(pm).toString()
                    val pkg = r.activityInfo.packageName
                    if (list.none { it.packageName == pkg }) {
                        list.add(DefaultAppCandidate(label, pkg))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    fun getInstalledAppsCount(): Int {
        return try { context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).size } catch (e: Exception) { 48 }
    }

    // ========================================
    // ХРАНИЛИЩЕ И ДАННЫЕ О СИСТЕМЕ
    // ========================================

    data class StorageDetailedBreakdown(
        val totalBytes: Long,
        val freeBytes: Long,
        val usedBytes: Long,
        val totalFormatted: String,
        val usedFormatted: String,
        val freeFormatted: String,
        val usedPercent: Int,
        val systemBytes: Long,
        val systemFormatted: String,
        val appsBytes: Long,
        val appsFormatted: String,
        val mediaBytes: Long,
        val mediaFormatted: String,
        val downloadsBytes: Long,
        val downloadsFormatted: String,
        val otherBytes: Long,
        val otherFormatted: String,
        val hasMediaAccess: Boolean = false
    )

    /**
     * Честная разбивка: занятое место раскладывается только на то, что
     * реально можно измерить без выдуманных долей.
     *
     * - «Приложения» — сумма размеров APK и split-APK всех пакетов.
     * - «Медиа» и «Загрузки» — агрегаты MediaStore; без доступа ко всем
     *   файлам они не считаются, и UI предлагает выдать доступ.
     * - «Система и прочее» — остаток занятого места: данные приложений,
     *   кэш, системные файлы. Это разность, а не оценка «50/30/10».
     */
    fun getDetailedStorageBreakdown(): StorageDetailedBreakdown {
        return try {
            val dataPath = Environment.getDataDirectory()
            val stat = StatFs(dataPath.path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val used = (total - free).coerceAtLeast(0L)
            val pct = if (total > 0) ((used * 100) / total).toInt() else 0

            val appsSize = getInstalledApkBytes()
            val mediaAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                Environment.isExternalStorageManager()

            var mediaSize = 0L
            var downloadsSize = 0L
            if (mediaAccess) {
                mediaSize = sumOfMediaSizes()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadsSize = sumMediaSize(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                }
            }

            val known = (appsSize + mediaSize + downloadsSize).coerceAtMost(used)
            val systemSize = (used - known).coerceAtLeast(0L)

            StorageDetailedBreakdown(
                totalBytes = total,
                freeBytes = free,
                usedBytes = used,
                totalFormatted = Formatter.formatFileSize(context, total),
                usedFormatted = Formatter.formatFileSize(context, used),
                freeFormatted = Formatter.formatFileSize(context, free),
                usedPercent = pct,
                systemBytes = systemSize,
                systemFormatted = Formatter.formatFileSize(context, systemSize),
                appsBytes = appsSize,
                appsFormatted = Formatter.formatFileSize(context, appsSize),
                mediaBytes = mediaSize,
                mediaFormatted = Formatter.formatFileSize(context, mediaSize),
                downloadsBytes = downloadsSize,
                downloadsFormatted = Formatter.formatFileSize(context, downloadsSize),
                otherBytes = 0L,
                otherFormatted = Formatter.formatFileSize(context, 0L),
                hasMediaAccess = mediaAccess
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Нули означают «нет данных», а не выдуманные 128 ГБ.
            StorageDetailedBreakdown(
                totalBytes = 0L,
                freeBytes = 0L,
                usedBytes = 0L,
                totalFormatted = "—",
                usedFormatted = "—",
                freeFormatted = "—",
                usedPercent = 0,
                systemBytes = 0L,
                systemFormatted = "—",
                appsBytes = 0L,
                appsFormatted = "—",
                mediaBytes = 0L,
                mediaFormatted = "—",
                downloadsBytes = 0L,
                downloadsFormatted = "—",
                otherBytes = 0L,
                otherFormatted = "—",
                hasMediaAccess = false
            )
        }
    }

    /** Суммарный размер APK и split-APK всех установленных пакетов. */
    private fun getInstalledApkBytes(): Long {
        var total = 0L
        try {
            val pm = context.packageManager
            for (pkg in pm.getInstalledPackages(0)) {
                val ai = pkg.applicationInfo ?: continue
                total += fileLength(ai.sourceDir)
                ai.splitSourceDirs?.forEach { total += fileLength(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return total
    }

    private fun fileLength(path: String?): Long = try {
        if (path.isNullOrBlank()) 0L else java.io.File(path).length().coerceAtLeast(0L)
    } catch (e: Exception) {
        0L
    }

    private fun sumOfMediaSizes(): Long {
        val images = sumMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val video = sumMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val audio = sumMediaSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        return images + video + audio
    }

    private fun sumMediaSize(uri: android.net.Uri): Long {
        return try {
            val projection = arrayOf("SUM(${MediaStore.MediaColumns.SIZE})")
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getRealStorageSummary(): String {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - freeBytes
            val percentUsed = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
            val usedFmt = Formatter.formatFileSize(context, usedBytes)
            val totalFmt = Formatter.formatFileSize(context, totalBytes)
            "Занято $percentUsed% · $usedFmt из $totalFmt"
        } catch (e: Exception) { "128 GB" }
    }

    fun getRealStorageDetails(): Triple<String, String, Int> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - freeBytes
            val percentUsed = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
            val usedFmt = Formatter.formatFileSize(context, usedBytes)
            val totalFmt = Formatter.formatFileSize(context, totalBytes)
            Triple(usedFmt, totalFmt, percentUsed)
        } catch (e: Exception) {
            Triple("50 GB", "128 GB", 39)
        }
    }

    fun getRealDeviceModel(): String = Build.MODEL
    fun getRealManufacturer(): String = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    fun getRealAndroidVersion(): String = Build.VERSION.RELEASE
    fun getRealBuildNumber(): String = Build.DISPLAY
    fun getRealHardware(): String = Build.HARDWARE
    fun getSecurityPatch(): String = Build.VERSION.SECURITY_PATCH
    fun getKernelVersion(): String = System.getProperty("os.version") ?: "6.1-android15"

    /** Режим SELinux из ядра: /sys/fs/selinux/enforce, fallback на ro.boot.selinux. */
    fun getSelinuxMode(): String {
        return try {
            val enforceFile = java.io.File("/sys/fs/selinux/enforce")
            if (enforceFile.exists() && enforceFile.canRead()) {
                when (enforceFile.readText().trim()) {
                    "1" -> "Enforcing"
                    "0" -> "Permissive"
                    else -> getSelinuxFromProp()
                }
            } else {
                getSelinuxFromProp()
            }
        } catch (e: Exception) {
            getSelinuxFromProp()
        }
    }

    private fun getSelinuxFromProp(): String {
        return when (getSystemProp("ro.boot.selinux", "").lowercase()) {
            "enforcing" -> "Enforcing"
            "permissive" -> "Permissive"
            "disabled" -> "Disabled"
            else -> "Неизвестно"
        }
    }

    fun getSupportedAbis(): String {
        return try {
            Build.SUPPORTED_ABIS.joinToString(", ")
        } catch (e: Exception) {
            "Неизвестно"
        }
    }

    fun isTrebleEnabled(): Boolean {
        return getSystemProp("ro.treble.enabled", "false").equals("true", ignoreCase = true)
    }

    fun getAppVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /** true, если прошивка подписана release-ключами (официальная сборка), а не test-keys. */
    fun isOfficialBuild(): Boolean = Build.TAGS?.contains("release-keys") == true

    // ========================================
    // БЕЗОПАСНОСТЬ И КОНФИДЕНЦИАЛЬНОСТЬ
    // ========================================

    enum class ScreenLockType(val title: String) {
        NONE("Нет"),
        PATTERN("Графический ключ"),
        PIN("PIN-код"),
        PASSWORD("Пароль")
    }

    fun getScreenLockType(): ScreenLockType {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("dumpsys", "lock_settings"))
            val reader = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
            var credType = ""
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("CredentialType:")) {
                    credType = trimmed.substringAfter("CredentialType:").trim()
                    break
                }
                line = reader.readLine()
            }
            p.waitFor()
            when (credType.uppercase()) {
                "PIN" -> ScreenLockType.PIN
                "PATTERN" -> ScreenLockType.PATTERN
                "PASSWORD" -> ScreenLockType.PASSWORD
                else -> {
                    if (keyguardManager?.isDeviceSecure == true) ScreenLockType.PIN else ScreenLockType.NONE
                }
            }
        } catch (e: Exception) {
            if (keyguardManager?.isDeviceSecure == true) ScreenLockType.PIN else ScreenLockType.NONE
        }
    }

    fun verifyCurrentCredential(oldCredential: String): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("cmd", "lock_settings", "verify", "--old", oldCredential))
            val completed = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (!completed) {
                p.destroyForcibly()
                return false
            }
            val text = p.inputStream.bufferedReader().readText()
            p.exitValue() == 0 || text.contains("verified successfully")
        } catch (e: Exception) { false }
    }

    fun setScreenLock(
        newType: ScreenLockType,
        newCredential: String? = null,
        oldCredential: String? = null
    ): Pair<Boolean, String> {
        return try {
            val cmdList = mutableListOf("cmd", "lock_settings")
            when (newType) {
                ScreenLockType.NONE -> {
                    cmdList.add("clear")
                    if (!oldCredential.isNullOrEmpty()) {
                        cmdList.add("--old")
                        cmdList.add(oldCredential)
                    }
                }
                ScreenLockType.PIN -> {
                    cmdList.add("set-pin")
                    if (!oldCredential.isNullOrEmpty()) {
                        cmdList.add("--old")
                        cmdList.add(oldCredential)
                    }
                    cmdList.add(newCredential ?: "")
                }
                ScreenLockType.PATTERN -> {
                    cmdList.add("set-pattern")
                    if (!oldCredential.isNullOrEmpty()) {
                        cmdList.add("--old")
                        cmdList.add(oldCredential)
                    }
                    cmdList.add(newCredential ?: "")
                }
                ScreenLockType.PASSWORD -> {
                    cmdList.add("set-password")
                    if (!oldCredential.isNullOrEmpty()) {
                        cmdList.add("--old")
                        cmdList.add(oldCredential)
                    }
                    cmdList.add(newCredential ?: "")
                }
            }

            val p = Runtime.getRuntime().exec(cmdList.toTypedArray())
            val completed = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (!completed) {
                p.destroyForcibly()
                return Pair(false, "Таймаут выполнения команды LockSettings")
            }

            val output = p.inputStream.bufferedReader().readText().trim()
            val error = p.errorStream.bufferedReader().readText().trim()
            val exit = p.exitValue()

            if (exit == 0) {
                Pair(true, output.ifEmpty { "Успешно применено" })
            } else {
                val msg = if (output.isNotEmpty()) output else error
                Pair(false, msg.ifEmpty { "Ошибка применения блокировки" })
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Ошибка вызова сервиса")
        }
    }

    fun isDeviceSecure(): Boolean {
        return try {
            keyguardManager?.isDeviceSecure == true
        } catch (e: Exception) { false }
    }

    fun isKeyguardLocked(): Boolean {
        return try {
            keyguardManager?.isKeyguardLocked == true
        } catch (e: Exception) { false }
    }

    fun getScreenLockSummary(): String {
        return try {
            when (getScreenLockType()) {
                ScreenLockType.NONE -> "Нет"
                ScreenLockType.PATTERN -> "Графический ключ"
                ScreenLockType.PIN -> "PIN-код"
                ScreenLockType.PASSWORD -> "Пароль"
            }
        } catch (e: Exception) { "Нет" }
    }

    fun isCameraAccessBlocked(): Boolean {
        return try {
            if (sensorPrivacyManager != null) {
                val method = sensorPrivacyManager.javaClass.getMethod("isSensorPrivacyEnabled", Int::class.javaPrimitiveType)
                method.invoke(sensorPrivacyManager, 2) as? Boolean ?: false // 2 = SensorPrivacyManager.Sensors.CAMERA
            } else false
        } catch (e: Exception) { false }
    }

    fun setCameraAccessBlocked(blocked: Boolean) {
        try {
            if (sensorPrivacyManager != null) {
                val method = sensorPrivacyManager.javaClass.getMethod("setSensorPrivacy", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                method.invoke(sensorPrivacyManager, 2, blocked)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isMicrophoneAccessBlocked(): Boolean {
        return try {
            if (sensorPrivacyManager != null) {
                val method = sensorPrivacyManager.javaClass.getMethod("isSensorPrivacyEnabled", Int::class.javaPrimitiveType)
                method.invoke(sensorPrivacyManager, 1) as? Boolean ?: false // 1 = SensorPrivacyManager.Sensors.MICROPHONE
            } else false
        } catch (e: Exception) { false }
    }

    fun setMicrophoneAccessBlocked(blocked: Boolean) {
        try {
            if (sensorPrivacyManager != null) {
                val method = sensorPrivacyManager.javaClass.getMethod("setSensorPrivacy", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                method.invoke(sensorPrivacyManager, 1, blocked)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isShowPasswordsOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.TEXT_SHOW_PASSWORD, 1) == 1
        } catch (e: Exception) { true }
    }

    fun setShowPasswords(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, Settings.System.TEXT_SHOW_PASSWORD, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isAppPinningOn(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "lock_to_app_enabled", 0) == 1
        } catch (e: Exception) { false }
    }

    fun setAppPinning(enable: Boolean) {
        try {
            Settings.System.putInt(context.contentResolver, "lock_to_app_enabled", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isSignatureSpoofingOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "signature_spoofing", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setSignatureSpoofing(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "signature_spoofing", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ========================================
    // ОБНОВЛЕНИЕ СИСТЕМЫ (AOSP Seamless A/B OTA & UpdateEngine)
    // ========================================

    fun isAbUpdateSupported(): Boolean {
        return try {
            val propClass = Class.forName("android.os.SystemProperties")
            val getMethod = propClass.getMethod("get", String::class.java, String::class.java)
            (getMethod.invoke(null, "ro.build.ab_update", "false") as String) == "true"
        } catch (e: Exception) { false }
    }

    fun getActiveSlot(): String {
        return try {
            val propClass = Class.forName("android.os.SystemProperties")
            val getMethod = propClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, "ro.boot.slot_suffix", "_a") as String
        } catch (e: Exception) { "_a" }
    }

    fun isAutoCheckUpdatesOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "triangle_auto_check_updates", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setAutoCheckUpdates(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "triangle_auto_check_updates", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isUpdateOnlyOverWifiOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, "triangle_ota_wifi_only", 1) == 1
        } catch (e: Exception) { true }
    }

    fun setUpdateOnlyOverWifi(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, "triangle_ota_wifi_only", if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun rebootDevice() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.reboot(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========================================
    // РАЗДЕЛ "СИСТЕМА" (Дата, Время, Жесты, Языки)
    // ========================================

    fun isAutoTimeOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) == 1
        } catch (e: Exception) { true }
    }

    fun setAutoTime(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AUTO_TIME, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isAutoTimeZoneOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, 1) == 1
        } catch (e: Exception) { true }
    }

    fun setAutoTimeZone(enable: Boolean) {
        try {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AUTO_TIME_ZONE, if (enable) 1 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun is24HourFormat(): Boolean {
        return try {
            android.text.format.DateFormat.is24HourFormat(context)
        } catch (e: Exception) { true }
    }

    fun set24HourFormat(enable: Boolean) {
        try {
            Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, if (enable) "24" else "12")
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isGestureNavigationOn(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, "navigation_mode", 2) == 2
        } catch (e: Exception) { true }
    }

    fun setGestureNavigation(gestures: Boolean) {
        try {
            Settings.Secure.putInt(context.contentResolver, "navigation_mode", if (gestures) 2 else 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getCurrentSystemLanguage(): String {
        return try {
            val loc = java.util.Locale.getDefault()
            "${loc.displayLanguage.replaceFirstChar { it.uppercase() }} (${loc.displayCountry})"
        } catch (e: Exception) { "Русский (Россия)" }
    }

    fun getCurrentTimeFormatted(): String {
        return try {
            val df = java.text.SimpleDateFormat(if (is24HourFormat()) "HH:mm" else "hh:mm a", java.util.Locale.getDefault())
            df.format(java.util.Date())
        } catch (e: Exception) { "12:00" }
    }

    fun getCurrentDateFormatted(): String {
        return try {
            val df = java.text.SimpleDateFormat("d MMMM yyyy г.", java.util.Locale.getDefault())
            df.format(java.util.Date())
        } catch (e: Exception) { "26 октября 2026 г." }
    }

    fun getCurrentTimeZoneFormatted(): String {
        return try {
            val tz = java.util.TimeZone.getDefault()
            "${tz.id} (GMT${tz.getDisplayName(false, java.util.TimeZone.SHORT)})"
        } catch (e: Exception) { "GMT+05:00" }
    }
}
