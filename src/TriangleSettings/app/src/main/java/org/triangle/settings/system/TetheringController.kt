package org.triangle.settings.system

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Реальное управление тетерингом через скрытый `android.net.TetheringManager`.
 *
 * Класс системный, поэтому берётся рефлексией. Старт асинхронный: результат
 * приходит в `StartTetheringCallback`, ошибки не выдумываются — код ошибки
 * системы превращается в понятное сообщение.
 */
class TetheringController(private val context: Context) {

    companion object {
        const val TYPE_WIFI = 0
        const val TYPE_USB = 1
        const val TYPE_BLUETOOTH = 2

        private const val TAG = "TriangleTether"
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Держим ссылку, чтобы прокси-callback не собрал GC до ответа системы. */
    private var activeCallback: Any? = null

    private val manager: Any? by lazy {
        try {
            val cls = Class.forName("android.net.TetheringManager")
            context.getSystemService(cls)
        } catch (t: Throwable) {
            Log.w(TAG, "TetheringManager недоступен: ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    fun isAvailable(): Boolean = manager != null

    /** Интерфейсы, которые система реально держит в тетеринге. */
    fun tetheredInterfaces(): List<String> {
        val m = manager ?: return emptyList()
        return try {
            val raw = m.javaClass.getMethod("getTetheredIfaces").invoke(m) as? Array<*>
            raw?.filterIsInstance<String>() ?: emptyList()
        } catch (t: Throwable) {
            Log.w(TAG, "getTetheredIfaces: ${t.javaClass.simpleName}: ${t.message}")
            emptyList()
        }
    }

    fun isWifiTetheringOn(): Boolean {
        val ifaces = tetheredInterfaces()
        if (ifaces.any { it.startsWith("swlan") || it.startsWith("ap") || it.matches(Regex("wlan[1-9].*")) }) {
            return true
        }
        // Fallback для платформ, где softap-интерфейс называется иначе.
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val method = wifi.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            (method.invoke(wifi) as? Boolean) == true
        } catch (t: Throwable) {
            false
        }
    }

    fun isUsbTetheringOn(): Boolean =
        tetheredInterfaces().any { it.startsWith("rndis") || it.startsWith("usb") }

    fun isBluetoothTetheringOn(): Boolean =
        tetheredInterfaces().any { it.startsWith("bt-pan") }

    /**
     * Включает/выключает тетеринг. [onResult] вызывается на главном потоке:
     * `null` — успех, иначе текст ошибки от системы.
     */
    fun setTethering(type: Int, enable: Boolean, onResult: (String?) -> Unit) {
        if (enable) startTethering(type, onResult) else stopTethering(type, onResult)
    }

    private fun startTethering(type: Int, onResult: (String?) -> Unit) {
        val m = manager ?: return onResult("Служба тетеринга недоступна")
        try {
            val method = m.javaClass.methods.firstOrNull {
                it.name == "startTethering" && it.parameterTypes.size == 3
            } ?: return onResult("Системный API тетеринга не найден")

            val callbackType = method.parameterTypes[2]
            val proxy = Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType), InvocationHandler { _, call, args ->
                when (call.name) {
                    "onTetheringStarted" -> handler.post { onResult(null) }
                    "onTetheringFailed" -> {
                        val code = (args?.firstOrNull() as? Int) ?: -1
                        Log.w(TAG, "onTetheringFailed type=$type code=$code")
                        handler.post { onResult(describeError(code)) }
                    }
                    else -> Log.d(TAG, "callback ${call.name}")
                }
                null
            })
            activeCallback = proxy
            method.invoke(m, type, context.mainExecutor, proxy)
        } catch (t: Throwable) {
            Log.w(TAG, "startTethering failed: ${t.javaClass.simpleName}: ${t.message}")
            onResult(describeThrowable(t))
        }
    }

    private fun stopTethering(type: Int, onResult: (String?) -> Unit) {
        val m = manager ?: return onResult("Служба тетеринга недоступна")
        try {
            val method = m.javaClass.methods.firstOrNull {
                it.name == "stopTethering" && it.parameterTypes.size == 1
            } ?: return onResult("Системный API тетеринга не найден")
            method.invoke(m, type)
            onResult(null)
        } catch (t: Throwable) {
            Log.w(TAG, "stopTethering failed: ${t.javaClass.simpleName}: ${t.message}")
            onResult(describeThrowable(t))
        }
    }

    private fun describeError(code: Int): String = when (code) {
        1 -> "Оператор не разрешает раздачу интернета"
        2 -> "Не удалось подготовить подключение (provisioning)"
        3 -> "Не удалось включить NAT"
        4 -> "Нет доступа к мобильному интернету"
        5 -> "Ошибка настройки интерфейса"
        6 -> "Не удалось поднять интерфейс тетеринга"
        7 -> "Не удалось включить пересылку пакетов"
        8 -> "Не удалось выключить пересылку пакетов"
        9 -> "Внутренняя ошибка службы тетеринга"
        else -> "Система отклонила тетеринг (код $code)"
    }

    private fun describeThrowable(t: Throwable): String {
        val root = generateSequence(t) { it.cause }.last()
        return when (root) {
            is SecurityException -> "Нет привилегии TETHER_PRIVILEGED у приложения"
            is UnsupportedOperationException -> "Тетеринг не поддерживается этой сборкой"
            else -> "Ошибка тетеринга: ${root.javaClass.simpleName}: ${root.message ?: "без описания"}"
        }
    }
}
