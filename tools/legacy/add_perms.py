path = r'C:\Triangle\src\TriangleSettings\app\src\main\AndroidManifest.xml'
with open(path, 'r', encoding='utf-8') as f:
    manifest = f.read()

permissions_to_add = '''    <uses-permission android:name="android.permission.OVERRIDE_WIFI_CONFIG" />
    <uses-permission android:name="android.permission.NETWORK_SETTINGS" />
    <uses-permission android:name="android.permission.NETWORK_SETUP_WIZARD" />
    <uses-permission android:name="android.permission.PEERS_MAC_ADDRESS" />
    <uses-permission android:name="android.permission.LOCAL_MAC_ADDRESS" />
    <uses-permission android:name="android.permission.TETHER_PRIVILEGED" />
    <uses-permission android:name="android.permission.MANAGE_USERS" />
    <uses-permission android:name="android.permission.ACCESS_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.STATUS_BAR" />
'''

if 'android.permission.OVERRIDE_WIFI_CONFIG' not in manifest:
    manifest = manifest.replace('<uses-permission android:name="android.permission.INTERNET" />', permissions_to_add + '    <uses-permission android:name="android.permission.INTERNET" />')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(manifest)
    print('Added privileged permissions to AndroidManifest.xml!')
else:
    print('Permissions already present')
