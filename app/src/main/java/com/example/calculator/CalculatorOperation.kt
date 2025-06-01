package com.example.calculator

sealed class CalculatorOperation(val symbol: String, val precedence: Int, val isLeftAssociative: Boolean = true) {
    // Mayor precedencia
    data object Power : CalculatorOperation("^", 3, false) // ^ es asociativo a la derecha
    data object Root : CalculatorOperation("√", 3, false) // √, si lo usas como binario, alta precedencia
    data object Multiply : CalculatorOperation("×", 2)
    data object Divide : CalculatorOperation("÷", 2)
    data object Mod : CalculatorOperation("%", 2)
    // Menor precedencia
    data object Add : CalculatorOperation("+", 1)
    data object Subtract : CalculatorOperation("-", 1)
}