package com.example.calculator.calculator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.calculator.calculator.data.CalculatorAction
import com.example.calculator.calculator.data.CalculatorOperation
import com.example.calculator.calculator.data.CalculatorState
import com.example.calculator.calculator.data.Operand
import com.example.calculator.calculator.data.getDisplayValue
import com.example.calculator.calculator.data.getNumericValue
import com.example.calculator.calculator.domain.CalculatorEngine
import com.example.calculator.calculator.domain.CalculatorError
import java.util.Stack
import kotlin.math.pow
import kotlin.math.sqrt

class CalculatorViewModel : ViewModel() {
    var state by mutableStateOf(CalculatorState())
        private set

    private val calculatorEngine = CalculatorEngine()

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
            is CalculatorAction.SquareRoot -> enterSquareRootOperation()
        }
        updateDisplayExpression()
    }

    private fun enterNumber(number: Int) {
        val current = state.currentInput
        var newCurrentInput: Operand = current
        var clearInputTokens = false
        var resetResult = false

        if (current is Operand.Error) {
            newCurrentInput = Operand.Number(number.toString())
            clearInputTokens = true
            resetResult = true
        }
        else if (state.result != "0" && state.inputTokens.isEmpty() && current !is Operand.Number) { // Añadido check de current para evitar resetear al escribir un numero despues de resultado
            newCurrentInput = Operand.Number(number.toString())
            clearInputTokens = true
            resetResult = true
        }
        else if (current is Operand.Number && current.value == "0" && state.inputTokens.isEmpty()) {
            if (number == 0) return // Evitar "00"
            newCurrentInput = Operand.Number(number.toString())
            resetResult = true // Se resetea el resultado ya que estamos comenzando una nueva entrada.
        }
        else if (current is Operand.Number) {
            val currentValue = current.value
            if (currentValue.length >= 9 && !currentValue.contains(".")) return

            newCurrentInput = Operand.Number(currentValue + number.toString())
            resetResult = true // Se resetea el resultado ya que se está editando un número.
        }
        else if (current is Operand.Empty || current is Operand.Constant) {
            newCurrentInput = Operand.Number(number.toString())
            resetResult = true // Se resetea el resultado ya que se está iniciando una nueva entrada.
        }

        state = state.copy(
            currentInput = newCurrentInput,
            inputTokens = if (clearInputTokens) emptyList() else state.inputTokens,
            result = if (resetResult) "0" else state.result
        )
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
        } else if (state.currentInput !is Operand.Empty && state.currentInput !is Operand.Error) {
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
        } else if (state.currentInput is Operand.Empty) {
            if (newTokens.isNotEmpty() && newTokens.last() is CalculatorOperation) {
                newTokens.add(constant)
                state = state.copy(currentInput = Operand.Empty, inputTokens = newTokens)
            } else {
                state = state.copy(currentInput = constant, inputTokens = emptyList(), result = "0")
            }
        } else if (state.currentInput is Operand.Error) {
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

    private fun enterSquareRootOperation() {
        val newTokens = mutableListOf<Any>()

        newTokens.add(CalculatorOperation.SquareRoot)

        state = state.copy(
            inputTokens = newTokens,
            currentInput = Operand.Empty,
            result = "0"
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

        try {
            val rpnTokens = calculatorEngine.convertInfixToRpn(finalTokens)
            val calculationResult = calculatorEngine.evaluateRpn(rpnTokens)

            val formattedResult = calculatorEngine.formatResult(calculationResult)

            state = state.copy(
                currentInput = Operand.Number(formattedResult),
                inputTokens = emptyList(),
                result = formattedResult
            )
        } catch (e: CalculatorError) {
            state = state.copy(
                currentInput = Operand.Error,
                inputTokens = emptyList(),
                result = "Error: ${e.message}"
            )
            return
        } catch (e: Exception) {
            state = state.copy(
                currentInput = Operand.Error,
                inputTokens = emptyList(),
                result = "Error: ${e.message}"
            )
            return
        }
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
                state = state.copy(
                    currentInput = Operand.Number(
                        (state.currentInput as Operand.Number).value.dropLast(1)
                    )
                )
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
                val newValue =
                    if (current.value.startsWith("-")) current.value.removePrefix("-") else "-" + current.value
                state = state.copy(currentInput = Operand.Number(newValue))
            }

            is Operand.Constant -> {
                val negativeValue = -current.value
                state =
                    state.copy(currentInput = Operand.Number(calculatorEngine.formatResult(negativeValue))) // Conviértelo en un número
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

}