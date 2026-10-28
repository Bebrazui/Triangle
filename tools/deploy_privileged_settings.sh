#!/system/bin/sh
# Deploy TriangleSettings into the Magisk module and clear Magisk bootloop protection.
#
# Использование:
#   adb push Settings_signed.apk /data/local/tmp/Settings_new.apk
#   adb push triangle-settings-privapp.xml /data/local/tmp/
#   adb push deploy_privileged_settings.sh /data/local/tmp/
#   adb shell su -c 'sh /data/local/tmp/deploy_privileged_settings.sh'
#   adb reboot
set -e

SRC=/data/local/tmp/Settings_new.apk
PERM=/data/local/tmp/triangle-settings-privapp.xml
MOD=/data/adb/modules/triangle_bootanim
MAGISK=/debug_ramdisk/magisk

echo "== source =="
md5sum "$SRC"

# Newer Magisk mounts both top-level partition dirs (system_ext/) and the
# classic system/... mirror, so populate both.
for ROOT in "$MOD" "$MOD/system"; do
    APK_DIR="$ROOT/system_ext/priv-app/Settings"
    PERM_DIR="$ROOT/system_ext/etc/permissions"
    mkdir -p "$APK_DIR" "$PERM_DIR"
    cp "$SRC" "$APK_DIR/Settings.apk"
    cp "$PERM" "$PERM_DIR/triangle-settings-privapp.xml"
    chmod 644 "$APK_DIR/Settings.apk" "$PERM_DIR/triangle-settings-privapp.xml"
    chown -R 0:0 "$APK_DIR" "$PERM_DIR"
done

echo "== installed copies =="
md5sum "$MOD/system_ext/priv-app/Settings/Settings.apk" \
       "$MOD/system/system_ext/priv-app/Settings/Settings.apk"

echo "== magisk settings before =="
$MAGISK --sqlite "SELECT * FROM settings"

# Если boot ни разу не завершился, Magisk считает это bootloop и на следующем
# старте не монтирует модули. Тогда наш FallbackHome никогда не появится, boot
# снова не завершится — замкнутый круг. Сбрасываем флаг явно.
$MAGISK --sqlite "UPDATE settings SET value=0 WHERE key='bootloop'"

echo "== magisk settings after =="
$MAGISK --sqlite "SELECT * FROM settings"

echo "== service.d =="
ls -l /data/adb/service.d
