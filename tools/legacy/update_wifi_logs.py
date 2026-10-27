import os

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace connectToWifi with thorough logging and both suggestion + specifier + wifiConfig
new_impl = '''    fun connectToWifi(ssid: String, password: String? = null): Boolean {
        val cleanSsid = ssid.replace("\"", "")
        val secType = if (password.isNullOrEmpty()) "open" else "wpa2"
        android.util.Log.i("TriangleSettings", "connectToWifi called for SSID: , sec: ")

        // 1. WifiConfiguration API with detailed log
        try {
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = "\\"" + cleanSsid + "\\""
                if (password.isNullOrEmpty()) {
                    allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                } else {
                    preSharedKey = "\\"" + password + "\\""
                }
            }
            val netId = wifiManager?.addNetwork(wifiConfig) ?: -1
            android.util.Log.i("TriangleSettings", "addNetwork returned netId: ")
            if (netId != -1) {
                try {
                    val listenerClass = Class.forName("android.net.wifi.WifiManager\\")
                    val connectMethod = wifiManager?.javaClass?.getMethod("connect", Int::class.javaPrimitiveType, listenerClass)
                    connectMethod?.invoke(wifiManager, netId, null)
                    android.util.Log.i("TriangleSettings", "Invoked hidden WifiManager.connect successfully")
                    return true
                } catch (e: Exception) {
                    android.util.Log.w("TriangleSettings", "Hidden connect failed: ")
                }

                val disconnected = wifiManager?.disconnect() == true
                val enabled = wifiManager?.enableNetwork(netId, true) == true
                val reconnected = wifiManager?.reconnect() == true
                android.util.Log.i("TriangleSettings", "Legacy enableNetwork result: disc=, en=, rec=")
                if (enabled) return true
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "WifiConfiguration block error: ", e)
        }

        // 2. Network Suggestions API
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
                android.util.Log.i("TriangleSettings", "addNetworkSuggestions status: ")
                if (status == android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "Suggestion error: ")
        }

        // 3. ProcessBuilder cmd wifi
        try {
            val cmd = if (password.isNullOrEmpty()) {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType)
            } else {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType, password)
            }
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            android.util.Log.i("TriangleSettings", "ProcessBuilder output:  (code: )")
            if (proc.exitValue() == 0 || !output.contains("Exception", ignoreCase = true)) {
                return true
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "ProcessBuilder error: ")
        }

        return false
    }
'''

lines = text.splitlines(keepends=True)
s_idx = None
e_idx = None
for i, l in enumerate(lines):
    if 'fun connectToWifi(' in l:
        s_idx = i
    if s_idx is not None and 'fun getRealSimCards()' in l:
        e_idx = i
        break

lines[s_idx:e_idx] = [new_impl + '\n\n    // SIM\n']
with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Updated connectToWifi with full logging!')
