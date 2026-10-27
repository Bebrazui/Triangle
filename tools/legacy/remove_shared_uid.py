path = r'C:\Triangle\src\TriangleSettings\app\src\main\AndroidManifest.xml'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace(' android:sharedUserId="android.uid.system"', '')
text = text.replace('android:sharedUserId="android.uid.system"', '')

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
print('Removed sharedUserId to match ROM!')
