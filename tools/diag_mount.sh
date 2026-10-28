#!/system/bin/sh
M=/debug_ramdisk/magisk
echo "== sqlite tables =="
$M --sqlite "SELECT name FROM sqlite_master WHERE type='table'"
echo "== modules =="
$M --sqlite "SELECT * FROM modules"
echo "== mountinfo lines =="
wc -l /proc/1/mountinfo
echo "== system mounts from self =="
mount | grep -iE "settings|system_ext|magisk" | head -10
echo "== init namespace =="
$M --sqlite "SELECT * FROM settings"
