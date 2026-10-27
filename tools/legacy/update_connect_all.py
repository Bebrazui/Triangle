import os

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace connectToWifi and connectToWifiLegacy
old_fun_start = 'fun connectToWifi(ssid: String'
new_impl = '''    fun connectToWifi(ssid: String, password: String? = null): Boolean {
        val cleanSsid = ssid.replace("\"", "")
        val secType = if (password.isNullOrEmpty()) "open" else "wpa2"

        // 1. Privileged framework connection via WifiConfiguration + hidden connect() API (works without root on priv-app)
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
            if (netId != -1) {
                try {
                    val listenerClass = Class.forName("android.net.wifi.WifiManager")
                    val connectMethod = wifiManager?.javaClass?.getMethod("connect", Int::class.javaPrimitiveType, listenerClass)
                    connectMethod?.invoke(wifiManager, netId, null)
                    return true
                } catch (e: Exception) {}

                wifiManager?.disconnect()
                wifiManager?.enableNetwork(netId, true)
                wifiManager?.reconnect()
                return true
            }
        } catch (e: Exception) {}

        // 2. Privileged process execution (cmd wifi connect-network)
        try {
            val cmd = if (password.isNullOrEmpty()) {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType)
            } else {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType, password)
            }
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (proc.exitValue() == 0 || !output.contains("Exception", ignoreCase = true)) {
                return true
            }
        } catch (e: Exception) {}

        // 3. Root bridge fallback (if root is available)
        try {
            val wifiCmd = if (password.isNullOrEmpty()) {
                "cmd wifi connect-network \\"" + cleanSsid + "\\" " + secType
            } else {
                "cmd wifi connect-network \\"" + cleanSsid + "\\" " + secType + " \\"" + password + "\\""
            }
            if (executeRootCommand(wifiCmd)) return true
        } catch (e: Exception) {}

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
print('Optimized connectToWifi for non-root priv-app!')
