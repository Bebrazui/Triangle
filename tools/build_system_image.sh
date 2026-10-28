#!/bin/bash
# Собирает system-образ с TriangleSettings, allowlist и бутанимацией.
# Запуск из WSL от root:
#   wsl -d Ubuntu-24.04 -u root -- bash /mnt/c/Triangle/tools/build_system_image.sh
#
# Образ берётся из output/system_triangle_aosp15.img, рядом сохраняется .bak.
set -euo pipefail

APK="${APK:-/mnt/c/Triangle/custom_apps/Settings_signed.apk}"
PERM="${PERM:-/mnt/c/Triangle/system/permissions/triangle-settings-privapp.xml}"
BOOTANIM="${BOOTANIM:-/mnt/c/Triangle/bootanim/bootanimation.zip}"
OUT="${OUT:-/mnt/c/Triangle/output/system_triangle_aosp15.img}"

WORK=/mnt/build
IMG="$WORK/system_work.img"
MNT=/mnt/img

mkdir -p "$WORK" "$MNT"

echo "== источник =="
cp "$OUT" "$OUT.bak"
cp "$OUT" "$IMG"

echo "== unshare blocks (образ собран с shared_blocks, иначе rw не монтируется) =="
e2fsck -f -y -E unshare_blocks "$IMG" >/dev/null || true

echo "== монтирование =="
mount -o loop,rw "$IMG" "$MNT"
trap 'umount "$MNT" 2>/dev/null || true' EXIT

echo "== замена Settings.apk =="
APK_DIR="$MNT/system/system_ext/priv-app/Settings"
install -m 0644 -o 0 -g 0 "$APK" "$APK_DIR/Settings.apk"
# Старый odex/vdex невалиден для нового APK — убираем, система перекомпилирует.
rm -rf "$APK_DIR/oat"

echo "== allowlist привилегированных разрешений =="
PERM_DIR="$MNT/system/system_ext/etc/permissions"
install -m 0644 -o 0 -g 0 "$PERM" "$PERM_DIR/triangle-settings-privapp.xml"

echo "== бутанимация =="
MEDIA_DIR="$MNT/system/product/media"
mkdir -p "$MEDIA_DIR"
install -m 0644 -o 0 -g 0 "$BOOTANIM" "$MEDIA_DIR/bootanimation.zip"

echo "== SELinux-метки =="
python3 - <<'PY'
import os
paths = [
    "/mnt/img/system/system_ext/priv-app/Settings",
    "/mnt/img/system/system_ext/priv-app/Settings/Settings.apk",
    "/mnt/img/system/system_ext/etc/permissions/triangle-settings-privapp.xml",
    "/mnt/img/system/product/media",
    "/mnt/img/system/product/media/bootanimation.zip",
]
for p in paths:
    os.setxattr(p, "security.selinux", b"u:object_r:system_file:s0\x00", follow_symlinks=False)
print("ok")
PY

sync
umount "$MNT"
trap - EXIT

echo "== проверка ФС =="
e2fsck -fn "$IMG" >/dev/null

echo "== запись результата =="
cp "$IMG" "$OUT"
echo "готово: $OUT"
ls -lh "$OUT"
