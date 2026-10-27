path = r'C:\Triangle\src\TriangleSettings\app\src\main\AndroidManifest.xml'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

import re
text = re.sub(r'<manifest\s+xmlns:android="http://schemas.android.com/apk/res/android"[^>]*>', '<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n    android:sharedUserId="android.uid.system">', text)
with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
print('Set android:sharedUserId="android.uid.system"')
