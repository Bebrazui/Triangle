import os

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Let's see how configuredNetworks is queried
new_impl = '''    @Suppress("DEPRECATION")
    fun connectToWifi(ssid: String, password: String? = null): Boolean {
        val cleanSsid = ssid.replace("\"", "")
        val secType = if (password.isNullOrEmpty()) "open" else "wpa2"
        android.util.Log.i("TriangleSettings", "connectToWifi called for SSID: , sec: ")

        // 1. Check if network is ALREADY SAVED in WifiManager configuredNetworks
        try {
            val configured = wifiManager?.configuredNetworks
            val existing = configured?.firstOrNull { it.SSID?.replace("\"", "") == cleanSsid }
            if (existing != null && existing.networkId != -1) {
                android.util.Log.i("TriangleSettings", "Found existing saved networkId:  for ")
                try {
                    val listenerClass = Class.forName("android.net.wifi.WifiManager" + "$" + "ActionListener")
                    val connectMethod = wifiManager?.javaClass?.getMethod("connect", Int::class.javaPrimitiveType, listenerClass)
                    connectMethod?.invoke(wifiManager, existing.networkId, null)
                    android.util.Log.i("TriangleSettings", "Connected to existing networkId via hidden connect()")
                    return true
                } catch (e: Exception) {
                    android.util.Log.w("TriangleSettings", "Existing connect failed: ")
                }
                wifiManager?.disconnect()
                wifiManager?.enableNetwork(existing.networkId, true)
                wifiManager?.reconnect()
                return true
            }
        } catch (e: Exception) {
            android.util.Log.w("TriangleSettings", "configuredNetworks check failed: ")
        }

        // 2. If not saved, add new configuration
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
                    val listenerClass = Class.forName("android.net.wifi.WifiManager" + "$" + "ActionListener")
                    val connectMethod = wifiManager?.javaClass?.getMethod("connect", Int::class.javaPrimitiveType, listenerClass)
                    connectMethod?.invoke(wifiManager, netId, null)
                    return true
                } catch (e: Exception) {}

                wifiManager?.disconnect()
                wifiManager?.enableNetwork(netId, true)
                wifiManager?.reconnect()
                return true
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "addNetwork block error: ")
        }

        // 3. Network Suggestions API with initial autojoin
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

        // 4. ProcessBuilder cmd wifi
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
print('Updated connectToWifi with existing network reuse!')
