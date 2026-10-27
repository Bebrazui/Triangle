path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

print('Line 249:', repr(lines[248]))
lines[248] = '        val cleanSsid = ssid.replace("\\"", "")\n'
with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Fixed quote on line 249')
