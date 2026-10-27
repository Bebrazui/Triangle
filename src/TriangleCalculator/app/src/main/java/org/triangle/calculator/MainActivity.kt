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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.triangle.calculator.calc.CalcInput
import org.triangle.calculator.calc.CalculatorEngine
import org.triangle.calculator.calc.EvalResult
import org.triangle.calculator.calc.HistoryEntry
import org.triangle.calculator.calc.HistoryStore
import org.triangle.calculator.ui.icons.CalcIcons
import org.triangle.calculator.ui.icons.Phosphor
import org.triangle.calculator.ui.theme.AccentColor
import org.triangle.calculator.ui.theme.BorderColor
import org.triangle.calculator.ui.theme.BorderBold
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
    val scope = rememberCoroutineScope()
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
    val flash = remember { Animatable(0f) }

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

    fun onOpenParenthesis() {
        beginFreshIfNeeded()
        expression = CalcInput.openParenthesis(expression)
    }

    fun onCloseParenthesis() {
        error = null
        expression = CalcInput.closeParenthesis(expression)
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
                scope.launch {
                    flash.snapTo(0f)
                    flash.animateTo(1f, tween(durationMillis = 90, easing = LinearOutSlowInEasing))
                    flash.animateTo(0f, tween(durationMillis = 720, easing = LinearOutSlowInEasing))
                }
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

    val expressionLine = expression.ifBlank { "0" }
    val primaryText = when {
        error != null -> error!!
        justEvaluated -> committedResult.ifEmpty { "0" }
        preview != null -> preview
        else -> "0"
    }
    val canCopy = error == null && primaryText.isNotBlank() &&
        (justEvaluated || preview != null || expression.isNotBlank())

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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Header(
                historyCount = history.size,
                onOpenHistory = { showHistory = true }
            )

            Spacer(Modifier.height(14.dp))
            DisplayCard(
                expression = expressionLine,
                primaryText = primaryText,
                isError = error != null,
                justEvaluated = justEvaluated,
                flash = flash.value,
                canCopy = canCopy,
                onCopy = { copyToClipboard(primaryText) }
            )

            Spacer(Modifier.height(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Keypad(
                    onDigit = ::onDigit,
                    onDecimal = ::onDecimal,
                    onOperator = ::onOperator,
                    onPercent = ::onPercent,
                    onToggleSign = ::onToggleSign,
                    onOpenParenthesis = ::onOpenParenthesis,
                    onCloseParenthesis = ::onCloseParenthesis,
                    onBackspace = ::onBackspace,
                    onClear = ::onClear,
                    onTapFeedback = { tapFeedback() }
                )
                Spacer(Modifier.height(10.dp))
                CalcKey(
                    label = "=",
                    kind = KeyKind.Equals,
                    index = 20,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    onClick = {
                        tapFeedback()
                        onEquals()
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Затемнение под историей
        AnimatedVisibility(
            visible = showHistory,
            enter = fadeIn(tween(200, easing = LinearOutSlowInEasing)),
            exit = fadeOut(tween(160, easing = FastOutSlowInEasing)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showHistory = false }
            )
        }

        // Панель истории выезжает снизу
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Калькулятор",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "TRIANGLE OS",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.6.sp
            )
        }
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
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
                    modifier = Modifier.size(19.dp),
                    tint = TextPrimary
                )
            }
            if (historyCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(AccentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (historyCount > 9) "9+" else historyCount.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayCard(
    expression: String,
    primaryText: String,
    isError: Boolean,
    justEvaluated: Boolean,
    flash: Float,
    canCopy: Boolean,
    onCopy: () -> Unit
) {
    val borderColor = lerp(BorderColor, AccentColor, flash)
    val scrollState = rememberScrollState()
    LaunchedEffect(expression, scrollState.maxValue) {
        // Автопрокрутка к концу выражения с мягким ease-out.
        scrollState.animateScrollTo(
            value = scrollState.maxValue,
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        )
    }
    val targetFontSize = when {
        isError -> 22f
        primaryText.length <= 9 -> 56f
        primaryText.length <= 13 -> 44f
        primaryText.length <= 18 -> 34f
        else -> 26f
    }
    val resultFontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "resultFontSize"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ВЫРАЖЕНИЕ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.weight(1f))
                if (canCopy) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCopy
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CalcIcons.Copy,
                            contentDescription = "Скопировать результат",
                            modifier = Modifier.size(14.dp),
                            tint = TextTertiary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "КОПИЯ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 30.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = expression,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.horizontalScroll(scrollState)
                )
            }

            Spacer(Modifier.height(2.dp))

            AnimatedContent(
                targetState = primaryText,
                transitionSpec = {
                    (fadeIn(tween(240, easing = LinearOutSlowInEasing)) +
                        slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { height -> height / 4 })
                        .togetherWith(
                            fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(160, easing = FastOutSlowInEasing)) { height -> -height / 6 }
                        )
                },
                label = "result"
            ) { text ->
                Text(
                    text = text,
                    fontFamily = FontFamily.Serif,
                    fontSize = resultFontSize.sp,
                    lineHeight = (resultFontSize * 1.05f).sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isError) AccentColor else TextPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onOperator: (Char) -> Unit,
    onPercent: () -> Unit,
    onToggleSign: () -> Unit,
    onOpenParenthesis: () -> Unit,
    onCloseParenthesis: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onTapFeedback: () -> Unit
) {
    val rows: List<List<Pair<String, Pair<KeyKind, () -> Unit>>>> = listOf(
        listOf(
            "C" to (KeyKind.Function to onClear),
            "(" to (KeyKind.Function to onOpenParenthesis),
            ")" to (KeyKind.Function to onCloseParenthesis),
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
            "±" to (KeyKind.Function to onToggleSign),
            "0" to (KeyKind.Digit to { onDigit('0') }),
            "," to (KeyKind.Digit to onDecimal),
            "+" to (KeyKind.Operator to { onOperator('+') })
        )
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        var keyIndex = 0
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                row.forEach { (label, config) ->
                    val (kind, action) = config
                    CalcKey(
                        label = label,
                        kind = kind,
                        index = keyIndex,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            onTapFeedback()
                            action()
                        }
                    )
                    keyIndex++
                }
            }
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    kind: KeyKind,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "keyScale"
    )

    val baseColor = if (kind == KeyKind.Equals) AccentColor else SurfaceCard
    val targetColor = if (pressed) {
        if (kind == KeyKind.Equals) lerp(AccentColor, TextPrimary, 0.10f)
        else lerp(SurfaceCard, AccentColor, 0.10f)
    } else {
        baseColor
    }
    val background by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "keyColor"
    )

    // Появление клавиш лесенкой — ease-out, без пружин.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 14L)
        appeared = true
    }
    val appear by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(320, easing = LinearOutSlowInEasing),
        label = "keyAppear"
    )

    val textColor = when (kind) {
        KeyKind.Digit -> TextPrimary
        KeyKind.Operator -> AccentColor
        KeyKind.Function -> TextSecondary
        KeyKind.Equals -> MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = appear
                translationY = (1f - appear) * 14.dp.toPx()
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (kind == KeyKind.Equals) BorderBold else BorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = if (kind == KeyKind.Digit || kind == KeyKind.Operator) FontFamily.Serif else FontFamily.SansSerif,
            fontSize = when (kind) {
                KeyKind.Digit -> 26.sp
                KeyKind.Operator -> 28.sp
                KeyKind.Function -> if (label == "⌫") 20.sp else 17.sp
                KeyKind.Equals -> 30.sp
            },
            fontWeight = when (kind) {
                KeyKind.Equals -> FontWeight.SemiBold
                KeyKind.Function -> FontWeight.Medium
                else -> FontWeight.Normal
            },
            color = textColor
        )
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
            .border(
                width = 1.dp,
                color = BorderColor,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
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
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "История",
                style = MaterialTheme.typography.headlineMedium,
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
                        .size(36.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(9.dp))
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
                        modifier = Modifier.size(17.dp),
                        tint = TextSecondary
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(9.dp))
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
                    modifier = Modifier.size(17.dp),
                    tint = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

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
                    modifier = Modifier.size(32.dp),
                    tint = TextTertiary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Пока пусто",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextSecondary,
                    fontSize = 20.sp
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
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(history, key = { "${it.timestamp}-${it.expression}" }) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(entry) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = entry.expression,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "= ${entry.result}",
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (entry.timestamp > 0L) {
                                Text(
                                    text = timeFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
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
