package org.triangle.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.triangle.calculator.calc.CalcInput
import org.triangle.calculator.calc.CalculatorEngine
import org.triangle.calculator.calc.EvalResult
import org.triangle.calculator.calc.HistoryEntry
import org.triangle.calculator.calc.HistoryStore
import org.triangle.calculator.calc.UnitCategory
import org.triangle.calculator.calc.UnitConverter
import org.triangle.calculator.calc.UnitDef
import org.triangle.calculator.calc.UnitInput
import org.triangle.calculator.ui.icons.CalcIcons
import org.triangle.calculator.ui.icons.Phosphor
import org.triangle.calculator.ui.theme.AccentColor
import org.triangle.calculator.ui.theme.BorderColor
import org.triangle.calculator.ui.theme.KeySurface
import org.triangle.calculator.ui.theme.PaperBackground
import org.triangle.calculator.ui.theme.SurfaceCard
import org.triangle.calculator.ui.theme.TextPrimary
import org.triangle.calculator.ui.theme.TextSecondary
import org.triangle.calculator.ui.theme.TextTertiary
import org.triangle.calculator.ui.theme.TriangleCalculatorTheme
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TriangleCalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}

private enum class KeyKind { Digit, Operator, Function, Equals }

private enum class Sheet { None, History, HistorySettings, Units }

private val modeLabels = listOf("Счёт", "Площадь", "Объём", "Длина", "Масса")

private fun categoryFor(modeIndex: Int): UnitCategory? =
    UnitCategory.entries.getOrNull(modeIndex - 1)

@Composable
fun CalculatorScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val historyStore = remember { HistoryStore(context) }

    var expression by rememberSaveable { mutableStateOf("") }
    var committedResult by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }
    var showClearConfirm by remember { mutableStateOf(false) }

    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var unitInput by rememberSaveable { mutableStateOf("") }
    var activeUnitIndex by rememberSaveable { mutableIntStateOf(0) }

    var historyLimit by rememberSaveable { mutableIntStateOf(historyStore.limit) }
    var historyPersist by rememberSaveable { mutableStateOf(historyStore.persist) }

    val history = remember {
        mutableStateListOf<HistoryEntry>().apply { addAll(historyStore.load()) }
    }

    fun tapFeedback(strong: Boolean = false) {
        view.performHapticFeedback(
            if (strong) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    fun beginFreshIfNeeded() {
        if (justEvaluated) {
            expression = ""
            committedResult = ""
            justEvaluated = false
        }
        error = null
    }

    fun onDigit(digit: Char) {
        beginFreshIfNeeded()
        expression = CalcInput.digit(expression, digit)
    }

    fun onDecimal() {
        beginFreshIfNeeded()
        expression = CalcInput.decimal(expression)
    }

    fun onOperator(op: Char) {
        error = null
        if (justEvaluated) {
            expression = CalcInput.operator(committedResult, op)
            committedResult = ""
            justEvaluated = false
        } else {
            expression = CalcInput.operator(expression, op)
        }
    }

    fun onPercent() {
        error = null
        if (justEvaluated) {
            expression = committedResult + "%"
            committedResult = ""
            justEvaluated = false
        } else {
            expression = CalcInput.percent(expression)
        }
    }

    fun onToggleSign() {
        error = null
        if (justEvaluated) {
            expression = CalcInput.toggleSign(committedResult)
            committedResult = ""
            justEvaluated = false
        } else {
            expression = CalcInput.toggleSign(expression)
        }
    }

    fun onBackspace() {
        error = null
        if (justEvaluated) {
            expression = committedResult
            committedResult = ""
            justEvaluated = false
        }
        expression = CalcInput.backspace(expression)
    }

    fun onClear() {
        expression = ""
        committedResult = ""
        error = null
        justEvaluated = false
    }

    fun saveHistory() {
        while (history.size > historyLimit) {
            history.removeAt(history.lastIndex)
        }
        historyStore.save(history)
    }

    fun onEquals() {
        val closed = CalculatorEngine.normalizeExpression(expression)
        if (closed.isBlank()) return
        when (val result = CalculatorEngine.evaluate(closed)) {
            is EvalResult.Success -> {
                expression = closed
                committedResult = result.formatted
                error = null
                justEvaluated = true
                history.add(0, HistoryEntry(closed, result.formatted, System.currentTimeMillis()))
                saveHistory()
                tapFeedback(strong = true)
            }
            is EvalResult.Failure -> {
                error = result.message
                justEvaluated = false
                tapFeedback()
            }
        }
    }

    fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Результат", text))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
            }
            tapFeedback()
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось скопировать", Toast.LENGTH_SHORT).show()
        }
    }

    fun selectMode(index: Int) {
        if (index == modeIndex) return
        modeIndex = index
        activeUnitIndex = 0
        unitInput = ""
        sheet = Sheet.None
    }

    fun selectUnit(category: UnitCategory, index: Int) {
        if (index !in category.units.indices) return
        val value = UnitConverter.parse(unitInput)
        if (value != null && index != activeUnitIndex && activeUnitIndex in category.units.indices) {
            val formatted = UnitConverter.format(
                UnitConverter.convert(value, category.units[activeUnitIndex], category.units[index])
            )
            unitInput = formatted
        }
        activeUnitIndex = index
        sheet = Sheet.None
        tapFeedback()
    }

    val category = categoryFor(modeIndex)

    val preview = remember(expression, justEvaluated) {
        if (justEvaluated || expression.isBlank()) {
            null
        } else {
            (CalculatorEngine.evaluate(expression) as? EvalResult.Success)?.formatted
        }
    }

    val primaryText = when {
        error != null -> error!!
        justEvaluated -> committedResult.ifEmpty { "0" }
        preview != null -> preview
        expression.isNotBlank() -> expression
        else -> "0"
    }
    // Строку выражения показываем только когда она несёт смысл (операторы,
    // скобки, процент или уже вычисленное выражение).
    val expressionLine = expression
    val showExpressionLine = justEvaluated || expression.any { it in "()%+−×÷" }
    val canCopy = error == null && primaryText.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Header(
                historyCount = history.size,
                onOpenHistory = { sheet = Sheet.History }
            )
            ModeTabs(selected = modeIndex, onSelect = ::selectMode)

            if (category == null) {
                NumberBody(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    expression = expressionLine,
                    showExpression = showExpressionLine,
                    primaryText = primaryText,
                    isError = error != null,
                    canCopy = canCopy,
                    onCopy = { copyToClipboard(primaryText) },
                    onDigit = ::onDigit,
                    onDecimal = ::onDecimal,
                    onOperator = ::onOperator,
                    onPercent = ::onPercent,
                    onToggleSign = ::onToggleSign,
                    onBackspace = ::onBackspace,
                    onClear = ::onClear,
                    onEquals = ::onEquals,
                    onTapFeedback = { tapFeedback() }
                )
            } else {
                ConverterBody(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    category = category,
                    input = unitInput,
                    activeUnitIndex = activeUnitIndex.coerceIn(category.units.indices),
                    onDigit = { unitInput = UnitInput.digit(unitInput, it) },
                    onDecimal = { unitInput = UnitInput.decimal(unitInput) },
                    onBackspace = { unitInput = UnitInput.backspace(unitInput) },
                    onClear = { unitInput = UnitInput.clear() },
                    onToggleSign = { unitInput = UnitInput.toggleSign(unitInput) },
                    onOpenUnits = { sheet = Sheet.Units },
                    onSelectUnit = { selectUnit(category, it) },
                    onTapFeedback = { tapFeedback() }
                )
            }
        }

        // Затемнение под панелью
        AnimatedVisibility(
            visible = sheet != Sheet.None,
            enter = fadeIn(tween(200, easing = LinearOutSlowInEasing)),
            exit = fadeOut(tween(160, easing = FastOutSlowInEasing)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { sheet = Sheet.None }
            )
        }

        AnimatedVisibility(
            visible = sheet != Sheet.None,
            enter = slideInVertically(
                animationSpec = tween(360, easing = LinearOutSlowInEasing)
            ) { fullHeight -> fullHeight } + fadeIn(tween(180, easing = LinearOutSlowInEasing)),
            exit = slideOutVertically(
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight } + fadeOut(tween(140, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            when (sheet) {
                Sheet.History -> HistorySheet(
                    history = history,
                    onOpenSettings = { sheet = Sheet.HistorySettings },
                    onClose = { sheet = Sheet.None },
                    onClear = { showClearConfirm = true },
                    onSelect = { entry ->
                        modeIndex = 0
                        expression = entry.result
                        committedResult = ""
                        error = null
                        justEvaluated = false
                        sheet = Sheet.None
                        tapFeedback()
                    }
                )

                Sheet.HistorySettings -> HistorySettingsSheet(
                    limit = historyLimit,
                    persist = historyPersist,
                    onLimitChange = { value ->
                        historyLimit = value
                        historyStore.limit = value
                        saveHistory()
                    },
                    onPersistChange = { value ->
                        historyPersist = value
                        historyStore.persist = value
                        saveHistory()
                    },
                    onClear = { showClearConfirm = true },
                    onBack = { sheet = Sheet.History },
                    onClose = { sheet = Sheet.None }
                )

                Sheet.Units -> {
                    val unitsCategory = category
                    if (unitsCategory != null) {
                        UnitPickerSheet(
                            category = unitsCategory,
                            activeIndex = activeUnitIndex.coerceIn(unitsCategory.units.indices),
                            onSelect = { selectUnit(unitsCategory, it) },
                            onClose = { sheet = Sheet.None }
                        )
                    }
                }

                Sheet.None -> Unit
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Очистить историю?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Все сохранённые вычисления будут удалены без возможности восстановления.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    history.clear()
                    historyStore.save(history)
                    showClearConfirm = false
                }) {
                    Text("Очистить", color = AccentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

@Composable
private fun Header(historyCount: Int, onOpenHistory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenHistory
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CalcIcons.History,
                contentDescription = "История вычислений",
                modifier = Modifier.size(24.dp),
                tint = TextSecondary
            )
            if (historyCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(5.dp)
                        .background(AccentColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ModeTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        modeLabels.forEachIndexed { index, label ->
            val active = index == selected
            val color by animateColorAsState(
                targetValue = if (active) TextPrimary else TextTertiary,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "tabColor"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(top = 4.dp, bottom = 2.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = color
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(
                            if (active) AccentColor else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun NumberBody(
    modifier: Modifier = Modifier,
    expression: String,
    showExpression: Boolean,
    primaryText: String,
    isError: Boolean,
    canCopy: Boolean,
    onCopy: () -> Unit,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onTapFeedback: () -> Unit
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomStart
        ) {
            Display(
                expression = expression,
                showExpression = showExpression,
                primaryText = primaryText,
                isError = isError,
                canCopy = canCopy,
                onCopy = onCopy
            )
        }

        Spacer(Modifier.height(20.dp))

        Keypad(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.25f),
            onDigit = onDigit,
            onDecimal = onDecimal,
            onOperator = onOperator,
            onPercent = onPercent,
            onToggleSign = onToggleSign,
            onBackspace = onBackspace,
            onClear = onClear,
            onEquals = onEquals,
            onTapFeedback = onTapFeedback
        )
        Spacer(Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Display(
    expression: String,
    showExpression: Boolean,
    primaryText: String,
    isError: Boolean,
    canCopy: Boolean,
    onCopy: () -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(expression, scrollState.maxValue) {
        scrollState.animateScrollTo(
            value = scrollState.maxValue,
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        )
    }

    val targetFontSize = when {
        isError -> 22f
        primaryText.length <= 6 -> 84f
        primaryText.length <= 9 -> 72f
        primaryText.length <= 12 -> 58f
        primaryText.length <= 16 -> 44f
        else -> 34f
    }
    val resultFontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "resultFontSize"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = { if (canCopy) onCopy() }
            )
            .padding(bottom = 4.dp)
    ) {
        if (showExpression && expression.isNotBlank()) {
            Text(
                text = expression,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = TextTertiary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.horizontalScroll(scrollState)
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = primaryText,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = resultFontSize.sp,
            lineHeight = (resultFontSize * 1.02f).sp,
            letterSpacing = (-1.5).sp,
            color = if (isError) AccentColor else TextPrimary,
            textAlign = TextAlign.Start,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Keypad(
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onTapFeedback: () -> Unit
) {
    val rows: List<List<Pair<String, Pair<KeyKind, () -> Unit>>>> = listOf(
        listOf(
            "C" to (KeyKind.Function to onClear),
            "±" to (KeyKind.Function to onToggleSign),
            "%" to (KeyKind.Function to onPercent),
            "⌫" to (KeyKind.Function to onBackspace)
        ),
        listOf(
            "7" to (KeyKind.Digit to { onDigit('7') }),
            "8" to (KeyKind.Digit to { onDigit('8') }),
            "9" to (KeyKind.Digit to { onDigit('9') }),
            "÷" to (KeyKind.Operator to { onOperator('÷') })
        ),
        listOf(
            "4" to (KeyKind.Digit to { onDigit('4') }),
            "5" to (KeyKind.Digit to { onDigit('5') }),
            "6" to (KeyKind.Digit to { onDigit('6') }),
            "×" to (KeyKind.Operator to { onOperator('×') })
        ),
        listOf(
            "1" to (KeyKind.Digit to { onDigit('1') }),
            "2" to (KeyKind.Digit to { onDigit('2') }),
            "3" to (KeyKind.Digit to { onDigit('3') }),
            "−" to (KeyKind.Operator to { onOperator('−') })
        ),
        listOf(
            "0" to (KeyKind.Digit to { onDigit('0') }),
            "," to (KeyKind.Digit to onDecimal),
            "=" to (KeyKind.Equals to onEquals),
            "+" to (KeyKind.Operator to { onOperator('+') })
        )
    )

    KeypadAppearance(modifier = modifier) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (label, config) ->
                    val (kind, action) = config
                    CalcKey(
                        label = label,
                        kind = kind,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onTapFeedback()
                            action()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadAppearance(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30L)
        entered = true
    }
    val appear by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(320, easing = LinearOutSlowInEasing),
        label = "keypadAppear"
    )

    Column(
        modifier = modifier.graphicsLayer {
            alpha = appear
            translationY = (1f - appear) * 18.dp.toPx()
        },
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun CalcKey(
    label: String,
    kind: KeyKind,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(110, easing = FastOutSlowInEasing),
        label = "keyScale"
    )

    val base = if (kind == KeyKind.Equals) AccentColor else KeySurface
    val background by animateColorAsState(
        targetValue = if (pressed) lerp(base, TextPrimary, 0.10f) else base,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "keyColor"
    )

    val textColor = if (kind == KeyKind.Equals) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        TextPrimary
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = CalcIcons.Backspace,
                contentDescription = "Удалить символ",
                modifier = Modifier.size(24.dp),
                tint = textColor
            )
        } else {
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = when (kind) {
                    KeyKind.Digit -> 30.sp
                    KeyKind.Operator -> 30.sp
                    KeyKind.Function -> if (label == "±") 26.sp else 24.sp
                    KeyKind.Equals -> 32.sp
                },
                fontWeight = if (kind == KeyKind.Equals) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Пересчёт единиц                                                     */
/* ------------------------------------------------------------------ */

private data class KeySpec(
    val label: String,
    val kind: KeyKind,
    val weight: Float = 1f,
    val action: () -> Unit
)

@Composable
private fun ConverterBody(
    modifier: Modifier = Modifier,
    category: UnitCategory,
    input: String,
    activeUnitIndex: Int,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onToggleSign: () -> Unit,
    onOpenUnits: () -> Unit,
    onSelectUnit: (Int) -> Unit,
    onTapFeedback: () -> Unit
) {
    val activeUnit = category.units[activeUnitIndex]
    val value = UnitConverter.parse(input)
    val conversions = remember(value, activeUnit, category) {
        if (value == null) {
            category.units.filter { it !== activeUnit }.map { it to "—" }
        } else {
            UnitConverter.convertAll(value, activeUnit, category.units)
        }
    }

    Column(modifier = modifier) {
        Spacer(Modifier.height(10.dp))

        ConverterValue(
            input = input,
            unit = activeUnit,
            onOpenUnits = onOpenUnits
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(conversions, key = { it.first.symbol }) { (unit, text) ->
                ConversionRow(
                    unit = unit,
                    value = text,
                    onClick = { onSelectUnit(category.units.indexOf(unit)) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        ConverterKeypad(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp),
            onDigit = onDigit,
            onDecimal = onDecimal,
            onBackspace = onBackspace,
            onClear = onClear,
            onToggleSign = onToggleSign,
            onTapFeedback = onTapFeedback
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ConverterValue(
    input: String,
    unit: UnitDef,
    onOpenUnits: () -> Unit
) {
    val text = input.ifEmpty { "0" }
    val targetFontSize = when {
        text.length <= 6 -> 84f
        text.length <= 9 -> 70f
        text.length <= 12 -> 56f
        text.length <= 16 -> 44f
        else -> 34f
    }
    val fontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "converterFontSize"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.02f).sp,
            letterSpacing = (-1.5).sp,
            color = TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenUnits
                )
                .padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
            Text(
                text = unit.symbol,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.6.sp,
                color = AccentColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = unit.name,
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Phosphor.CaretRight,
                contentDescription = "Выбрать единицу",
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer { rotationZ = 90f },
                tint = TextTertiary
            )
        }
    }
}

@Composable
private fun ConversionRow(
    unit: UnitDef,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = unit.symbol,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = unit.name,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = if (value == "—") TextTertiary else TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun ConverterKeypad(
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onToggleSign: () -> Unit,
    onTapFeedback: () -> Unit
) {
    val rows = listOf(
        listOf(
            KeySpec("7", KeyKind.Digit) { onDigit('7') },
            KeySpec("8", KeyKind.Digit) { onDigit('8') },
            KeySpec("9", KeyKind.Digit) { onDigit('9') },
            KeySpec("⌫", KeyKind.Function, action = onBackspace)
        ),
        listOf(
            KeySpec("4", KeyKind.Digit) { onDigit('4') },
            KeySpec("5", KeyKind.Digit) { onDigit('5') },
            KeySpec("6", KeyKind.Digit) { onDigit('6') },
            KeySpec("C", KeyKind.Function, action = onClear)
        ),
        listOf(
            KeySpec("1", KeyKind.Digit) { onDigit('1') },
            KeySpec("2", KeyKind.Digit) { onDigit('2') },
            KeySpec("3", KeyKind.Digit) { onDigit('3') },
            KeySpec(",", KeyKind.Digit, action = onDecimal)
        ),
        listOf(
            KeySpec("±", KeyKind.Function, action = onToggleSign),
            KeySpec("0", KeyKind.Digit, weight = 3f) { onDigit('0') }
        )
    )

    KeypadAppearance(modifier = modifier) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    CalcKey(
                        label = key.label,
                        kind = key.kind,
                        modifier = Modifier
                            .weight(key.weight)
                            .fillMaxHeight(),
                        onClick = {
                            onTapFeedback()
                            key.action()
                        }
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Нижние панели                                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun BottomSheetContainer(
    heightFraction: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(SurfaceCard)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 38.dp, height = 4.dp)
                .background(TextTertiary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun SheetActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
    }
}

@Composable
private fun HistorySheet(
    history: List<HistoryEntry>,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onSelect: (HistoryEntry) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

    BottomSheetContainer(heightFraction = 0.7f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "История",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            if (history.isNotEmpty()) {
                Text(
                    text = history.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.weight(1f))
            SheetActionIcon(
                icon = Phosphor.GearSix,
                description = "Настройки истории",
                tint = TextSecondary,
                onClick = onOpenSettings
            )
            if (history.isNotEmpty()) {
                SheetActionIcon(
                    icon = Phosphor.Trash,
                    description = "Очистить историю",
                    tint = TextSecondary,
                    onClick = onClear
                )
            }
            SheetActionIcon(
                icon = Phosphor.X,
                description = "Закрыть историю",
                onClick = onClose
            )
        }

        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = CalcIcons.History,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = TextTertiary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Пока пусто",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Здесь появятся ваши вычисления",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(history, key = { "${it.timestamp}-${it.expression}" }) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(entry) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = entry.expression,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "= ${entry.result}",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 22.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (entry.timestamp > 0L) {
                                Text(
                                    text = timeFormat.format(Date(entry.timestamp)),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HistorySettingsSheet(
    limit: Int,
    persist: Boolean,
    onLimitChange: (Int) -> Unit,
    onPersistChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    BottomSheetContainer(heightFraction = 0.62f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetActionIcon(
                icon = Phosphor.ArrowLeft,
                description = "Назад к истории",
                tint = TextSecondary,
                onClick = onBack
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Настройки истории",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            SheetActionIcon(
                icon = Phosphor.X,
                description = "Закрыть",
                onClick = onClose
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сохранять историю",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Не пропадёт после закрытия приложения",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
            Spacer(Modifier.width(12.dp))
            TriangleSwitch(checked = persist, onChange = onPersistChange)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Хранить записей",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            HistoryStore.LIMIT_OPTIONS.forEach { option ->
                val active = option == limit
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onLimitChange(option) }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Text(
                        text = option.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp,
                        color = if (active) AccentColor else TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(
                                if (active) AccentColor else Color.Transparent,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Лишние записи удаляются сразу при уменьшении лимита.",
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            color = TextTertiary
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClear
                )
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Очистить историю",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = AccentColor
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun UnitPickerSheet(
    category: UnitCategory,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    BottomSheetContainer(heightFraction = 0.6f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Единицы",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                Text(
                    text = category.label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = TextTertiary
                )
            }
            SheetActionIcon(
                icon = Phosphor.X,
                description = "Закрыть выбор единицы",
                onClick = onClose
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(category.units) { index, unit ->
                val active = index == activeIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) }
                        .padding(vertical = 13.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = unit.symbol,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (active) AccentColor else TextPrimary,
                        modifier = Modifier.width(64.dp)
                    )
                    Text(
                        text = unit.name,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) {
                        Icon(
                            imageVector = Phosphor.Check,
                            contentDescription = "Выбрано",
                            modifier = Modifier.size(18.dp),
                            tint = AccentColor
                        )
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TriangleSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "switchProgress"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "switchKnob"
    )
    val trackColor = lerp(BorderColor, AccentColor, progress)
    val knobColor = lerp(TextTertiary, PaperBackground, progress)

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(20.dp)
                .background(knobColor, CircleShape)
        )
    }
}
