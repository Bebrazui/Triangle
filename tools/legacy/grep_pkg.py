with open(r'C:\Triangle\packages.xml', 'r', encoding='utf-8', errors='ignore') as f:
    lines = f.readlines()

for i, l in enumerate(lines):
    if 'com.android.settings' in l:
        print(f'{i+1}: {l.strip()[:150]}')
