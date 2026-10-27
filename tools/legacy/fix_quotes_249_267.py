path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

lines[248] = '        val cleanSsid = ssid.replace("\\"", "")\n'
lines[266] = '                    val listenerClass = Class.forName("android.net.wifi.WifiManager" + "$" + "ActionListener")\n'

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Fixed quotes in 249 and 267!')
