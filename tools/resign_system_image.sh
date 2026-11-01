#!/bin/bash
# resign_system_image.sh
# Переподписывает системные APK в system.img стандартным системным ключом AOSP platform.pk8
# Это позволяет TriangleSettings запускаться с android:sharedUserId="android.uid.system" (UID 1000).
set -euo pipefail

SRC_IMG="${SRC_IMG:-/mnt/c/Triangle/output/system_triangle_aosp15.img}"
KEY_PK8="/mnt/c/Triangle/keys/platform.pk8"
KEY_PEM="/mnt/c/Triangle/keys/platform.x509.pem"
PONCES_SHA1="faaf6a5a7032aef89e9559b6ceed266e59b2bd29"

WORK=/mnt/build
IMG="$WORK/system_resign.img"
MNT=/mnt/img_resign

echo "== 1. Подготовка рабочего пространства =="
mkdir -p "$WORK" "$MNT"
cp "$SRC_IMG" "$SRC_IMG.pre_resign.bak"
cp "$SRC_IMG" "$IMG"

echo "== 2. Unshare blocks =="
e2fsck -f -y -E unshare_blocks "$IMG" >/dev/null || true

echo "== 3. Монтирование образа =="
mount -o loop,rw "$IMG" "$MNT"
trap 'umount "$MNT" 2>/dev/null || true' EXIT

echo "== 4. Поиск и переподписание системных APK =="
COUNT=0
TMP_DIR="$WORK/tmp_apk"
mkdir -p "$TMP_DIR"

while IFS= read -r -d '' apk_path; do
    CERTS=$(apksigner verify --print-certs "$apk_path" 2>/dev/null || true)
    if echo "$CERTS" | grep -qi "$PONCES_SHA1"; then
        COUNT=$((COUNT + 1))
        REL_PATH="${apk_path#$MNT/}"
        echo "[$COUNT] Переподписание: $REL_PATH"
        
        # apksigner in-place
        apksigner sign --key "$KEY_PK8" --cert "$KEY_PEM" \
            --v1-signing-enabled true \
            --v2-signing-enabled true \
            --v3-signing-enabled true \
            "$apk_path"
            
        # Удаляем oat / odex / vdex рядом с apk
        APK_DIR=$(dirname "$apk_path")
        if [ -d "$APK_DIR/oat" ]; then
            rm -rf "$APK_DIR/oat"
        fi
    fi
done < <(find "$MNT" -type f -name "*.apk" -print0)

echo "== Всего переподписано: $COUNT пакетов =="

echo "== 5. Обновление SELinux контекстов =="
python3 - <<'PY'
import os
for root, dirs, files in os.walk("/mnt/img_resign"):
    for f in files:
        if f.endswith(".apk"):
            p = os.path.join(root, f)
            try:
                os.setxattr(p, "security.selinux", b"u:object_r:system_file:s0")
            except OSError:
                pass
PY

echo "== 6. Отмонтирование и синхронизация =="
sync
umount "$MNT"
trap - EXIT

echo "== 7. Проверка файловой системы =="
e2fsck -f -y "$IMG" >/dev/null || true

echo "== 8. Перенос готового образа =="
cp "$IMG" "$SRC_IMG"
rm -f "$IMG"
rm -rf "$TMP_DIR"

echo "== Готово! Образ $SRC_IMG переподписан ключом AOSP platform.pk8 =="
