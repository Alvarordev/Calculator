package com.example.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.Stack

class CalculatorViewModel : ViewModel() {
    var state by mutableStateOf(CalculatorState())
        private set

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Number -> enterNumber(action.number)
            is CalculatorAction.Decimal -> enterDecimal()
            is CalculatorAction.Clear -> state = CalculatorState()
            is CalculatorAction.Operation -> enterOperation(action.operation)
            is CalculatorAction.Calculate -> performCalculation()
            is CalculatorAction.Delete -> performDeletion()
            is CalculatorAction.ToggleSign -> toggleSign()
            is CalculatorAction.Constant -> enterConstant(action.constant)
        }
        updateDisplayExpression()
    }

    private fun enterNumber(number: Int) {
        val current = state.currentInput
        var newCurrentInput: Operand = current

        // Si hay un error o el resultado no es "0", reiniciar el currentInput
        if (current is Operand.Error || state.result != "0") {
            newCurrentInput = Operand.Number(number.toString())
            state = state.copy(inputTokens = emptyList(), result = "0")
        } else if (current is Operand.Number) {
            val currentValue = current.value
            if (currentValue == "0" && number == 0) return // No hacer nada si se intenta agregar un 0 a "0"
            if (currentValue == "0") {
                newCurrentInput = Operand.Number(number.toString()) // Reemplazar el "0" inicial
            } else {
                newCurrentInput = Operand.Number(currentValue + number) // Concatenar el número al actual
            }
        } else if (current is Operand.Empty || current is Operand.Constant) {
            newCurrentInput = Operand.Number(number.toString()) // Si es Empty o Constant, al poner un número, se convierte en Number
        }

        state = state.copy(currentInput = newCurrentInput)

    }

    private fun enterDecimal() {
        val current = state.currentInput
        var newCurrentInput: Operand = current

        if (current is Operand.Error || state.result != "0") {
            newCurrentInput = Operand.Number("0.")
            state = state.copy(inputTokens = emptyList(), result = "0")
        } else if (current is Operand.Number && !current.value.contains('.')) {
            newCurrentInput = Operand.Number(current.value + ".")
        } else if (current is Operand.Empty || current is Operand.Constant) {
            newCurrentInput = Operand.Number("0.")
        }

        state = state.copy(currentInput = newCurrentInput)

    }

    private fun enterConstant(constant: Operand.Constant) {
        val newTokens = state.inputTokens.toMutableList()

        if (state.currentInput is Operand.Number && (state.currentInput as Operand.Number).value == "0" && newTokens.isEmpty()) {
            // En este caso, no añadimos el "0" y simplemente seteamos la constante como currentInput
            state = state.copy(currentInput = constant, inputTokens = emptyList(), result = "0")
            return
        }

        else if (state.currentInput !is Operand.Empty && state.currentInput !is Operand.Error) {
            if (newTokens.isEmpty() || newTokens.last() !is CalculatorOperation) {
                // No hay tokens o el último token NO es una operación.
                // Esto significa que la constante debe reemplazar el currentInput
                // o ser el primer token.
                state = state.copy(currentInput = constant, inputTokens = emptyList(), result = "0")
                return
            } else {
                // Último token ES una operación (ej. "5 + ")
                // Entonces añadimos el currentInput (si lo hay) y luego la constante
                if (state.currentInput !is Operand.Empty) {
                    newTokens.add(state.currentInput)
                }
                newTokens.add(constant)
                state = state.copy(currentInput = Operand.Empty, inputTokens = newTokens)
            }
        }
        else if (state.currentInput is Operand.Empty) {
            if (newTokens.isNotEmpty() && newTokens.last() is CalculatorOperation) {
                newTokens.add(constant)
                state = state.copy(currentInput = Operand.Empty, inputTokens = newTokens)
            } else {
                state = state.copy(currentInput = constant, inputTokens = emptyList(), result = "0")
            }
        }
        else if (state.currentInput is Operand.Error) {
            state = state.copy(currentInput = constant, inputTokens = emptyList(), result = "0")
        }
    }

    private fun enterOperation(operation: CalculatorOperation) {
        val newTokens = state.inputTokens.toMutableList()

        // Si currentInput no es Empty, agregarlo a los tokens
        if (state.currentInput !is Operand.Empty && state.currentInput !is Operand.Error) {
            newTokens.add(state.currentInput)
        }

        // Si el último token es una operación, reemplazarlo (ej. 5+ -> 5-)
        if (newTokens.isNotEmpty() && newTokens.last() is CalculatorOperation) {
            newTokens[newTokens.lastIndex] = operation
        } else {
            newTokens.add(operation)
        }

        state = state.copy(
            inputTokens = newTokens,
            currentInput = Operand.Empty
        )
    }

    private fun performCalculation() {
        val finalTokens = state.inputTokens.toMutableList()
        if (state.currentInput !is Operand.Empty && state.currentInput !is Operand.Error) {
            finalTokens.add(state.currentInput)
        }

        if (finalTokens.isEmpty()) {
            state = state.copy(result = state.currentInput.getDisplayValue())
            return
        }

        val rpnTokens = convertInfixToRpn(finalTokens)

        val calculationResult = evaluateRpn(rpnTokens)

        state = state.copy(
            currentInput = Operand.Number(formatResult(calculationResult)), // El resultado se convierte en el nuevo input
            inputTokens = emptyList(), // Limpiar la lista de tokens
            result = formatResult(calculationResult) // Guardar el resultado para mostrar
        )
    }

    private fun convertInfixToRpn(infixTokens: List<Any>): List<Any> {
        val outputQueue = mutableListOf<Any>()
        val operatorStack = Stack<CalculatorOperation>()

        infixTokens.forEach { token ->
            when (token) {
                is Operand -> { // Es un número o una constante
                    outputQueue.add(token)
                }
                is CalculatorOperation -> { // Es un operador
                    while (operatorStack.isNotEmpty() && operatorStack.peek() is CalculatorOperation &&
                        ((token.isLeftAssociative && token.precedence <= operatorStack.peek().precedence) ||
                                (!token.isLeftAssociative && token.precedence < operatorStack.peek().precedence))
                    ) {
                        outputQueue.add(operatorStack.pop())
                    }
                    operatorStack.push(token)
                }
            }
        }

        // Mover cualquier operador restante de la pila a la cola de salida
        while (operatorStack.isNotEmpty()) {
            outputQueue.add(operatorStack.pop())
        }
        return outputQueue
    }

    private fun evaluateRpn(rpnTokens: List<Any>): Double {
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
                    if (operandStack.size < 2) { // La mayoría son binarios
                        throw IllegalArgumentException("Not enough operands for operation: ${token.symbol}")
                    }
                    val operand2 = operandStack.pop()
                    val operand1 = operandStack.pop()
                    val result = when (token) {
                        is CalculatorOperation.Add -> operand1 + operand2
                        is CalculatorOperation.Subtract -> operand1 - operand2
                        is CalculatorOperation.Multiply -> operand1 * operand2
                        is CalculatorOperation.Divide -> {
                            if (operand2 == 0.0) {
                                state = state.copy(result = "Error") // Actualiza el estado de error
                                throw ArithmeticException("Division by zero")
                            }
                            operand1 / operand2
                        }
                        is CalculatorOperation.Mod -> operand1 % operand2
                        is CalculatorOperation.Power -> Math.pow(operand1, operand2)
                        is CalculatorOperation.Root -> Math.pow(operand1, 1.0 / operand2) // Asumiendo root(x, y) = x^(1/y)
                    }
                    operandStack.push(result)
                }
            }
        }

        if (operandStack.size != 1) {
            throw IllegalArgumentException("Invalid RPN expression: $rpnTokens")
        }
        return operandStack.pop()
    }

    private fun updateDisplayExpression() {
        val expressionString = buildString {
            state.inputTokens.forEach { token ->
                when (token) {
                    is Operand -> append(token.getDisplayValue())
                    is CalculatorOperation -> append(token.symbol)
                }
                append("")
            }
            if (state.currentInput !is Operand.Empty) {
                append(state.currentInput.getDisplayValue())
            }
        }.trim()

        state = state.copy(displayExpression = expressionString.ifBlank { "0" })
    }

    private fun performDeletion() {
        if (state.currentInput !is Operand.Empty && state.currentInput !is Operand.Error) {
            if (state.currentInput is Operand.Number && (state.currentInput as Operand.Number).value.length > 1) {
                state = state.copy(currentInput = Operand.Number((state.currentInput as Operand.Number).value.dropLast(1)))
            } else {
                state = state.copy(currentInput = Operand.Empty)
            }
        } else if (state.inputTokens.isNotEmpty()) {
            val lastToken = state.inputTokens.last()
            val newTokens = state.inputTokens.dropLast(1).toMutableList()
            if (lastToken is Operand) {
                state = state.copy(currentInput = lastToken, inputTokens = newTokens)
            } else {
                state = state.copy(inputTokens = newTokens)
            }
        } else {
            state = CalculatorState()
        }
        updateDisplayExpression()
    }

    private fun toggleSign() {
        when (val current = state.currentInput) {
            is Operand.Number -> {
                val newValue = if (current.value.startsWith("-")) current.value.removePrefix("-") else "-" + current.value
                state = state.copy(currentInput = Operand.Number(newValue))
            }
            is Operand.Constant -> {
                val negativeValue = -current.value
                state = state.copy(currentInput = Operand.Number(formatResult(negativeValue))) // Conviértelo en un número
            }
            Operand.Empty -> {
                state = state.copy(currentInput = Operand.Number("-"))
            }
            Operand.Error -> {
                state = state.copy(currentInput = Operand.Number("-"))
            }
        }
        updateDisplayExpression()
    }

    private fun formatResult(value: Double): String {
        return if (value % 1 == 0.0) {
            value.toLong().toString()
        } else {
            val stringValue = value.toString()
            if (stringValue.length > MAX_NUM_LENGTH + 1) {
                val parts = stringValue.split(".")
                if (parts.size > 1) {
                    val integerPart = parts[0]
                    val decimalPart = parts[1].take(
                        MAX_NUM_LENGTH - integerPart.length - (if (integerPart.contains("-")) 1 else 0)
                    )
                    "$integerPart.$decimalPart"
                } else {
                    stringValue.take(MAX_NUM_LENGTH + 1)
                }
            } else {
                stringValue
            }
        }
    }

    companion object {
        private const val MAX_NUM_LENGTH = 9
    }
}