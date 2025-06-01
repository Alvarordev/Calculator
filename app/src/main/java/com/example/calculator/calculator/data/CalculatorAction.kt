package com.example.calculator.calculator.data

sealed class CalculatorAction {
    data class Number(val number: Int): CalculatorAction()
    object Decimal: CalculatorAction()
    object Clear: CalculatorAction()
    object Delete: CalculatorAction()
    object Calculate: CalculatorAction()
    data class Operation(val operation: CalculatorOperation): CalculatorAction()
    object ToggleSign: CalculatorAction()
    object SquareRoot: CalculatorAction()
    data class Constant(val constant: Operand.Constant): CalculatorAction()
}

object MathConstants {
    val PI = Operand.Constant("π", Math.PI)
    val E = Operand.Constant("e", Math.E)
}


