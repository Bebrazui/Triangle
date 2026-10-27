package org.triangle.calculator.calc

/**
 * Чистые функции редактирования строки выражения.
 * Никаких скрытых побочных эффектов: на вход строка — на выход строка.
 */
object CalcInput {

    private fun last(expr: String): Char? = expr.lastOrNull()

    private fun isOperator(c: Char?): Boolean =
        c == '+' || c == '−' || c == '×' || c == '÷' || c == '-' || c == '*' || c == '/'

    private fun endsWithValue(expr: String): Boolean {
        val c = last(expr) ?: return false
        return c.isDigit() || c == ')' || c == '%'
    }

    private fun currentNumberStart(expr: String): Int {
        for (i in expr.lastIndex downTo 0) {
            val c = expr[i]
            if (isOperator(c) || c == '(' || c == ')') return i + 1
        }
        return 0
    }

    private fun currentNumber(expr: String): String = expr.substring(currentNumberStart(expr))

    private fun unaryMinusAt(expr: String, index: Int): Boolean {
        if (index < 0 || index >= expr.length) return false
        val c = expr[index]
        if (c != '−' && c != '-') return false
        if (index == 0) return true
        val before = expr[index - 1]
        return isOperator(before) || before == '('
    }

    fun digit(expr: String, digit: Char): String {
        if (expr.length >= CalculatorEngine.MAX_EXPRESSION_LENGTH) return expr
        val base = if (last(expr) == ')' || last(expr) == '%') "$expr×" else expr
        val number = currentNumber(base)
        val digitsInNumber = number.count { it.isDigit() }
        if (digitsInNumber >= 15) return base
        // Ведущий ноль заменяется первой цифрой, но «0,» сохраняется.
        if (number == "0" || number == "−0") {
            return base.dropLast(1) + digit
        }
        return base + digit
    }

    fun decimal(expr: String): String {
        if (expr.length >= CalculatorEngine.MAX_EXPRESSION_LENGTH) return expr
        val base = if (last(expr) == ')' || last(expr) == '%') "$expr×" else expr
        val number = currentNumber(base)
        if (number.contains(',')) return base
        return when {
            number.isEmpty() -> base + "0,"
            last(base) == '(' -> base + "0,"
            else -> base + ","
        }
    }

    fun operator(expr: String, op: Char): String {
        if (expr.length >= CalculatorEngine.MAX_EXPRESSION_LENGTH) return expr
        if (expr.isEmpty()) return if (op == '−') "−" else expr
        if (last(expr) == '(') return if (op == '−') expr + '−' else expr
        if (last(expr) == ',') return operator(expr + "0", op)
        if (isOperator(last(expr))) {
            val lastIndex = expr.lastIndex
            // Унарный минус нельзя заменить другим оператором — только снять.
            if (unaryMinusAt(expr, lastIndex)) {
                return if (op == '−') expr else expr.dropLast(1)
            }
            return expr.dropLast(1) + op
        }
        return expr + op
    }

    fun percent(expr: String): String {
        if (expr.length >= CalculatorEngine.MAX_EXPRESSION_LENGTH) return expr
        val c = last(expr) ?: return expr
        return if (c.isDigit() || c == ')') expr + '%' else expr
    }

    fun openParenthesis(expr: String): String {
        if (expr.length >= CalculatorEngine.MAX_EXPRESSION_LENGTH) return expr
        return if (endsWithValue(expr)) "$expr×(" else "$expr("
    }

    fun closeParenthesis(expr: String): String {
        if (expr.length >= CalculatorEngine.MAX_EXPRESSION_LENGTH) return expr
        if (openCount(expr) <= 0) return expr
        return if (endsWithValue(expr)) "$expr)" else expr
    }

    /** Меняет знак последнего числа: 5 → −5, 5+3 → 5+−3 (парсер понимает унарный минус). */
    fun toggleSign(expr: String): String {
        if (expr.isEmpty()) return "−"
        if (last(expr) == '(') return "$expr−"
        var index = expr.length
        while (index > 0 && (expr[index - 1].isDigit() || expr[index - 1] == ',')) index--
        if (index == expr.length) return expr
        if (index > 0 && unaryMinusAt(expr, index - 1)) {
            return expr.removeRange(index - 1, index)
        }
        return expr.substring(0, index) + "−" + expr.substring(index)
    }

    fun backspace(expr: String): String = if (expr.isEmpty()) expr else expr.dropLast(1)

    fun openCount(expr: String): Int {
        var open = 0
        for (c in expr) {
            if (c == '(') open++
            if (c == ')') open--
        }
        return open.coerceAtLeast(0)
    }

    /** Есть ли незавершённое число, к которому можно дописать цифру. */
    fun canContinueNumber(expr: String): Boolean = endsWithValue(expr)
}
