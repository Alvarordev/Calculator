package com.example.calculator.calculator.domain

import com.example.calculator.calculator.data.CalculatorOperation
import com.example.calculator.calculator.data.Operand
import com.example.calculator.calculator.data.getNumericValue
import java.util.Stack
import kotlin.math.pow
import kotlin.math.sqrt

sealed class CalculatorError(message: String) : Exception(message) {
    data class DivisionByZero(override val message: String = "Division by zero") :
        CalculatorError(message)

    data class InvalidRoot(override val message: String = "Invalid root operation") :
        CalculatorError(message)

    data class NotEnoughOperands(override val message: String = "Not enough operands") :
        CalculatorError(message)

    data class InvalidExpression(override val message: String = "Invalid expression") :
        CalculatorError(message)

    data class UnknownCalculatorError(override val message: String = "Unknown calculator error") :
        CalculatorError(message)
}

class CalculatorEngine {

    companion object {
        private const val MAX_NUM_LENGTH = 9
    }

    fun convertInfixToRpn(infixTokens: List<Any>): List<Any> {
        val outputQueue = mutableListOf<Any>()
        val operatorStack = Stack<CalculatorOperation>()

        infixTokens.forEach { token ->
            when (token) {
                is Operand -> { // Es un número o una constante
                    outputQueue.add(token)
                }

                is CalculatorOperation -> { // Es un operador
                    while (operatorStack.isNotEmpty() && operatorStack.peek() is CalculatorOperation) {
                        val topOp = operatorStack.peek()
                        if (topOp.precedence > token.precedence || (topOp.precedence == token.precedence && token.isLeftAssociative)) {
                            outputQueue.add(operatorStack.pop())
                        } else {
                            break
                        }
                    }
                    operatorStack.push(token)
                }
            }
        }

        while (operatorStack.isNotEmpty()) {
            outputQueue.add(operatorStack.pop())
        }
        return outputQueue
    }

    fun evaluateRpn(rpnTokens: List<Any>): Double {
        val operandStack = Stack<Double>()

        rpnTokens.forEach { token ->
            when (token) {
                is Operand -> {
                    val value = token.getNumericValue()
                    if (value == null) {
                        throw IllegalArgumentException("Invalid operand in RPN: $token")
                    }
                    operandStack.push(value)
                }

                is CalculatorOperation -> {
                    // Manejo de operadores unarios
                    if (token == CalculatorOperation.SquareRoot) {
                        if (operandStack.isEmpty()) {
                            throw CalculatorError.NotEnoughOperands("Not enough operands for square root operation")
                        }
                        val operand = operandStack.pop() // Solo saca UN operando
                        if (operand < 0) {
                            throw CalculatorError.InvalidRoot("Cannot calculate square root of a negative number: $operand")
                        }
                        operandStack.push(sqrt(operand))
                    } else {
                        if (operandStack.size < 2) {
                            throw IllegalArgumentException("Not enough operands for binary operation: ${token.symbol}")
                        }
                        val operand2 = operandStack.pop()
                        val operand1 = operandStack.pop()
                        val result = when (token) {
                            is CalculatorOperation.Add -> operand1 + operand2
                            is CalculatorOperation.Subtract -> operand1 - operand2
                            is CalculatorOperation.Multiply -> operand1 * operand2
                            is CalculatorOperation.Divide -> {
                                if (operand2 == 0.0) {
                                    throw CalculatorError.DivisionByZero()
                                }
                                operand1 / operand2
                            }

                            is CalculatorOperation.Mod -> operand1 % operand2
                            is CalculatorOperation.Power -> operand1.pow(operand2)
                            else -> throw CalculatorError.InvalidExpression("Unknown operator: ${token.symbol}")
                        }
                        operandStack.push(result)
                    }
                }
            }
        }

        if (operandStack.size != 1) {
            throw CalculatorError.InvalidExpression("Invalid RPN expression, expected one result but got ${operandStack.size}")
        }
        return operandStack.pop()
    }

    fun formatResult(value: Double): String {
        return if (value % 1 == 0.0) {
            value.toLong().toString()
        } else {
            val stringValue = value.toString()
            if (stringValue.length > MAX_NUM_LENGTH + 1) {
                val parts = stringValue.split(".")
                if (parts.size > 1) {
                    val integerPart = parts[0]
                    val signLength = if (integerPart.contains("-")) 1 else 0
                    val availableDecimalLength =
                        MAX_NUM_LENGTH - integerPart.length - signLength - 1
                    if (availableDecimalLength <= 0) {
                        integerPart
                    } else {
                        val decimalPart = parts[1].take(availableDecimalLength)
                        "$integerPart.$decimalPart"
                    }
                } else {
                    stringValue.take(MAX_NUM_LENGTH + 1)
                }
            } else {
                stringValue
            }
        }
    }
}