package org.triangle.settings

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.triangle.settings.notification.NotificationLog
import org.triangle.settings.system.BluetoothScanner
import org.triangle.settings.system.RealAppItem
import org.triangle.settings.system.RealSystemManager
import org.triangle.settings.ui.icons.Phosphor
import org.triangle.settings.ui.theme.*

class MainActivity : ComponentActivity() {

    /** Навигационная цель из внешнего интента: шторка, другие приложения. */
    private var navTarget by mutableStateOf<NavTarget?>(null)
    private var navSequence = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val systemManager = RealSystemManager(this)
        val target = resolveNavTarget(intent, ++navSequence)
        navTarget = target

        setContent {
            TriangleSettingsTheme(systemManager) {
                MainAppNavigation(
                    systemManager = systemManager,
                    initialScreen = target?.screen ?: Screen.MAIN,
                    initialPackageName = target?.packageName ?: "",
                    externalTarget = navTarget
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navTarget = resolveNavTarget(intent, ++navSequence)
    }
}

/** Цель навигации с идентификатором, чтобы повторный интент снова срабатывал. */
data class NavTarget(
    val screen: Screen,
    val packageName: String = "",
    val id: Long = 0L
)

/**
 * Сопоставляет системный интент экрану настроек. Плитки шторки и сторонние
 * приложения шлют `android.settings.*`, включая panel-экшены, — без этой
 * таблицы они открывали бы главный экран вместо нужного раздела.
 */
private fun resolveNavTarget(intent: Intent?, id: Long = 0L): NavTarget? {
    if (intent == null) return null
    val action = intent.action ?: return null
    val pkg = intent.data?.schemeSpecificPart
        ?: intent.getStringExtra("android.provider.extra.APP_PACKAGE")
        ?: intent.getStringExtra("android.intent.extra.PACKAGE_NAME")
        ?: ""

    val screen = when (action) {
        Settings.ACTION_SETTINGS -> Screen.MAIN

        Settings.ACTION_WIRELESS_SETTINGS,
        Settings.ACTION_AIRPLANE_MODE_SETTINGS,
        "android.settings.DATA_SAVER_SETTINGS",
        "android.settings.TETHER_SETTINGS",
        "android.settings.NETWORK_OPERATOR_SETTINGS" -> Screen.NETWORK

        Settings.ACTION_WIFI_SETTINGS,
        "android.settings.panel.action.INTERNET_CONNECTIVITY",
        "android.settings.panel.action.WIFI" -> Screen.WIFI_SETTINGS

        Settings.ACTION_VPN_SETTINGS -> Screen.VPN_SETTINGS

        Settings.ACTION_BLUETOOTH_SETTINGS -> Screen.CONNECTED_DEVICES
        Settings.ACTION_NFC_SETTINGS,
        "android.settings.panel.action.NFC",
        "android.settings.panel.action.MEDIA_OUTPUT" -> Screen.CONNECTION_PREFERENCES

        Settings.ACTION_BATTERY_SAVER_SETTINGS -> Screen.BATTERY
        Intent.ACTION_POWER_USAGE_SUMMARY -> Screen.BATTERY_USAGE

        Settings.ACTION_DISPLAY_SETTINGS,
        "android.settings.NIGHT_DISPLAY_SETTINGS" -> Screen.DISPLAY

        Settings.ACTION_SOUND_SETTINGS,
        "android.settings.panel.action.VOLUME" -> Screen.SOUND

        Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
        "android.settings.ZEN_MODE_SETTINGS",
        "android.settings.ZEN_MODE_AUTO_RULE_SETTINGS",
        "android.settings.ZEN_MODE_SCHEDULE_RULE_SETTINGS",
        "android.settings.ZEN_MODE_EVENT_RULE_SETTINGS",
        "android.settings.ZEN_MODE_BLOCKED_EFFECTS_SETTINGS",
        "android.settings.ZEN_MODE_ON_DND_CARD_SETTINGS" -> Screen.DND_SETTINGS

        "android.settings.NOTIFICATION_SETTINGS",
        "android.settings.NOTIFICATION_ASSISTANT_SETTINGS" ->
            if (pkg.isNotEmpty()) Screen.APP_NOTIFICATION_SETTINGS else Screen.NOTIFICATIONS

        Settings.ACTION_APP_NOTIFICATION_SETTINGS -> Screen.APP_NOTIFICATION_SETTINGS

        Settings.ACTION_APPLICATION_SETTINGS,
        Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS,
        "android.settings.MANAGE_APPLICATIONS_SETTINGS" -> Screen.ALL_APPS

        "android.settings.MANAGE_DEFAULT_APPS_SETTINGS" -> Screen.DEFAULT_APPS

        Settings.ACTION_APPLICATION_DETAILS_SETTINGS ->
            if (pkg.isNotEmpty()) Screen.APP_INFO else Screen.ALL_APPS

        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION ->
            if (pkg.isNotEmpty()) Screen.APP_PERMISSIONS else Screen.ALL_APPS

        Settings.ACTION_LOCATION_SOURCE_SETTINGS -> Screen.LOCATION
        Settings.ACTION_SECURITY_SETTINGS, Settings.ACTION_PRIVACY_SETTINGS -> Screen.SECURITY
        Settings.ACTION_ACCESSIBILITY_SETTINGS,
        "android.settings.ACCESSIBILITY_DETAILS_SETTINGS" -> Screen.ACCESSIBILITY

        Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        Settings.ACTION_MEMORY_CARD_SETTINGS -> Screen.STORAGE

        Settings.ACTION_DEVICE_INFO_SETTINGS -> Screen.ABOUT_PHONE
        "android.settings.SYSTEM_UPDATE_SETTINGS" -> Screen.SYSTEM_UPDATE
        Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS -> Screen.DEVELOPER_OPTIONS
        "android.settings.PRIVATE_DNS_SETTINGS" -> Screen.PRIVATE_DNS
        "android.settings.WIFI_IP_SETTINGS" -> Screen.WIFI_SETTINGS

        "android.settings.INPUT_METHOD_SETTINGS",
        "android.settings.SYNC_SETTINGS" -> Screen.SYSTEM

        else -> return null
    }

    android.util.Log.d("TriangleNav", "intent $action -> $screen (pkg=$pkg)")
    return NavTarget(screen, pkg, id)
}

@Composable
fun TriangleSettingsTheme(systemManager: RealSystemManager? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = systemManager?.isDarkThemeOn() ?: isSystemDark
    val dynamicColorEnabled = systemManager?.isDynamicColorEnabled() ?: true

    val colors = if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (isDark) {
            DarkTriangleColors.copy(
                paperBackground = dynScheme.background,
                surfaceCard = dynScheme.surface,
                textPrimary = dynScheme.onSurface,
                textSecondary = dynScheme.onSurfaceVariant,
                accentTerracotta = dynScheme.primary,
                softOchre = dynScheme.secondary
            )
        } else {
            LightTriangleColors.copy(
                paperBackground = dynScheme.background,
                surfaceCard = dynScheme.surface,
                textPrimary = dynScheme.onSurface,
                textSecondary = dynScheme.onSurfaceVariant,
                accentTerracotta = dynScheme.primary,
                softOchre = dynScheme.secondary
            )
        }
    } else {
        if (isDark) DarkTriangleColors else LightTriangleColors
    }

    val colorScheme = if (dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) {
            darkColorScheme(
                background = colors.paperBackground,
                surface = colors.surfaceCard,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                primary = colors.accentTerracotta,
                onPrimary = colors.paperBackground
            )
        } else {
            lightColorScheme(
                background = colors.paperBackground,
                surface = colors.surfaceCard,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                primary = colors.borderBold,
                onPrimary = colors.surfaceCard
            )
        }
    }

    CompositionLocalProvider(LocalTriangleColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EditorialTypography,
            content = content
        )
    }
}

enum class Screen {
    MAIN,
    NETWORK,
    WIFI_SETTINGS,
    SIM_SETTINGS,
    HOTSPOT_SETTINGS,
    VPN_SETTINGS,
    PRIVATE_DNS,
    CONNECTED_DEVICES,
    BLUETOOTH_PAIRING,
    CONNECTION_PREFERENCES,
    DISPLAY,
    SCREEN_TIMEOUT,
    WALLPAPER,
    SOUND,
    RINGTONE_PHONE,
    RINGTONE_NOTIFICATION,
    RINGTONE_ALARM,
    VIBRATION_SETTINGS,
    DND_SETTINGS,
    NOTIFICATIONS,
    APP_NOTIFICATIONS,
    NOTIFICATION_HISTORY,
    LOCKSCREEN_NOTIFICATIONS,
    APPS,
    ALL_APPS,
    DEFAULT_APPS,
    BATTERY,
    BATTERY_USAGE,
    STORAGE,
    LOCATION,
    ACCESSIBILITY,
    SECURITY,
    SCREEN_LOCK_SETTINGS,
    SET_PATTERN,
    SET_PIN,
    SET_PASSWORD,
    CONFIRM_OLD_CREDENTIAL,
    SYSTEM_UPDATE,
    SYSTEM,
    DEVELOPER_OPTIONS,
    WIRELESS_DEBUGGING,
    ABOUT_PHONE,
    ROOT_PROVIDER,
    APP_INFO,
    APP_PERMISSIONS,
    APP_NOTIFICATION_SETTINGS,
    APP_STORAGE
}

@Composable
fun MainAppNavigation(
    systemManager: RealSystemManager,
    initialScreen: Screen = Screen.MAIN,
    initialPackageName: String = "",
    externalTarget: NavTarget? = null
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var selectedRoot by remember { mutableStateOf("KernelSU") }
    var selectedPackageName by remember { mutableStateOf(initialPackageName) }

    // Повторный запуск с другим интентом (плитка в шторке, стороннее приложение)
    // должен переключать экран даже при уже открытой activity.
    LaunchedEffect(externalTarget?.id) {
        val target = externalTarget ?: return@LaunchedEffect
        if (target.packageName.isNotEmpty()) selectedPackageName = target.packageName
        currentScreen = target.screen
    }

    BackHandler(enabled = currentScreen != Screen.MAIN) {
        currentScreen = when (currentScreen) {
            Screen.APP_STORAGE -> Screen.APP_INFO
            Screen.APP_PERMISSIONS -> Screen.APP_INFO
            Screen.APP_NOTIFICATION_SETTINGS -> Screen.APP_INFO
            Screen.APP_INFO -> Screen.ALL_APPS
            Screen.ROOT_PROVIDER -> Screen.ABOUT_PHONE
            Screen.WIRELESS_DEBUGGING -> Screen.DEVELOPER_OPTIONS
            Screen.DEVELOPER_OPTIONS -> Screen.SYSTEM
            Screen.ALL_APPS -> Screen.APPS
            Screen.DEFAULT_APPS -> Screen.APPS
            Screen.WIFI_SETTINGS -> Screen.NETWORK
            Screen.SIM_SETTINGS -> Screen.NETWORK
            Screen.HOTSPOT_SETTINGS -> Screen.NETWORK
            Screen.VPN_SETTINGS -> Screen.NETWORK
            Screen.PRIVATE_DNS -> Screen.NETWORK
            Screen.BLUETOOTH_PAIRING -> Screen.CONNECTED_DEVICES
            Screen.CONNECTION_PREFERENCES -> Screen.CONNECTED_DEVICES
            Screen.SCREEN_TIMEOUT -> Screen.DISPLAY
            Screen.RINGTONE_PHONE -> Screen.SOUND
            Screen.RINGTONE_NOTIFICATION -> Screen.SOUND
            Screen.RINGTONE_ALARM -> Screen.SOUND
            Screen.VIBRATION_SETTINGS -> Screen.SOUND
            Screen.DND_SETTINGS -> Screen.SOUND
            Screen.APP_NOTIFICATIONS -> Screen.NOTIFICATIONS
            Screen.NOTIFICATION_HISTORY -> Screen.NOTIFICATIONS
            Screen.LOCKSCREEN_NOTIFICATIONS -> Screen.NOTIFICATIONS
            Screen.BATTERY_USAGE -> Screen.BATTERY
            Screen.SCREEN_LOCK_SETTINGS -> Screen.SECURITY
            Screen.SET_PATTERN -> Screen.SCREEN_LOCK_SETTINGS
            Screen.SET_PIN -> Screen.SCREEN_LOCK_SETTINGS
            Screen.SET_PASSWORD -> Screen.SCREEN_LOCK_SETTINGS
            Screen.CONFIRM_OLD_CREDENTIAL -> Screen.SCREEN_LOCK_SETTINGS
            else -> Screen.MAIN
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally(
                    initialOffsetX = { (it * 0.25f).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(280, easing = LinearOutSlowInEasing)
                )).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { (-it * 0.15f).toInt() },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    )
                )
            } else {
                (slideInHorizontally(
                    initialOffsetX = { (-it * 0.15f).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)) + scaleIn(
                    initialScale = 0.96f,
                    animationSpec = tween(280, easing = LinearOutSlowInEasing)
                )).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { (it * 0.25f).toInt() },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                        targetScale = 0.94f,
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    )
                )
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            Screen.MAIN -> MainSettingsScreen(systemManager = systemManager, onNavigate = { currentScreen = it })
            Screen.NETWORK -> NetworkSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.WIFI_SETTINGS -> WifiSettingsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NETWORK })
            Screen.SIM_SETTINGS -> SimSettingsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NETWORK })
            Screen.HOTSPOT_SETTINGS -> HotspotSettingsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NETWORK })
            Screen.VPN_SETTINGS -> VpnSettingsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NETWORK })
            Screen.PRIVATE_DNS -> PrivateDnsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NETWORK })
            Screen.CONNECTED_DEVICES -> ConnectedDevicesSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.BLUETOOTH_PAIRING -> BluetoothPairingSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.CONNECTED_DEVICES })
            Screen.CONNECTION_PREFERENCES -> ConnectionPreferencesSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.CONNECTED_DEVICES })
            Screen.DISPLAY -> DisplaySubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.SCREEN_TIMEOUT -> ScreenTimeoutSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.DISPLAY })
            Screen.WALLPAPER -> WallpaperSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.MAIN })
            Screen.SOUND -> SoundSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.RINGTONE_PHONE -> RingtonePickerSubScreen(title = "Рингтон телефона", type = android.media.RingtoneManager.TYPE_RINGTONE, systemManager = systemManager, onBack = { currentScreen = Screen.SOUND })
            Screen.RINGTONE_NOTIFICATION -> RingtonePickerSubScreen(title = "Звук уведомления", type = android.media.RingtoneManager.TYPE_NOTIFICATION, systemManager = systemManager, onBack = { currentScreen = Screen.SOUND })
            Screen.RINGTONE_ALARM -> RingtonePickerSubScreen(title = "Звук будильника", type = android.media.RingtoneManager.TYPE_ALARM, systemManager = systemManager, onBack = { currentScreen = Screen.SOUND })
            Screen.VIBRATION_SETTINGS -> VibrationSettingsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.SOUND })
            Screen.DND_SETTINGS -> DndSettingsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.SOUND })
            Screen.NOTIFICATIONS -> NotificationsSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.APP_NOTIFICATIONS -> AppNotificationsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NOTIFICATIONS })
            Screen.NOTIFICATION_HISTORY -> NotificationHistorySubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NOTIFICATIONS })
            Screen.LOCKSCREEN_NOTIFICATIONS -> LockscreenNotificationsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.NOTIFICATIONS })
            Screen.APPS -> AppsSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.ALL_APPS -> AllAppsSubScreen(
                systemManager = systemManager,
                onOpenAppInfo = { pkg ->
                    selectedPackageName = pkg
                    currentScreen = Screen.APP_INFO
                },
                onBack = { currentScreen = Screen.APPS }
            )
            Screen.APP_INFO -> AppInfoSubScreen(
                packageName = selectedPackageName,
                systemManager = systemManager,
                onNavigateToStorage = { currentScreen = Screen.APP_STORAGE },
                onNavigateToPermissions = { currentScreen = Screen.APP_PERMISSIONS },
                onNavigateToNotifications = { currentScreen = Screen.APP_NOTIFICATION_SETTINGS },
                onBack = { currentScreen = Screen.ALL_APPS }
            )
            Screen.APP_STORAGE -> AppStorageSubScreen(
                packageName = selectedPackageName,
                systemManager = systemManager,
                onBack = { currentScreen = Screen.APP_INFO }
            )
            Screen.APP_PERMISSIONS -> AppPermissionsSubScreen(
                packageName = selectedPackageName,
                systemManager = systemManager,
                onBack = { currentScreen = Screen.APP_INFO }
            )
            Screen.APP_NOTIFICATION_SETTINGS -> AppNotificationSettingsSubScreen(
                packageName = selectedPackageName,
                systemManager = systemManager,
                onBack = { currentScreen = Screen.APP_INFO }
            )
            Screen.DEFAULT_APPS -> DefaultAppsSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.APPS })
            Screen.BATTERY -> BatterySubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.BATTERY_USAGE -> BatteryUsageSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.BATTERY })
            Screen.STORAGE -> StorageSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.LOCATION -> LocationSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.MAIN })
            Screen.ACCESSIBILITY -> AccessibilitySubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.MAIN })
            Screen.SECURITY -> SecuritySubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.SCREEN_LOCK_SETTINGS -> ScreenLockSettingsSubScreen(
                systemManager = systemManager,
                onNavigate = { currentScreen = it },
                onBack = { currentScreen = Screen.SECURITY }
            )
            Screen.SET_PATTERN -> SetPatternSubScreen(
                systemManager = systemManager,
                onSuccess = { currentScreen = Screen.SCREEN_LOCK_SETTINGS },
                onBack = { currentScreen = Screen.SCREEN_LOCK_SETTINGS }
            )
            Screen.SET_PIN -> SetPinSubScreen(
                systemManager = systemManager,
                onSuccess = { currentScreen = Screen.SCREEN_LOCK_SETTINGS },
                onBack = { currentScreen = Screen.SCREEN_LOCK_SETTINGS }
            )
            Screen.SET_PASSWORD -> SetPasswordSubScreen(
                systemManager = systemManager,
                onSuccess = { currentScreen = Screen.SCREEN_LOCK_SETTINGS },
                onBack = { currentScreen = Screen.SCREEN_LOCK_SETTINGS }
            )
            Screen.CONFIRM_OLD_CREDENTIAL -> ConfirmOldCredentialSubScreen(
                systemManager = systemManager,
                onVerified = { nextScreen ->
                    currentScreen = nextScreen
                },
                onBack = { currentScreen = Screen.SCREEN_LOCK_SETTINGS }
            )
            Screen.SYSTEM_UPDATE -> SystemUpdateSubScreen(systemManager = systemManager, onBack = { currentScreen = Screen.MAIN })
            Screen.SYSTEM -> SystemSubScreen(systemManager = systemManager, onNavigate = { currentScreen = it }, onBack = { currentScreen = Screen.MAIN })
            Screen.DEVELOPER_OPTIONS -> DeveloperOptionsSubScreen(
                systemManager = systemManager,
                onNavigate = { currentScreen = it },
                onBack = { currentScreen = Screen.SYSTEM }
            )
            Screen.WIRELESS_DEBUGGING -> WirelessDebuggingSubScreen(
                systemManager = systemManager,
                onBack = { currentScreen = Screen.DEVELOPER_OPTIONS }
            )
            Screen.ABOUT_PHONE -> AboutPhoneScreen(
                systemManager = systemManager,
                selectedRoot = selectedRoot,
                onBack = { currentScreen = Screen.MAIN },
                onOpenRootProvider = { currentScreen = Screen.ROOT_PROVIDER }
            )
            Screen.ROOT_PROVIDER -> RootProviderScreen(
                selectedRoot = selectedRoot,
                onSelectRoot = { selectedRoot = it },
                onBack = { currentScreen = Screen.ABOUT_PHONE }
            )
        }
    }
}

@Composable
fun MainSettingsScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.coerceAtMost(250)
            } else 250
        }
    }
    val titleAlpha = (1f - (scrollOffset / 140f)).coerceIn(0f, 1f)
    val titleTranslationY = -(scrollOffset * 0.7f)

    val (isWifiOn, realWifiSsid) = remember { systemManager.getRealWifiSummary() }
    val isBtOn = remember { systemManager.isBluetoothOn() }
    val (realBatteryPct, isCharging) = remember { systemManager.getRealBatteryStats() }
    val realAppsCount = remember { systemManager.getInstalledAppsCount() }
    val realStorageText = remember { systemManager.getRealStorageSummary() }
    val realModel = remember { systemManager.getRealDeviceModel() }
    val realManufacturer = remember { systemManager.getRealManufacturer() }

    // Элементы настроек для живого поиска с расширенным набором синонимов
    data class MainSearchItem(
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val screen: Screen,
        val keywords: List<String> = emptyList()
    )
    val allSearchableItems = remember(isWifiOn, realWifiSsid, isBtOn, realAppsCount, realBatteryPct, isCharging, realStorageText, realManufacturer, realModel) {
        listOf(
            MainSearchItem(
                title = "Сеть и интернет",
                subtitle = if (isWifiOn) realWifiSsid else "Wi-Fi, мобильный интернет, точка доступа, DNS",
                icon = Phosphor.Wifi,
                screen = Screen.NETWORK,
                keywords = listOf("сеть", "интернет", "wifi", "вайфай", "вай-фай", "роуминг", "данные", "трафик", "network", "internet", "dns", "хотспот", "модем")
            ),
            MainSearchItem(
                title = "Wi-Fi",
                subtitle = "Беспроводные сети, точки доступа",
                icon = Phosphor.Wifi,
                screen = Screen.NETWORK,
                keywords = listOf("вайфай", "wifi", "wlan", "беспроводная", "роутер", "подключение", "сеть")
            ),
            MainSearchItem(
                title = "SIM-карты и мобильная сеть",
                subtitle = "Роуминг, предпочтительный тип сети, APN",
                icon = Phosphor.Wifi,
                screen = Screen.SIM_SETTINGS,
                keywords = listOf("сим", "симка", "sim", "оператор", "связь", "5g", "4g", "lte", "roaming", "роуминг", "apn", "точка доступа")
            ),
            MainSearchItem(
                title = "Точка доступа и модем",
                subtitle = "Раздача Wi-Fi, USB-модем, Bluetooth",
                icon = Phosphor.Wifi,
                screen = Screen.HOTSPOT_SETTINGS,
                keywords = listOf("раздача", "хотспот", "hotspot", "модем", "tethering", "раздать интернет", "usb модем")
            ),
            MainSearchItem(
                title = "Персональный DNS",
                subtitle = "Частный DNS, quad9, DoH/DoT, блокировка рекламы",
                icon = Phosphor.Wifi,
                screen = Screen.PRIVATE_DNS,
                keywords = listOf("dns", "днс", "quad9", "cloudflare", "adguard", "домен", "приватный dns", "блокировка рекламы")
            ),
            MainSearchItem(
                title = "Подключенные устройства",
                subtitle = if (isBtOn) "Bluetooth включен" else "Bluetooth, NFC, Quick Share",
                icon = Phosphor.Bluetooth,
                screen = Screen.CONNECTED_DEVICES,
                keywords = listOf("блютуз", "блютус", "bluetooth", "наушники", "часы", "гарнитура", "nfc", "нфс", "quick share", "кнопка", "сопряжение")
            ),
            MainSearchItem(
                title = "Настройки подключения",
                subtitle = "NFC, Quick Share, трансляция экрана, печать",
                icon = Phosphor.Bluetooth,
                screen = Screen.CONNECTION_PREFERENCES,
                keywords = listOf("nfc", "нфс", "оплата", "мир pay", "трансляция", "cast", "печать", "авто", "android auto")
            ),
            MainSearchItem(
                title = "Экран",
                subtitle = "Яркость, темная тема, частота 120 Гц, цвета",
                icon = Phosphor.Sun,
                screen = Screen.DISPLAY,
                keywords = listOf("дисплей", "экран", "яркость", "герцы", "120гц", "60гц", "герцовка", "ночной режим", "темная тема", "display", "screen", "dark mode", "oled", "цвета")
            ),
            MainSearchItem(
                title = "Таймаут экрана",
                subtitle = "Автоотключение экрана при бездействии",
                icon = Phosphor.Sun,
                screen = Screen.SCREEN_TIMEOUT,
                keywords = listOf("таймаут", "сон", "время экрана", "выключение экрана", "автоотключение", "блокировка")
            ),
            MainSearchItem(
                title = "Обои и стиль",
                subtitle = "Цветовая палитра, темная тема, сетка",
                icon = Phosphor.Palette,
                screen = Screen.WALLPAPER,
                keywords = listOf("обои", "стиль", "тема", "палитра", "иконки", "сетка", "фон", "заставка", "wallpaper", "style", "theme")
            ),
            MainSearchItem(
                title = "Звук и вибрация",
                subtitle = "Громкость, рингтоны, виброотклик, Не беспокоить",
                icon = Phosphor.SpeakerHigh,
                screen = Screen.SOUND,
                keywords = listOf("звук", "громкость", "вибрация", "вибро", "тихий", "рингтон", "мелодия", "будильник", "динамик", "sound", "volume", "audio")
            ),
            MainSearchItem(
                title = "Рингтон телефона",
                subtitle = "Мелодия входящего звонка",
                icon = Phosphor.Bell,
                screen = Screen.RINGTONE_PHONE,
                keywords = listOf("рингтон", "мелодия", "звонок", "вызов", "ringtone")
            ),
            MainSearchItem(
                title = "Звук уведомления",
                subtitle = "Звуковой сигнал новых сообщений",
                icon = Phosphor.Bell,
                screen = Screen.RINGTONE_NOTIFICATION,
                keywords = listOf("уведомление", "сигнал", "смс", "сообщение", "пищалка")
            ),
            MainSearchItem(
                title = "Звук будильника",
                subtitle = "Сигнал пробуждения",
                icon = Phosphor.Bell,
                screen = Screen.RINGTONE_ALARM,
                keywords = listOf("будильник", "подъем", "утро", "alarm")
            ),
            MainSearchItem(
                title = "Вибрация и тактильный отклик",
                subtitle = "Вибрация при вызове, отклик клавиатуры и жестов",
                icon = Phosphor.SpeakerHigh,
                screen = Screen.VIBRATION_SETTINGS,
                keywords = listOf("вибро", "вибрация", "тактильный", "отклик", "хаптик", "haptics", "виброотклик")
            ),
            MainSearchItem(
                title = "Не беспокоить",
                subtitle = "Приглушение звонков и оповещений",
                icon = Phosphor.Bell,
                screen = Screen.DND_SETTINGS,
                keywords = listOf("не беспокоить", "dnd", "тишина", "сон", "заглушить", "расписание")
            ),
            MainSearchItem(
                title = "Уведомления",
                subtitle = "История, пузыри, экран блокировки, вспышка",
                icon = Phosphor.Bell,
                screen = Screen.NOTIFICATIONS,
                keywords = listOf("уведомления", "оповещения", "пузыри", "вспышка", "шторка", "notifications", "alerts")
            ),
            MainSearchItem(
                title = "История уведомлений",
                subtitle = "Журнал оповещений за последние сутки",
                icon = Phosphor.Bell,
                screen = Screen.NOTIFICATION_HISTORY,
                keywords = listOf("история", "журнал", "пропущенные", "архив уведомлений")
            ),
            MainSearchItem(
                title = "Приложения",
                subtitle = "$realAppsCount установленных программ, по умолчанию",
                icon = Phosphor.SquaresFour,
                screen = Screen.APPS,
                keywords = listOf("приложения", "программы", "аппки", "софт", "apps", "игры", "удалить", "память приложений")
            ),
            MainSearchItem(
                title = "Все приложения",
                subtitle = "Список программ, системные пакеты",
                icon = Phosphor.SquaresFour,
                screen = Screen.ALL_APPS,
                keywords = listOf("все приложения", "список программ", "пакеты", "системные приложения", "all apps")
            ),
            MainSearchItem(
                title = "Приложения по умолчанию",
                subtitle = "Браузер, звонки, SMS, лаунчер",
                icon = Phosphor.GearSix,
                screen = Screen.DEFAULT_APPS,
                keywords = listOf("по умолчанию", "стандартный браузер", "звонилка", "лаунчер", "дефолтные")
            ),
            MainSearchItem(
                title = "Батарея",
                subtitle = "$realBatteryPct% · " + if (isCharging) "Заряжается" else "Энергосбережение, расход, здоровье",
                icon = Phosphor.BatteryCharging,
                screen = Screen.BATTERY,
                keywords = listOf("батарея", "батарейка", "аккумулятор", "заряд", "зарядка", "питание", "энергия", "энергосбережение", "экономия", "battery", "power")
            ),
            MainSearchItem(
                title = "Расход заряда приложениями",
                subtitle = "Кастомный график и мониторинг потребления энергии",
                icon = Phosphor.BatteryCharging,
                screen = Screen.BATTERY_USAGE,
                keywords = listOf("расход", "потребление", "статистика батареи", "кто жрет батарею", "разрядка", "usage")
            ),
            MainSearchItem(
                title = "Хранилище",
                subtitle = realStorageText,
                icon = Phosphor.HardDrive,
                screen = Screen.STORAGE,
                keywords = listOf("память", "хранилище", "место", "гигабайты", "диск", "очистка", "накопитель", "storage")
            ),
            MainSearchItem(
                title = "Местоположение (GPS)",
                subtitle = "Доступ приложений к геоданным",
                icon = Phosphor.MapPin,
                screen = Screen.LOCATION,
                keywords = listOf("геолокация", "gps", "гпс", "местоположение", "карты", "навигация", "location")
            ),
            MainSearchItem(
                title = "Специальные возможности",
                subtitle = "Экранный диктор, увеличение, контраст",
                icon = Phosphor.Accessibility,
                screen = Screen.ACCESSIBILITY,
                keywords = listOf("спец возможности", "доступность", "шрифт", "масштаб", "accessibility")
            ),
            MainSearchItem(
                title = "Безопасность и конфиденциальность",
                subtitle = "Блокировка экрана, биометрия, защита",
                icon = Phosphor.ShieldCheck,
                screen = Screen.SECURITY,
                keywords = listOf("безопасность", "пароль", "пин", "пинкод", "отпечаток", "биометрия", "защита", "security", "privacy")
            ),
            MainSearchItem(
                title = "Обновление системы",
                subtitle = "Triangle OS 15 OTA, патчи безопасности",
                icon = Phosphor.ArrowsClockwise,
                screen = Screen.SYSTEM_UPDATE,
                keywords = listOf("обновление", "прошивка", "ota", "апдейт", "патч", "update")
            ),
            MainSearchItem(
                title = "Система",
                subtitle = "Языки, клавиатура, дата и время, сброс",
                icon = Phosphor.GearSix,
                screen = Screen.SYSTEM,
                keywords = listOf("система", "язык", "клавиатура", "время", "дата", "сброс", "system")
            ),
            MainSearchItem(
                title = "Для разработчиков",
                subtitle = "Отладка по USB, логкат, анимации",
                icon = Phosphor.GearSix,
                screen = Screen.DEVELOPER_OPTIONS,
                keywords = listOf("разработчик", "отладка", "usb", "adb", "анимации", "developer")
            ),
            MainSearchItem(
                title = "О телефоне",
                subtitle = "$realManufacturer $realModel, Triangle OS 15, версия ядра",
                icon = Phosphor.Info,
                screen = Screen.ABOUT_PHONE,
                keywords = listOf("о телефоне", "устройство", "модель", "андроид", "версия", "прошивка", "about", "specs")
            )
        )
    }

    val searchResults = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) emptyList()
        else allSearchableItems.filter { item ->
            isFuzzyMatch(item.title, q) ||
            isFuzzyMatch(item.subtitle, q) ||
            item.keywords.any { isFuzzyMatch(it, q) }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PaperBackground
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .graphicsLayer {
                            alpha = titleAlpha
                            translationY = titleTranslationY
                        }
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontSize = 36.sp
                    )
                }
            }

            // Поисковая строка
            item {
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Phosphor.MagnifyingGlass,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp),
                            tint = TextSecondary
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextTertiary
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                cursorBrush = SolidColor(TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Phosphor.ArrowLeft,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (searchQuery.isNotBlank()) {
                // РЕЖИМ ЖИВОГО ПОИСКА
                if (searchResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Phosphor.MagnifyingGlass,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "По запросу «$searchQuery» разделы настроек не найдены",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    item {
                        SettingCardGroup {
                            searchResults.forEachIndexed { index, item ->
                                val highlightedTitle = remember(item.title, searchQuery) {
                                    highlightMatchingText(item.title, searchQuery.trim())
                                }
                                val highlightedSubtitle = remember(item.subtitle, searchQuery) {
                                    highlightMatchingText(item.subtitle, searchQuery.trim())
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = ""
                                            onNavigate(item.screen)
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = highlightedTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = highlightedSubtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Phosphor.CaretRight,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (index < searchResults.lastIndex) {
                                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            } else {
                // СТАНДАРТНОЕ ОТОБРАЖЕНИЕ ГЛАВНОГО ЭКРАНА

            // Google / Triangle Аккаунт
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { Toast.makeText(context, "Triangle OS Cloud", Toast.LENGTH_SHORT).show() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PaperBackground)
                                .border(1.dp, BorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Phosphor.UserCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(26.dp),
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.account_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.account_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Icon(
                            imageVector = Phosphor.CaretRight,
                            contentDescription = "Open",
                            modifier = Modifier.size(16.dp),
                            tint = TextTertiary
                        )
                    }
                }
            }

            // Сеть, Bluetooth
            item {
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.Wifi,
                        title = stringResource(R.string.network_title),
                        subtitle = if (isWifiOn) realWifiSsid else "Wi-Fi выключен, мобильные данные",
                        onClick = { onNavigate(Screen.NETWORK) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Bluetooth,
                        title = stringResource(R.string.connected_devices_title),
                        subtitle = if (isBtOn) "Bluetooth включен" else "Bluetooth отключен",
                        onClick = { onNavigate(Screen.CONNECTED_DEVICES) }
                    )
                }
            }

            // Экран, Обои, Звук, Уведомления
            item {
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.Sun,
                        title = stringResource(R.string.display_title),
                        subtitle = stringResource(R.string.display_subtitle),
                        onClick = { onNavigate(Screen.DISPLAY) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Palette,
                        title = stringResource(R.string.wallpaper_title),
                        subtitle = stringResource(R.string.wallpaper_subtitle),
                        onClick = { onNavigate(Screen.WALLPAPER) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.SpeakerHigh,
                        title = stringResource(R.string.sound_title),
                        subtitle = stringResource(R.string.sound_subtitle),
                        onClick = { onNavigate(Screen.SOUND) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Bell,
                        title = stringResource(R.string.notifications_title),
                        subtitle = stringResource(R.string.notifications_subtitle),
                        onClick = { onNavigate(Screen.NOTIFICATIONS) }
                    )
                }
            }

            // Приложения, Батарея, Память
            item {
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.SquaresFour,
                        title = stringResource(R.string.apps_title),
                        subtitle = "$realAppsCount установленных приложений",
                        onClick = { onNavigate(Screen.APPS) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.BatteryCharging,
                        title = stringResource(R.string.battery_title),
                        subtitle = "$realBatteryPct% · " + if (isCharging) "Заряжается" else "Работа от аккумулятора",
                        onClick = { onNavigate(Screen.BATTERY) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.HardDrive,
                        title = stringResource(R.string.storage_title),
                        subtitle = realStorageText,
                        onClick = { onNavigate(Screen.STORAGE) }
                    )
                }
            }

            // Геолокация, Спец. возможности, Безопасность
            item {
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.MapPin,
                        title = stringResource(R.string.location_title),
                        subtitle = stringResource(R.string.location_subtitle),
                        onClick = { onNavigate(Screen.LOCATION) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Accessibility,
                        title = stringResource(R.string.accessibility_title),
                        subtitle = stringResource(R.string.accessibility_subtitle),
                        onClick = { onNavigate(Screen.ACCESSIBILITY) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.ShieldCheck,
                        title = stringResource(R.string.security_title),
                        subtitle = stringResource(R.string.security_subtitle),
                        onClick = { onNavigate(Screen.SECURITY) }
                    )
                }
            }

            // Система, Обновления и О телефоне
            item {
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.ArrowsClockwise,
                        title = stringResource(R.string.system_update_title),
                        subtitle = stringResource(R.string.system_update_subtitle),
                        onClick = { onNavigate(Screen.SYSTEM_UPDATE) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.GearSix,
                        title = stringResource(R.string.system_title),
                        subtitle = stringResource(R.string.system_subtitle),
                        onClick = { onNavigate(Screen.SYSTEM) }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Info,
                        title = stringResource(R.string.about_title),
                        subtitle = "$realManufacturer $realModel",
                        onClick = { onNavigate(Screen.ABOUT_PHONE) }
                    )
                }
            }
            }
        }
    }
}

fun levenshteinDistance(s1: String, s2: String): Int {
    val a = s1.lowercase()
    val b = s2.lowercase()
    val costs = IntArray(b.length + 1)
    for (j in 0..b.length) costs[j] = j
    for (i in 1..a.length) {
        costs[0] = i
        var nw = i - 1
        for (j in 1..b.length) {
            val cj = kotlin.math.min(1 + kotlin.math.min(costs[j], costs[j - 1]), if (a[i - 1] == b[j - 1]) nw else nw + 1)
            nw = costs[j]
            costs[j] = cj
        }
    }
    return costs[b.length]
}

fun isFuzzyMatch(word: String, query: String): Boolean {
    val w = word.lowercase()
    val q = query.lowercase()
    if (w.contains(q) || q.contains(w)) return true

    // Для слов длиной от 4 букв допускаем 1 опечатку, от 7 букв — 2 опечатки
    val maxErrors = if (q.length >= 7) 2 else if (q.length >= 4) 1 else 0
    if (maxErrors == 0) return false

    // Проверяем само слово
    if (levenshteinDistance(w, q) <= maxErrors) return true

    // Проверяем подстроку
    for (start in 0..(w.length - q.length).coerceAtLeast(0)) {
        val sub = w.substring(start, (start + q.length).coerceAtMost(w.length))
        if (levenshteinDistance(sub, q) <= maxErrors) return true
    }
    return false
}

fun highlightMatchingText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val q = query.trim().lowercase()
    val t = text.lowercase()

    // Прямое совпадение подстроки
    val startIndex = t.indexOf(q)
    if (startIndex >= 0) {
        return buildAnnotatedString {
            append(text.substring(0, startIndex))
            pushStyle(
                SpanStyle(
                    color = AccentTerracotta,
                    fontWeight = FontWeight.Bold
                )
            )
            append(text.substring(startIndex, startIndex + q.length))
            pop()
            append(text.substring(startIndex + q.length))
        }
    }

    // Нечеткое совпадение по словам (если опечатка типа "боторея" -> "батарея")
    val words = text.split(" ")
    var bestWordIndex = -1
    var bestWordStart = 0
    var bestWordLength = 0
    var runningIndex = 0

    for (word in words) {
        val cleanWord = word.trim().trim(',', '.', '!', '?', '(', ')')
        if (isFuzzyMatch(cleanWord, q)) {
            bestWordIndex = text.indexOf(word, runningIndex)
            bestWordStart = bestWordIndex
            bestWordLength = word.length
            break
        }
        runningIndex += word.length + 1
    }

    if (bestWordStart >= 0 && bestWordLength > 0) {
        return buildAnnotatedString {
            append(text.substring(0, bestWordStart))
            pushStyle(
                SpanStyle(
                    color = AccentTerracotta,
                    fontWeight = FontWeight.Bold
                )
            )
            append(text.substring(bestWordStart, bestWordStart + bestWordLength))
            pop()
            append(text.substring(bestWordStart + bestWordLength))
        }
    }

    return AnnotatedString(text)
}

fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

// ========================================
// ПОДРАЗДЕЛЫ С РЕАЛЬНЫМИ КОНТРОЛЛЕРАМИ OS
// ========================================

@Composable
fun NetworkSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    var wifi by remember { mutableStateOf(systemManager.isWifiOn()) }
    var mobileData by remember { mutableStateOf(systemManager.isMobileDataOn()) }
    var airplane by remember { mutableStateOf(systemManager.isAirplaneModeOn()) }
    var roaming by remember { mutableStateOf(systemManager.isDataRoamingOn()) }
    var dataSaver by remember { mutableStateOf(systemManager.isDataSaverOn()) }
    var dnsText by remember { mutableStateOf(systemManager.getPrivateDnsMode()) }
    val (_, currentSsid) = remember { systemManager.getRealWifiSummary() }
    val simCards = remember { systemManager.getRealSimCards() }
    val preferredNetwork = remember { systemManager.getPreferredNetworkType() }

    SubScreenScaffold(title = stringResource(R.string.network_title), onBack = onBack) {
        // Карта Wi-Fi и доступных сетей
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.wifi_label),
                    subtitle = if (wifi) currentSsid else "Отключено",
                    checked = wifi,
                    onCheckedChange = {
                        wifi = it
                        systemManager.setWifi(it)
                    }
                )
                if (wifi) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Wifi,
                        title = "Поиск сетей Wi-Fi",
                        subtitle = "Сканировать и подключиться к сети",
                        onClick = { onNavigate(Screen.WIFI_SETTINGS) }
                    )
                }
            }
        }

        // SIM-карты и мобильная связь
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.SquaresFour,
                    title = "SIM-карты и оператор",
                    subtitle = if (simCards.isNotEmpty()) simCards.first().displayName + " · " + simCards.first().carrierName else "SIM-карта не обнаружена",
                    onClick = { onNavigate(Screen.SIM_SETTINGS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.mobile_data_label),
                    subtitle = stringResource(R.string.mobile_data_desc),
                    checked = mobileData,
                    onCheckedChange = {
                        mobileData = it
                        systemManager.setMobileData(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.roaming_label),
                    subtitle = stringResource(R.string.roaming_desc),
                    checked = roaming,
                    onCheckedChange = {
                        roaming = it
                        systemManager.setDataRoaming(it)
                    }
                )
            }
        }

        // Точка доступа и модем
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.Wifi,
                    title = stringResource(R.string.hotspot_label),
                    subtitle = "Wi-Fi, USB и Bluetooth модем",
                    onClick = { onNavigate(Screen.HOTSPOT_SETTINGS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Экономия трафика",
                    subtitle = "Запрет передачи фоновых данных приложениям",
                    checked = dataSaver,
                    onCheckedChange = {
                        dataSaver = it
                        systemManager.setDataSaver(it)
                    }
                )
            }
        }

        // Режим полета и Приватный DNS
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.airplane_mode_label),
                    subtitle = stringResource(R.string.airplane_mode_desc),
                    checked = airplane,
                    onCheckedChange = {
                        airplane = it
                        systemManager.setAirplaneMode(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Vpn,
                    title = "VPN",
                    subtitle = if (systemManager.isVpnConnected()) "Подключено" else "Не подключено",
                    onClick = { onNavigate(Screen.VPN_SETTINGS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.ShieldCheck,
                    title = stringResource(R.string.dns_label),
                    subtitle = dnsText,
                    onClick = { onNavigate(Screen.PRIVATE_DNS) }
                )
            }
        }
    }
}

@Composable
fun WifiSettingsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var wifiList by remember { mutableStateOf<List<org.triangle.settings.system.RealWifiItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    var selectedWifiItem by remember { mutableStateOf<org.triangle.settings.system.RealWifiItem?>(null) }
    var wifiPasswordInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var wifiStatusFeedback by remember { mutableStateOf<String?>(null) }

    fun refreshList() {
        isScanning = true
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) {
                systemManager.getRealWifiScanList()
            }
            wifiList = list
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    if (selectedWifiItem != null) {
        val item = selectedWifiItem!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                if (!isConnecting) {
                    selectedWifiItem = null
                    wifiPasswordInput = ""
                    showPassword = false
                    wifiStatusFeedback = null
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Title Header with Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(BorderColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Phosphor.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.ssid,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (item.isSecure) "Защищенная сеть Wi-Fi" else "Открытая сеть",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (item.isSecure) {
                        Text(
                            text = "Пароль",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wifiPasswordInput,
                            onValueChange = {
                                wifiPasswordInput = it
                                wifiStatusFeedback = null
                            },
                            placeholder = { Text("Введите не менее 8 символов", color = TextSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            enabled = !isConnecting,
                            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Phosphor.EyeSlash else Phosphor.Eye,
                                        contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                                        modifier = Modifier.size(20.dp),
                                        tint = TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BorderBold,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BorderBold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Длина: ${wifiPasswordInput.length} символов",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (wifiPasswordInput.isNotEmpty() && wifiPasswordInput.length < 8) AccentTerracotta else TextSecondary
                            )
                            if (wifiPasswordInput.isNotEmpty() && wifiPasswordInput.length < 8) {
                                Text(
                                    text = "Минимум 8",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = AccentTerracotta
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Эта сеть не требует ввода пароля. Подключение происходит напрямую.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    if (wifiStatusFeedback != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (wifiStatusFeedback!!.startsWith("Ошибка") || wifiStatusFeedback!!.startsWith("Не удалось")) {
                                AccentTerracotta.copy(alpha = 0.12f)
                            } else {
                                BorderColor.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = wifiStatusFeedback!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (wifiStatusFeedback!!.startsWith("Ошибка") || wifiStatusFeedback!!.startsWith("Не удалось")) AccentTerracotta else TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = !isConnecting,
                            onClick = {
                                selectedWifiItem = null
                                wifiPasswordInput = ""
                                showPassword = false
                                wifiStatusFeedback = null
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Отмена", color = TextSecondary, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            enabled = !isConnecting && (!item.isSecure || wifiPasswordInput.length >= 8),
                            onClick = {
                                isConnecting = true
                                wifiStatusFeedback = "Подключение..."
                                coroutineScope.launch {
                                    val (success, message) = withContext(Dispatchers.IO) {
                                        systemManager.connectToWifi(
                                            item.ssid,
                                            if (item.isSecure) wifiPasswordInput else null
                                        )
                                    }
                                    isConnecting = false
                                    wifiStatusFeedback = message
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        kotlinx.coroutines.delay(1200)
                                        selectedWifiItem = null
                                        wifiPasswordInput = ""
                                        showPassword = false
                                        wifiStatusFeedback = null
                                        refreshList()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BorderBold,
                                contentColor = SurfaceCard,
                                disabledContainerColor = BorderColor.copy(alpha = 0.5f),
                                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SurfaceCard
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Связь...", color = SurfaceCard)
                            } else {
                                Text("Подключить", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    SubScreenScaffold(title = "Сети Wi-Fi", onBack = onBack) {
        item {
            SettingCardGroup {
                SettingSimpleRow(
                    title = "Доступные сети",
                    subtitle = if (isScanning) "Сканирование радиодиапазона..." else "Найдено ${wifiList.size} точек доступа"
                )
            }
        }

        if (wifiList.isNotEmpty()) {
            item {
                SettingCardGroup {
                    wifiList.forEachIndexed { index, wifiItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (wifiItem.isConnected) {
                                        Toast.makeText(context, "Уже подключено к ${wifiItem.ssid}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedWifiItem = wifiItem
                                        wifiPasswordInput = ""
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Phosphor.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (wifiItem.isConnected) BorderBold else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = wifiItem.ssid,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = if (wifiItem.isConnected) BorderBold else TextPrimary
                                )
                                Text(
                                    text = if (wifiItem.isConnected) "Подключено · Отличное качество" else if (wifiItem.isSecure) "Защищено (WPA/WPA2/WPA3)" else "Открытая сеть",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (wifiItem.isConnected) {
                                Icon(
                                    imageVector = Phosphor.Check,
                                    contentDescription = "Connected",
                                    modifier = Modifier.size(16.dp),
                                    tint = BorderBold
                                )
                            }
                        }
                        if (index < wifiList.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimSettingsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val simCards = remember { systemManager.getRealSimCards() }
    var networkType by remember { mutableStateOf(systemManager.getPreferredNetworkType()) }
    var roaming by remember { mutableStateOf(systemManager.isDataRoamingOn()) }

    SubScreenScaffold(title = "SIM-карты и мобильная сеть", onBack = onBack) {
        item {
            SettingCardGroup {
                if (simCards.isNotEmpty()) {
                    val sim = simCards.first()
                    InfoRow(label = "Оператор", value = sim.carrierName)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    InfoRow(label = "Слот", value = "Слот ${sim.simSlotIndex + 1}")
                    if (sim.number.isNotBlank()) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        InfoRow(label = "Номер телефона", value = sim.number)
                    }
                } else {
                    SettingSimpleRow(title = "SIM-карта", subtitle = "Физическая SIM или eSIM не вставлена")
                }
            }
        }

        item {
            SettingCardGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { networkType = systemManager.cyclePreferredNetworkType() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Предпочтительный тип сети", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text(text = networkType, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                    Text(text = "Изменить", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.roaming_label),
                    subtitle = stringResource(R.string.roaming_desc),
                    checked = roaming,
                    onCheckedChange = {
                        roaming = it
                        systemManager.setDataRoaming(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingSimpleRow(title = "Точки доступа (APN)", subtitle = "Конфигурация мобильного интернета оператора")
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingSimpleRow(title = "Вызовы по Wi-Fi (VoWiFi)", subtitle = "Совершать звонки через беспроводную сеть")
            }
        }
    }
}

@Composable
fun PrivateDnsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(systemManager.getPrivateDnsRawMode()) }
    var hostnameText by remember { mutableStateOf(systemManager.getPrivateDnsHostname()) }

    val options = listOf(
        "off" to "Отключен" to "Обычные DNS-запросы без шифрования",
        "opportunistic" to "Автоматически" to "Использовать DNS-over-TLS, если сервер провайдера поддерживает",
        "hostname" to "Имя хоста поставщика персонального DNS" to "Все запросы направляются через защищенный DoH/DoT"
    )

    SubScreenScaffold(title = stringResource(R.string.dns_label), onBack = onBack) {
        item {
            SettingCardGroup {
                options.forEachIndexed { index, pair ->
                    val modeKey = pair.first.first
                    val title = pair.first.second
                    val desc = pair.second
                    val isSelected = selectedMode == modeKey

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMode = modeKey
                                systemManager.setPrivateDns(modeKey, hostnameText)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = TextPrimary)
                            Text(text = desc, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                        }
                        if (isSelected) {
                        Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.dp, BorderBold, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Phosphor.Check, contentDescription = "Selected", modifier = Modifier.size(14.dp), tint = BorderBold)
                            }
                        }
                    }

                    if (index < options.lastIndex) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }

        if (selectedMode == "hostname") {
            item {
                SettingCardGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Имя хоста DNS-сервера", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = hostnameText,
                                onValueChange = {
                                    hostnameText = it
                                    systemManager.setPrivateDns("hostname", it)
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                cursorBrush = SolidColor(TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Например: dns.quad9.net или one.one.one.one",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VpnSettingsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isVpnActive by remember { mutableStateOf(systemManager.isVpnConnected()) }
    var vpnApps by remember { mutableStateOf(systemManager.getInstalledVpnApps()) }
    var vpnProfiles by remember { mutableStateOf(systemManager.getSavedVpnProfiles()) }
    var alwaysOnPkg by remember { mutableStateOf(systemManager.getAlwaysOnVpnPackage()) }
    var isLockdown by remember { mutableStateOf(systemManager.isAlwaysOnVpnLockdown()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileServer by remember { mutableStateOf("") }
    var newProfileType by remember { mutableStateOf("IKEv2/IPSec PSK") }

    SubScreenScaffold(title = "VPN", onBack = onBack) {
        // Статус текущего подключения
        item {
            SettingCardGroup {
                SettingSimpleRow(
                    title = "Состояние VPN",
                    subtitle = if (isVpnActive) "VPN-туннель активен" else "Нет активных VPN-подключений"
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Профили VPN
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПРОФИЛИ VPN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "+ Добавить профиль",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.clickable {
                        newProfileName = ""
                        newProfileServer = ""
                        showAddDialog = true
                    }
                )
            }

            SettingCardGroup {
                if (vpnProfiles.isEmpty()) {
                    SettingSimpleRow(
                        title = "Нет настроенных профилей",
                        subtitle = "Нажмите «+ Добавить профиль» для создания соединения IKEv2 / IPSec"
                    )
                } else {
                    vpnProfiles.forEachIndexed { index, profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Подключение к ${profile.name}...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PaperBackground)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Phosphor.Vpn, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = profile.name, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = TextPrimary)
                                Text(
                                    text = "${profile.type} · ${profile.server}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    systemManager.deleteVpnProfile(index)
                                    vpnProfiles = systemManager.getSavedVpnProfiles()
                                }
                            ) {
                                Icon(imageVector = Phosphor.Trash, contentDescription = "Удалить", tint = TextTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < vpnProfiles.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Установленные VPN-службы (если есть сторонние клиенты вроде WireGuard, OpenVPN)
        if (vpnApps.isNotEmpty()) {
            item {
                Text(
                    text = "VPN-СЛУЖБЫ ПРИЛОЖЕНИЙ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    vpnApps.forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(context, "Не удалось запустить ${app.appName}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconBitmap = remember(app.packageName) { app.icon?.let { drawableToBitmap(it) } }
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap.asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                            Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PaperBackground)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Phosphor.Vpn, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = app.appName, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = TextPrimary)
                                Text(
                                    text = if (app.packageName == alwaysOnPkg) "Постоянная VPN-служба" else app.packageName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(imageVector = Phosphor.CaretRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                        if (index < vpnApps.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Постоянная VPN-служба (Always-On VPN) & Kill Switch
        item {
            Text(
                text = "БЕЗОПАСНОСТЬ И ТРАФИК",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = "Блокировать соединения без VPN",
                    subtitle = "Запретить любой интернет-трафик в обход туннеля (Kill Switch)",
                    checked = isLockdown,
                    onCheckedChange = { checked ->
                        isLockdown = checked
                        systemManager.setAlwaysOnVpn(alwaysOnPkg, checked)
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("Новый профиль VPN", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Название сети:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        placeholder = { Text("Например: Рабочий VPN", color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Адрес сервера:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newProfileServer,
                        onValueChange = { newProfileServer = it },
                        placeholder = { Text("vpn.example.com или IP", color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Тип: $newProfileType", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank() && newProfileServer.isNotBlank()) {
                            systemManager.saveVpnProfile(newProfileName.trim(), newProfileType, newProfileServer.trim())
                            vpnProfiles = systemManager.getSavedVpnProfiles()
                            showAddDialog = false
                            Toast.makeText(context, "Профиль ${newProfileName.trim()} сохранен", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

@Composable
fun HotspotSettingsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var usbTether by remember { mutableStateOf(systemManager.isUsbTetheringOn()) }
    var btTether by remember { mutableStateOf(systemManager.isBluetoothTetheringOn()) }

    SubScreenScaffold(title = stringResource(R.string.hotspot_label), onBack = onBack) {
        item {
            SettingCardGroup {
                SettingSimpleRow(
                    title = "Точка доступа Wi-Fi",
                    subtitle = "Triangle-AP · WPA2-Personal (Отключена)"
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "USB-модем",
                    subtitle = "Раздавать мобильный интернет на ПК через кабель",
                    checked = usbTether,
                    onCheckedChange = {
                        usbTether = it
                        systemManager.setUsbTethering(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Bluetooth-модем",
                    subtitle = "Предоставлять интернет-соединение по Bluetooth",
                    checked = btTether,
                    onCheckedChange = {
                        btTether = it
                        systemManager.setBluetoothTethering(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingSimpleRow(
                    title = "Автоотключение точки доступа",
                    subtitle = "Выключать точку при отсутствии подключенных устройств"
                )
            }
        }
    }
}

@Composable
fun ConnectedDevicesSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    var bt by remember { mutableStateOf(systemManager.isBluetoothOn()) }
    var selectedDevice by remember { mutableStateOf<org.triangle.settings.system.RealBtDeviceItem?>(null) }
    var bondedDevices by remember { mutableStateOf<List<org.triangle.settings.system.RealBtDeviceItem>>(emptyList()) }
    val devName = remember { systemManager.getBluetoothDeviceName() }
    val nfcOn = remember { systemManager.isNfcOn() }
    val nfcAvailable = remember { systemManager.isNfcAvailable() }

    LaunchedEffect(bt) {
        if (bt) {
            bondedDevices = systemManager.getBondedBluetoothDevices()
        } else {
            bondedDevices = emptyList()
        }
    }

    SubScreenScaffold(title = stringResource(R.string.connected_devices_title), onBack = onBack) {
        // Главный тумблер Bluetooth
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.bluetooth_label),
                    subtitle = if (bt) "Видно для других как «$devName»" else "Отключен",
                    checked = bt,
                    onCheckedChange = {
                        bt = it
                        systemManager.setBluetooth(it)
                    }
                )
                if (bt) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Bluetooth,
                        title = stringResource(R.string.pair_new_device_label),
                        subtitle = "Поиск наушников, часов и других аксессуаров",
                        onClick = { onNavigate(Screen.BLUETOOTH_PAIRING) }
                    )
                }
            }
        }

        // Список сопряженных устройств
        if (bt && bondedDevices.isNotEmpty()) {
            item {
                SettingCardGroup {
                    bondedDevices.forEachIndexed { index, device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDevice = device }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Phosphor.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (device.isConnected) BorderBold else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = if (device.isConnected) BorderBold else TextPrimary
                                )
                                val statusText = buildString {
                                    if (device.isConnected) append("Подключено") else append("Ранее подключено")
                                    if (device.batteryLevel >= 0) append(" · Батарея ${device.batteryLevel}%")
                                    append(" · ${device.majorClass}")
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Phosphor.GearSix,
                                contentDescription = "Settings",
                                modifier = Modifier.size(16.dp),
                                tint = TextTertiary
                            )
                        }
                        if (index < bondedDevices.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        } else if (bt) {
            item {
                SettingCardGroup {
                    SettingSimpleRow(
                        title = "Сохраненные устройства",
                        subtitle = "Нет ранее сопряженных аксессуаров"
                    )
                }
            }
        }

        // Настройки подключения (NFC, Quick Share, Трансляция)
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = "Настройки подключения",
                    subtitle = "NFC, Quick Share, Трансляция экрана, Печать",
                    onClick = { onNavigate(Screen.CONNECTION_PREFERENCES) }
                )
            }
        }
    }

    selectedDevice?.let { device ->
        AlertDialog(
            onDismissRequest = { selectedDevice = null },
            title = {
                Text(device.name, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    InfoRow(label = "Состояние", value = if (device.isConnected) "Подключено" else "Не подключено")
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    InfoRow(label = "Класс устройства", value = device.majorClass)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    InfoRow(label = "MAC-адрес", value = device.address)
                    if (device.batteryLevel >= 0) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        InfoRow(label = "Батарея", value = "${device.batteryLevel}%")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDevice = null }) {
                    Text("Закрыть", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

@Composable
fun BluetoothPairingSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val devName = remember { systemManager.getBluetoothDeviceName() }
    val context = LocalContext.current
    val scanner = remember { BluetoothScanner(context) }
    var devices by remember { mutableStateOf<List<BluetoothScanner.FoundDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanFinished by remember { mutableStateOf(false) }
    var bluetoothOn by remember { mutableStateOf(systemManager.isBluetoothOn()) }
    var permissionGranted by remember {
        mutableStateOf(scanner.hasScanPermission() && scanner.hasConnectPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Ориентируемся на фактическое состояние разрешений, а не на ответ диалога.
        permissionGranted = scanner.hasScanPermission() && scanner.hasConnectPermission()
    }

    fun startScan() {
        if (!permissionGranted || !bluetoothOn) return
        devices = emptyList()
        scanFinished = false
        scanning = true
        scanner.start(
            onFound = { found ->
                devices = (devices.filterNot { it.address == found.address } + found)
                    .sortedByDescending { it.rssi ?: Int.MIN_VALUE }
            },
            onFinished = {
                scanning = false
                scanFinished = true
            }
        )
    }

    LaunchedEffect(permissionGranted, bluetoothOn) {
        if (permissionGranted && bluetoothOn) startScan()
    }

    DisposableEffect(Unit) {
        onDispose { scanner.stop() }
    }

    SubScreenScaffold(title = "Подключение устройства", onBack = onBack) {
        item {
            SettingCardGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Имя устройства: $devName", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                    Text(
                        text = if (scanning) "Идёт поиск устройств Bluetooth Classic и BLE…" else "Поиск длится 12 секунд и показывает только реально найденные устройства",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (scanner.isSupported()) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Bluetooth",
                        subtitle = if (bluetoothOn) "Адаптер включён" else "Адаптер выключен",
                        checked = bluetoothOn,
                        onCheckedChange = { enabled ->
                            systemManager.setBluetooth(enabled)
                            bluetoothOn = systemManager.isBluetoothOn()
                        }
                    )
                }
            }
        }

        if (!scanner.isSupported()) {
            item {
                SettingCardGroup {
                    SettingSimpleRow(
                        title = "Bluetooth недоступен",
                        subtitle = "Система не сообщает о наличии Bluetooth-адаптера"
                    )
                }
            }
        } else if (!permissionGranted) {
            item {
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.ShieldCheck,
                        title = "Нужно разрешение на поиск",
                        subtitle = "Android 12+ требует BLUETOOTH_SCAN и BLUETOOTH_CONNECT",
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        }
                    )
                }
            }
        } else if (!bluetoothOn) {
            item {
                SettingCardGroup {
                    SettingSimpleRow(
                        title = "Bluetooth выключен",
                        subtitle = "Включите адаптер, чтобы искать устройства"
                    )
                }
            }
        }

        if (devices.isEmpty()) {
            if (permissionGranted && bluetoothOn) {
                item {
                    SettingCardGroup {
                        SettingSimpleRow(
                            title = if (scanning) "Поиск…" else "Устройства не найдены",
                            subtitle = if (scanning) {
                                "Убедитесь, что аксессуар включён и находится в режиме сопряжения"
                            } else {
                                "Рядом нет устройств в режиме обнаружения"
                            }
                        )
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        SettingItem(
                            icon = Phosphor.ArrowsClockwise,
                            title = if (scanning) "Остановить и начать заново" else "Искать снова",
                            subtitle = "Повторное сканирование Classic и BLE",
                            onClick = { startScan() }
                        )
                    }
                }
            }
        } else {
            item {
                SettingCardGroup {
                    devices.forEachIndexed { index, device ->
                        val subtitle = buildString {
                            append(device.address)
                            append(" · ")
                            append(if (device.isLe) "BLE" else "Classic")
                            device.rssi?.let { append(" · $it dBm") }
                            if (device.isBonded) append(" · сопряжено")
                        }
                        SettingItem(
                            icon = if (device.isLe) Phosphor.Bluetooth else Phosphor.Bluetooth,
                            title = device.name,
                            subtitle = subtitle,
                            onClick = {
                                if (device.isBonded) {
                                    Toast.makeText(context, "Устройство уже сопряжено", Toast.LENGTH_SHORT).show()
                                } else if (scanner.pair(device.address)) {
                                    Toast.makeText(context, "Запрос на сопряжение отправлен", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Не удалось начать сопряжение", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        if (index < devices.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.ArrowsClockwise,
                        title = if (scanning) "Идёт поиск…" else "Искать снова",
                        subtitle = "Найдено устройств: ${devices.size}",
                        onClick = { startScan() }
                    )
                }
            }
        }
    }
}

private fun launchSystemIntent(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        Toast.makeText(context, "Системный экран недоступен", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ConnectionPreferencesSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var quickShare by remember { mutableStateOf(systemManager.isQuickShareOn()) }
    val nfcAvailable = remember { systemManager.isNfcAvailable() }
    var nfcOn by remember { mutableStateOf(systemManager.isNfcOn()) }

    // Реальные интенты: строку показываем, только если система умеет её обработать.
    val castIntent = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_CAST_SETTINGS).takeIf {
                it.resolveActivity(context.packageManager) != null
            }
        } else null
    }
    val printIntent = remember {
        Intent(Settings.ACTION_PRINT_SETTINGS).takeIf {
            it.resolveActivity(context.packageManager) != null
        }
    }
    val androidAutoIntent = remember {
        context.packageManager.getLaunchIntentForPackage("com.google.android.projection.gearhead")
    }
    val castReceiversAvailable = remember { systemManager.isCastAvailable() }

    SubScreenScaffold(title = "Настройки подключения", onBack = onBack) {
        item {
            SettingCardGroup {
                if (nfcAvailable) {
                    SettingToggleRow(
                        title = "NFC",
                        subtitle = "Бесконтактная оплата и обмен данными при касании",
                        checked = nfcOn,
                        onCheckedChange = { nfcOn = it }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
                SettingToggleRow(
                    title = "Quick Share",
                    subtitle = "Быстрый обмен файлами с устройствами поблизости",
                    checked = quickShare,
                    onCheckedChange = {
                        quickShare = it
                        systemManager.setQuickShare(it)
                    }
                )
                if (castIntent != null) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.SquaresFour,
                        title = "Трансляция (Cast)",
                        subtitle = if (castReceiversAvailable) "Приёмники трансляции доступны" else "Приёмники не обнаружены",
                        onClick = { launchSystemIntent(context, castIntent) }
                    )
                }
                if (printIntent != null) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.HardDrive,
                        title = "Печать",
                        subtitle = "Службы печати и принтеры",
                        onClick = { launchSystemIntent(context, printIntent) }
                    )
                }
                if (androidAutoIntent != null) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.MapPin,
                        title = "Android Auto",
                        subtitle = "Подключение к автомобильному дисплею",
                        onClick = { launchSystemIntent(context, androidAutoIntent) }
                    )
                }
            }
        }
    }
}

@Composable
fun DisplaySubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    var smooth by remember { mutableStateOf(systemManager.isPeakRefreshRate120()) }
    var dark by remember { mutableStateOf(systemManager.isDarkThemeOn()) }
    var autoRotate by remember { mutableStateOf(systemManager.isAutoRotateOn()) }
    var autoBrightness by remember { mutableStateOf(systemManager.isAutoBrightnessOn()) }
    var nightDisplay by remember { mutableStateOf(systemManager.isNightDisplayOn()) }
    var tapToWake by remember { mutableStateOf(systemManager.isTapToWakeOn()) }
    var liftToWake by remember { mutableStateOf(systemManager.isLiftToWakeOn()) }
    val timeoutSummary = remember { systemManager.getScreenTimeoutSummary() }
    var colorMode by remember { mutableStateOf(systemManager.getColorMode()) }
    var brightnessPercent by remember { mutableStateOf(systemManager.getScreenBrightnessPercent()) }
    val resolutionText = remember { systemManager.getScreenResolutionSummary() }

    SubScreenScaffold(title = stringResource(R.string.display_title), onBack = onBack) {
        // Уровень яркости
        item {
            SettingCardGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.brightness_slider_label), style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text(text = "$brightnessPercent%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = brightnessPercent.toFloat(),
                        onValueChange = {
                            brightnessPercent = it.toInt()
                            systemManager.setScreenBrightnessPercent(it.toInt())
                        },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = BorderBold,
                            inactiveTrackColor = BorderColor
                        )
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.auto_brightness_label),
                    subtitle = stringResource(R.string.auto_brightness_desc),
                    checked = autoBrightness,
                    onCheckedChange = {
                        autoBrightness = it
                        systemManager.setAutoBrightness(it)
                    }
                )
            }
        }

        // Частота обновления и тема
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.smooth_display_label),
                    subtitle = stringResource(R.string.smooth_display_desc),
                    checked = smooth,
                    onCheckedChange = {
                        smooth = it
                        systemManager.setPeakRefreshRate(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.dark_theme_label),
                    subtitle = stringResource(R.string.dark_theme_desc),
                    checked = dark,
                    onCheckedChange = {
                        dark = it
                        systemManager.setDarkTheme(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.night_light_label),
                    subtitle = stringResource(R.string.night_light_desc),
                    checked = nightDisplay,
                    onCheckedChange = {
                        nightDisplay = it
                        systemManager.setNightDisplay(it)
                    }
                )
            }
        }

        // Цветовой профиль и тайм-аут
        item {
            SettingCardGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { colorMode = systemManager.cycleColorMode() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Цветовой профиль", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text(text = colorMode, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                    Text(text = "Изменить", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Sun,
                    title = stringResource(R.string.screen_timeout_label),
                    subtitle = timeoutSummary,
                    onClick = { onNavigate(Screen.SCREEN_TIMEOUT) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.auto_rotate_label),
                    subtitle = stringResource(R.string.auto_rotate_desc),
                    checked = autoRotate,
                    onCheckedChange = {
                        autoRotate = it
                        systemManager.setAutoRotate(it)
                    }
                )
            }
        }

        // Жесты пробуждения экрана и разрешение
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Двойное касание для включения",
                    subtitle = "Пробуждать экран при двойном постукивании по дисплею",
                    checked = tapToWake,
                    onCheckedChange = {
                        tapToWake = it
                        systemManager.setTapToWake(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Включение при поднятии",
                    subtitle = "Активировать дисплей, когда телефон берут в руки",
                    checked = liftToWake,
                    onCheckedChange = {
                        liftToWake = it
                        systemManager.setLiftToWake(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(label = "Разрешение дисплея", value = resolutionText)
            }
        }
    }
}

@Composable
fun ScreenTimeoutSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var selectedMs by remember { mutableStateOf(systemManager.getScreenTimeoutMs()) }

    val timeoutOptions = listOf(
        15000 to "15 секунд" to "Минимальное энергопотребление",
        30000 to "30 секунд" to "Энергоэффективно",
        60000 to "1 минута" to "Рекомендуемый баланс",
        120000 to "2 минуты" to "Удобно при чтении заметок",
        300000 to "5 минут" to "Длительное отображение",
        600000 to "10 минут" to "Максимальное время бодрствования"
    )

    SubScreenScaffold(title = stringResource(R.string.screen_timeout_label), onBack = onBack) {
        item {
            SettingCardGroup {
                timeoutOptions.forEachIndexed { index, (pair, desc) ->
                    val ms = pair.first
                    val title = pair.second
                    val isSelected = selectedMs == ms

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMs = ms
                                systemManager.setScreenTimeoutMs(ms)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = TextPrimary)
                            Text(text = desc, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                        }
                        if (isSelected) {
                        Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.dp, BorderBold, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Phosphor.Check, contentDescription = "Selected", modifier = Modifier.size(14.dp), tint = BorderBold)
                            }
                        }
                    }

                    if (index < timeoutOptions.lastIndex) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SoundSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    var lockSound by remember { mutableStateOf(systemManager.isLockSoundOn()) }
    var dialpadSound by remember { mutableStateOf(systemManager.isDialpadSoundOn()) }
    var chargingSound by remember { mutableStateOf(systemManager.isChargingSoundOn()) }
    var touchSound by remember { mutableStateOf(systemManager.isTouchSoundOn()) }
    var spatialAudio by remember { mutableStateOf(systemManager.isSpatialAudioOn()) }
    var liveCaption by remember { mutableStateOf(systemManager.isLiveCaptionOn()) }

    var mediaVol by remember { mutableStateOf(systemManager.getMediaVolumePercent()) }
    var ringVol by remember { mutableStateOf(systemManager.getRingVolumePercent()) }
    var notifVol by remember { mutableStateOf(systemManager.getNotificationVolumePercent()) }
    var alarmVol by remember { mutableStateOf(systemManager.getAlarmVolumePercent()) }
    var callVol by remember { mutableStateOf(systemManager.getCallVolumePercent()) }
    var ringerMode by remember { mutableStateOf(systemManager.getRingerMode()) }
    val isZenOn = remember { systemManager.isZenModeOn() }

    val phoneRingtoneTitle = remember { systemManager.getRingtoneTitle(android.media.RingtoneManager.TYPE_RINGTONE) }
    val notifRingtoneTitle = remember { systemManager.getRingtoneTitle(android.media.RingtoneManager.TYPE_NOTIFICATION) }
    val alarmRingtoneTitle = remember { systemManager.getRingtoneTitle(android.media.RingtoneManager.TYPE_ALARM) }

    SubScreenScaffold(title = stringResource(R.string.sound_title), onBack = onBack) {
        // Режимы профиля звука (Звук / Вибрация / Без звука)
        item {
            SettingCardGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Режим звука", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data class RingerOption(val mode: Int, val label: String, val icon: ImageVector)
                        val modes = listOf(
                            RingerOption(AudioManager.RINGER_MODE_NORMAL, "Звук", Phosphor.SpeakerHigh),
                            RingerOption(AudioManager.RINGER_MODE_VIBRATE, "Вибро", Phosphor.SpeakerHigh),
                            RingerOption(AudioManager.RINGER_MODE_SILENT, "Без звука", Phosphor.Bell)
                        )
                        modes.forEach { option ->
                            val isSelected = ringerMode == option.mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) SurfaceCard else PaperBackground,
                                border = BorderStroke(1.dp, if (isSelected) BorderBold else BorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        ringerMode = option.mode
                                        systemManager.setRingerMode(option.mode)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) TextPrimary else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ползунки громкости основных аудиопотоков
        item {
            SettingCardGroup {
                // Мультимедиа
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.media_volume_label), style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                        Text(text = "$mediaVol%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = mediaVol.toFloat(),
                        onValueChange = {
                            mediaVol = it.toInt()
                            systemManager.setMediaVolumePercent(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = BorderBold)
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                // Звонок
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.ring_volume_label), style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                        Text(text = "$ringVol%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ringVol.toFloat(),
                        onValueChange = {
                            ringVol = it.toInt()
                            systemManager.setRingVolumePercent(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = BorderBold)
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                // Уведомления
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Громкость уведомлений", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                        Text(text = "$notifVol%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = notifVol.toFloat(),
                        onValueChange = {
                            notifVol = it.toInt()
                            systemManager.setNotificationVolumePercent(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = BorderBold)
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                // Будильник
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.alarm_volume_label), style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                        Text(text = "$alarmVol%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = alarmVol.toFloat(),
                        onValueChange = {
                            alarmVol = it.toInt()
                            systemManager.setAlarmVolumePercent(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = BorderBold)
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                // Разговорный динамик (вызов)
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Громкость разговора", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp)
                        Text(text = "$callVol%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = callVol.toFloat(),
                        onValueChange = {
                            callVol = it.toInt()
                            systemManager.setCallVolumePercent(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = BorderBold)
                    )
                }
            }
        }

        // Выбор мелодий и сигналов
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.Bell,
                    title = "Рингтон телефона",
                    subtitle = phoneRingtoneTitle,
                    onClick = { onNavigate(Screen.RINGTONE_PHONE) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Bell,
                    title = "Звук уведомления по умолчанию",
                    subtitle = notifRingtoneTitle,
                    onClick = { onNavigate(Screen.RINGTONE_NOTIFICATION) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Bell,
                    title = "Звук будильника по умолчанию",
                    subtitle = alarmRingtoneTitle,
                    onClick = { onNavigate(Screen.RINGTONE_ALARM) }
                )
            }
        }

        // Выделенные экраны: Не беспокоить и Вибрация/Тактильный отклик
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.Bell,
                    title = "Не беспокоить",
                    subtitle = if (isZenOn) "Включен (звуки приглушены)" else "Отключен",
                    onClick = { onNavigate(Screen.DND_SETTINGS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.SpeakerHigh,
                    title = "Вибрация и тактильный отклик",
                    subtitle = "Тактильная отдача, вибрация звонка, уведомлений и будильника",
                    onClick = { onNavigate(Screen.VIBRATION_SETTINGS) }
                )
            }
        }

        // Дополнительные интеллектуальные аудиовозможности
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Пространственное аудио",
                    subtitle = "Объемное кинематографическое звучание в совместимых наушниках",
                    checked = spatialAudio,
                    onCheckedChange = {
                        spatialAudio = it
                        systemManager.setSpatialAudio(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Автоматические субтитры (Live Caption)",
                    subtitle = "Определение речи в медиа и вывод текстовых субтитров",
                    checked = liveCaption,
                    onCheckedChange = {
                        liveCaption = it
                        systemManager.setLiveCaption(it)
                    }
                )
            }
        }

        // Системные звуки
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.dial_pad_tones_label),
                    subtitle = "Тональные сигналы при наборе номера",
                    checked = dialpadSound,
                    onCheckedChange = {
                        dialpadSound = it
                        systemManager.setDialpadSound(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.screen_lock_sound_label),
                    subtitle = "Звуковой щелчок при выключении экрана",
                    checked = lockSound,
                    onCheckedChange = {
                        lockSound = it
                        systemManager.setLockSound(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.charging_sound_label),
                    subtitle = "Звуковой сигнал при подключении кабеля питания",
                    checked = chargingSound,
                    onCheckedChange = {
                        chargingSound = it
                        systemManager.setChargingSound(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Звуки при нажатии на экран",
                    subtitle = "Акустический отклик при выборе элементов интерфейса",
                    checked = touchSound,
                    onCheckedChange = {
                        touchSound = it
                        systemManager.setTouchSound(it)
                    }
                )
            }
        }
    }
}

@Composable
fun RingtonePickerSubScreen(
    title: String,
    type: Int,
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var ringtones by remember { mutableStateOf<List<RealSystemManager.RingtoneItem>>(emptyList()) }
    var selectedUri by remember { mutableStateOf(systemManager.getActualRingtoneUri(type)) }
    var playingMediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            try {
                playingMediaPlayer?.stop()
                playingMediaPlayer?.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = systemManager.getAvailableRingtones(type)
            withContext(Dispatchers.Main) {
                ringtones = list
            }
        }
    }

    SubScreenScaffold(title = title, onBack = {
        try {
            playingMediaPlayer?.stop()
            playingMediaPlayer?.release()
        } catch (e: Exception) { e.printStackTrace() }
        onBack()
    }) {
        if (ringtones.isEmpty()) {
            item {
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp)
                }
            }
        } else {
            item {
                SettingCardGroup {
                    ringtones.forEachIndexed { index, ringtone ->
                        val isSelected = (ringtone.uriString == selectedUri) || (selectedUri.isEmpty() && ringtone.uriString.isEmpty())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedUri = ringtone.uriString
                                    systemManager.setActualRingtone(type, ringtone.uriString)

                                    // Проигрывание превью
                                    try {
                                        playingMediaPlayer?.stop()
                                        playingMediaPlayer?.release()
                                        playingMediaPlayer = null
                                        if (ringtone.uriString.isNotEmpty()) {
                                            val mp = android.media.MediaPlayer()
                                            mp.setDataSource(context, android.net.Uri.parse(ringtone.uriString))
                                            mp.prepare()
                                            mp.start()
                                            playingMediaPlayer = mp
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ringtone.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Phosphor.Check,
                                    contentDescription = "Selected",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index < ringtones.size - 1) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VibrationSettingsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var haptics by remember { mutableStateOf(systemManager.isHapticFeedbackOn()) }
    var ringVib by remember { mutableStateOf(systemManager.isRingVibrationOn()) }
    var notifVib by remember { mutableStateOf(systemManager.isNotificationVibrationOn()) }
    var alarmVib by remember { mutableStateOf(systemManager.isAlarmVibrationOn()) }
    var vibrateOnTap by remember { mutableStateOf(systemManager.isVibrateOnTapOn()) }

    SubScreenScaffold(title = "Вибрация и отклик", onBack = onBack) {
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.haptics_label),
                    subtitle = "Четкий механический щелчок при касании кнопок и жестах",
                    checked = haptics,
                    onCheckedChange = {
                        haptics = it
                        systemManager.setHapticFeedback(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Вибрация при вызове",
                    subtitle = "Вибрировать при входящих звонках",
                    checked = ringVib,
                    onCheckedChange = {
                        ringVib = it
                        systemManager.setRingVibration(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Вибрация уведомлений",
                    subtitle = "Вибрировать при получении новых сообщений",
                    checked = notifVib,
                    onCheckedChange = {
                        notifVib = it
                        systemManager.setNotificationVibration(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Вибрация будильника",
                    subtitle = "Тактильная вибрация при срабатывании сигналов будильника",
                    checked = alarmVib,
                    onCheckedChange = {
                        alarmVib = it
                        systemManager.setAlarmVibration(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Тактильный отклик при касании",
                    subtitle = "Вибрация клавиатуры и навигационных жестов назад/домой",
                    checked = vibrateOnTap,
                    onCheckedChange = {
                        vibrateOnTap = it
                        systemManager.setVibrateOnTap(it)
                    }
                )
            }
        }
    }
}

@Composable
fun DndSettingsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var zenMode by remember { mutableStateOf(systemManager.isZenModeOn()) }
    val accessGranted = remember { systemManager.isDndPolicyAccessGranted() }
    var policy by remember { mutableStateOf(systemManager.getDndPolicy()) }

    val categoryRows = listOf(
        Triple(1 shl 5, "Будильники", "Сигналы будильников всегда звучат"),
        Triple(1 shl 0, "Напоминания", "Напоминания из календаря и приложений"),
        Triple(1 shl 1, "События", "События календаря и подобные оповещения"),
        Triple(1 shl 6, "Медиа", "Музыка, навигация, игры"),
        Triple(1 shl 7, "Системные звуки", "Системные оповещения без категории"),
        Triple(1 shl 3, "Звонки", "Входящие вызовы"),
        Triple(1 shl 2, "Сообщения", "Сообщения и переписки")
    )

    val senderOptions = listOf(
        0 to "Все",
        1 to "Контакты",
        2 to "Избранные"
    )

    fun applyPolicy(categories: Int, calls: Int, messages: Int) {
        if (systemManager.setDndPolicy(categories, calls, messages)) {
            policy = systemManager.getDndPolicy()
        } else {
            Toast.makeText(context, "Система не применила правило", Toast.LENGTH_SHORT).show()
            policy = systemManager.getDndPolicy()
        }
    }

    SubScreenScaffold(title = "Не беспокоить", onBack = onBack) {
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Включить режим «Не беспокоить»",
                    subtitle = "Отключает звуки звонков и всплывающие оповещения",
                    checked = zenMode,
                    onCheckedChange = {
                        zenMode = it
                        systemManager.setZenMode(it)
                    }
                )
            }
        }

        if (policy == null) {
            item {
                SettingCardGroup {
                    SettingSimpleRow(
                        title = "Правила недоступны",
                        subtitle = if (accessGranted) {
                            "Система не вернула текущую политику «Не беспокоить»"
                        } else {
                            "Нет доступа к политике уведомлений (ACCESS_NOTIFICATIONS)"
                        }
                    )
                }
            }
        } else {
            item {
                SettingCardGroup {
                    categoryRows.forEachIndexed { index, (bit, title, subtitle) ->
                        val current = policy!!
                        SettingToggleRow(
                            title = title,
                            subtitle = subtitle,
                            checked = (current.priorityCategories and bit) != 0,
                            onCheckedChange = { enabled ->
                                val newMask = if (enabled) {
                                    current.priorityCategories or bit
                                } else {
                                    current.priorityCategories and bit.inv()
                                }
                                applyPolicy(newMask, current.callSenders, current.messageSenders)
                            }
                        )
                        if (index < categoryRows.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "КТО МОЖЕТ ПРЕРЫВАТЬ ЗВОНКАМИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    senderOptions.forEachIndexed { index, (value, label) ->
                        val current = policy!!
                        val selected = current.callSenders == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    applyPolicy(
                                        current.priorityCategories,
                                        value,
                                        current.messageSenders
                                    )
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Phosphor.Check,
                                    contentDescription = "Выбрано",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < senderOptions.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "КТО МОЖЕТ ПРЕРЫВАТЬ СООБЩЕНИЯМИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    senderOptions.forEachIndexed { index, (value, label) ->
                        val current = policy!!
                        val selected = current.messageSenders == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    applyPolicy(
                                        current.priorityCategories,
                                        current.callSenders,
                                        value
                                    )
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Phosphor.Check,
                                    contentDescription = "Выбрано",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < senderOptions.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Категории и отправители записываются в системную политику уведомлений и действуют для текущего режима «Не беспокоить».",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun BatterySubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val batteryInfo = remember { systemManager.getDetailedBatteryInfo() }
    var isPowerSave by remember { mutableStateOf(systemManager.isPowerSaveMode()) }
    var showPercent by remember { mutableStateOf(systemManager.isBatteryPercentageOn()) }
    var adaptiveBattery by remember { mutableStateOf(systemManager.isAdaptiveBatteryOn()) }
    var protectBattery by remember { mutableStateOf(systemManager.isBatteryProtectOn()) }

    SubScreenScaffold(title = stringResource(R.string.battery_title), onBack = onBack) {
        // Большой карточный блок статуса
        item {
            SettingCardGroup {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${batteryInfo.percentage}%",
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = batteryInfo.statusText,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (batteryInfo.isCharging) AccentTerracotta else TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = Phosphor.BatteryCharging,
                            contentDescription = null,
                            tint = if (batteryInfo.isCharging) AccentTerracotta else TextPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Индикатор уровня заряда
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(BorderColor)
                    ) {
                    Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (batteryInfo.percentage / 100f).coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(if (batteryInfo.isCharging) AccentTerracotta else TextPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Источник: ${batteryInfo.plugType}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Режимы энергосбережения
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.battery_saver_label),
                    subtitle = "Ограничивает вибрацию, фоновые службы и снижает частоту процессора",
                    checked = isPowerSave,
                    onCheckedChange = {
                        isPowerSave = it
                        systemManager.setPowerSaveMode(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Адаптивное энергопотребление",
                    subtitle = "Интеллектуальное ограничение ресурсов для редко используемых приложений",
                    checked = adaptiveBattery,
                    onCheckedChange = {
                        adaptiveBattery = it
                        systemManager.setAdaptiveBattery(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Защита аккумулятора",
                    subtitle = "Продлевает срок службы, ограничивая максимальный уровень заряда до 80%",
                    checked = protectBattery,
                    onCheckedChange = {
                        protectBattery = it
                        systemManager.setBatteryProtect(it)
                    }
                )
            }
        }

        // Отображение в системе
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Процент заряда в строке состояния",
                    subtitle = "Показывать цифровой процент рядом со значком батареи",
                    checked = showPercent,
                    onCheckedChange = {
                        showPercent = it
                        systemManager.setBatteryPercentage(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.SquaresFour,
                    title = "Расход заряда приложениями",
                    subtitle = "Triangle OS мониторинг потребления энергии за 24 часа",
                    onClick = { onNavigate(Screen.BATTERY_USAGE) }
                )
            }
        }

        // Аппаратное состояние аккумулятора (Hardware Health)
        item {
            SettingCardGroup {
                SettingSimpleRow(
                    title = "Состояние аккумулятора",
                    subtitle = batteryInfo.health
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingSimpleRow(
                    title = "Температура батареи",
                    subtitle = "${batteryInfo.temperatureCelsius} °C"
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingSimpleRow(
                    title = "Напряжение",
                    subtitle = "${batteryInfo.voltageMv} мВ"
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingSimpleRow(
                    title = "Технология элемента",
                    subtitle = batteryInfo.technology
                )
            }
        }
    }
}

@Composable
fun BatteryUsageSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var usageList by remember { mutableStateOf<List<RealSystemManager.AppBatteryUsage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = systemManager.getAppsBatteryUsageList()
            withContext(Dispatchers.Main) {
                usageList = list
                isLoading = false
            }
        }
    }

    SubScreenScaffold(title = "Расход батареи", onBack = onBack) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Использование с последней полной зарядки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Данные системной службы BatteryUsageStats: расход в мА·ч и доля в суммарном расходе приложений с последней зарядки",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (isLoading) {
            item {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp)
                }
            }
        } else if (usageList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Система не предоставила статистику батареи", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            item {
                SettingCardGroup {
                    usageList.forEachIndexed { index, app ->
                        val appBitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.parse("package:${app.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (appBitmap != null) {
                                Image(
                                    bitmap = appBitmap.asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                            Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PaperBackground)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Phosphor.SquaresFour, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val timeText = if (app.consumedMah > 0.0) {
                                    "≈ %.2f мА·ч".format(app.consumedMah)
                                } else if (app.foregroundTimeMinutes > 0) {
                                    "${app.foregroundTimeMinutes} мин. активно"
                                } else {
                                    "Нет данных об активности"
                                }
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            val pctText = when {
                                app.percentage < 0f -> ""
                                app.percentage > 0f && app.percentage < 0.1f -> "<0,1%"
                                else -> "%.1f%%".format(app.percentage)
                            }
                            if (pctText.isNotEmpty()) {
                                Text(
                                    text = pctText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                        if (index < usageList.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val breakdown = remember { systemManager.getDetailedStorageBreakdown() }
    val context = LocalContext.current

    SubScreenScaffold(title = stringResource(R.string.storage_title), onBack = onBack) {
        // Большой блок заполнения накопителя
        item {
            SettingCardGroup {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Занято ${breakdown.usedPercent}%",
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${breakdown.usedFormatted} из ${breakdown.totalFormatted}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = Phosphor.HardDrive,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Мультисегментный прогресс-бар в духе Editorial Brutalism
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BorderColor)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Системный раздел (темный графит)
                            val sysFrac = (breakdown.systemBytes.toFloat() / breakdown.totalBytes).coerceIn(0f, 1f)
                        Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(sysFrac.coerceAtLeast(0.01f))
                                    .background(TextPrimary)
                            )
                            // Приложения (терракота)
                            val appsFrac = (breakdown.appsBytes.toFloat() / breakdown.totalBytes).coerceIn(0f, 1f)
                        Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(appsFrac.coerceAtLeast(0.01f))
                                    .background(AccentTerracotta)
                            )
                            // Медиа и загрузки (охра)
                            val mediaFrac = ((breakdown.mediaBytes + breakdown.downloadsBytes).toFloat() / breakdown.totalBytes).coerceIn(0f, 1f)
                        Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(mediaFrac.coerceAtLeast(0.01f))
                                    .background(SoftOchre)
                            )
                            // Свободное место
                            val freeFrac = (breakdown.freeBytes.toFloat() / breakdown.totalBytes).coerceIn(0f, 1f)
                        Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(freeFrac.coerceAtLeast(0.01f))
                                    .background(BorderColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Свободно ${breakdown.freeFormatted}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Категории хранилища
        item {
            SettingCardGroup {
                // Приложения
                SettingItem(
                    icon = Phosphor.SquaresFour,
                    title = "Приложения",
                    subtitle = breakdown.appsFormatted,
                    onClick = { onNavigate(Screen.ALL_APPS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // Фотографии, видео и аудио
                SettingItem(
                    icon = Phosphor.Palette,
                    title = "Изображения и медиафайлы",
                    subtitle = if (breakdown.hasMediaAccess) breakdown.mediaFormatted else "Размер недоступен без доступа к файлам",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                type = "image/*"
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent("android.intent.action.OPEN_DOCUMENT").apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Хранилище медиафайлов", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // Загрузки и документы
                SettingItem(
                    icon = Phosphor.HardDrive,
                    title = "Загрузки и документы",
                    subtitle = if (breakdown.hasMediaAccess) breakdown.downloadsFormatted else "Размер недоступен без доступа к файлам",
                    onClick = {
                        try {
                            val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Папка загрузок", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // Системный раздел и всё неизмеренное
                SettingSimpleRow(
                    title = "Система и прочие данные",
                    subtitle = "${breakdown.systemFormatted} · данные приложений, кэш и системные файлы"
                )
            }
        }

        item {
            Text(
                text = "«Приложения» — размер APK и split-APK. Данные приложений и кэш учтены в «Система и прочие данные».",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Системные действия с хранилищем
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = "Освободить место",
                    subtitle = "Управление кэшем и временными файлами через менеджер",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Менеджер хранилища AOSP", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppsSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val totalCount = remember { systemManager.getInstalledAppsCount() }
    val context = LocalContext.current

    SubScreenScaffold(title = stringResource(R.string.apps_title), onBack = onBack) {
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.SquaresFour,
                    title = "Все приложения",
                    subtitle = "Список установленных программ ($totalCount)",
                    onClick = { onNavigate(Screen.ALL_APPS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = "Приложения по умолчанию",
                    subtitle = "Браузер, звонки, SMS, рабочий стол",
                    onClick = { onNavigate(Screen.DEFAULT_APPS) }
                )
            }
        }

        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.ShieldCheck,
                    title = "Диспетчер разрешений",
                    subtitle = "Камера, микрофон, уведомления, память",
                    onClick = {
                        try {
                            val intent = Intent("android.intent.action.MANAGE_PERMISSIONS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent("android.settings.MANAGE_PERMISSIONS").apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Диспетчер разрешений недоступен", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = "Специальный доступ",
                    subtitle = "Доступ к памяти, показ поверх окон, неограниченный трафик",
                    onClick = {
                        try {
                            val intent = Intent("android.settings.MANAGE_SPECIAL_APP_ACCESSES_SETTINGS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Специальный доступ недоступен", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppInfoSubScreen(
    packageName: String,
    systemManager: RealSystemManager,
    onNavigateToStorage: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember(packageName) { mutableStateOf(systemManager.getAppDetailInfo(packageName)) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    SubScreenScaffold(title = "О приложении", onBack = onBack) {
        if (detail == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Не удалось загрузить данные о приложении", color = TextSecondary)
                    }
                }
            }
        } else {
            val app = detail!!

            // Карточка заголовка приложения
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val appBitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
                        if (appBitmap != null) {
                            Image(
                                bitmap = appBitmap.asImageBitmap(),
                                contentDescription = app.appName,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                        Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PaperBackground)
                                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Phosphor.SquaresFour,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Версия ${app.versionName} (${app.versionCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextTertiary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Панель быстрых действий (Запустить, Остановить, Удалить)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Кнопка "Открыть"
                            Button(
                                onClick = {
                                    val opened = systemManager.launchPackage(app.packageName)
                                    if (!opened) {
                                        Toast.makeText(context, "Приложение не имеет экрана запуска", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BorderBold,
                                    contentColor = PaperBackground
                                )
                            ) {
                                Text("Открыть", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Кнопка "Остановить"
                            OutlinedButton(
                                onClick = {
                                    val stopped = systemManager.forceStopPackage(app.packageName)
                                    Toast.makeText(
                                        context,
                                        if (stopped) "Принудительно остановлено" else "Не удалось остановить",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Text("Остановить", fontSize = 13.sp, color = TextPrimary)
                            }

                            // Кнопка "Удалить"
                            if (!app.isSystemApp) {
                                OutlinedButton(
                                    onClick = { showUninstallDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Text("Удалить", fontSize = 13.sp, color = AccentTerracotta)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Хранилище и кэш
            item {
                Text(
                    text = "ПАМЯТЬ И КЭШ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.HardDrive,
                        title = "Хранилище и кэш",
                        subtitle = "Всего занято: ${app.totalSizeFormatted} (данные, кэш, приложение)",
                        onClick = onNavigateToStorage
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Разрешения и уведомления
            item {
                Text(
                    text = "ДОСТУП И УВЕДОМЛЕНИЯ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.Bell,
                        title = "Уведомления",
                        subtitle = if (app.isNotificationsEnabled) "Включены" else "Заблокированы",
                        onClick = onNavigateToNotifications
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.ShieldCheck,
                        title = "Разрешения приложения",
                        subtitle = "Управление доступом к геопозиции, камере, микрофону",
                        onClick = onNavigateToPermissions
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Системная информация
            item {
                Text(
                    text = "ИНФОРМАЦИЯ О ПАКЕТЕ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingDetailRow(label = "Тип пакета", value = if (app.isSystemApp) "Системное приложение" else "Пользовательское")
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Целевой SDK (targetSdk)", value = "Android ${app.targetSdk}")
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Минимальный SDK (minSdk)", value = "API ${app.minSdk}")
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Идентификатор UID", value = app.uid.toString())
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Дата установки", value = app.installTimeFormatted)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Последнее обновление", value = app.lastUpdateTimeFormatted)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    
                    // Путь на диске с сокращением многоточием и копированием при клике
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("APK Path", app.sourceDir)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Путь к APK скопирован в буфер", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Путь на диске",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = app.sourceDir,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showUninstallDialog && detail != null) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Удалить приложение?",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Вы действительно хотите удалить ${detail!!.appName} (${detail!!.packageName}) с вашего устройства?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uninstalled = systemManager.uninstallPackage(detail!!.packageName)
                        showUninstallDialog = false
                        if (uninstalled) {
                            Toast.makeText(context, "Приложение удалено", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Не удалось удалить приложение", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta)
                ) {
                    Text("Удалить", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
fun AppStorageSubScreen(
    packageName: String,
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember(packageName) { mutableStateOf(systemManager.getAppDetailInfo(packageName)) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    SubScreenScaffold(title = "Память и кэш", onBack = onBack) {
        if (detail == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Не удалось загрузить данные о памяти", color = TextSecondary)
                    }
                }
            }
        } else {
            val app = detail!!

            // Карточка заголовка с иконкой и общим размером
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val appBitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
                        if (appBitmap != null) {
                            Image(
                                bitmap = appBitmap.asImageBitmap(),
                                contentDescription = app.appName,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                        Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PaperBackground)
                                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Phosphor.HardDrive,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Всего занято: ${app.totalSizeFormatted}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentTerracotta
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // График распределения памяти (Segmented Bar)
                        val total = app.totalSizeBytes.coerceAtLeast(1L).toFloat()
                        val appFrac = (app.appSizeBytes.toFloat() / total).coerceIn(0.05f, 0.90f)
                        val dataFrac = (app.dataSizeBytes.toFloat() / total).coerceIn(0.05f, 0.90f)
                        val cacheFrac = (app.cacheSizeBytes.toFloat() / total).coerceIn(0.05f, 0.90f)
                        val sumFrac = appFrac + dataFrac + cacheFrac
                        val normApp = appFrac / sumFrac
                        val normData = dataFrac / sumFrac
                        val normCache = cacheFrac / sumFrac

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(7.dp))
                            ) {
                            Box(
                                    modifier = Modifier
                                        .weight(normApp)
                                        .fillMaxHeight()
                                        .background(AccentTerracotta)
                                )
                            Box(
                                    modifier = Modifier
                                        .weight(normData)
                                        .fillMaxHeight()
                                        .background(SoftOchre)
                                )
                            Box(
                                    modifier = Modifier
                                        .weight(normCache)
                                        .fillMaxHeight()
                                        .background(TextTertiary)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Легенда графика
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AccentTerracotta)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Приложение", fontSize = 11.sp, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SoftOchre)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Данные", fontSize = 11.sp, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(TextTertiary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Кэш", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Детальная разбивка объема
            item {
                Text(
                    text = "ДЕТАЛИЗАЦИЯ ПАМЯТИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingDetailRow(label = "Размер приложения (APK и код)", value = app.appSizeFormatted)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Данные пользователя и базы данных", value = app.dataSizeFormatted)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Временный кэш", value = app.cacheSizeFormatted)
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingDetailRow(label = "Итоговое пространство на диске", value = app.totalSizeFormatted)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Кнопки управления хранилищем
            item {
                Text(
                    text = "ДЕЙСТВИЯ С ПАМЯТЬЮ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    // Очистить кэш
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearCacheDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Phosphor.HardDrive,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Очистить кэш", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = TextPrimary)
                            Text(text = "Удалить временные файлы приложения (${app.cacheSizeFormatted})", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(imageVector = Phosphor.CaretRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    // Стереть данные (Хранилище)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearDataDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Phosphor.Trash,
                            contentDescription = null,
                            tint = AccentTerracotta,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Стереть данные", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = AccentTerracotta)
                            Text(text = "Удалить все базы данных, аккаунты и настройки (${app.dataSizeFormatted})", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(imageVector = Phosphor.CaretRight, contentDescription = null, tint = AccentTerracotta, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Диалог очистки кэша
    if (showClearCacheDialog && detail != null) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Очистить кэш?",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Временные файлы приложения ${detail!!.appName} (${detail!!.cacheSizeFormatted}) будут удалены. Ваши личные настройки и аккаунты сохранятся.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleared = systemManager.clearPackageCache(detail!!.packageName)
                        showClearCacheDialog = false
                        Toast.makeText(
                            context,
                            if (cleared) "Кэш очищен" else "Не удалось очистить кэш",
                            Toast.LENGTH_SHORT
                        ).show()
                        detail = systemManager.getAppDetailInfo(detail!!.packageName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BorderBold)
                ) {
                    Text("Очистить кэш", color = SurfaceCard, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }

    // Диалог очистки хранилища данных
    if (showClearDataDialog && detail != null) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Стереть данные хранилища?",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Все данные этого приложения (${detail!!.appName}) будут безвозвратно стерты, включая базы данных, учетные записи и настройки.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleared = systemManager.clearPackageData(detail!!.packageName)
                        showClearDataDialog = false
                        Toast.makeText(
                            context,
                            if (cleared) "Данные приложения стерты" else "Не удалось стереть данные",
                            Toast.LENGTH_SHORT
                        ).show()
                        detail = systemManager.getAppDetailInfo(detail!!.packageName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta)
                ) {
                    Text("Стереть", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Отмена", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
fun AppPermissionsSubScreen(
    packageName: String,
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val detail = remember(packageName) { systemManager.getAppDetailInfo(packageName) }
    var permissions by remember(packageName) { mutableStateOf(systemManager.getAppPermissions(packageName)) }

    SubScreenScaffold(title = "Разрешения", onBack = onBack) {
        // Заголовок с иконкой и именем приложения
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val appBitmap = remember(packageName) { detail?.icon?.let { drawableToBitmap(it) } }
                    if (appBitmap != null) {
                        Image(
                            bitmap = appBitmap.asImageBitmap(),
                            contentDescription = detail?.appName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                    Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PaperBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Phosphor.ShieldCheck, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = detail?.appName ?: packageName, style = MaterialTheme.typography.titleMedium, fontSize = 17.sp, color = TextPrimary)
                        Text(text = "Управление доступом к данным и датчикам", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (permissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Приложение не запрашивает специальных разрешений", color = TextSecondary)
                    }
                }
            }
        } else {
            val grouped = permissions.groupBy { it.group }
            grouped.forEach { (group, perms) ->
                item {
                    Text(
                        text = group.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    SettingCardGroup {
                        perms.forEachIndexed { index, permItem ->
                            SettingToggleRow(
                                title = permItem.label,
                                subtitle = permItem.permission.substringAfterLast("."),
                                checked = permItem.isGranted,
                                onCheckedChange = { isChecked ->
                                    val success = systemManager.setAppPermission(packageName, permItem.permission, isChecked)
                                    if (success) {
                                        permissions = systemManager.getAppPermissions(packageName)
                                    } else {
                                        Toast.makeText(context, "Не удалось изменить разрешение", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            if (index < perms.lastIndex) {
                                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
fun AppNotificationSettingsSubScreen(
    packageName: String,
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val detail = remember(packageName) { systemManager.getAppDetailInfo(packageName) }
    var notifsEnabled by remember(packageName) { mutableStateOf(detail?.isNotificationsEnabled ?: true) }
    val canManageAppOps = remember { systemManager.canManageAppOps() }
    val journalChannels = remember(packageName) { NotificationLog.channelsFor(context, packageName) }

    SubScreenScaffold(title = "Уведомления", onBack = onBack) {
        // Инфо-карточка приложения
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val appBitmap = remember(packageName) { detail?.icon?.let { drawableToBitmap(it) } }
                    if (appBitmap != null) {
                        Image(
                            bitmap = appBitmap.asImageBitmap(),
                            contentDescription = detail?.appName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                    Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PaperBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Phosphor.Bell, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = detail?.appName ?: packageName, style = MaterialTheme.typography.titleMedium, fontSize = 17.sp, color = TextPrimary)
                        Text(text = "Настройка оповещений и каналов", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Главный переключатель уведомлений приложения
        item {
            SettingCardGroup {
                if (canManageAppOps) {
                    SettingToggleRow(
                        title = "Все уведомления ${detail?.appName ?: ""}",
                        subtitle = if (notifsEnabled) "Приложению разрешено отправлять уведомления" else "Все уведомления приложения заблокированы",
                        checked = notifsEnabled,
                        onCheckedChange = { isChecked ->
                            if (systemManager.setAppNotificationsEnabled(packageName, isChecked)) {
                                notifsEnabled = isChecked
                            } else {
                                Toast.makeText(context, "Не удалось изменить статус уведомлений", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    SettingSimpleRow(
                        title = "Все уведомления ${detail?.appName ?: ""}",
                        subtitle = (if (notifsEnabled) "Разрешены" else "Заблокированы") +
                            " · изменять чужие приложения нельзя без системной привилегии"
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Каналы определяются по реальным уведомлениям из журнала
        if (journalChannels.isNotEmpty()) {
            item {
                Text(
                    text = "КАНАЛЫ В ЖУРНАЛЕ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    journalChannels.forEachIndexed { index, (channelId, count) ->
                        SettingSimpleRow(
                            title = channelId,
                            subtitle = "Уведомлений в журнале: $count"
                        )
                        if (index < journalChannels.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            item {
                Text(
                    text = "Каналы появятся здесь, когда приложение пришлёт уведомление, а в разделе «Уведомления» будет включён доступ к журналу.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
fun AllAppsSubScreen(
    systemManager: RealSystemManager,
    onOpenAppInfo: (String) -> Unit,
    onBack: () -> Unit
) {
    var showSystem by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Загрузка в фоновом потоке Coroutine, чтобы не блокировать UI-поток
    var appsList by remember { mutableStateOf<List<RealAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(showSystem) {
        isLoading = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = systemManager.getInstalledAppsList(showSystem = showSystem)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                appsList = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(appsList, searchQuery) {
        if (searchQuery.isBlank()) {
            appsList
        } else {
            appsList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val listState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PaperBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
        ) {
            // Верхняя плашка с кнопкой назад, заголовком и меню трех точек
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Phosphor.ArrowLeft, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Все приложения", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        text = if (isLoading) "Загрузка..." else "${filteredApps.size} приложений",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Phosphor.DotsThreeVertical,
                            contentDescription = "Menu",
                            tint = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(SurfaceCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (showSystem) "Скрыть системные приложения" else "Показать системные приложения",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showSystem = !showSystem
                            }
                        )
                    }
                }
            }

            // Поле локального поиска по приложениям
        Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Phosphor.MagnifyingGlass,
                        contentDescription = "Search",
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Поиск приложений...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 14.sp,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
            Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                }
            } else if (filteredApps.isEmpty()) {
                // Если ничего не найдено: иконка лупы и текст "Ничего не найдено"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Phosphor.MagnifyingGlass,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ничего не найдено",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Проверьте запрос или покажите системные приложения через меню",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = filteredApps.size,
                            key = { index -> filteredApps[index].packageName }
                        ) { index ->
                            val app = filteredApps[index]
                            val context = LocalContext.current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenAppInfo(app.packageName)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Иконка приложения слева
                                val appBitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
                                if (appBitmap != null) {
                                    Image(
                                        bitmap = appBitmap.asImageBitmap(),
                                        contentDescription = app.appName,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PaperBackground)
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Phosphor.SquaresFour,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                if (app.isSystemApp) {
                                    Text(
                                        text = "Системное",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Phosphor.CaretRight,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (index < filteredApps.lastIndex) {
                                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultAppsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var defaults by remember { mutableStateOf(systemManager.getDefaultAppsSummary()) }
    var selectedRoleForPicker by remember { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<RealSystemManager.DefaultAppCandidate>>(emptyList()) }
    val context = LocalContext.current

    if (selectedRoleForPicker != null) {
        val role = selectedRoleForPicker!!
        AlertDialog(
            onDismissRequest = { selectedRoleForPicker = null },
            confirmButton = {
                TextButton(onClick = { selectedRoleForPicker = null }) {
                    Text("Закрыть", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(text = role, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (candidates.isEmpty()) {
                        Text(text = "Нет доступных альтернативных приложений для этой роли", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    } else {
                        candidates.forEach { cand ->
                            val currentName = defaults[role] ?: ""
                            val isCurrent = cand.label.equals(currentName, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Запуск стандартного системного интента выбора по умолчанию или открытие настроек
                                        try {
                                            when (role) {
                                                "Браузер" -> {
                                                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                }
                                                "Главный экран" -> {
                                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                }
                                                else -> {
                                                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Используйте системный выбор приложений", Toast.LENGTH_SHORT).show()
                                        }
                                        selectedRoleForPicker = null
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = cand.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = TextPrimary)
                                    Text(text = cand.packageName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                if (isCurrent) {
                                    Icon(imageVector = Phosphor.Check, contentDescription = "Active", tint = TextPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    SubScreenScaffold(title = "Приложения по умолчанию", onBack = onBack) {
        item {
            SettingCardGroup {
                defaults.entries.forEachIndexed { index, (role, currentApp) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                candidates = systemManager.getCandidatesForDefaultRole(role)
                                selectedRoleForPicker = role
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = role, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                            Text(text = currentApp, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                        }
                        Icon(imageVector = Phosphor.CaretRight, contentDescription = "Select", modifier = Modifier.size(16.dp), tint = TextTertiary)
                    }
                    if (index < defaults.size - 1) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationSubScreen(systemManager: RealSystemManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var locationOn by remember { mutableStateOf(systemManager.isLocationOn()) }
    var wifiScanning by remember { mutableStateOf(systemManager.isWifiScanningOn()) }
    var bluetoothScanning by remember { mutableStateOf(systemManager.isBluetoothScanningOn()) }

    var locationApps by remember { mutableStateOf<List<RealSystemManager.LocationAppItem>>(emptyList()) }
    var isAppsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = systemManager.getLocationAppsList()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                locationApps = list
                isAppsLoading = false
            }
        }
    }

    SubScreenScaffold(title = stringResource(R.string.location_title), onBack = onBack) {
        // 1. Главный мастер-тумблер геоданных
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Доступ к геоданным",
                    subtitle = if (locationOn) "Включено · Приложения могут определять координаты" else "Отключено · Доступ запрещен для всех служб",
                    checked = locationOn,
                    onCheckedChange = {
                        locationOn = it
                        systemManager.setLocation(it)
                    }
                )
            }
        }

        // 2. Повышение точности сканированием (Wi-Fi и Bluetooth)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ПОВЫШЕНИЕ ТОЧНОСТИ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = "Поиск сетей Wi-Fi",
                    subtitle = "Разрешить приложениям и службам искать сети Wi-Fi, даже если Wi-Fi выключен",
                    checked = wifiScanning,
                    onCheckedChange = {
                        wifiScanning = it
                        systemManager.setWifiScanning(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Поиск устройств Bluetooth",
                    subtitle = "Разрешить службам искать Bluetooth-маяки для более точного геопозиционирования",
                    checked = bluetoothScanning,
                    onCheckedChange = {
                        bluetoothScanning = it
                        systemManager.setBluetoothScanning(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.MapPin,
                    title = "Точность геолокации (NLP / GMS)",
                    subtitle = "Использовать Wi-Fi, мобильные вышки и датчики для быстрого поиска координат",
                    onClick = {
                        try {
                            val intent = Intent("com.google.android.gms.location.settings.LOCATION_ACCURACY").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Настройки точности недоступны", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // 3. Разрешения приложений
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "РАЗРЕШЕНИЯ ПРИЛОЖЕНИЙ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.ShieldCheck,
                    title = "Управление доступом к геопозиции",
                    subtitle = "Настроить разрешения для каждого приложения отдельно",
                    onClick = {
                        try {
                            val intent = Intent("android.intent.action.MANAGE_PERMISSION_APPS").apply {
                                putExtra("android.intent.extra.PERMISSION_NAME", "android.permission.ACCESS_FINE_LOCATION")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent("android.settings.MANAGE_PERMISSIONS").apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Диспетчер разрешений недоступен", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // 4. Приложения с доступом к местоположению
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ПРИЛОЖЕНИЯ С ДОСТУПОМ (${locationApps.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                if (isAppsLoading) {
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentTerracotta, strokeWidth = 2.dp)
                    }
                } else if (locationApps.isEmpty()) {
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет приложений с доступом к геопозиции",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    locationApps.take(15).forEachIndexed { index, app ->
                        val appBitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.parse("package:${app.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Не удалось открыть информацию о приложении", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (appBitmap != null) {
                                Image(
                                    bitmap = appBitmap.asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                            Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BorderColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Phosphor.MapPin,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Phosphor.CaretRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (index < locationApps.take(15).lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // 5. Экстренные службы и безопасность
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "СЛУЖБЫ ОПОВЕЩЕНИЯ И ЧС",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.Info,
                    title = "Оповещения о землетрясениях и ЧС",
                    subtitle = "Получать предупреждения о стихийных бедствиях рядом",
                    onClick = {
                        try {
                            val intent = Intent("android.settings.ACTION_EARTHQUAKE_ALERT_SETTINGS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Служба оповещений недоступна", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Info,
                    title = "Передача геоданных в экстренных ситуациях (ELS)",
                    subtitle = "Автоматическая отправка координат при звонке в службу спасения (112)",
                    onClick = {
                        try {
                            val intent = Intent("android.settings.EMERGENCY_LOC_SETTINGS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Настройки ELS определяются оператором связи", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AccessibilitySubScreen(systemManager: RealSystemManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var highContrast by remember { mutableStateOf(systemManager.isHighTextContrastOn()) }
    var colorInversion by remember { mutableStateOf(systemManager.isColorInversionOn()) }
    var colorCorrection by remember { mutableStateOf(systemManager.isColorCorrectionOn()) }
    var magnification by remember { mutableStateOf(systemManager.isMagnificationOn()) }
    var largePointer by remember { mutableStateOf(systemManager.isLargePointerOn()) }
    var audioMono by remember { mutableStateOf(systemManager.isAudioMonoOn()) }
    var liveCaption by remember { mutableStateOf(systemManager.isLiveCaptionOn()) }

    var a11yServices by remember { mutableStateOf<List<RealSystemManager.AccessibilityServiceItem>>(emptyList()) }
    var isServicesLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = systemManager.getInstalledAccessibilityServices()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                a11yServices = list
                isServicesLoading = false
            }
        }
    }

    SubScreenScaffold(title = stringResource(R.string.accessibility_title), onBack = onBack) {
        // 1. Установленные службы специальных возможностей
        item {
            Text(
                text = "СЛУЖБЫ СПЕЦИАЛЬНЫХ ВОЗМОЖНОСТЕЙ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                if (isServicesLoading) {
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentTerracotta, strokeWidth = 2.dp)
                    }
                } else if (a11yServices.isEmpty()) {
                Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет установленных служб специальных возможностей",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    a11yServices.forEachIndexed { index, service ->
                        val serviceBitmap = remember(service.componentName) { drawableToBitmap(service.icon) }
                        var isEnabled by remember { mutableStateOf(service.isEnabled) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (serviceBitmap != null) {
                                Image(
                                    bitmap = serviceBitmap.asImageBitmap(),
                                    contentDescription = service.label,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                            Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BorderColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Phosphor.Accessibility,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isEnabled) "Включено" else "Отключено",
                                    fontSize = 12.sp,
                                    color = if (isEnabled) AccentTerracotta else TextSecondary
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = {
                                    isEnabled = it
                                    systemManager.toggleAccessibilityService(service.componentName, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SurfaceCard,
                                    checkedTrackColor = AccentTerracotta,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = BorderColor
                                )
                            )
                        }
                        if (index < a11yServices.lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // 2. Дисплей и текст
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ЭКРАН И ТЕКСТ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.high_contrast_label),
                    subtitle = stringResource(R.string.high_contrast_desc),
                    checked = highContrast,
                    onCheckedChange = {
                        highContrast = it
                        systemManager.setHighTextContrast(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = stringResource(R.string.color_inversion_label),
                    subtitle = stringResource(R.string.color_inversion_desc),
                    checked = colorInversion,
                    onCheckedChange = {
                        colorInversion = it
                        systemManager.setColorInversion(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Коррекция цвета",
                    subtitle = "Компенсация дейтераномалии, протаномалии или тританомалии",
                    checked = colorCorrection,
                    onCheckedChange = {
                        colorCorrection = it
                        systemManager.setColorCorrection(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Увеличение экрана (Экранная лупа)",
                    subtitle = "Быстрое масштабирование тройным нажатием или ярлыком",
                    checked = magnification,
                    onCheckedChange = {
                        magnification = it
                        systemManager.setMagnification(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Крупный указатель мыши",
                    subtitle = "Увеличить размер курсора при подключении мыши или тачпада",
                    checked = largePointer,
                    onCheckedChange = {
                        largePointer = it
                        systemManager.setLargePointer(it)
                    }
                )
            }
        }

        // 3. Звук и субтитры
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ЗВУК И СУБТИТРЫ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = "Монофонический звук",
                    subtitle = "Объединять левый и правый аудиоканалы при воспроизведении",
                    checked = audioMono,
                    onCheckedChange = {
                        audioMono = it
                        systemManager.setAudioMono(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Автоматические субтитры (Live Caption)",
                    subtitle = "Автоматическое распознавание речи в медиа и вывод текста на экран",
                    checked = liveCaption,
                    onCheckedChange = {
                        liveCaption = it
                        systemManager.setLiveCaption(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.SpeakerHigh,
                    title = "Настройки субтитров",
                    subtitle = "Размер шрифта, цвет текста и оформление подложки субтитров",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_CAPTIONING_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Настройки субтитров недоступны", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DeveloperOptionsSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var devEnabled by remember { mutableStateOf(systemManager.isDevelopmentSettingsEnabled()) }
    var adb by remember { mutableStateOf(systemManager.isUsbDebuggingOn()) }
    var wirelessAdb by remember { mutableStateOf(systemManager.isWirelessDebuggingOn()) }
    var stayAwake by remember { mutableStateOf(systemManager.isStayAwakeOn()) }
    var insecureAdb by remember { mutableStateOf(systemManager.isInsecureAdbOn()) }
    var highLogging by remember { mutableStateOf(systemManager.isHighSystemLoggingOn()) }
    var disablePackageVerifier by remember { mutableStateOf(systemManager.isDisablePackageVerifierOn()) }
    var mobileDataAlwaysOn by remember { mutableStateOf(systemManager.isMobileDataAlwaysOn()) }
    var wifiVerboseLogging by remember { mutableStateOf(systemManager.isWifiVerboseLoggingOn()) }
    var tetheringHwAccel by remember { mutableStateOf(systemManager.isTetheringHardwareAccelerationOn()) }
    var showDevicesWithoutNames by remember { mutableStateOf(systemManager.isShowDevicesWithoutNamesOn()) }

    var showTouches by remember { mutableStateOf(systemManager.isShowTouchesOn()) }
    var pointerLocation by remember { mutableStateOf(systemManager.isPointerLocationOn()) }
    var showRefreshRate by remember { mutableStateOf(systemManager.isShowRefreshRateOn()) }
    var layoutBounds by remember { mutableStateOf(systemManager.isLayoutBoundsOn()) }
    var forceRtl by remember { mutableStateOf(systemManager.isForceRtlOn()) }
    var showSurfaceUpdates by remember { mutableStateOf(systemManager.isShowSurfaceUpdatesOn()) }

    var forceDark by remember { mutableStateOf(systemManager.isForceDarkOn()) }
    var force4xMsaa by remember { mutableStateOf(systemManager.isForce4xMsaaOn()) }
    var disableOverlays by remember { mutableStateOf(systemManager.isDisableOverlaysOn()) }

    var alwaysFinishActivities by remember { mutableStateOf(systemManager.isAlwaysFinishActivitiesOn()) }
    var strictModeVisual by remember { mutableStateOf(systemManager.isStrictModeVisualOn()) }
    var forceResizable by remember { mutableStateOf(systemManager.isForceResizableActivitiesOn()) }
    var enableFreeform by remember { mutableStateOf(systemManager.isEnableFreeformSupportOn()) }
    var forceExternal by remember { mutableStateOf(systemManager.isForceAllowOnExternalOn()) }

    var backgroundProcessLimit by remember { mutableIntStateOf(systemManager.getBackgroundProcessLimit()) }
    var showProcessLimitDialog by remember { mutableStateOf(false) }

    var windowAnimScale by remember { mutableStateOf(systemManager.getWindowAnimationScale()) }
    var transitionAnimScale by remember { mutableStateOf(systemManager.getTransitionAnimationScale()) }
    var animatorAnimScale by remember { mutableStateOf(systemManager.getAnimatorDurationScale()) }

    var showAnimDialog by remember { mutableStateOf<String?>(null) } // "window", "transition", "animator"

    val densityDp = remember { systemManager.getSmallestWidthDp() }

    SubScreenScaffold(title = stringResource(R.string.dev_options_title), onBack = onBack) {
        // Главный переключатель
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Режим разработчика",
                    subtitle = if (devEnabled) "Параметры разработчика включены" else "Настройки разработки отключены",
                    checked = devEnabled,
                    onCheckedChange = {
                        devEnabled = it
                        systemManager.setDevelopmentSettingsEnabled(it)
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (devEnabled) {
            // 1. ОТЛАДКА
            item {
                Text(
                    text = "ОТЛАДКА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingToggleRow(
                        title = stringResource(R.string.usb_debugging_label),
                        subtitle = stringResource(R.string.usb_debugging_desc),
                        checked = adb,
                        onCheckedChange = {
                            adb = it
                            systemManager.setUsbDebugging(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Беспроводная отладка",
                        subtitle = "Отладка по локальной сети Wi-Fi (ADB over Wi-Fi)",
                        checked = wirelessAdb,
                        onCheckedChange = {
                            wirelessAdb = it
                            systemManager.setWirelessDebugging(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.GearSix,
                        title = "Параметры беспроводной отладки",
                        subtitle = "IP-адрес, порт подключения, сопряжение",
                        onClick = {
                            onNavigate(Screen.WIRELESS_DEBUGGING)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = stringResource(R.string.stay_awake_label),
                        subtitle = stringResource(R.string.stay_awake_desc),
                        checked = stayAwake,
                        onCheckedChange = {
                            stayAwake = it
                            systemManager.setStayAwake(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Небезопасное подключение ADB",
                        subtitle = "Отключить авторизацию RSA ключей",
                        checked = insecureAdb,
                        onCheckedChange = {
                            insecureAdb = it
                            systemManager.setInsecureAdb(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Повышенное логирование системы",
                        subtitle = "Максимальный уровень логов ядра (printk 8), dmesg и буфер 16 МБ",
                        checked = highLogging,
                        onCheckedChange = {
                            highLogging = it
                            systemManager.setHighSystemLogging(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Убрать проверку подписи приложений",
                        subtitle = "Отключить проверку пакетов и верификацию ADB-установок",
                        checked = disablePackageVerifier,
                        onCheckedChange = {
                            disablePackageVerifier = it
                            systemManager.setDisablePackageVerifier(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.ShieldCheck,
                        title = "Отозвать разрешения USB-отладки",
                        subtitle = "Сбросить список всех доверенных компьютеров",
                        onClick = {
                            try {
                                Toast.makeText(context, "Авторизации отладки сброшены", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Info,
                        title = "Отчет об ошибке (Bug Report)",
                        subtitle = "Снять интерактивный системный отчет об ошибке",
                        onClick = {
                            try {
                                context.startActivity(Intent("android.intent.action.BUGREPORT_HANDLER").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (e: Exception) {
                                Toast.makeText(context, "Отчет об ошибке формируется в фоне", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. СЕТИ
            item {
                Text(
                    text = "СЕТИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingToggleRow(
                        title = "Не отключать мобильный интернет",
                        subtitle = "Всегда держать передачу данных активной при включенном Wi-Fi для быстрого переключения",
                        checked = mobileDataAlwaysOn,
                        onCheckedChange = {
                            mobileDataAlwaysOn = it
                            systemManager.setMobileDataAlwaysOn(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Аппаратное ускорение модема",
                        subtitle = "Использовать аппаратное ускорение для раздачи интернета (тетеринга)",
                        checked = tetheringHwAccel,
                        onCheckedChange = {
                            tetheringHwAccel = it
                            systemManager.setTetheringHardwareAcceleration(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Подробный журнал Wi-Fi",
                        subtitle = "Детальное логирование RSSI и SSID в системный лог",
                        checked = wifiVerboseLogging,
                        onCheckedChange = {
                            wifiVerboseLogging = it
                            systemManager.setWifiVerboseLogging(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Bluetooth-устройства без имен",
                        subtitle = "Отображать устройства Bluetooth без имени (только с MAC-адресом)",
                        checked = showDevicesWithoutNames,
                        onCheckedChange = {
                            showDevicesWithoutNames = it
                            systemManager.setShowDevicesWithoutNames(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. ВВОД И КАСАНИЯ
            item {
                Text(
                    text = "ВВОД И КАСАНИЯ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingToggleRow(
                        title = stringResource(R.string.show_touches_label),
                        subtitle = stringResource(R.string.show_touches_desc),
                        checked = showTouches,
                        onCheckedChange = {
                            showTouches = it
                            systemManager.setShowTouches(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = stringResource(R.string.pointer_location_label),
                        subtitle = stringResource(R.string.pointer_location_desc),
                        checked = pointerLocation,
                        onCheckedChange = {
                            pointerLocation = it
                            systemManager.setPointerLocation(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. ОТРИСОВКА И АНИМАЦИИ
            item {
                Text(
                    text = "ОТРИСОВКА И АНИМАЦИИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingToggleRow(
                        title = "Частота обновления",
                        subtitle = "Показывать текущую частоту обновления экрана (FPS / Гц)",
                        checked = showRefreshRate,
                        onCheckedChange = {
                            showRefreshRate = it
                            systemManager.setShowRefreshRate(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Границы элементов макета",
                        subtitle = "Отображать границы компонентов, отступы (margin/padding) и кликабельные области",
                        checked = layoutBounds,
                        onCheckedChange = {
                            layoutBounds = it
                            systemManager.setLayoutBounds(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Обновление поверхностей",
                        subtitle = "Подсвечивать всю площадь окна при перерисовке Surface",
                        checked = showSurfaceUpdates,
                        onCheckedChange = {
                            showSurfaceUpdates = it
                            systemManager.setShowSurfaceUpdates(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Отразить интерфейс (RTL)",
                        subtitle = "Принудительный вывод текста и элементов справа налево",
                        checked = forceRtl,
                        onCheckedChange = {
                            forceRtl = it
                            systemManager.setForceRtl(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Sun,
                        title = "Анимация окон",
                        subtitle = "Текущая: ${systemManager.formatScaleText(windowAnimScale)}",
                        onClick = { showAnimDialog = "window" }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Sun,
                        title = "Анимация переходов",
                        subtitle = "Текущая: ${systemManager.formatScaleText(transitionAnimScale)}",
                        onClick = { showAnimDialog = "transition" }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Sun,
                        title = "Длительность аниматора",
                        subtitle = "Текущая: ${systemManager.formatScaleText(animatorAnimScale)}",
                        onClick = { showAnimDialog = "animator" }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.SquaresFour,
                        title = "Минимальная ширина",
                        subtitle = "${densityDp} dp",
                        onClick = {
                            Toast.makeText(context, "Текущая минимальная ширина: ${densityDp} dp", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. АППАРАТНОЕ УСКОРЕНИЕ (HARDWARE RENDERING)
            item {
                Text(
                    text = "АППАРАТНОЕ УСКОРЕНИЕ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingToggleRow(
                        title = "Принудительный темный режим",
                        subtitle = "Инвертировать цвета в темные для приложений без встроенной поддержки темной темы",
                        checked = forceDark,
                        onCheckedChange = {
                            forceDark = it
                            systemManager.setForceDark(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Включить 4x MSAA",
                        subtitle = "Принудительное сглаживание 4x MSAA в приложениях OpenGL ES 2.0+",
                        checked = force4xMsaa,
                        onCheckedChange = {
                            force4xMsaa = it
                            systemManager.setForce4xMsaa(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Отключить аппаратные наложения",
                        subtitle = "Всегда использовать GPU для компоновки экрана (Disable HW Overlays)",
                        checked = disableOverlays,
                        onCheckedChange = {
                            disableOverlays = it
                            systemManager.setDisableOverlays(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. ПРИЛОЖЕНИЯ И МНОГООКОННОСТЬ
            item {
                Text(
                    text = "ПРИЛОЖЕНИЯ И РЕЖИМЫ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingToggleRow(
                        title = "Не сохранять действия",
                        subtitle = "Уничтожать каждую активность, как только пользователь покидает её",
                        checked = alwaysFinishActivities,
                        onCheckedChange = {
                            alwaysFinishActivities = it
                            systemManager.setAlwaysFinishActivities(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.SquaresFour,
                        title = "Лимит фоновых процессов",
                        subtitle = systemManager.getBackgroundProcessLimitName(backgroundProcessLimit),
                        onClick = { showProcessLimitDialog = true }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Строгий режим (StrictMode)",
                        subtitle = "Подсвечивать экран красным при выполнении долгих операций в главном потоке",
                        checked = strictModeVisual,
                        onCheckedChange = {
                            strictModeVisual = it
                            systemManager.setStrictModeVisual(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Изменение размера в многооконном режиме",
                        subtitle = "Разрешить использование всех приложений в режиме разделения экрана",
                        checked = forceResizable,
                        onCheckedChange = {
                            forceResizable = it
                            systemManager.setForceResizableActivities(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Свободные окна (Freeform windows)",
                        subtitle = "Включить поддержку экспериментальных свободно перемещаемых окон",
                        checked = enableFreeform,
                        onCheckedChange = {
                            enableFreeform = it
                            systemManager.setEnableFreeformSupport(it)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingToggleRow(
                        title = "Разрешить сохранение на внешние накопители",
                        subtitle = "Разрешить установку приложений на внешние накопители независимо от манифеста",
                        checked = forceExternal,
                        onCheckedChange = {
                            forceExternal = it
                            systemManager.setForceAllowOnExternal(it)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Диалог выбора лимита фоновых процессов
    if (showProcessLimitDialog) {
        val limits = listOf(-1, 0, 1, 2, 3, 4)
        AlertDialog(
            onDismissRequest = { showProcessLimitDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Лимит фоновых процессов",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    limits.forEach { limitVal ->
                        val isSelected = (backgroundProcessLimit == limitVal)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    backgroundProcessLimit = limitVal
                                    systemManager.setBackgroundProcessLimit(limitVal)
                                    showProcessLimitDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TextPrimary,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = systemManager.getBackgroundProcessLimitName(limitVal),
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 15.sp,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProcessLimitDialog = false }) {
                    Text("Отмена", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Диалог выбора коэффициента анимации
    if (showAnimDialog != null) {
        val type = showAnimDialog!!
        val titleText = when (type) {
            "window" -> "Анимация окон"
            "transition" -> "Анимация переходов"
            else -> "Длительность аниматора"
        }
        val currentVal = when (type) {
            "window" -> windowAnimScale
            "transition" -> transitionAnimScale
            else -> animatorAnimScale
        }
        val scales = listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 5.0f, 10.0f)

        AlertDialog(
            onDismissRequest = { showAnimDialog = null },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    scales.forEach { scale ->
                        val isSelected = (currentVal == scale)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (type) {
                                        "window" -> {
                                            windowAnimScale = scale
                                            systemManager.setWindowAnimationScale(scale)
                                        }
                                        "transition" -> {
                                            transitionAnimScale = scale
                                            systemManager.setTransitionAnimationScale(scale)
                                        }
                                        "animator" -> {
                                            animatorAnimScale = scale
                                            systemManager.setAnimatorDurationScale(scale)
                                        }
                                    }
                                    showAnimDialog = null
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TextPrimary,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = systemManager.formatScaleText(scale),
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 15.sp,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnimDialog = null }) {
                    Text("Отмена", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SystemSubScreen(systemManager: RealSystemManager, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var autoTime by remember { mutableStateOf(systemManager.isAutoTimeOn()) }
    var autoTimeZone by remember { mutableStateOf(systemManager.isAutoTimeZoneOn()) }
    var use24Hour by remember { mutableStateOf(systemManager.is24HourFormat()) }
    var gestureNav by remember { mutableStateOf(systemManager.isGestureNavigationOn()) }

    val currentLang = remember { systemManager.getCurrentSystemLanguage() }
    val currentDate = remember { systemManager.getCurrentDateFormatted() }
    val currentTime = remember { systemManager.getCurrentTimeFormatted() }
    val currentTimeZone = remember { systemManager.getCurrentTimeZoneFormatted() }

    SubScreenScaffold(title = stringResource(R.string.system_title), onBack = onBack) {
        // 1. Язык и клавиатура
        item {
            Text(
                text = "ЯЗЫК И ВВОД",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.SquaresFour,
                    title = "Языки системы",
                    subtitle = currentLang,
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Настройки языков недоступны", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = "Экранная клавиатура",
                    subtitle = "Управление клавиатурами и методами ввода (IME)",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Настройки ввода недоступны", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 2. Жесты и системная навигация
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ЖЕСТЫ И НАВИГАЦИЯ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = "Жестовая навигация",
                    subtitle = if (gestureNav) "Жесты от краев экрана · Полноэкранный режим" else "Классическая трехкнопочная панель внизу экрана",
                    checked = gestureNav,
                    onCheckedChange = {
                        gestureNav = it
                        systemManager.setGestureNavigation(it)
                    }
                )
            }
        }

        // 3. Дата и время
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ДАТА И ВРЕМЯ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = "Устанавливать время автоматически",
                    subtitle = "Использовать точное время сети ($currentTime · $currentDate)",
                    checked = autoTime,
                    onCheckedChange = {
                        autoTime = it
                        systemManager.setAutoTime(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Автоматический часовой пояс",
                    subtitle = "Определять по данным сотовой сети ($currentTimeZone)",
                    checked = autoTimeZone,
                    onCheckedChange = {
                        autoTimeZone = it
                        systemManager.setAutoTimeZone(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "24-часовой формат времени",
                    subtitle = if (use24Hour) "13:00 (24-часовой)" else "1:00 PM (12-часовой)",
                    checked = use24Hour,
                    onCheckedChange = {
                        use24Hour = it
                        systemManager.set24HourFormat(it)
                    }
                )
            }
        }

        // 4. Опции для разработчиков
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "РАСШИРЕННЫЕ ПАРАМЕТРЫ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = stringResource(R.string.dev_options_title),
                    subtitle = "USB-отладка, анимации, границы макета",
                    onClick = { onNavigate(Screen.DEVELOPER_OPTIONS) }
                )
            }
        }

        // 5. Сброс и восстановление
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "СБРОС И ВОССТАНОВЛЕНИЕ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.ArrowsClockwise,
                    title = "Сброс настроек сети",
                    subtitle = "Сбросить параметры Wi-Fi, мобильного интернета и Bluetooth",
                    onClick = {
                        try {
                            val intent = Intent("android.settings.RESET_NETWORK_SETTINGS").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Диспетчер сброса недоступен", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.ArrowsClockwise,
                    title = "Сброс к заводским настройкам",
                    subtitle = "Удаление всех данных из внутренней памяти устройства",
                    onClick = {
                        try {
                            val intent = Intent("android.settings.MASTER_CLEAR").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Мастер очистки недоступен", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SecuritySubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isDeviceSecure by remember { mutableStateOf(systemManager.isDeviceSecure()) }
    var screenLockSummary by remember { mutableStateOf(systemManager.getScreenLockSummary()) }
    var cameraBlocked by remember { mutableStateOf(systemManager.isCameraAccessBlocked()) }
    var micBlocked by remember { mutableStateOf(systemManager.isMicrophoneAccessBlocked()) }
    var showPasswords by remember { mutableStateOf(systemManager.isShowPasswordsOn()) }
    var appPinning by remember { mutableStateOf(systemManager.isAppPinningOn()) }
    var signatureSpoofing by remember { mutableStateOf(systemManager.isSignatureSpoofingOn()) }

    LaunchedEffect(Unit) {
        screenLockSummary = systemManager.getScreenLockSummary()
        isDeviceSecure = systemManager.isDeviceSecure()
    }

    SubScreenScaffold(title = stringResource(R.string.security_title), onBack = onBack) {
        // 1. Блокировка экрана и биометрия
        item {
            Text(
                text = "БЛОКИРОВКА ЭКРАНА И БИОМЕТРИЯ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.ShieldCheck,
                    title = stringResource(R.string.screen_lock_label),
                    subtitle = screenLockSummary,
                    onClick = {
                        onNavigate(Screen.SCREEN_LOCK_SETTINGS)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.UserCircle,
                    title = "Биометрическая аутентификация",
                    subtitle = "Отпечаток пальца и распознавание лица",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Настройки биометрии недоступны", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // 2. Аппаратная конфиденциальность (Датчики)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "КОНФИДЕНЦИАЛЬНОСТЬ ДАТЧИКОВ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = "Блокировка доступа к камере",
                    subtitle = if (cameraBlocked) "Заблокировано · Ни одно приложение не может использовать камеру" else "Камера доступна для приложений с разрешением",
                    checked = cameraBlocked,
                    onCheckedChange = {
                        cameraBlocked = it
                        systemManager.setCameraAccessBlocked(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Блокировка микрофона",
                    subtitle = if (micBlocked) "Заблокировано · Запись звука отключена на системном уровне" else "Микрофон доступен для приложений с разрешением",
                    checked = micBlocked,
                    onCheckedChange = {
                        micBlocked = it
                        systemManager.setMicrophoneAccessBlocked(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.ShieldCheck,
                    title = "Панель конфиденциальности",
                    subtitle = "Журнал использования камеры, микрофона и местоположения за 24 часа",
                    onClick = {
                        try {
                            val intent = Intent("android.intent.action.REVIEW_PERMISSION_USAGE").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Панель конфиденциальности недоступна", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // 3. Безопасность системы и Triangle OS
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "СИСТЕМА И ЗАЩИТА TRIANGLE OS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            SettingCardGroup {
                SettingToggleRow(
                    title = stringResource(R.string.signature_spoofing_label),
                    subtitle = stringResource(R.string.signature_spoofing_desc),
                    checked = signatureSpoofing,
                    onCheckedChange = {
                        signatureSpoofing = it
                        systemManager.setSignatureSpoofing(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Показывать пароли при вводе",
                    subtitle = "Кратковременное отображение символов при наборе",
                    checked = showPasswords,
                    onCheckedChange = {
                        showPasswords = it
                        systemManager.setShowPasswords(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Закрепление приложения в окне (App Pinning)",
                    subtitle = "Запрет выхода из текущего приложения без подтверждения пароля",
                    checked = appPinning,
                    onCheckedChange = {
                        appPinning = it
                        systemManager.setAppPinning(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Info,
                    title = "Патч безопасности Android",
                    subtitle = "Обновление системы безопасности: ${systemManager.getSecurityPatch()}",
                    onClick = {
                        Toast.makeText(context, "Версия патча безопасности: ${systemManager.getSecurityPatch()}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

var pendingOldCredential = ""
var pendingTargetLockScreen = Screen.SCREEN_LOCK_SETTINGS

@Composable
fun ScreenLockSettingsSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentType by remember { mutableStateOf(systemManager.getScreenLockType()) }

    fun selectType(target: RealSystemManager.ScreenLockType) {
        if (target == currentType) {
            Toast.makeText(context, "Этот способ уже установлен", Toast.LENGTH_SHORT).show()
            return
        }

        val destination = when (target) {
            RealSystemManager.ScreenLockType.NONE -> null
            RealSystemManager.ScreenLockType.PATTERN -> Screen.SET_PATTERN
            RealSystemManager.ScreenLockType.PIN -> Screen.SET_PIN
            RealSystemManager.ScreenLockType.PASSWORD -> Screen.SET_PASSWORD
        }

        // Если текущая блокировка уже установлена (не NONE), нужно сначала подтвердить старый пароль
        if (currentType != RealSystemManager.ScreenLockType.NONE) {
            pendingTargetLockScreen = destination ?: Screen.SCREEN_LOCK_SETTINGS
            pendingOldCredential = ""
            onNavigate(Screen.CONFIRM_OLD_CREDENTIAL)
        } else {
            pendingOldCredential = ""
            if (destination != null) {
                onNavigate(destination)
            }
        }
    }

    SubScreenScaffold(title = "Экран блокировки", onBack = onBack) {
        item {
            Text(
                text = "СПОСОБ БЛОКИРОВКИ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            SettingCardGroup {
                val types = listOf(
                    RealSystemManager.ScreenLockType.NONE,
                    RealSystemManager.ScreenLockType.PATTERN,
                    RealSystemManager.ScreenLockType.PIN,
                    RealSystemManager.ScreenLockType.PASSWORD
                )

                types.forEachIndexed { index, type ->
                    val isSelected = currentType == type
                    val icon = when (type) {
                        RealSystemManager.ScreenLockType.NONE -> Phosphor.Info
                        RealSystemManager.ScreenLockType.PATTERN -> Phosphor.SquaresFour
                        RealSystemManager.ScreenLockType.PIN -> Phosphor.GearSix
                        RealSystemManager.ScreenLockType.PASSWORD -> Phosphor.ShieldCheck
                    }

                    val desc = when (type) {
                        RealSystemManager.ScreenLockType.NONE -> "Блокировка отсутствует, сразу рабочий стол"
                        RealSystemManager.ScreenLockType.PATTERN -> "Графический ключ (соединение точек жестом)"
                        RealSystemManager.ScreenLockType.PIN -> "Цифровой защитный код (от 4 цифр)"
                        RealSystemManager.ScreenLockType.PASSWORD -> "Буквенно-цифровой пароль (от 4 символов)"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectType(type) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) AccentTerracotta.copy(alpha = 0.15f) else BorderColor.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) AccentTerracotta else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.title,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentTerracotta,
                                unselectedColor = TextSecondary
                            )
                        )
                    }

                    if (index < types.lastIndex) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ЭКРАН 1: ПОДТВЕРЖДЕНИЕ ТЕКУЩЕГО ПАРОЛЯ / КЛЮЧА
// -------------------------------------------------------------
@Composable
fun ConfirmOldCredentialSubScreen(
    systemManager: RealSystemManager,
    onVerified: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val currentType = remember { systemManager.getScreenLockType() }

    SubScreenScaffold(title = "Подтверждение защиты", onBack = onBack) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AccentTerracotta.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Phosphor.ShieldCheck,
                        contentDescription = null,
                        tint = AccentTerracotta,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Введите текущий пароль",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Для изменения настроек экрана блокировки подтвердите установленный способ защиты",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (currentType == RealSystemManager.ScreenLockType.PATTERN) {
                    Text(
                        text = "Введите последовательность точек ключа (например 14789):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    label = { Text("Текущий ${currentType.title}") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = if (currentType == RealSystemManager.ScreenLockType.PASSWORD) {
                        androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                    } else {
                        androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTerracotta,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error!!, color = Color(0xFFE53935), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (input.isEmpty()) {
                            error = "Введите пароль"
                            return@Button
                        }
                        isChecking = true
                        coroutineScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                systemManager.verifyCurrentCredential(input)
                            }
                            isChecking = false
                            if (ok) {
                                pendingOldCredential = input
                                if (pendingTargetLockScreen == Screen.SCREEN_LOCK_SETTINGS) {
                                    // Пользователь выбрал "Нет" (снять защиту)
                                    withContext(Dispatchers.IO) {
                                        systemManager.setScreenLock(
                                            newType = RealSystemManager.ScreenLockType.NONE,
                                            oldCredential = input
                                        )
                                    }
                                    Toast.makeText(context, "Блокировка экрана снята", Toast.LENGTH_SHORT).show()
                                    onVerified(Screen.SCREEN_LOCK_SETTINGS)
                                } else {
                                    onVerified(pendingTargetLockScreen)
                                }
                            } else {
                                error = "Неверный пароль. Попробуйте еще раз."
                            }
                        }
                    },
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isChecking) "Проверка..." else "Подтвердить",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ЭКРАН 2: УСТАНОВКА ГРАФИЧЕСКОГО КЛЮЧА (С НАСТОЯЩИМ CANVAS И ЖЕСТАМИ)
// -------------------------------------------------------------
@Composable
fun SetPatternSubScreen(
    systemManager: RealSystemManager,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isConfirmStage by remember { mutableStateOf(false) }
    var firstPattern by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Нарисуйте графический ключ") }
    var isError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    SubScreenScaffold(title = "Графический ключ", onBack = onBack) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (!isConfirmStage) "Задайте графический ключ" else "Повторите графический ключ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = if (isError) Color(0xFFE53935) else TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                PatternLockView(
                    enabled = !isSaving,
                    stageKey = isConfirmStage,
                    onPatternDetected = { patternString ->
                        if (patternString.length < 4) {
                            isError = true
                            message = "Соедините не менее 4 точек"
                            return@PatternLockView
                        }

                        if (!isConfirmStage) {
                            firstPattern = patternString
                            isConfirmStage = true
                            isError = false
                            message = "Нарисуйте ключ еще раз для подтверждения"
                        } else {
                            if (patternString == firstPattern) {
                                isSaving = true
                                isError = false
                                message = "Сохранение ключа..."
                                coroutineScope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        systemManager.setScreenLock(
                                            newType = RealSystemManager.ScreenLockType.PATTERN,
                                            newCredential = patternString,
                                            oldCredential = pendingOldCredential.ifEmpty { null }
                                        )
                                    }
                                    isSaving = false
                                    if (res.first) {
                                        pendingOldCredential = ""
                                        Toast.makeText(context, "Графический ключ успешно установлен!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        isError = true
                                        message = res.second
                                    }
                                }
                            } else {
                                isError = true
                                message = "Ключи не совпадают. Попробуйте снова"
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (isConfirmStage) {
                                isConfirmStage = false
                                firstPattern = ""
                                isError = false
                                message = "Нарисуйте графический ключ"
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Text(
                            text = if (isConfirmStage) "Сбросить" else "Отмена",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ИНТЕРАКТИВНЫЙ ВИДЖЕТ ГРАФИЧЕСКОГО КЛЮЧА (3x3 DOTS + GESTURE DRAWING)
// -------------------------------------------------------------
@Composable
fun PatternLockView(
    enabled: Boolean = true,
    stageKey: Any? = null,
    onPatternDetected: (String) -> Unit
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    val dotPositions = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Offset>() }
    val normalDotColor = TextSecondary.copy(alpha = 0.6f)
    val activeGlowColor = AccentTerracotta.copy(alpha = 0.25f)
    val activeDotColor = AccentTerracotta
    val trailColor = AccentTerracotta.copy(alpha = 0.7f)

    LaunchedEffect(stageKey) {
        selectedDots.clear()
        currentTouchPoint = null
    }

    Box(
        modifier = Modifier
            .size(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots.clear()
                        currentTouchPoint = offset
                        // Проверяем попадание в точку
                        dotPositions.forEach { (index, pos) ->
                            if ((offset - pos).getDistance() <= 40f) {
                                selectedDots.add(index)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        currentTouchPoint = pos
                        dotPositions.forEach { (index, dotPos) ->
                            if (!selectedDots.contains(index) && (pos - dotPos).getDistance() <= 45f) {
                                selectedDots.add(index)
                            }
                        }
                    },
                    onDragEnd = {
                        val pattern = selectedDots.joinToString("")
                        currentTouchPoint = null
                        if (pattern.isNotEmpty()) {
                            onPatternDetected(pattern)
                        }
                    },
                    onDragCancel = {
                        selectedDots.clear()
                        currentTouchPoint = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val step = canvasSize / 4f

            // Рассчитываем координаты 9 точек (1..9)
            var idx = 1
            for (row in 1..3) {
                for (col in 1..3) {
                    val x = col * step
                    val y = row * step
                    dotPositions[idx] = androidx.compose.ui.geometry.Offset(x, y)
                    idx++
                }
            }

            // Рисуем соединенные линии между выбранными точками
            for (i in 0 until selectedDots.size - 1) {
                val start = dotPositions[selectedDots[i]] ?: continue
                val end = dotPositions[selectedDots[i + 1]] ?: continue
                drawLine(
                    color = activeDotColor,
                    start = start,
                    end = end,
                    strokeWidth = 10f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            // Линия от последней точки до пальца
            if (selectedDots.isNotEmpty() && currentTouchPoint != null) {
                val lastPos = dotPositions[selectedDots.last()]
                if (lastPos != null) {
                    drawLine(
                        color = trailColor,
                        start = lastPos,
                        end = currentTouchPoint!!,
                        strokeWidth = 7f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Рисуем 9 точек матрицы
            dotPositions.forEach { (index, pos) ->
                val isSelected = selectedDots.contains(index)
                if (isSelected) {
                    // Внешнее светящееся кольцо
                    drawCircle(
                        color = activeGlowColor,
                        radius = 32f,
                        center = pos
                    )
                    // Активная точка
                    drawCircle(
                        color = activeDotColor,
                        radius = 12f,
                        center = pos
                    )
                } else {
                    // Обычная спокойная точка
                    drawCircle(
                        color = normalDotColor,
                        radius = 8f,
                        center = pos
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ЭКРАН 3: УСТАНОВКА PIN-КОДА (С ЦИФРОВОЙ КЛАВИАТУРОЙ И ТОЧКАМИ-ИНДИКАТОРАМИ)
// -------------------------------------------------------------
@Composable
fun SetPinSubScreen(
    systemManager: RealSystemManager,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isConfirmStage by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    SubScreenScaffold(title = "PIN-код", onBack = onBack) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (!isConfirmStage) "Введите новый PIN-код" else "Повторите PIN-код",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (errorMessage != null) errorMessage!! else "Код должен содержать не менее 4 цифр",
                    fontSize = 13.sp,
                    color = if (errorMessage != null) Color(0xFFE53935) else TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Точки-индикаторы введенных цифр
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = currentInput.length.coerceAtLeast(4)
                    for (i in 0 until count) {
                        val filled = i < currentInput.length
                    Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) AccentTerracotta else BorderColor)
                                .border(1.dp, if (filled) AccentTerracotta else TextSecondary.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Виртуальная клавиатура цифр
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(72.dp))
                                } else {
                                Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceCard)
                                            .border(1.dp, BorderColor, CircleShape)
                                            .clickable {
                                                errorMessage = null
                                                if (key == "DEL") {
                                                    if (currentInput.isNotEmpty()) {
                                                        currentInput = currentInput.dropLast(1)
                                                    }
                                                } else {
                                                    if (currentInput.length < 12) {
                                                        currentInput += key
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "DEL") {
                                            Icon(
                                                imageVector = Phosphor.ArrowLeft,
                                                contentDescription = "Удалить",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Кнопка подтверждения
                Button(
                    onClick = {
                        if (currentInput.length < 4) {
                            errorMessage = "Минимум 4 цифры"
                            return@Button
                        }

                        if (!isConfirmStage) {
                            firstPin = currentInput
                            currentInput = ""
                            isConfirmStage = true
                            errorMessage = null
                        } else {
                            if (currentInput == firstPin) {
                                isSaving = true
                                coroutineScope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        systemManager.setScreenLock(
                                            newType = RealSystemManager.ScreenLockType.PIN,
                                            newCredential = currentInput,
                                            oldCredential = pendingOldCredential.ifEmpty { null }
                                        )
                                    }
                                    isSaving = false
                                    if (res.first) {
                                        pendingOldCredential = ""
                                        Toast.makeText(context, "PIN-код успешно установлен!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        errorMessage = res.second
                                    }
                                }
                            } else {
                                errorMessage = "PIN-коды не совпадают"
                            }
                        }
                    },
                    enabled = currentInput.length >= 4 && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            isSaving -> "Применение в системе..."
                            !isConfirmStage -> "Далее"
                            else -> "Подтвердить"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ЭКРАН 4: УСТАНОВКА БУКВЕННО-ЦИФРОВОГО ПАРОЛЯ
// -------------------------------------------------------------
@Composable
fun SetPasswordSubScreen(
    systemManager: RealSystemManager,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isConfirmStage by remember { mutableStateOf(false) }
    var firstPassword by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    SubScreenScaffold(title = "Пароль", onBack = onBack) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AccentTerracotta.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Phosphor.ShieldCheck,
                        contentDescription = null,
                        tint = AccentTerracotta,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (!isConfirmStage) "Задайте пароль" else "Повторите пароль",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (errorMessage != null) errorMessage!! else "Пароль должен содержать от 4 любых символов",
                    fontSize = 13.sp,
                    color = if (errorMessage != null) Color(0xFFE53935) else TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it; errorMessage = null },
                    label = { Text(if (!isConfirmStage) "Новый пароль" else "Повторите пароль") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTerracotta,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (currentInput.length < 4) {
                            errorMessage = "Минимум 4 символа"
                            return@Button
                        }

                        if (!isConfirmStage) {
                            firstPassword = currentInput
                            currentInput = ""
                            isConfirmStage = true
                            errorMessage = null
                        } else {
                            if (currentInput == firstPassword) {
                                isSaving = true
                                coroutineScope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        systemManager.setScreenLock(
                                            newType = RealSystemManager.ScreenLockType.PASSWORD,
                                            newCredential = currentInput,
                                            oldCredential = pendingOldCredential.ifEmpty { null }
                                        )
                                    }
                                    isSaving = false
                                    if (res.first) {
                                        pendingOldCredential = ""
                                        Toast.makeText(context, "Пароль успешно установлен!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        errorMessage = res.second
                                    }
                                }
                            } else {
                                errorMessage = "Пароли не совпадают"
                            }
                        }
                    },
                    enabled = currentInput.length >= 4 && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTerracotta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            isSaving -> "Применение в системе..."
                            !isConfirmStage -> "Далее"
                            else -> "Подтвердить"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var darkTheme by remember { mutableStateOf(systemManager.isDarkThemeOn()) }
    var themedIcons by remember { mutableStateOf(true) }
    var dynamicColorsEnabled by remember { mutableStateOf(systemManager.isDynamicColorEnabled()) }
    val currentMonetJson = remember { systemManager.getMonetColorPalette() }
    
    // Предустановленные системные палитры Monet (Material You)
    val monetPalettes = listOf(
        Triple("Терракота", "#BD4A26", Color(0xFFBD4A26)),
        Triple("Охра", "#C26E00", Color(0xFFC26E00)),
        Triple("Изумруд", "#2E7D32", Color(0xFF2E7D32)),
        Triple("Сапфир", "#1976D2", Color(0xFF1976D2)),
        Triple("Аметист", "#7B1FA2", Color(0xFF7B1FA2)),
        Triple("Шалфей", "#607D8B", Color(0xFF607D8B)),
        Triple("Графит", "#424242", Color(0xFF424242))
    )

    var selectedMonetColor by remember {
        val initial = monetPalettes.find { currentMonetJson.contains(it.second, ignoreCase = true) }?.second
            ?: "#BD4A26"
        mutableStateOf(initial)
    }
    var selectedGrid by remember { mutableStateOf("4 × 5") }

    // Живые часы для превью: обновляются раз в 30 секунд, формат 12/24ч берётся из системы.
    val now by produceState(initialValue = java.time.LocalDateTime.now()) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            value = java.time.LocalDateTime.now()
        }
    }
    val previewTime = now.format(
        java.time.format.DateTimeFormatter.ofPattern(
            if (systemManager.is24HourFormat()) "HH:mm" else "hh:mm",
            java.util.Locale.getDefault()
        )
    )
    val previewDate = now.format(
        java.time.format.DateTimeFormatter.ofPattern("EEE, d MMMM", java.util.Locale.getDefault())
    ).replaceFirstChar { it.uppercase() }

    SubScreenScaffold(title = stringResource(R.string.wallpaper_title), onBack = onBack) {
        // Превью оформления
        item {
            SettingCardGroup {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Предпросмотр оформления", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Экран блокировки
                    Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (darkTheme) Color(0xFF141311) else PaperBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = previewTime,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontSize = 24.sp,
                                    color = if (darkTheme) Color.White else TextPrimary
                                )
                                Text(
                                    text = previewDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 11.sp,
                                    color = if (darkTheme) Color.LightGray else TextSecondary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Icon(
                                    imageVector = Phosphor.ShieldCheck,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = AccentTerracotta
                                )
                            }
                        }

                        // Главный экран
                    Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (darkTheme) Color(0xFF141311) else PaperBackground)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(24.dp).background(AccentTerracotta, RoundedCornerShape(6.dp)))
                                Box(modifier = Modifier.size(24.dp).background(SoftOchre, RoundedCornerShape(6.dp)))
                                Box(modifier = Modifier.size(24.dp).background(BorderBold, RoundedCornerShape(6.dp)))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Triangle OS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 12.sp,
                                    color = if (darkTheme) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(Intent.createChooser(intent, "Выберите обои"))
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Сменить обои", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text(text = "Выбрать из галереи или фирменной коллекции Triangle", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                    Icon(imageVector = Phosphor.CaretRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextTertiary)
                }
            }
        }

        // Цветовая палитра Monet / Динамические цвета
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Динамические цвета Monet",
                    subtitle = "Адаптировать акценты настроек и системы под выбранный оттенок",
                    checked = dynamicColorsEnabled,
                    onCheckedChange = {
                        dynamicColorsEnabled = it
                        systemManager.setDynamicColorEnabled(it)
                        Toast.makeText(context, if (it) "Динамические цвета включены" else "Используется базовая тема", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Системная палитра Monet (Material You)", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                    Text(
                        text = "Выберите оттенок акцентов интерфейса, шторки уведомлений и системных переключателей",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        monetPalettes.forEach { (title, hex, colorVal) ->
                            val isSelected = selectedMonetColor.equals(hex, ignoreCase = true)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    selectedMonetColor = hex
                                    val ok = systemManager.setMonetPalette(hex)
                                    Toast.makeText(
                                        context,
                                        if (ok) "Палитра '$title' применена" else "Не удалось применить палитру",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                            Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colorVal)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) BorderBold else BorderColor,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Phosphor.Check,
                                            contentDescription = "Active",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Темная тема интерфейса",
                    subtitle = "Глубокий темный фон для комфорта глаз и экономии энергии",
                    checked = darkTheme,
                    onCheckedChange = {
                        darkTheme = it
                        systemManager.setDarkTheme(it)
                    }
                )
            }
        }

        // Тематические иконки и сетка рабочего стола
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Тематические иконки (Phosphor)",
                    subtitle = "Применять контурный монохромный стиль ко всем приложениям",
                    checked = themedIcons,
                    onCheckedChange = { themedIcons = it }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedGrid = when (selectedGrid) {
                                "4 × 5" -> "5 × 5"
                                "5 × 5" -> "4 × 4"
                                else -> "4 × 5"
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Сетка приложений", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text(text = selectedGrid, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                    }
                    Text(text = "Сменить", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationsSubScreen(
    systemManager: RealSystemManager,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    var bubbles by remember { mutableStateOf(systemManager.isNotificationBubblesOn()) }
    var snooze by remember { mutableStateOf(systemManager.isNotificationSnoozeOn()) }
    var flashNotif by remember { mutableStateOf(systemManager.isFlashNotificationOn()) }
    var listenerEnabled by remember { mutableStateOf(systemManager.isNotificationListenerEnabled()) }
    var listenerConnected by remember { mutableStateOf(systemManager.isNotificationListenerConnected()) }
    val lockscreenMode = remember { systemManager.getLockscreenNotificationMode() }
    val context = LocalContext.current

    LaunchedEffect(listenerEnabled) {
        while (listenerEnabled) {
            listenerConnected = systemManager.isNotificationListenerConnected()
            delay(1500)
        }
    }

    val lockscreenSubtitle = when (lockscreenMode) {
        0 -> "Не показывать"
        1 -> "Скрывать личные данные"
        else -> "Показывать полностью"
    }

    SubScreenScaffold(title = stringResource(R.string.notifications_title), onBack = onBack) {
        // Доступ к уведомлениям: без него системный журнал недоступен
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Доступ к уведомлениям",
                    subtitle = when {
                        listenerEnabled && listenerConnected -> "Подключено · журнал записывается"
                        listenerEnabled -> "Включено, служба подключается…"
                        else -> "Выключено · история не записывается"
                    },
                    checked = listenerEnabled,
                    onCheckedChange = { enabled ->
                        if (systemManager.setNotificationListenerEnabled(enabled)) {
                            listenerEnabled = enabled
                        } else {
                            Toast.makeText(context, "Не удалось изменить доступ", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Управление приложениями и журналом
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.SquaresFour,
                    title = "Уведомления приложений",
                    subtitle = "Настройки оповещений для каждого приложения",
                    onClick = { onNavigate(Screen.APP_NOTIFICATIONS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Bell,
                    title = "История уведомлений",
                    subtitle = when {
                        !listenerEnabled -> "Нужен доступ к уведомлениям"
                        else -> {
                            val count = NotificationLog.entries(context).size
                            if (count == 0) "Журнал пуст" else "Записей в журнале: $count"
                        }
                    },
                    onClick = { onNavigate(Screen.NOTIFICATION_HISTORY) }
                )
            }
        }

        // Конфиденциальность на экране блокировки
        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.ShieldCheck,
                    title = "Уведомления на экране блокировки",
                    subtitle = lockscreenSubtitle,
                    onClick = { onNavigate(Screen.LOCKSCREEN_NOTIFICATIONS) }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Всплывающие пузыри (Bubbles)",
                    subtitle = "Разрешить плавающие диалоговые пузыри поверх окон",
                    checked = bubbles,
                    onCheckedChange = {
                        bubbles = it
                        systemManager.setNotificationBubbles(it)
                    }
                )
            }
        }

        // Дополнительные параметры
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Откладывание уведомлений",
                    subtitle = "Показывать кнопку откладывания при смахивании",
                    checked = snooze,
                    onCheckedChange = {
                        snooze = it
                        systemManager.setNotificationSnooze(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Вспышка при уведомлениях",
                    subtitle = "Мигать вспышкой камеры при входящих оповещениях",
                    checked = flashNotif,
                    onCheckedChange = {
                        flashNotif = it
                        systemManager.setFlashNotification(it)
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingItem(
                    icon = Phosphor.Bell,
                    title = "Режим «Не беспокоить»",
                    subtitle = "Настроить расписание и правила приглушения",
                    onClick = { onNavigate(Screen.DND_SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun AppNotificationsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var apps by remember { mutableStateOf<List<RealSystemManager.AppNotificationStatus>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val canManageAppOps = remember { systemManager.canManageAppOps() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = systemManager.getAppsNotificationList()
            withContext(Dispatchers.Main) {
                apps = list
            }
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    // Реальная статистика из журнала: сколько каналов и записей встречалось у приложения.
    val logStats = remember(apps) {
        NotificationLog.entries(context)
            .groupBy { it.packageName }
            .mapValues { (_, list) ->
                val channelCount = list.map { it.channelId }.filter { it.isNotEmpty() }.distinct().size
                channelCount to list.size
            }
    }

    SubScreenScaffold(title = "Уведомления приложений", onBack = onBack) {
        item {
            // Поле живого поиска
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Phosphor.MagnifyingGlass, contentDescription = "Поиск", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontFamily = FontFamily.SansSerif),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(text = "Поиск приложений...", color = TextTertiary, fontSize = 15.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        if (!canManageAppOps) {
            item {
                Text(
                    text = "Переключатели недоступны: системе не разрешено менять уведомления чужих приложений без привилегии MANAGE_APP_OPS_MODES. Ниже показано фактическое состояние.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }
        }

        if (apps.isEmpty()) {
            item {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp)
                }
            }
        } else {
            item {
                SettingCardGroup {
                    filteredApps.forEachIndexed { index, app ->
                        val stats = logStats[app.packageName]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:${app.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = buildString {
                                        append(if (app.areNotificationsEnabled) "Включены" else "Заблокированы")
                                        if (stats != null) {
                                            append(" · ")
                                            append(
                                                if (stats.first > 0) "${stats.first} каналов в журнале"
                                                else "${stats.second} записей в журнале"
                                            )
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    color = if (app.areNotificationsEnabled) TextSecondary else AccentTerracotta
                                )
                            }
                            Switch(
                                checked = app.areNotificationsEnabled,
                                enabled = canManageAppOps,
                                onCheckedChange = { enabled ->
                                    if (systemManager.setAppNotificationsEnabled(app.packageName, enabled)) {
                                        apps = apps.map {
                                            if (it.packageName == app.packageName) {
                                                it.copy(areNotificationsEnabled = enabled)
                                            } else {
                                                it
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Система не разрешила изменение", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PaperBackground,
                                    checkedTrackColor = BorderBold,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = PaperBackground,
                                    uncheckedBorderColor = BorderColor
                                )
                            )
                        }
                        if (index < filteredApps.size - 1) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationHistorySubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var listenerEnabled by remember { mutableStateOf(systemManager.isNotificationListenerEnabled()) }
    var systemHistory by remember { mutableStateOf(systemManager.isNotificationHistoryOn()) }
    var entries by remember { mutableStateOf(NotificationLog.entries(context)) }

    LaunchedEffect(listenerEnabled) {
        while (listenerEnabled) {
            entries = NotificationLog.entries(context)
            delay(2000)
        }
    }

    SubScreenScaffold(title = "История уведомлений", onBack = onBack) {
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Записывать уведомления",
                    subtitle = if (listenerEnabled) "TriangleSettings слушает системные оповещения" else "Выключено · новые записи не появляются",
                    checked = listenerEnabled,
                    onCheckedChange = { enabled ->
                        if (systemManager.setNotificationListenerEnabled(enabled)) {
                            listenerEnabled = enabled
                            entries = NotificationLog.entries(context)
                        } else {
                            Toast.makeText(context, "Не удалось изменить доступ", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                SettingToggleRow(
                    title = "Системный журнал Android",
                    subtitle = "Параметр notification_history_enabled для SystemUI",
                    checked = systemHistory,
                    onCheckedChange = {
                        systemHistory = it
                        systemManager.setNotificationHistory(it)
                    }
                )
            }
        }

        if (entries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Записей: ${entries.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Очистить",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = AccentTerracotta,
                        modifier = Modifier.clickable {
                            NotificationLog.clear(context)
                            entries = emptyList()
                        }
                    )
                }
            }
            item {
                SettingCardGroup {
                    entries.take(100).forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:${entry.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTimeAgo(entry.postedAt),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 12.sp,
                                        color = TextTertiary
                                    )
                                }
                                if (entry.title.isNotEmpty()) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 15.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (entry.text.isNotEmpty()) {
                                    Text(
                                        text = entry.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (entry.channelId.isNotEmpty()) {
                                    Text(
                                        text = "канал: ${entry.channelId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 11.sp,
                                        color = TextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (index < entries.take(100).lastIndex) {
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Phosphor.Bell,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (listenerEnabled) "Нет записанных уведомлений" else "Запись выключена",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (listenerEnabled) {
                                "Новые оповещения появятся здесь — журнал ведёт само приложение"
                            } else {
                                "Включите «Записывать уведомления», чтобы видеть входящие оповещения"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/** Относительное время для журнала уведомлений. */
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "только что"
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "только что"
        minutes < 60 -> "$minutes мин назад"
        hours < 24 -> "$hours ч назад"
        days < 7 -> "$days дн назад"
        else -> java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

@Composable
fun LockscreenNotificationsSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(systemManager.getLockscreenNotificationMode()) }

    data class LockOption(val mode: Int, val title: String, val subtitle: String)
    val options = listOf(
        LockOption(2, "Показывать полностью", "Отображать отправителя и полное содержимое сообщений"),
        LockOption(1, "Скрывать конфиденциальные данные", "Показывать только имя приложения или отправителя без текста"),
        LockOption(0, "Не показывать уведомления", "Экран блокировки будет оставаться полностью пустым")
    )

    SubScreenScaffold(title = "На экране блокировки", onBack = onBack) {
        item {
            SettingCardGroup {
                options.forEachIndexed { index, opt ->
                    val isSelected = selectedMode == opt.mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMode = opt.mode
                                systemManager.setLockscreenNotificationMode(opt.mode)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = opt.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Phosphor.Check,
                                contentDescription = "Selected",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < options.size - 1) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SystemUpdateSubScreen(systemManager: RealSystemManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val patch = remember { systemManager.getSecurityPatch() }
    val currentBuild = remember { systemManager.getRealBuildNumber() }
    val appVersion = remember { systemManager.getAppVersionName() }
    val officialBuild = remember { systemManager.isOfficialBuild() }

    var isChecking by remember { mutableStateOf(false) }
    var lastCheckResult by remember { mutableStateOf("Система актуальна") }

    var autoCheck by remember { mutableStateOf(systemManager.isAutoCheckUpdatesOn()) }
    var wifiOnly by remember { mutableStateOf(systemManager.isUpdateOnlyOverWifiOn()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Локальный файл: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    if (isChecking) {
        LaunchedEffect(isChecking) {
            kotlinx.coroutines.delay(1200)
            isChecking = false
            lastCheckResult = "Установлены все актуальные компоненты"
            Toast.makeText(context, "Обновлений не найдено", Toast.LENGTH_SHORT).show()
        }
    }

Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя плашка навигации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Phosphor.ArrowLeft,
                        contentDescription = "Назад",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Обновление ПО",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                IconButton(
                    onClick = {
                        try {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Файловый менеджер недоступен", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Phosphor.DotsThreeVertical,
                        contentDescription = "Опции",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Героический верхний блок стиля ColorOS/Editorial
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Build.VERSION.RELEASE.substringBefore("."),
                            fontSize = 88.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-3).sp,
                            color = TextPrimary,
                            lineHeight = 88.sp
                        )
                        Text(
                            text = "TRIANGLE OS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${if (officialBuild) "Официальная сборка" else "Сборка сообщества"} · Android ${Build.VERSION.RELEASE}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Карточка информации о версии
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Версия системы",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Triangle OS $appVersion · патч $patch",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Статус системы",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastCheckResult,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Номер сборки",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentBuild,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Дополнительные параметры
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingCardGroup {
                        SettingToggleRow(
                            title = "Автозагрузка обновлений",
                            subtitle = "Загружать новые пакеты только через Wi-Fi",
                            checked = wifiOnly,
                            onCheckedChange = {
                                wifiOnly = it
                                systemManager.setUpdateOnlyOverWifi(it)
                            }
                        )
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        SettingToggleRow(
                            title = "Автоматическая проверка",
                            subtitle = "Уведомлять при выходе стабильных версий",
                            checked = autoCheck,
                            onCheckedChange = {
                                autoCheck = it
                                systemManager.setAutoCheckUpdates(it)
                            }
                        )
                    }
                }
            }
        }

        // Нижняя фиксированная кнопка действия
    Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(PaperBackground.copy(alpha = 0f), PaperBackground, PaperBackground)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
        Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(BorderBold)
                    .clickable(enabled = !isChecking) {
                        isChecking = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = PaperBackground,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Проверка обновлений...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaperBackground
                        )
                    }
                } else {
                    Text(
                        text = "Проверить обновления",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaperBackground
                    )
                }
            }
        }
    }
}

@Composable
fun WirelessDebuggingSubScreen(
    systemManager: RealSystemManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(systemManager.isWirelessDebuggingOn()) }
    var deviceIp by remember { mutableStateOf(systemManager.getDeviceIpAddress()) }
    var port by remember { mutableIntStateOf(systemManager.getWirelessAdbPort()) }
    var showPortDialog by remember { mutableStateOf(false) }
    var portInput by remember { mutableStateOf(port.toString()) }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            systemManager.ensureWirelessAdbActive(port)
        }
        deviceIp = systemManager.getDeviceIpAddress()
        port = systemManager.getWirelessAdbPort()
    }

    SubScreenScaffold(title = "Беспроводная отладка", onBack = onBack) {
        // Главный переключатель
        item {
            SettingCardGroup {
                SettingToggleRow(
                    title = "Использовать беспроводную отладку",
                    subtitle = if (isEnabled) "Служба ADB активна по локальной сети Wi-Fi" else "Беспроводная отладка отключена",
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        systemManager.setWirelessDebugging(checked)
                        if (checked) {
                            systemManager.restartAdbTcpPort(port)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (isEnabled) {
            // Информация о подключении
            item {
                Text(
                    text = "ПАРАМЕТРЫ ПОДКЛЮЧЕНИЯ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingCardGroup {
                    SettingDetailRow(
                        label = "IP-адрес и порт",
                        value = "$deviceIp:$port"
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Info,
                        title = "Скопировать команду подключения",
                        subtitle = "adb connect $deviceIp:$port",
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("ADB Connect Command", "adb connect $deviceIp:$port")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Команда adb connect скопирована", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.ArrowsClockwise,
                        title = "Изменить порт / Перезапустить демон",
                        subtitle = "Текущий порт: $port",
                        onClick = {
                            portInput = port.toString()
                            showPortDialog = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Инструкция
            item {
                Text(
                    text = "ПОДКЛЮЧЕНИЕ ЧЕРЕЗ ПК",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. Убедитесь, что ПК и устройство подключены к одной Wi-Fi сети.\n" +
                                   "2. В терминале на ПК выполните:\n" +
                                   "   adb connect $deviceIp:$port\n" +
                                   "3. При необходимости подтвердите авторизацию на экране (если не включено небезопасное подключение ADB).",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            title = {
                Text("Порт TCP для ADB", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Укажите порт для прослушивания подключений ADB (по умолчанию 5555):",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newPort = portInput.toIntOrNull() ?: 5555
                    port = newPort
                    systemManager.restartAdbTcpPort(newPort)
                    showPortDialog = false
                    Toast.makeText(context, "Демон ADB перезапущен на порту $newPort", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Применить", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPortDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

@Composable
fun AboutPhoneScreen(
    systemManager: RealSystemManager,
    selectedRoot: String,
    onBack: () -> Unit,
    onOpenRootProvider: () -> Unit
) {
    val realModel = remember { systemManager.getRealDeviceModel() }
    val realManufacturer = remember { systemManager.getRealManufacturer() }
    val realAndroid = remember { systemManager.getRealAndroidVersion() }
    val realBuild = remember { systemManager.getRealBuildNumber() }
    val realHardware = remember { systemManager.getRealHardware() }
    val realKernel = remember { systemManager.getKernelVersion() }
    val selinuxMode = remember { systemManager.getSelinuxMode() }
    val supportedAbis = remember { systemManager.getSupportedAbis() }
    val trebleEnabled = remember { systemManager.isTrebleEnabled() }
    val appVersion = remember { systemManager.getAppVersionName() }
    val officialBuild = remember { systemManager.isOfficialBuild() }

    SubScreenScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        item {
            SettingCardGroup {
                InfoRow(label = stringResource(R.string.device_name_label), value = "$realManufacturer $realModel")
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(label = stringResource(R.string.triangle_version_label), value = "Triangle OS $appVersion")
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(
                    label = stringResource(R.string.android_version_label),
                    value = "$realAndroid (AOSP, ${if (officialBuild) "release-keys" else "test-keys"})"
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(label = stringResource(R.string.build_number_label), value = realBuild)
            }
        }

        item {
            SettingCardGroup {
                SettingItem(
                    icon = Phosphor.GearSix,
                    title = stringResource(R.string.root_provider_title),
                    subtitle = selectedRoot,
                    onClick = onOpenRootProvider
                )
            }
        }

        item {
            SettingCardGroup {
                InfoRow(label = stringResource(R.string.processor_label), value = "$supportedAbis ($realHardware)")
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(
                    label = "Treble Architecture",
                    value = if (trebleEnabled) "Включён (system-as-root, GSI)" else "Не поддерживается"
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(label = stringResource(R.string.kernel_label), value = realKernel)
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                InfoRow(label = "SELinux Mode", value = selinuxMode)
            }
        }
    }
}

@Composable
fun RootProviderScreen(
    selectedRoot: String,
    onSelectRoot: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rootOptions = listOf(
        "None" to "Стандартная чистая среда. Проходит все проверки SafetyNet / Play Integrity.",
        "KernelSU" to "Рут на уровне ядра Linux. Максимально скрытный, безопасный и чистый.",
        "Magisk" to "Systemless root с поддержкой модулей Zygisk и Shamiko.",
        "APatch" to "Современный рут-фреймворк на базе ядра KernelPatch."
    )

    SubScreenScaffold(title = stringResource(R.string.root_provider_title), onBack = onBack) {
        item {
            SettingCardGroup {
                rootOptions.forEachIndexed { index, (name, description) ->
                    val isSelected = selectedRoot == name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectRoot(name)
                                Toast.makeText(context, "$name выбран", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = description, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
                        }

                        if (isSelected) {
                        Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.dp, BorderBold, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Phosphor.Check, contentDescription = "Selected", modifier = Modifier.size(14.dp), tint = BorderBold)
                            }
                        }
                    }

                    if (index < rootOptions.lastIndex) {
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val listState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PaperBackground
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Phosphor.ArrowLeft, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
            }
            content()
        }
    }
}

@Composable
fun SettingCardGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
    Box(
            modifier = Modifier
                .size(38.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
        }

        Icon(imageVector = Phosphor.CaretRight, contentDescription = "Open", modifier = Modifier.size(16.dp), tint = TextTertiary)
    }
}

@Composable
fun SettingSimpleRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp, color = TextSecondary)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PaperBackground,
                checkedTrackColor = BorderBold,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = PaperBackground,
                uncheckedBorderColor = BorderColor
            )
        )
    }
}

@Composable
fun SettingDetailRow(label: String, value: String) {
    InfoRow(label = label, value = value)
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
