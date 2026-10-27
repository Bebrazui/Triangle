with open(r'C:\Triangle\packages.xml', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

import re
matches = re.findall(r'<package\s+name="com\.android\.settings"[^>]*>.*?</package>', text, re.DOTALL)
print('Package matches found:', len(matches))
for m in matches:
    print(m[:400])
