package com.example.calculator.calculator.data

sealed class CalculatorOperation(
    val symbol: String,
    val precedence: Int,
    val isLeftAssociative: Boolean = true
) {
    data object SquareRoot : CalculatorOperation("√", 4, false)
    data object Power : CalculatorOperation("^", 3, false)
    data object Multiply : CalculatorOperation("×", 2)
    data object Divide : CalculatorOperation("÷", 2)
    data object Mod : CalculatorOperation("%", 2)
    data object Add : CalculatorOperation("+", 1)
    data object Subtract : CalculatorOperation("-", 1)
}