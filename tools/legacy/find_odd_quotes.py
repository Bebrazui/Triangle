with open(r'C:\Triangle\src\TriangleSettings\app\src\main\java\org\triangle\settings\system\RealSystemManager.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, l in enumerate(lines):
    # remove escaped quotes
    un = l.replace('\\"', '')
    if un.count('"') % 2 != 0:
        print(f'{i+1}: {repr(l)}')
