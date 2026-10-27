import os

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace connectToWifi to use standard WifiNetworkSuggestion first, then processBuilder cmd wifi
new_impl = '''    fun connectToWifi(ssid: String, password: String? = null): Boolean {
        val cleanSsid = ssid.replace("\"", "")
        val secType = if (password.isNullOrEmpty()) "open" else "wpa2"
        android.util.Log.i("TriangleSettings", "connectToWifi start for: , hasPwd: ")

        // 1. ProcessBuilder cmd wifi connect-network (works across all network configurations)
        try {
            val cmd = if (password.isNullOrEmpty()) {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType)
            } else {
                arrayOf("cmd", "-w", "wifi", "connect-network", cleanSsid, secType, password)
            }
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)
            android.util.Log.i("TriangleSettings", "cmd wifi output: , exitCode: ")
            if (proc.exitValue() == 0 || !output.contains("Exception", ignoreCase = true)) {
                return true
            }
        } catch (e: Exception) {
            android.util.Log.e("TriangleSettings", "cmd wifi error: ")
        }

        // 2. WifiNetworkSuggestion API (standard Android 10-15 API)
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
print('Updated connectToWifi to prioritize cmd wifi clean connect!')
