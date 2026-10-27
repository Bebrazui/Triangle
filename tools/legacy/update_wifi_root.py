import os

path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Let's replace connectToWifi to use su / root / cmd wifi with fallback
q = chr(34)
b = chr(92)
dlr = chr(36)

old_start = 'fun connectToWifi(ssid: String'
new_method = f'''    fun connectToWifi(ssid: String, password: String? = null): Boolean {{
        return try {{
            val cleanSsid = ssid.replace({q}\"{q}, {q}{q})
            val secType = if (password.isNullOrEmpty()) {q}open{q} else {q}wpa2{q}
            val wifiCmd = if (password.isNullOrEmpty()) {{
                {q}cmd wifi connect-network \"{dlr}cleanSsid\" {dlr}secType{q}
            }} else {{
                {q}cmd wifi connect-network \"{dlr}cleanSsid\" {dlr}secType \"{dlr}password\"{q}
            }}

            // 1. First attempt: Root command (guaranteed to execute cmd wifi without SELinux drops)
            val rootOk = executeRootCommand(wifiCmd)
            if (rootOk) return true

            // 2. Second attempt: Direct ProcessBuilder cmd wifi
            val cmd = if (password.isNullOrEmpty()) {{
                arrayOf({q}cmd{q}, {q}-w{q}, {q}wifi{q}, {q}connect-network{q}, cleanSsid, secType)
            }} else {{
                arrayOf({q}cmd{q}, {q}-w{q}, {q}wifi{q}, {q}connect-network{q}, cleanSsid, secType, password)
            }}
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use {{ it.readText() }}
            proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (proc.exitValue() == 0 || !output.contains({q}Exception{q}, ignoreCase = true)) {{
                return true
            }}

            // 3. Fallback: connectToWifiLegacy
            connectToWifiLegacy(cleanSsid, password)
        }} catch (e: Exception) {{
            connectToWifiLegacy(ssid, password)
        }}
    }}'''

lines = text.splitlines(keepends=True)
s_idx = None
e_idx = None
for i, l in enumerate(lines):
    if 'fun connectToWifi(' in l:
        s_idx = i
    if s_idx is not None and 'private fun connectToWifiLegacy(' in l:
        e_idx = i
        break

lines[s_idx:e_idx] = [new_method + '\n\n']
with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Updated connectToWifi with executeRootCommand!')
