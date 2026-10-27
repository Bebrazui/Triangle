path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Replace triple quoted json with single escaped string
old_str = 'val json = """{"android.theme.customization.system_palette":"","android.theme.customization.color_source":"preset","android.theme.customization.theme_style":""}"""'
new_str = 'val json = "{\\"android.theme.customization.system_palette\\":\\"" + colorHex + "\\",\\"android.theme.customization.color_source\\":\\"preset\\",\\"android.theme.customization.theme_style\\":\\"" + style + "\\"}"'

if old_str in text:
    text = text.replace(old_str, new_str)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Replaced triple quote json!')
else:
    print('old_str not matched')
