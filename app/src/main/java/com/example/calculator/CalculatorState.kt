package com.example.calculator

sealed class Operand {
    data class Number(val value: String) : Operand()
    data class Constant(val symbol: String, val value: Double) : Operand()
    data object Empty : Operand()
    data object Error : Operand()
}

data class CalculatorState(
    val inputTokens: List<Any> = emptyList(),
    val currentInput: Operand = Operand.Number("0"),
    val result: String = "",
    val displayExpression: String = "0",
)

fun Operand.getNumericValue(): Double? = when (this) {
    is Operand.Number -> value.toDoubleOrNull()
    is Operand.Constant -> value
    is Operand.Empty -> null
    is Operand.Error -> null
}

fun Operand.getDisplayValue(): String = when (this) {
    is Operand.Number -> value
    is Operand.Constant -> symbol
    is Operand.Empty -> ""
    is Operand.Error -> "Error"
}

fun Operand.isEmpty(): Boolean = when (this) {
    is Operand.Number -> value.isBlank() || value == "0"
    is Operand.Constant -> false
    is Operand.Empty -> true
    is Operand.Error -> true
}
