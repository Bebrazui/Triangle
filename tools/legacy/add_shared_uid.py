path = r'C:\Triangle\src\TriangleSettings\app\src\main\AndroidManifest.xml'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = '<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
replacement = '<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n    android:sharedUserId="android.uid.system">'

if target in text:
    text = text.replace(target, replacement)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Added android:sharedUserId="android.uid.system"')
else:
    print('Target not found')
