package com.example.calculator

import androidx.compose.ui.graphics.Color
import com.example.calculator.ui.theme.AccentOrange
import com.example.calculator.ui.theme.ButtonBlue
import com.example.calculator.ui.theme.NumberButtonGrayBlue

sealed class CalculatorButton(
    val symbol: String, val color: Color, val textColor: Color = Color.Black
) {
    data object Clear : CalculatorButton("AC", AccentOrange)

    data object Parentheses : CalculatorButton("( )", ButtonBlue)
    data object Percentage : CalculatorButton("%", ButtonBlue)
    data object Divide : CalculatorButton("÷", ButtonBlue)
    data object Multiply : CalculatorButton("×", ButtonBlue)
    data object Minus : CalculatorButton("-", ButtonBlue)
    data object Plus : CalculatorButton("+", ButtonBlue)
    data object Equals : CalculatorButton("=", ButtonBlue)

    data class Number(val num: Int) : CalculatorButton(num.toString(), NumberButtonGrayBlue)
    data object Decimal : CalculatorButton(".", NumberButtonGrayBlue)
    data object Delete : CalculatorButton("⌫", NumberButtonGrayBlue)
}


