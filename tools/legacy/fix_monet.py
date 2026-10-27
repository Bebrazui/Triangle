path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, l in enumerate(lines):
    if 'fun setMonetPalette(' in l:
        print(f'{i+1}: {repr(l)}')
        print(f'{i+2}: {repr(lines[i+1])}')
        lines[i+1] = '        val json = "{\\"android.theme.customization.system_palette\\":\\"" + colorHex + "\\",\\"android.theme.customization.color_source\\":\\"preset\\",\\"android.theme.customization.theme_style\\":\\"" + style + "\\"}"\n'
        break

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Updated setMonetPalette line!')
