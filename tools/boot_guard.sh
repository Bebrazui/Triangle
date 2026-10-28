#!/system/bin/sh
# Triangle boot guard.
#
# Почему это нужно:
#   WMS включает экран и ввод только из WindowManagerService.enableScreenAfterBoot(),
#   а boot завершается из ActivityTaskSupervisor.checkFinishBootingLocked().
#   Эта функция вызывается только когда какая-нибудь activity становится idle.
#   Если на старте не нашлось home-activity (например, роль HOME указывает на
#   отключённый пакет), то ни одна activity не запускается, boot не завершается,
#   WMS остаётся с mSystemBooted=false, InputDispatcher — с DispatchEnabled=false.
#   В результате экран блокировки нарисован, но не реагирует ни на касания, ни на
#   кнопки питания/громкости.
#
# Что делает guard:
#   Если через GUARD_TIMEOUT секунд sys.boot_completed так и не выставлен,
#   запускаем direct-boot-aware activity (dialer). Её запуск порождает событие
#   activity idle, и система сама завершает boot: включает экран и input dispatch.
#
# Скрипт кладётся в /data/adb/service.d/ (Magisk выполняет его от root на late_start).

GUARD_TIMEOUT=${GUARD_TIMEOUT:-45}
LOG=/data/local/tmp/triangle_boot_guard.log

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"
}

i=0
while [ "$i" -lt "$GUARD_TIMEOUT" ]; do
    if [ "$(getprop sys.boot_completed)" = "1" ]; then
        exit 0
    fi
    sleep 1
    i=$((i + 1))
done

# Даём системе ещё пару секунд: иногда boot_completed выставляется с задержкой.
sleep 3
if [ "$(getprop sys.boot_completed)" = "1" ]; then
    exit 0
fi

log "boot not completed after ${GUARD_TIMEOUT}s, forcing activity idle"

# com.android.dialer есть в GSI и помечен directBootAware, поэтому его можно
# запустить даже для заблокированного пользователя.
am start -W -n com.android.dialer/.main.impl.MainActivity --user 0 >> "$LOG" 2>&1

sleep 2
log "after kick: boot_completed=$(getprop sys.boot_completed) user=$(dumpsys user 2>/dev/null | grep -m1 -o 'State: [A-Z_]*')"
