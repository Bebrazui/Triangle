import os

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace getRealWifiScanList to explicitly trigger scan and add logging
old_scan_start = 'fun getRealWifiScanList(): List<RealWifiItem>'
new_scan_impl = '''    fun getRealWifiScanList(): List<RealWifiItem> {
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
'''

lines = text.splitlines(keepends=True)
s_idx = None
e_idx = None
for i, l in enumerate(lines):
    if 'fun getRealWifiScanList()' in l:
        s_idx = i
    if s_idx is not None and '@Suppress("DEPRECATION")\n' in l and i > s_idx + 10:
        e_idx = i
        break

lines[s_idx:e_idx] = [new_scan_impl + '\n']
with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Updated getRealWifiScanList with startScan and logs!')
