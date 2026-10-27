package org.triangle.calculator.calc

/**
 * Единица измерения внутри категории.
 *
 * [factorToBase] — сколько базовых единиц (м², м³, м, кг) в одной такой единице.
 * Коэффициенты — точные международные определения, а не округлённые.
 */
data class UnitDef(
    val symbol: String,
    val name: String,
    val factorToBase: Double
)

enum class UnitCategory(val label: String, val units: List<UnitDef>) {

    Area(
        label = "Площадь",
        units = listOf(
            UnitDef("м²", "квадратный метр", 1.0),
            UnitDef("га", "гектар", 10_000.0),
            UnitDef("сотка", "сотка", 100.0),
            UnitDef("км²", "квадратный километр", 1_000_000.0),
            UnitDef("фут²", "квадратный фут", 0.09290304)
        )
    ),

    Volume(
        label = "Объём",
        units = listOf(
            UnitDef("м³", "кубический метр", 1.0),
            UnitDef("л", "литр", 0.001),
            UnitDef("мл", "миллилитр", 0.000001),
            UnitDef("см³", "кубический сантиметр", 0.000001),
            UnitDef("гал", "галлон (US)", 0.003785411784)
        )
    ),

    Length(
        label = "Длина",
        units = listOf(
            UnitDef("м", "метр", 1.0),
            UnitDef("см", "сантиметр", 0.01),
            UnitDef("мм", "миллиметр", 0.001),
            UnitDef("км", "километр", 1000.0),
            UnitDef("дюйм", "дюйм", 0.0254),
            UnitDef("фут", "фут", 0.3048)
        )
    ),

    Mass(
        label = "Масса",
        units = listOf(
            UnitDef("кг", "килограмм", 1.0),
            UnitDef("г", "грамм", 0.001),
            UnitDef("т", "тонна", 1000.0),
            UnitDef("фунт", "фунт", 0.45359237)
        )
    )
}

/** Чистый пересчёт единиц — без побочных эффектов. */
object UnitConverter {

    fun convert(value: Double, from: UnitDef, to: UnitDef): Double =
        value * from.factorToBase / to.factorToBase

    /** То же форматирование, что и у калькулятора: запятая, до 12 значащих цифр. */
    fun format(value: Double): String = CalculatorEngine.format(value)

    /** Значение для подстановки в строку ввода; null — если ввод пуст или битый. */
    fun parse(raw: String): Double? {
        val text = raw.replace('−', '-').replace(',', '.')
        if (text.isBlank() || text == "-") return null
        val value = text.toDoubleOrNull() ?: return null
        return if (value.isNaN() || value.isInfinite()) null else value
    }

    /** Пара "единица + значение" для всех единиц категории, кроме исходной. */
    fun convertAll(
        value: Double,
        from: UnitDef,
        units: List<UnitDef>
    ): List<Pair<UnitDef, String>> =
        units.filter { it !== from }.map { unit ->
            unit to format(convert(value, from, unit))
        }
}

/**
 * Редактирование числа для пересчёта единиц: только цифры, одна запятая,
 * знак минус. Никаких операторов — они здесь не имеют смысла.
 */
object UnitInput {

    private const val MAX_DIGITS = 15

    fun digit(raw: String, digit: Char): String {
        val number = raw.removePrefix("−")
        if (number.count { it.isDigit() } >= MAX_DIGITS) return raw
        return when (number) {
            "0" -> (if (raw.startsWith("−")) "−" else "") + digit
            "" -> (if (raw.startsWith("−")) "−" else "") + digit
            else -> raw + digit
        }
    }

    fun decimal(raw: String): String {
        if (raw.contains(',')) return raw
        return when (raw) {
            "" -> "0,"
            "−" -> "−0,"
            else -> "$raw,"
        }
    }

    fun backspace(raw: String): String {
        val next = raw.dropLast(1)
        return if (next == "−") "" else next
    }

    fun toggleSign(raw: String): String = when {
        raw.isEmpty() -> "−"
        raw.startsWith("−") -> raw.drop(1)
        else -> "−$raw"
    }

    fun clear(): String = ""
}
