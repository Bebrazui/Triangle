package org.triangle.calculator.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/**
 * Результат вычисления. Ошибки типизированы строкой для показа пользователю,
 * никаких «молчаливых» нулей или NaN.
 */
sealed class EvalResult {
    data class Success(val value: Double, val formatted: String) : EvalResult()
    data class Failure(val message: String) : EvalResult()
}

private class CalcException(message: String) : Exception(message)

/**
 * Рекурсивный парсер выражений без eval/reflection.
 *
 * Грамматика:
 *   expr    := term (('+' | '−' | '-') term)*
 *   term    := unary (('×' | '÷' | '*' | '/') unary)*
 *   unary   := ('−' | '-' | '+') unary | postfix
 *   postfix := primary '%'*
 *   primary := number | '(' expr ')'
 *
 * Проценты трактуются как деление на 100 (50+10% = 50,1 — как в Google Калькуляторе).
 */
object CalculatorEngine {

    const val MAX_EXPRESSION_LENGTH = 96

    fun evaluate(raw: String): EvalResult {
        val normalized = normalizeExpression(raw)
        if (normalized.isBlank()) return EvalResult.Failure("Пустое выражение")
        return try {
            val value = Parser(normalized).parse()
            when {
                value.isNaN() -> EvalResult.Failure("Результат не число")
                value.isInfinite() -> EvalResult.Failure("Слишком большое число")
                else -> EvalResult.Success(value, format(value))
            }
        } catch (e: CalcException) {
            EvalResult.Failure(e.message ?: "Некорректное выражение")
        } catch (e: Exception) {
            EvalResult.Failure("Некорректное выражение")
        }
    }

    /**
     * Приводит введённое выражение к вычислимому виду:
     * убирает висящие операторы/запятые и закрывает незакрытые скобки.
     */
    fun normalizeExpression(raw: String): String {
        var s = raw.trim()
        while (s.isNotEmpty()) {
            val last = s.last()
            if (last in "+−-×÷*/,") {
                s = s.dropLast(1)
            } else {
                break
            }
        }
        if (s.isEmpty() || s == "−" || s == "-") return ""
        var open = 0
        for (c in s) {
            if (c == '(') open++
            if (c == ')') open--
        }
        if (open > 0) s += ")".repeat(open)
        return s
    }

    /** Форматирование без экспоненты для обычных чисел, с запятой как разделителем. */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        val absValue = abs(value)
        if (absValue >= 1e18 || absValue < 1e-9) {
            return scientific(value)
        }
        val stripped = BigDecimal.valueOf(value)
            .round(MathContext(12, RoundingMode.HALF_UP))
            .stripTrailingZeros()
        var plain = stripped.toPlainString()
        if (plain.contains('.')) {
            plain = plain.trimEnd('0').trimEnd('.')
        }
        if (plain.length > 18) return scientific(value)
        return plain.replace('.', ',')
    }

    private fun scientific(value: Double): String {
        val raw = String.format(Locale.US, "%.8e", value)
        val parts = raw.split("e")
        val mantissa = parts[0].trimEnd('0').trimEnd('.')
        val exponent = (parts.getOrNull(1) ?: "0").toIntOrNull() ?: 0
        return "${mantissa.replace('.', ',')}e$exponent"
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun parse(): Double {
            skipSpaces()
            if (pos >= src.length) throw CalcException("Пустое выражение")
            val value = expr()
            skipSpaces()
            if (pos != src.length) throw CalcException("Некорректное выражение")
            return value
        }

        private fun expr(): Double {
            var value = term()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { pos++; value += term() }
                    '−', '-' -> { pos++; value -= term() }
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = unary()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '×', '*' -> { pos++; value *= unary() }
                    '÷', '/' -> {
                        pos++
                        val denominator = unary()
                        if (denominator == 0.0) throw CalcException("Деление на ноль")
                        value /= denominator
                    }
                    else -> return value
                }
            }
        }

        private fun unary(): Double {
            skipSpaces()
            return when (peek()) {
                '−', '-' -> { pos++; -unary() }
                '+' -> { pos++; unary() }
                else -> postfix()
            }
        }

        private fun postfix(): Double {
            var value = primary()
            while (true) {
                skipSpaces()
                if (peek() == '%') {
                    pos++
                    value /= 100.0
                } else {
                    return value
                }
            }
        }

        private fun primary(): Double {
            skipSpaces()
            val ch = peek() ?: throw CalcException("Некорректное выражение")
            if (ch == '(') {
                pos++
                val value = expr()
                skipSpaces()
                if (peek() != ')') throw CalcException("Незакрытая скобка")
                pos++
                return value
            }
            return number()
        }

        private fun number(): Double {
            skipSpaces()
            val start = pos
            while (pos < src.length && src[pos].isDigit()) pos++
            if (pos < src.length && (src[pos] == '.' || src[pos] == ',')) {
                pos++
                while (pos < src.length && src[pos].isDigit()) pos++
            }
            if (pos == start) throw CalcException("Некорректное выражение")
            // Экспоненциальная запись нужна для повторного использования
            // очень больших/маленьких результатов из истории.
            if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                val expStart = pos
                pos++
                if (pos < src.length && (src[pos] == '+' || src[pos] == '−' || src[pos] == '-')) pos++
                var expDigits = 0
                while (pos < src.length && src[pos].isDigit()) {
                    pos++
                    expDigits++
                }
                if (expDigits == 0) pos = expStart
            }
            val text = src.substring(start, pos)
                .replace(',', '.')
                .replace('−', '-')
            return text.toDoubleOrNull() ?: throw CalcException("Некорректное число")
        }

        private fun skipSpaces() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char? = if (pos < src.length) src[pos] else null
    }
}
