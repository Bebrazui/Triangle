package org.triangle.calculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {

    private fun convert(value: Double, category: UnitCategory, from: String, to: String): Double {
        val fromUnit = category.units.first { it.symbol == from }
        val toUnit = category.units.first { it.symbol == to }
        return UnitConverter.convert(value, fromUnit, toUnit)
    }

    @Test
    fun areaConversions() {
        assertEquals(10_000.0, convert(1.0, UnitCategory.Area, "га", "м²"), 1e-9)
        assertEquals(100.0, convert(1.0, UnitCategory.Area, "сотка", "м²"), 1e-9)
        assertEquals(1.0, convert(100.0, UnitCategory.Area, "м²", "сотка"), 1e-9)
        assertEquals(1_000_000.0, convert(1.0, UnitCategory.Area, "км²", "м²"), 1e-9)
        assertEquals(0.09290304, convert(1.0, UnitCategory.Area, "фут²", "м²"), 1e-12)
    }

    @Test
    fun volumeConversions() {
        assertEquals(1000.0, convert(1.0, UnitCategory.Volume, "л", "мл"), 1e-9)
        assertEquals(1000.0, convert(1.0, UnitCategory.Volume, "м³", "л"), 1e-9)
        assertEquals(1.0, convert(1.0, UnitCategory.Volume, "мл", "см³"), 1e-12)
        assertEquals(3.785411784, convert(1.0, UnitCategory.Volume, "гал", "л"), 1e-12)
    }

    @Test
    fun lengthConversions() {
        assertEquals(0.3048, convert(1.0, UnitCategory.Length, "фут", "м"), 1e-12)
        assertEquals(0.0254, convert(1.0, UnitCategory.Length, "дюйм", "м"), 1e-12)
        assertEquals(1000.0, convert(1.0, UnitCategory.Length, "км", "м"), 1e-9)
        assertEquals(100.0, convert(1.0, UnitCategory.Length, "м", "см"), 1e-9)
    }

    @Test
    fun massConversions() {
        assertEquals(1000.0, convert(1.0, UnitCategory.Mass, "т", "кг"), 1e-9)
        assertEquals(1000.0, convert(1.0, UnitCategory.Mass, "кг", "г"), 1e-9)
        assertEquals(0.45359237, convert(1.0, UnitCategory.Mass, "фунт", "кг"), 1e-12)
    }

    @Test
    fun formattingUsesCommaAndPlainNumbers() {
        assertEquals("10000", UnitConverter.format(10_000.0))
        assertEquals("0,000001", UnitConverter.format(0.000001))
        assertEquals("0", UnitConverter.format(0.0))
        assertEquals("0,09290304", UnitConverter.format(0.09290304))
    }

    @Test
    fun parsingAcceptsCommaAndScientificNotation() {
        assertEquals(0.5, UnitConverter.parse("0,5") ?: 0.0, 1e-12)
        assertEquals(1500.0, UnitConverter.parse("1,5e3") ?: 0.0, 1e-9)
        assertNull(UnitConverter.parse(""))
        assertNull(UnitConverter.parse("−"))
        assertNull(UnitConverter.parse("abc"))
    }

    @Test
    fun convertAllSkipsSourceUnit() {
        val source = UnitCategory.Mass.units.first { it.symbol == "кг" }
        val result = UnitConverter.convertAll(1.0, source, UnitCategory.Mass.units)
        assertEquals(3, result.size)
        assertEquals(listOf("г", "т", "фунт"), result.map { it.first.symbol })
        assertEquals("1000", result.first().second)
    }

    @Test
    fun inputEditing() {
        assertEquals("5", UnitInput.digit("0", '5'))
        assertEquals("−5", UnitInput.digit("−0", '5'))
        assertEquals("0,5", UnitInput.digit("0,", '5'))
        assertEquals("0,", UnitInput.decimal(""))
        assertEquals("−0,", UnitInput.decimal("−"))
        assertEquals("5,", UnitInput.decimal("5"))
        assertEquals("5,", UnitInput.decimal("5,"))
        assertEquals("", UnitInput.backspace("−"))
        assertEquals("−5", UnitInput.toggleSign("5"))
        assertEquals("5", UnitInput.toggleSign("−5"))
        assertEquals("", UnitInput.clear())
    }

    @Test
    fun digitLimitIsEnforced() {
        val fifteen = "123456789012345"
        assertEquals(fifteen, UnitInput.digit(fifteen, '9'))
    }
}
