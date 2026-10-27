path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

start_idx = None
end_idx = None
for i, l in enumerate(lines):
    if 'fun connectToWifi(' in l:
        start_idx = i
    if start_idx is not None and 'private fun connectToWifiLegacy(' in l:
        for j in range(i, i+30):
            if lines[j].strip() == '}':
                end_idx = j + 1
                break
        break

print('Range:', start_idx, end_idx)
new_lines = '''    fun connectToWifi(ssid: String, password: String? = null): Boolean {
        return try {
            val cleanSsid = ssid.replace("\"", "")
            val secType = if (password.isNullOrEmpty()) "open" else "wpa2"
            val cmd = if (password.isNullOrEmpty()) {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType)
            } else {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType, password)
            }
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)
            if (proc.exitValue() == 0 || !output.contains("Exception", ignoreCase = true)) {
                return true
            }
            connectToWifiLegacy(cleanSsid, password)
        } catch (e: Exception) {
            connectToWifiLegacy(ssid, password)
        }
    }

    @Suppress("DEPRECATION")
    private fun connectToWifiLegacy(ssid: String, password: String?): Boolean {
        return try {
            val cleanSsid = ssid.replace("\"", "")
            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = "\"\""
                if (password.isNullOrEmpty()) {
                    allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                } else {
                    preSharedKey = "\"\""
                }
            }
            val netId = wifiManager?.addNetwork(wifiConfig) ?: -1
            if (netId != -1) {
                try {
                    val connectMethod = wifiManager?.javaClass?.getMethod("connect", Int::class.javaPrimitiveType, Class.forName("android.net.wifi.WifiManager\"))
                    connectMethod?.invoke(wifiManager, netId, null)
                    return true
                } catch (e: Exception) {}

                wifiManager?.disconnect()
                wifiManager?.enableNetwork(netId, true)
                wifiManager?.reconnect()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
'''

lines[start_idx:end_idx] = [new_lines]
with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Updated cleanly!')
