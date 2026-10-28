#!/system/bin/sh
# Устанавливает TriangleSettings в /system_ext/priv-app через Magisk-модуль.
# Запуск: adb push Settings_signed.apk /data/local/tmp/Settings.apk
#          adb push triangle-settings-privapp.xml /data/local/tmp/
#          adb shell su -c sh /data/local/tmp/deploy_privileged_settings.sh
# После установки нужен перезапуск: adb reboot
set -e

MODULE=/data/adb/modules/triangle_bootanim
APK_DIR="$MODULE/system_ext/priv-app/Settings"
PERM_DIR="$MODULE/system_ext/etc/permissions"

mkdir -p "$APK_DIR" "$PERM_DIR"
cp /data/local/tmp/Settings.apk "$APK_DIR/Settings.apk"
chmod 644 "$APK_DIR/Settings.apk"
cp /data/local/tmp/triangle-settings-privapp.xml "$PERM_DIR/triangle-settings-privapp.xml"
chmod 644 "$PERM_DIR/triangle-settings-privapp.xml"
chown -R 0:0 "$APK_DIR" "$PERM_DIR"

ls -lh "$APK_DIR/Settings.apk" "$PERM_DIR/triangle-settings-privapp.xml"
