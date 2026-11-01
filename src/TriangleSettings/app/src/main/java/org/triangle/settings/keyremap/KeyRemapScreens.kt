package org.triangle.settings.keyremap

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.triangle.settings.SettingCardGroup
import org.triangle.settings.SettingItem
import org.triangle.settings.SettingSimpleRow
import org.triangle.settings.SettingToggleRow
import org.triangle.settings.SubScreenScaffold
import org.triangle.settings.ui.icons.Phosphor
import org.triangle.settings.ui.theme.AccentColor
import org.triangle.settings.ui.theme.BorderColor
import org.triangle.settings.ui.theme.PaperBackground
import org.triangle.settings.ui.theme.SurfaceCard
import org.triangle.settings.ui.theme.TextPrimary
import org.triangle.settings.ui.theme.TextSecondary
import org.triangle.settings.ui.theme.TextTertiary

private const val TAG_UI = "KeyRemap"

// ---------------------------------------------------------------------------
// Список привязок
// ---------------------------------------------------------------------------

@Composable
fun KeyRemapSubScreen(
    onEdit: (Int, KeyTrigger) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(KeyRemapStore.load(context)) }
    var serviceEnabled by remember { mutableStateOf(KeyRemapStore.isServiceEnabled(context)) }
    var loadError by remember { mutableStateOf(KeyRemapStore.lastLoadError) }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(false) }
    var serviceGeneration by remember { mutableStateOf(0) }

    fun reload() {
        config = KeyRemapStore.load(context)
        loadError = KeyRemapStore.lastLoadError
        serviceEnabled = KeyRemapStore.isServiceEnabled(context)
    }

    LaunchedEffect(Unit) { reload() }

    // Пока служба включена, но процесс переподключается — обновляем состояние.
    LaunchedEffect(serviceEnabled, serviceGeneration) {
        if (!serviceEnabled) return@LaunchedEffect
        repeat(20) {
            if (KeyRemapService.isConnected()) return@LaunchedEffect
            delay(250)
            serviceEnabled = KeyRemapStore.isServiceEnabled(context)
        }
    }

    // Захват клавиши: следующий нажатый keyCode уходит в редактор.
    if (captureMode) {
        CaptureKeyDialog(
            onCaptured = { keyCode ->
                captureMode = false
                if (!KeyCatalog.isInterceptable(keyCode)) {
                    statusMessage = "Клавиша «${KeyCatalog.label(keyCode)}» не перехватывается системой: " +
                        KeyCatalog.note(keyCode)
                } else {
                    onEdit(keyCode, KeyTrigger.LONG)
                }
            },
            onCancel = { captureMode = false }
        )
    }

    if (showPicker) {
        KeyPickerDialog(
            onPick = { keyCode ->
                showPicker = false
                onEdit(keyCode, KeyTrigger.LONG)
            },
            onDismiss = { showPicker = false }
        )
    }

    SubScreenScaffold(title = "Переназначение клавиш", onBack = onBack) {

        item {
            SectionLabel("ПЕРЕХВАТ")
            SettingCardGroup {
                SettingToggleRow(
                    title = "Перехват клавиш",
                    subtitle = when {
                        busy -> "Применяю…"
                        serviceEnabled && KeyRemapService.isConnected() ->
                            "Служба подключена, привязки работают"
                        serviceEnabled ->
                            "Служба включена, но ещё не подключилась"
                        else ->
                            "Выключено: привязки не работают, клавиши ведут себя как обычно"
                    },
                    checked = serviceEnabled,
                    enabled = !busy,
                    onCheckedChange = { enable ->
                        busy = true
                        statusMessage = null
                        scope.launch {
                            val result = KeyRemapStore.setServiceEnabled(context, enable)
                            if (result is KeyRemapStore.SaveResult.Failed) {
                                statusMessage = "Не удалось изменить состояние службы: ${result.reason}"
                                busy = false
                                return@launch
                            }
                            var confirmed = false
                            for (i in 0 until 20) {
                                delay(250)
                                val enabledNow = KeyRemapStore.isServiceEnabled(context)
                                val connectedNow = KeyRemapService.isConnected()
                                if (enabledNow == enable && connectedNow == enable) {
                                    confirmed = true
                                    break
                                }
                            }
                            serviceEnabled = KeyRemapStore.isServiceEnabled(context)
                            serviceGeneration += 1
                            busy = false
                            if (!confirmed) {
                                statusMessage = if (enable) {
                                    "Система не подтвердила включение. Откройте «Специальные возможности» " +
                                        "и включите «Переназначение клавиш» вручную."
                                } else {
                                    "Служба всё ещё числится включённой. Проверьте «Специальные возможности»."
                                }
                            }
                        }
                    }
                )
                if (serviceEnabled && !KeyRemapService.isConnected()) {
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    SettingItem(
                        icon = Phosphor.Accessibility,
                        title = "Открыть «Специальные возможности»",
                        subtitle = "Если служба не подключается сама — включите её там",
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) {
                                statusMessage = "Не удалось открыть настройки: ${e.message}"
                            }
                        }
                    )
                }
            }
            statusMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(text = message, color = AccentColor, fontSize = 13.sp)
            }
            loadError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(text = error, color = AccentColor, fontSize = 13.sp)
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            SectionLabel("ПРИВЯЗКИ")
            if (config.bindings.isEmpty()) {
                SettingCardGroup {
                    SettingSimpleRow(
                        title = "Пока ничего не назначено",
                        subtitle = "Добавьте привязку и выберите, что делать при нажатии"
                    )
                }
            } else {
                SettingCardGroup {
                    val sorted = config.bindings.sortedWith(
                        compareBy({ KeyCatalog.label(it.keyCode) }, { it.trigger.ordinal })
                    )
                    sorted.forEachIndexed { index, binding ->
                        if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        BindingRow(
                            binding = binding,
                            onClick = { onEdit(binding.keyCode, binding.trigger) },
                            onDelete = {
                                val updated = config.bindings.filterNot {
                                    it.keyCode == binding.keyCode && it.trigger == binding.trigger
                                }
                                when (val result = KeyRemapStore.save(context, config.copy(bindings = updated))) {
                                    is KeyRemapStore.SaveResult.Ok -> reload()
                                    is KeyRemapStore.SaveResult.Failed ->
                                        statusMessage = "Не удалось удалить: ${result.reason}"
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    statusMessage = null
                    if (!KeyRemapService.isConnected()) {
                        statusMessage = "Сначала включите перехват клавиш, иначе нажатую клавишу не поймать. " +
                            "Можно выбрать из списка."
                    } else {
                        captureMode = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = PaperBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить: нажать клавишу", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выбрать клавишу из списка", color = TextSecondary)
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            SectionLabel("ТАЙМИНГИ")
            SettingCardGroup {
                TimeoutRow(
                    title = "Долгое нажатие",
                    valueMs = config.longPressTimeoutMs,
                    onChange = { newValue ->
                        val updated = config.copy(longPressTimeoutMs = newValue)
                        if (KeyRemapStore.save(context, updated) is KeyRemapStore.SaveResult.Ok) reload()
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                TimeoutRow(
                    title = "Окно двойного нажатия",
                    valueMs = config.doublePressTimeoutMs,
                    onChange = { newValue ->
                        val updated = config.copy(doublePressTimeoutMs = newValue)
                        if (KeyRemapStore.save(context, updated) is KeyRemapStore.SaveResult.Ok) reload()
                    }
                )
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                TimeoutRow(
                    title = "Окно тройного нажатия",
                    valueMs = config.triplePressTimeoutMs,
                    onChange = { newValue ->
                        val updated = config.copy(triplePressTimeoutMs = newValue)
                        if (KeyRemapStore.save(context, updated) is KeyRemapStore.SaveResult.Ok) reload()
                    }
                )
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            SectionLabel("ЖУРНАЛ ПЕРЕХВАТА")
            val entries = KeyRemapEngine.diagnosticsSnapshot(8)
            SettingCardGroup {
                if (entries.isEmpty()) {
                    SettingSimpleRow(
                        title = "Событий пока нет",
                        subtitle = "Здесь появятся нажатия, которые увидела служба"
                    )
                } else {
                    entries.forEachIndexed { index, line ->
                        if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        Text(
                            text = line,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            TextButton(
                onClick = {
                    when (val result = KeyRemapStore.clear(context)) {
                        is KeyRemapStore.SaveResult.Ok -> {
                            reload()
                            statusMessage = "Все привязки удалены"
                        }
                        is KeyRemapStore.SaveResult.Failed ->
                            statusMessage = "Не удалось сбросить: ${result.reason}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Удалить все привязки", color = AccentColor)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Редактор привязки
// ---------------------------------------------------------------------------

@Composable
fun KeyRemapEditScreen(
    keyCode: Int,
    initialTrigger: KeyTrigger,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyInfo = KeyCatalog.info(keyCode)
    val interceptable = keyInfo?.interceptable ?: true

    var config by remember { mutableStateOf(KeyRemapStore.load(context)) }
    var trigger by remember { mutableStateOf(initialTrigger) }
    val existing = remember(config.bindings) {
        config.bindings.firstOrNull { it.keyCode == keyCode && it.trigger == initialTrigger }
    }
    var actionType by remember { mutableStateOf(existing?.action?.type ?: RemapActionType.GLOBAL) }
    var globalAction by remember { mutableStateOf(existing?.action?.globalAction ?: GlobalAction.NONE.id) }
    var mediaAction by remember { mutableStateOf(existing?.action?.mediaAction ?: MediaAction.PLAY_PAUSE.id) }
    var appPackage by remember { mutableStateOf(existing?.action?.packageName ?: "") }
    var appLabel by remember { mutableStateOf<String?>(null) }
    var intentAction by remember { mutableStateOf(existing?.action?.intentAction ?: "") }
    var intentData by remember { mutableStateOf(existing?.action?.intentData ?: "") }
    var intentType by remember { mutableStateOf(existing?.action?.intentType ?: "") }
    var intentComponent by remember { mutableStateOf(existing?.action?.intentComponent ?: "") }
    var intentPackage by remember { mutableStateOf(existing?.action?.packageName ?: "") }
    var intentFlags by remember { mutableStateOf((existing?.action?.intentFlags ?: 0).toString()) }
    val extras = remember {
        mutableStateListOf<IntentExtra>().apply { addAll(existing?.action?.extras ?: emptyList()) }
    }
    var showAppPicker by remember { mutableStateOf(false) }
    var extraTypeTarget by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appPackage) {
        appLabel = if (appPackage.isBlank()) {
            null
        } else {
            try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(appPackage, 0)).toString()
            } catch (e: Exception) {
                null
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            currentPackage = appPackage,
            onPick = {
                appPackage = it
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    extraTypeTarget?.let { index ->
        ExtraTypeDialog(
            current = extras.getOrNull(index)?.type ?: "string",
            onPick = { type ->
                if (index < extras.size) {
                    extras[index] = extras[index].copy(type = type)
                }
                extraTypeTarget = null
            },
            onDismiss = { extraTypeTarget = null }
        )
    }

    val allTriggers = remember(config.bindings, keyCode) {
        config.bindings.filter { it.keyCode == keyCode }.map { it.trigger }.toSet()
    }

    SubScreenScaffold(title = "Клавиша «${KeyCatalog.label(keyCode)}»", onBack = onBack) {

        if (!interceptable) {
            item {
                SettingCardGroup {
                    SettingSimpleRow(
                        title = "Клавиша не перехватывается системой",
                        subtitle = KeyCatalog.note(keyCode) +
                            ". Привязка сохранится, но не сработает — это ограничение платформы, а не ошибка."
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            SectionLabel("СОБЫТИЕ")
            SettingCardGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyTrigger.entries.forEach { candidate ->
                        ChoiceChip(
                            label = candidate.label,
                            selected = trigger == candidate,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                trigger = candidate
                                val found = config.bindings.firstOrNull {
                                    it.keyCode == keyCode && it.trigger == candidate
                                }
                                if (found != null) {
                                    actionType = found.action.type
                                    globalAction = found.action.globalAction
                                    mediaAction = found.action.mediaAction
                                    appPackage = found.action.packageName
                                    intentAction = found.action.intentAction
                                    intentData = found.action.intentData
                                    intentType = found.action.intentType
                                    intentComponent = found.action.intentComponent
                                    intentPackage = found.action.packageName
                                    intentFlags = found.action.intentFlags.toString()
                                    extras.clear()
                                    extras.addAll(found.action.extras)
                                }
                            }
                        )
                    }
                }
                Text(
                    text = when (trigger) {
                        KeyTrigger.SINGLE -> "Обычное нажатие. Штатное действие клавиши будет заменено."
                        KeyTrigger.DOUBLE -> "Два нажатия подряд. Первое нажатие тоже поглощается."
                        KeyTrigger.TRIPLE -> "Три нажатия подряд."
                        KeyTrigger.LONG -> "Удержание дольше «Долгого нажатия» из таймингов."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            SectionLabel("ДЕЙСТВИЕ")
            SettingCardGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RemapActionType.entries.forEach { candidate ->
                        ChoiceChip(
                            label = when (candidate) {
                                RemapActionType.GLOBAL -> "Система"
                                RemapActionType.MEDIA -> "Медиа"
                                RemapActionType.LAUNCH_APP -> "Приложение"
                                RemapActionType.INTENT -> "Интент"
                            },
                            selected = actionType == candidate,
                            modifier = Modifier.weight(1f),
                            onClick = { actionType = candidate }
                        )
                    }
                }
            }
        }

        when (actionType) {
            RemapActionType.GLOBAL -> item {
                Spacer(Modifier.height(12.dp))
                SettingCardGroup {
                    val options = GlobalAction.entries
                    options.forEachIndexed { index, option ->
                        if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        SelectableRow(
                            title = option.label,
                            selected = globalAction == option.id,
                            onClick = { globalAction = option.id }
                        )
                    }
                }
            }

            RemapActionType.MEDIA -> item {
                Spacer(Modifier.height(12.dp))
                SettingCardGroup {
                    val options = MediaAction.entries
                    options.forEachIndexed { index, option ->
                        if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        SelectableRow(
                            title = option.label,
                            selected = mediaAction == option.id,
                            onClick = { mediaAction = option.id }
                        )
                    }
                }
            }

            RemapActionType.LAUNCH_APP -> item {
                Spacer(Modifier.height(12.dp))
                SettingCardGroup {
                    SettingItem(
                        icon = Phosphor.SquaresFour,
                        title = appLabel ?: "Выбрать приложение",
                        subtitle = appPackage.ifBlank { "Нажмите, чтобы выбрать установленное приложение" },
                        onClick = { showAppPicker = true }
                    )
                }
            }

            RemapActionType.INTENT -> item {
                Spacer(Modifier.height(12.dp))
                SettingCardGroup {
                    Column(modifier = Modifier.padding(12.dp)) {
                        KeyField(
                            label = "Action",
                            value = intentAction,
                            placeholder = "android.intent.action.VIEW"
                        ) { intentAction = it }
                        KeyField(
                            label = "Data (URI)",
                            value = intentData,
                            placeholder = "https://example.org или content://…"
                        ) { intentData = it }
                        KeyField(
                            label = "MIME-тип",
                            value = intentType,
                            placeholder = "text/plain (необязательно)"
                        ) { intentType = it }
                        KeyField(
                            label = "Компонент",
                            value = intentComponent,
                            placeholder = "pkg/com.example.Activity (необязательно)"
                        ) { intentComponent = it }
                        KeyField(
                            label = "Пакет",
                            value = intentPackage,
                            placeholder = "ограничить получателя пакетом"
                        ) { intentPackage = it }
                        KeyField(
                            label = "Flags",
                            value = intentFlags,
                            placeholder = "целое число, например 268435456"
                        ) { intentFlags = it }
                    }
                }
            }
        }

        if (actionType == RemapActionType.INTENT) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionLabel("EXTRAS")
                SettingCardGroup {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (extras.isEmpty()) {
                            Text(
                                "Дополнительных параметров нет",
                                fontSize = 13.sp,
                                color = TextTertiary
                            )
                        }
                        extras.forEachIndexed { index, extra ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = extra.key,
                                    onValueChange = { extras[index] = extra.copy(key = it) },
                                    label = { Text("Ключ", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = fieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(6.dp))
                                TextButton(onClick = { extraTypeTarget = index }) {
                                    Text(extra.type, color = TextPrimary, fontSize = 12.sp)
                                }
                                TextButton(onClick = { extras.removeAt(index) }) {
                                    Icon(
                                        imageVector = Phosphor.Trash,
                                        contentDescription = "Удалить",
                                        tint = AccentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = extra.value,
                                onValueChange = { extras[index] = extra.copy(value = it) },
                                label = { Text("Значение", fontSize = 11.sp) },
                                singleLine = true,
                                colors = fieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { extras.add(IntentExtra("", "string", "")) }) {
                            Text("+ Добавить extra", color = TextPrimary)
                        }
                    }
                }
            }
        }

        item {
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(text = it, color = AccentColor, fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    error = null
                    val action = buildAction(
                        type = actionType,
                        globalAction = globalAction,
                        mediaAction = mediaAction,
                        appPackage = appPackage,
                        intentAction = intentAction,
                        intentData = intentData,
                        intentType = intentType,
                        intentComponent = intentComponent,
                        intentPackage = intentPackage,
                        intentFlags = intentFlags,
                        extras = extras.toList()
                    )
                    if (action == null) {
                        error = "Проверьте поля: не хватает обязательных данных или неверный формат."
                        return@Button
                    }
                    val updated = config.bindings
                        .filterNot { it.keyCode == keyCode && it.trigger == trigger } +
                        KeyBinding(keyCode = keyCode, trigger = trigger, action = action)
                    when (val result = KeyRemapStore.save(context, config.copy(bindings = updated))) {
                        is KeyRemapStore.SaveResult.Ok -> onSaved()
                        is KeyRemapStore.SaveResult.Failed -> error = result.reason
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = PaperBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить привязку", fontWeight = FontWeight.Bold)
            }

            if (allTriggers.contains(trigger)) {
                TextButton(
                    onClick = {
                        val updated = config.bindings.filterNot {
                            it.keyCode == keyCode && it.trigger == trigger
                        }
                        when (val result = KeyRemapStore.save(context, config.copy(bindings = updated))) {
                            is KeyRemapStore.SaveResult.Ok -> onSaved()
                            is KeyRemapStore.SaveResult.Failed -> error = result.reason
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Удалить эту привязку", color = AccentColor)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun buildAction(
    type: RemapActionType,
    globalAction: String,
    mediaAction: String,
    appPackage: String,
    intentAction: String,
    intentData: String,
    intentType: String,
    intentComponent: String,
    intentPackage: String,
    intentFlags: String,
    extras: List<IntentExtra>
): RemapAction? {
    return when (type) {
        RemapActionType.GLOBAL -> RemapAction(type = type, globalAction = globalAction)
        RemapActionType.MEDIA -> RemapAction(type = type, mediaAction = mediaAction)
        RemapActionType.LAUNCH_APP -> {
            if (appPackage.isBlank()) return null
            RemapAction(type = type, packageName = appPackage)
        }
        RemapActionType.INTENT -> {
            val flags = if (intentFlags.isBlank()) 0 else intentFlags.trim().toIntOrNull() ?: return null
            val component = intentComponent.trim()
            if (component.isNotEmpty() &&
                android.content.ComponentName.unflattenFromString(component) == null
            ) {
                return null
            }
            if (intentAction.isBlank() && intentData.isBlank() &&
                intentPackage.isBlank() && component.isEmpty()
            ) {
                return null
            }
            val cleanExtras = extras.filter { it.key.isNotBlank() }
            if (cleanExtras.any { it.type !in IntentExtra.TYPES }) return null
            RemapAction(
                type = type,
                packageName = intentPackage.trim(),
                intentAction = intentAction.trim(),
                intentData = intentData.trim(),
                intentType = intentType.trim(),
                intentComponent = component,
                intentFlags = flags,
                extras = cleanExtras
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Диалоги
// ---------------------------------------------------------------------------

@Composable
private fun CaptureKeyDialog(onCaptured: (Int) -> Unit, onCancel: () -> Unit) {
    var waiting by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        KeyRemapEngine.startCapture { event ->
            waiting = false
            onCaptured(event.keyCode)
        }
        onDispose { KeyRemapEngine.stopCapture() }
    }

    AlertDialog(
        onDismissRequest = {
            KeyRemapEngine.stopCapture()
            onCancel()
        },
        title = { Text("Нажмите клавишу", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (waiting) "Служба ждёт нажатия. Можно нажать кнопку на гарнитуре, клавишу громкости " +
                    "или любую клавишу подключённой клавиатуры."
                else "Клавиша получена",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                KeyRemapEngine.stopCapture()
                onCancel()
            }) {
                Text("Отмена", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard
    )
}

@Composable
private fun KeyPickerDialog(onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор клавиши", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                KeyCatalog.selectable.forEach { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(info.keyCode) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                info.label,
                                color = if (info.interceptable) TextPrimary else TextTertiary,
                                fontSize = 15.sp
                            )
                            if (!info.interceptable) {
                                Text(info.note, color = TextTertiary, fontSize = 11.sp)
                            }
                        }
                        if (info.interceptable) {
                            Icon(
                                imageVector = Phosphor.CaretRight,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = TextSecondary) }
        },
        containerColor = SurfaceCard
    )
}

@Composable
private fun ExtraTypeDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тип значения", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                IntentExtra.TYPES.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(type) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            type,
                            color = if (type == current) AccentColor else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (type == current) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (type == current) {
                            Icon(
                                imageVector = Phosphor.Check,
                                contentDescription = null,
                                tint = AccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = TextSecondary) }
        },
        containerColor = SurfaceCard
    )
}

private data class AppEntry(val pkg: String, val label: String)

private fun loadLaunchableApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = try {
        pm.queryIntentActivities(intent, 0)
    } catch (e: Exception) {
        emptyList()
    }
    return resolved.mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        AppEntry(pkg, info.loadLabel(pm).toString())
    }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
}

@Composable
private fun AppPickerDialog(
    currentPackage: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apps = remember { loadLaunchableApps(context) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, true) || it.pkg.contains(query, true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Запуск приложения", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск", color = TextTertiary) },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            "Ничего не найдено",
                            color = TextTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    filtered.take(60).forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app.pkg) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, color = TextPrimary, fontSize = 15.sp)
                                Text(app.pkg, color = TextTertiary, fontSize = 11.sp)
                            }
                            if (app.pkg == currentPackage) {
                                Icon(
                                    imageVector = Phosphor.Check,
                                    contentDescription = null,
                                    tint = AccentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = TextSecondary) }
        },
        containerColor = SurfaceCard
    )
}

// ---------------------------------------------------------------------------
// Мелкие элементы
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun BindingRow(
    binding: KeyBinding,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${KeyCatalog.label(binding.keyCode)} · ${binding.trigger.label}",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = binding.action.summary(context),
                color = TextSecondary,
                fontSize = 12.sp
            )
            if (!KeyCatalog.isInterceptable(binding.keyCode)) {
                Text(
                    text = "Не сработает: ${KeyCatalog.note(binding.keyCode)}",
                    color = AccentColor,
                    fontSize = 11.sp
                )
            }
        }
        TextButton(onClick = onDelete) {
            Icon(
                imageVector = Phosphor.Trash,
                contentDescription = "Удалить",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SelectableRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Phosphor.Check,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (selected) TextPrimary else BorderColor,
                shape = RoundedCornerShape(9.dp)
            )
            .background(
                color = if (selected) TextPrimary else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) PaperBackground else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TimeoutRow(title: String, valueMs: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp)
            Text("$valueMs мс", color = TextSecondary, fontSize = 12.sp)
        }
        TextButton(onClick = {
            onChange((valueMs - 50).coerceAtLeast(KeyRemapConfig.MIN_TIMEOUT_MS))
        }) {
            Text("−50", color = TextPrimary)
        }
        TextButton(onClick = {
            onChange((valueMs + 50).coerceAtMost(KeyRemapConfig.MAX_TIMEOUT_MS))
        }) {
            Text("+50", color = TextPrimary)
        }
    }
}

@Composable
private fun KeyField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(placeholder, color = TextTertiary, fontSize = 12.sp) },
        singleLine = true,
        colors = fieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TextPrimary,
    unfocusedBorderColor = BorderColor,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextTertiary
)
