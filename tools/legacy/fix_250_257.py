path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

lines[249] = '        val cleanSsid = ssid.replace("\\"", "")\n'
lines[256] = '            val existing = configured?.firstOrNull { it.SSID?.replace("\\"", "") == cleanSsid }\n'

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Fixed quotes in 250 and 257!')
