package org.triangle.calculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorEngineTest {

    private fun value(expression: String): String? =
        (CalculatorEngine.evaluate(expression) as? EvalResult.Success)?.formatted

    private fun error(expression: String): String? =
        (CalculatorEngine.evaluate(expression) as? EvalResult.Failure)?.message

    @Test
    fun basicArithmetic() {
        assertEquals("4", value("2+2"))
        assertEquals("6", value("2+2×2"))
        assertEquals("8", value("(2+2)×2"))
        assertEquals("2,5", value("10÷4"))
    }

    @Test
    fun decimalSeparatorIsComma() {
        assertEquals("0,3", value("0,1+0,2"))
        assertEquals("1,5", value("1.5"))
        assertEquals("3", value("0,1+0,2+2,7"))
    }

    @Test
    fun negativeNumbersAndUnaryMinus() {
        assertEquals("-3", value("5−8"))
        assertEquals("4", value("−5+9"))
        assertEquals("8", value("2×−4×−1"))
        assertEquals("-3", value("−3"))
    }

    @Test
    fun percentIsHundredth() {
        assertEquals("0,5", value("50%"))
        assertEquals("20", value("200×10%"))
        assertEquals("50,5", value("50+10%×5"))
    }

    @Test
    fun parenthesesAreAutoClosed() {
        assertEquals("14", value("2×(3+4"))
        assertEquals("5", value("(2+3"))
        assertEquals("24", value("2×(3+4)+2×(4+1"))
    }

    @Test
    fun divisionByZeroIsReported() {
        assertEquals("Деление на ноль", error("1÷0"))
        assertEquals("Деление на ноль", error("0÷0"))
    }

    @Test
    fun invalidExpressionsAreReported() {
        assertEquals("Пустое выражение", error(""))
        assertEquals("Пустое выражение", error("   "))
        assertEquals("Некорректное выражение", error(")2+2("))
    }

    @Test
    fun danglingOperatorIsTrimmedBeforeEvaluation() {
        assertEquals("2", value("2+"))
        assertEquals("2", value("2×"))
    }

    @Test
    fun scientificNotationIsSupported() {
        assertEquals("1500", value("1,5e3"))
        assertEquals("1e18", value("1e18"))
        assertEquals("1e-10", value("0,0000000001"))
    }

    @Test
    fun normalizationDropsDanglingOperators() {
        assertEquals("5", CalculatorEngine.normalizeExpression("5+"))
        assertEquals("5×2", CalculatorEngine.normalizeExpression("5×2×"))
        assertEquals("(2+3)", CalculatorEngine.normalizeExpression("(2+3"))
        assertEquals("", CalculatorEngine.normalizeExpression("−"))
    }

    @Test
    fun emptyInputHasNoErrorButNoValue() {
        assertNull(value(""))
        assertEquals("Пустое выражение", error(""))
    }
}
