package org.triangle.settings.keyremap

import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.accessibilityservice.AccessibilityService

/**
 * Логика распознавания одиночного / двойного / тройного / долгого нажатия
 * и запуска действия.
 *
 * Работает только для клавиш, у которых есть привязка. Клавиши без привязки
 * проходят насквозь — служба не вмешивается в обычное поведение системы.
 */
object KeyRemapEngine {

    private const val TAG = "KeyRemap"
    private const val STALE_STATE_MS = 10_000L

    private data class DiagEntry(
        val at: Long,
        val keyLabel: String,
        val trigger: KeyTrigger?,
        val action: String,
        val ok: Boolean,
        val note: String
    )

    private class KeyState {
        var pressCount = 0
        var downConsumed = false
        var longFired = false
        var firedImmediately = false
        var lastEventAt = 0L
        var longRunnable: Runnable? = null
        var multiRunnable: Runnable? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private val states = HashMap<Long, KeyState>()
    private val diagnostics = ArrayDeque<DiagEntry>()

    private var appContext: Context? = null
    private var powerManager: PowerManager? = null
    private var keyguardManager: KeyguardManager? = null

    @Volatile
    var config: KeyRemapConfig = KeyRemapConfig()
        private set

    /** Колбэк режима «нажмите клавишу, чтобы назначить». */
    @Volatile
    var capture: ((KeyEvent) -> Unit)? = null

    private var captureStartedAt = 0L

    fun attach(service: AccessibilityService, context: Context) {
        appContext = context.applicationContext
        powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        reload(context)
        Log.i(TAG, "engine attached, bindings=${config.bindings.size}")
    }

    fun detach() {
        capture = null
        reset()
        appContext = null
        powerManager = null
        keyguardManager = null
        Log.i(TAG, "engine detached")
    }

    fun reload(context: Context) {
        config = KeyRemapStore.load(context)
        reset()
        Log.i(
            TAG,
            "config reloaded: enabled=${config.enabled} bindings=${config.bindings.size} " +
                "error=${KeyRemapStore.lastLoadError ?: "none"}"
        )
    }

    fun startCapture(onCaptured: (KeyEvent) -> Unit) {
        captureStartedAt = System.currentTimeMillis()
        capture = onCaptured
    }

    fun stopCapture() {
        capture = null
    }

    fun diagnosticsSnapshot(limit: Int = 20): List<String> {
        return diagnostics.take(limit).map { entry ->
            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(entry.at))
            val trigger = entry.trigger?.label ?: "—"
            val status = if (entry.ok) "" else " · ошибка: ${entry.note}"
            "$time  ${entry.keyLabel} · $trigger → ${entry.action}$status"
        }
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        states.values.forEach { state ->
            state.longRunnable = null
            state.multiRunnable = null
        }
        states.clear()
    }

    /**
     * @return true, если событие поглощено и не должно уйти приложению.
     */
    fun handle(event: KeyEvent, service: AccessibilityService?): Boolean {
        val captureCallback = capture
        if (captureCallback != null) {
            if (System.currentTimeMillis() - captureStartedAt > 15_000L) {
                // Зависший режим захвата не должен блокировать клавиши вечно.
                capture = null
            } else {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    capture = null
                    handler.post { captureCallback(event) }
                }
                return true
            }
        }

        if (!config.enabled) return false

        val keyCode = event.keyCode
        val bindings = config.bindingsFor(keyCode)
        if (bindings.isEmpty()) return false

        if (event.isCanceled) {
            resetKey(stateId(event))
            return false
        }

        // Комбинации с Ctrl/Alt/Meta не трогаем: горячие клавиши должны жить.
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false

        // На выключенном экране и на локскрине ничего не перехватываем.
        if (powerManager?.isInteractive == false) return false
        if (keyguardManager?.isKeyguardLocked == true) return false

        purgeStaleStates()

        val id = stateId(event)
        val state = states.getOrPut(id) { KeyState() }

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onDown(id, state, event, bindings, service)
            KeyEvent.ACTION_UP -> onUp(id, state, event, bindings, service)
            else -> false
        }
    }

    private fun onDown(
        id: Long,
        state: KeyState,
        event: KeyEvent,
        bindings: List<KeyBinding>,
        service: AccessibilityService?
    ): Boolean {
        if (event.repeatCount > 0) {
            // Автоповтор удерживаемой клавиши: поглощаем, если уже поглотили нажатие.
            return state.downConsumed
        }

        // Потеряли UP предыдущего нажатия (служба переподключалась) — начинаем заново.
        if (state.downConsumed) {
            resetState(state)
        }

        state.downConsumed = true
        state.pressCount += 1
        state.lastEventAt = System.currentTimeMillis()
        state.longFired = false
        state.firedImmediately = false
        cancelLong(state)
        cancelMulti(state)

        val hasSingle = bindings.any { it.trigger == KeyTrigger.SINGLE }
        val hasDouble = bindings.any { it.trigger == KeyTrigger.DOUBLE }
        val hasTriple = bindings.any { it.trigger == KeyTrigger.TRIPLE }
        val hasLong = bindings.any { it.trigger == KeyTrigger.LONG }

        // Быстрый путь: только одиночное нажатие — срабатываем сразу, без задержки.
        if (state.pressCount == 1 && hasSingle && !hasDouble && !hasTriple && !hasLong) {
            state.firedImmediately = true
            fire(bindings, KeyTrigger.SINGLE, event.keyCode, service)
            return true
        }

        if (state.pressCount == 1) {
            if (hasLong) scheduleLong(state, event.keyCode, bindings, service)
            if (hasDouble || hasTriple) {
                scheduleMulti(state, config.doublePressTimeoutMs.toLong(), event.keyCode, bindings, service)
            }
        } else if (state.pressCount == 2) {
            if (hasTriple) {
                scheduleMulti(state, config.triplePressTimeoutMs.toLong(), event.keyCode, bindings, service)
            } else {
                fire(bindings, KeyTrigger.DOUBLE, event.keyCode, service)
                resetState(state)
            }
        } else {
            fire(bindings, KeyTrigger.TRIPLE, event.keyCode, service)
            resetState(state)
        }

        return true
    }

    private fun onUp(
        id: Long,
        state: KeyState,
        event: KeyEvent,
        bindings: List<KeyBinding>,
        service: AccessibilityService?
    ): Boolean {
        if (!state.downConsumed) return false

        state.lastEventAt = System.currentTimeMillis()
        cancelLong(state)

        if (state.firedImmediately) {
            resetState(state)
            return true
        }

        if (state.longFired) {
            resetState(state)
            return true
        }

        val hasMulti = bindings.any {
            it.trigger == KeyTrigger.DOUBLE || it.trigger == KeyTrigger.TRIPLE
        }

        if (state.pressCount == 1 && !hasMulti) {
            fire(bindings, KeyTrigger.SINGLE, event.keyCode, service)
            resetState(state)
            return true
        }

        // Мультинажатие: ждём истечения окна, таймер уже запущен.
        return true
    }

    private fun scheduleLong(
        state: KeyState,
        keyCode: Int,
        bindings: List<KeyBinding>,
        service: AccessibilityService?
    ) {
        val runnable = Runnable {
            state.longRunnable = null
            if (state.pressCount != 1 || state.firedImmediately) return@Runnable
            state.longFired = true
            cancelMulti(state)
            fire(bindings, KeyTrigger.LONG, keyCode, service)
        }
        state.longRunnable = runnable
        handler.postDelayed(runnable, config.longPressTimeoutMs.toLong())
    }

    private fun scheduleMulti(
        state: KeyState,
        delayMs: Long,
        keyCode: Int,
        bindings: List<KeyBinding>,
        service: AccessibilityService?
    ) {
        val runnable = Runnable {
            state.multiRunnable = null
            if (state.longFired) return@Runnable
            when (state.pressCount) {
                1 -> fire(bindings, KeyTrigger.SINGLE, keyCode, service)
                2 -> fire(bindings, KeyTrigger.DOUBLE, keyCode, service)
                else -> fire(bindings, KeyTrigger.TRIPLE, keyCode, service)
            }
            resetState(state)
        }
        state.multiRunnable = runnable
        handler.postDelayed(runnable, delayMs)
    }

    private fun fire(
        bindings: List<KeyBinding>,
        trigger: KeyTrigger,
        keyCode: Int,
        service: AccessibilityService?
    ) {
        val binding = bindings.firstOrNull { it.trigger == trigger } ?: return
        val keyLabel = KeyCatalog.label(keyCode)
        if (service == null) {
            addDiagnostic(keyLabel, trigger, binding.action.summary(appContext), false, "служба недоступна")
            return
        }
        val result = KeyActionExecutor(service).execute(binding.action)
        when (result) {
            is ActionResult.Ok -> {
                addDiagnostic(keyLabel, trigger, binding.action.summary(appContext), true, "")
                Log.i(TAG, "$keyLabel / $trigger -> ${binding.action.type.id} ok")
            }
            is ActionResult.Error -> {
                addDiagnostic(keyLabel, trigger, binding.action.summary(appContext), false, result.message)
                Log.w(TAG, "$keyLabel / $trigger failed: ${result.message}")
            }
        }
    }

    private fun addDiagnostic(
        keyLabel: String,
        trigger: KeyTrigger?,
        action: String,
        ok: Boolean,
        note: String
    ) {
        diagnostics.addFirst(DiagEntry(System.currentTimeMillis(), keyLabel, trigger, action, ok, note))
        while (diagnostics.size > 100) {
            diagnostics.removeLast()
        }
    }

    private fun resetState(state: KeyState) {
        state.longRunnable?.let { handler.removeCallbacks(it) }
        state.multiRunnable?.let { handler.removeCallbacks(it) }
        state.longRunnable = null
        state.multiRunnable = null
        state.pressCount = 0
        state.downConsumed = false
        state.longFired = false
        state.firedImmediately = false
    }

    private fun resetKey(id: Long) {
        states.remove(id)?.let { resetState(it) }
    }

    private fun purgeStaleStates() {
        if (states.isEmpty()) return
        val now = System.currentTimeMillis()
        val iterator = states.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastEventAt > STALE_STATE_MS) {
                resetState(entry.value)
                iterator.remove()
            }
        }
    }

    private fun cancelLong(state: KeyState) {
        state.longRunnable?.let { handler.removeCallbacks(it) }
        state.longRunnable = null
    }

    private fun cancelMulti(state: KeyState) {
        state.multiRunnable?.let { handler.removeCallbacks(it) }
        state.multiRunnable = null
    }

    private fun stateId(event: KeyEvent): Long {
        val device = event.deviceId.toLong() and 0xFFFFFFFFL
        val code = event.keyCode.toLong() and 0xFFFFFFFFL
        return (device shl 32) or code
    }
}
