import os

# RescueParty mitigation count killer
p_wd = '/tmp_pkg_watchdog.xml'
os.system('adb -s RF8MC0K6E8E pull /data/system/package-watchdog.xml ' + p_wd)
if os.path.exists(p_wd):
    open(p_wd, 'wb').write(b'')
    os.system('adb -s RF8MC0K6E8E push ' + p_wd + ' /data/system/package-watchdog.xml')

print('Watchdog cleared')
