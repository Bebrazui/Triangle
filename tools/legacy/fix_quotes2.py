path = r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('replace(""", "")', 'replace("\\\"", "")')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("DONE")
