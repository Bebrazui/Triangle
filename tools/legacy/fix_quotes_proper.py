path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Line 250: replace("\"\"\"", "")
# Line 253: \"cmd wifi connect-network \\\"\" + cleanSsid + \"\\\" \" + secType
# Line 255: \"cmd wifi connect-network \\\"\" + cleanSsid + \"\\\" \" + secType + \" \\\"\" + password + \"\\\"\"

lines[249] = '            val cleanSsid = ssid.replace("\\"", "")\n'
lines[252] = '                "cmd wifi connect-network \\"" + cleanSsid + "\\" " + secType\n'
lines[254] = '                "cmd wifi connect-network \\"" + cleanSsid + "\\" " + secType + " \\"" + password + "\\""\n'

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Fixed quotes in connectToWifi!')
