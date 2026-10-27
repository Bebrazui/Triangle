import re

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

new_method = '''    @Suppress("DEPRECATION")
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
    }'''

start_marker = 'fun connectToWifi(ssid: String, password: String? = null)'
end_marker = '    // SIM'

start_pos = content.find(start_marker)
if start_pos != -1:
    line_start = content.rfind('@Suppress', 0, start_pos)
    if line_start != -1:
        start_pos = line_start - 4
    end_pos = content.find(end_marker, start_pos)
    if end_pos != -1:
        content = content[:start_pos] + new_method + '\n\n' + content[end_pos:]
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print('SUCCESS')
    else:
        print('END NOT FOUND')
else:
    print('START NOT FOUND')
