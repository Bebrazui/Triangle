package org.triangle.calculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class CalcInputTest {

    @Test
    fun leadingZeroIsReplaced() {
        assertEquals("5", CalcInput.digit("0", '5'))
        assertEquals("0", CalcInput.digit("", '0'))
        assertEquals("0,5", CalcInput.digit("0,", '5'))
    }

    @Test
    fun digitsAfterOperatorStartNewNumber() {
        assertEquals("5+7", CalcInput.digit("5+", '7'))
        assertEquals("5+0", CalcInput.digit("5+", '0'))
    }

    @Test
    fun multiplicationIsInsertedAfterParenthesisAndPercent() {
        assertEquals("5)×2", CalcInput.digit("5)", '2'))
        assertEquals("5%×2", CalcInput.digit("5%", '2'))
    }

    @Test
    fun onlyOneDecimalPerNumber() {
        assertEquals("0,", CalcInput.decimal(""))
        assertEquals("5,", CalcInput.decimal("5"))
        assertEquals("5,", CalcInput.decimal("5,"))
        assertEquals("5+0,", CalcInput.decimal("5+"))
    }

    @Test
    fun operatorsReplaceEachOther() {
        assertEquals("5×", CalcInput.operator("5+", '×'))
        assertEquals("5+", CalcInput.operator("5", '+'))
        assertEquals("−", CalcInput.operator("", '−'))
        assertEquals("", CalcInput.operator("", '+'))
    }

    @Test
    fun unaryMinusIsHandledSeparately() {
        assertEquals("5−", CalcInput.operator("5×", '−'))
        assertEquals("5×", CalcInput.operator("5×−", '+'))
        assertEquals("5×−3", CalcInput.digit("5×−", '3'))
    }

    @Test
    fun percentOnlyAfterValue() {
        assertEquals("5%", CalcInput.percent("5"))
        assertEquals("5%", CalcInput.percent("5%"))
        assertEquals("5+", CalcInput.percent("5+"))
        assertEquals("(5)%", CalcInput.percent("(5)"))
    }

    @Test
    fun parentheses() {
        assertEquals("5×(", CalcInput.openParenthesis("5"))
        assertEquals("(", CalcInput.openParenthesis(""))
        assertEquals("(5)", CalcInput.closeParenthesis("(5"))
        assertEquals("5", CalcInput.closeParenthesis("5"))
        assertEquals("2×(3+4)", CalcInput.closeParenthesis("2×(3+4"))
    }

    @Test
    fun toggleSign() {
        assertEquals("−5", CalcInput.toggleSign("5"))
        assertEquals("5", CalcInput.toggleSign("−5"))
        assertEquals("3", CalcInput.toggleSign("-3"))
        assertEquals("5+−3", CalcInput.toggleSign("5+3"))
        assertEquals("5+3", CalcInput.toggleSign("5+−3"))
        assertEquals("5×3", CalcInput.toggleSign("5×−3"))
    }

    @Test
    fun backspaceRemovesLastCharacter() {
        assertEquals("1", CalcInput.backspace("12"))
        assertEquals("", CalcInput.backspace(""))
        assertEquals("5×", CalcInput.backspace("5×−"))
    }

    @Test
    fun openParenthesisCount() {
        assertEquals(2, CalcInput.openCount("2×(3+(4"))
        assertEquals(0, CalcInput.openCount("(2+3)"))
        assertEquals(0, CalcInput.openCount(")"))
    }
}
