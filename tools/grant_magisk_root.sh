#!/system/bin/sh
# Выдаёт TriangleSettings root-доступ в Magisk без диалога подтверждения.
#
# Зачем root: часть системных API защищена разрешениями с protectionLevel
# signature (MANAGE_APP_OPS_MODES, MANAGE_NOTIFICATIONS). Наша сборка подписана
# не ключом GSI, поэтому получить их невозможно ни через privapp-allowlist, ни
# подписью. Переключение appops (уведомления приложений) делается командой
# `appops set` от uid 0 — это реальная системная операция, а не заглушка.
#
# Использование:
#   adb push tools/grant_magisk_root.sh /data/local/tmp/
#   adb shell su -c 'sh /data/local/tmp/grant_magisk_root.sh'
#
# Скрипт идемпотентен: повторный запуск обновляет запись для текущего UID.
set -e

MAGISK=/debug_ramdisk/magisk
PKG=com.android.settings

uid=$(pm list packages -U "$PKG" | sed -n 's/.*uid:\([0-9][0-9]*\).*/\1/p' | head -n1)
if [ -z "$uid" ]; then
    echo "не удалось определить UID пакета $PKG" >&2
    exit 1
fi

echo "== $PKG uid=$uid =="
"$MAGISK" --sqlite \
    "INSERT OR REPLACE INTO policies (uid, policy, until, logging, notification) VALUES ($uid, 2, 0, 1, 1)"

echo "== policies =="
"$MAGISK" --sqlite "SELECT * FROM policies"

echo "== проверка su для uid $uid =="
su "$uid" -c 'id' 2>&1 || echo "(su для чужого uid не проверяется — это нормально)"
