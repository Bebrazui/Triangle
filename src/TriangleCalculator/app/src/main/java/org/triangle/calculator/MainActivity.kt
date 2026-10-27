package org.triangle.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import android.text.format.DateFormat
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

@Composable
fun CalculatorScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val historyStore = remember { HistoryStore(context) }

    var expression by rememberSaveable { mutableStateOf("") }
    var committedResult by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var justEvaluated by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

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
                while (history.size > HistoryStore.MAX_ENTRIES) {
                    history.removeAt(history.lastIndex)
                }
                historyStore.save(history)
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
                onOpenHistory = { showHistory = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                Display(
                    expression = expressionLine,
                    showExpression = showExpressionLine,
                    primaryText = primaryText,
                    isError = error != null,
                    canCopy = canCopy,
                    onCopy = { copyToClipboard(primaryText) }
                )
            }

            Spacer(Modifier.height(20.dp))

            Keypad(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.25f),
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
            Spacer(Modifier.height(10.dp))
        }

        AnimatedVisibility(
            visible = showHistory,
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
                    ) { showHistory = false }
            )
        }

        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(
                animationSpec = tween(360, easing = LinearOutSlowInEasing)
            ) { fullHeight -> fullHeight } + fadeIn(tween(180, easing = LinearOutSlowInEasing)),
            exit = slideOutVertically(
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) { fullHeight -> fullHeight } + fadeOut(tween(140, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HistorySheet(
                history = history,
                onClose = { showHistory = false },
                onClear = { showClearConfirm = true },
                onSelect = { entry ->
                    expression = entry.result
                    committedResult = ""
                    error = null
                    justEvaluated = false
                    showHistory = false
                    tapFeedback()
                }
            )
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

@Composable
private fun HistorySheet(
    history: List<HistoryEntry>,
    onClose: () -> Unit,
    onClear: () -> Unit,
    onSelect: (HistoryEntry) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
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
            if (history.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Phosphor.Trash,
                        contentDescription = "Очистить историю",
                        modifier = Modifier.size(20.dp),
                        tint = TextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Phosphor.X,
                    contentDescription = "Закрыть историю",
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
            }
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
