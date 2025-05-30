package com.example.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
    }

    private fun enterConstant(constant: Operand.Constant) {
        if (state.operation == null) {
            // Reemplazar number1 con la constante
            state = if (state.number1 is Operand.Error) {
                CalculatorState(number1 = constant)
            } else {
                state.copy(number1 = constant)
            }
        } else {
            // Reemplazar number2 con la constante
            state = state.copy(number2 = constant)
        }
    }

    private fun performDeletion() {
        when {
            // Si number2 no está vacío, borramos de number2
            state.number2 !is Operand.Empty -> {
                when (val current = state.number2) {
                    is Operand.Number -> {
                        if (current.value.length > 1) {
                            state = state.copy(
                                number2 = Operand.Number(current.value.dropLast(1))
                            )
                        } else {
                            state = state.copy(number2 = Operand.Empty)
                        }
                    }
                    is Operand.Constant -> {
                        // Si es una constante, la eliminamos completamente
                        state = state.copy(number2 = Operand.Empty)
                    }
                    else -> {
                        state = state.copy(number2 = Operand.Empty)
                    }
                }
            }

            // Si hay operación pero number2 está vacío, eliminamos la operación
            state.operation != null -> {
                state = state.copy(operation = null)
            }

            // Si number1 no está vacío, borramos de number1
            !state.number1.isEmpty() -> {
                when (val current = state.number1) {
                    is Operand.Number -> {
                        if (current.value.length > 1) {
                            state = state.copy(
                                number1 = Operand.Number(current.value.dropLast(1))
                            )
                        } else {
                            state = state.copy(number1 = Operand.Number("0"))
                        }
                    }
                    is Operand.Constant -> {
                        // Si es una constante, la reemplazamos con "0"
                        state = state.copy(number1 = Operand.Number("0"))
                    }
                    else -> {
                        state = state.copy(number1 = Operand.Number("0"))
                    }
                }
            }
        }

        if (state.number1.isEmpty() && state.number2 is Operand.Empty && state.operation == null) {
            state = state.copy(number1 = Operand.Number("0"))
        }
    }

    private fun toggleSign() {
        if (state.operation == null) {
            // Trabajando con number1
            when (val current = state.number1) {
                is Operand.Number -> {
                    when {
                        current.value.isNotBlank() && current.value != "0" && current.value != "-" -> {
                            val newValue = if (current.value.startsWith("-")) {
                                current.value.removePrefix("-")
                            } else {
                                "-" + current.value
                            }
                            state = state.copy(number1 = Operand.Number(newValue))
                        }
                        current.value.isBlank() || current.value == "0" -> {
                            state = state.copy(number1 = Operand.Number("-"))
                        }
                        current.value == "-" -> {
                            state = state.copy(number1 = Operand.Number("0"))
                        }
                    }
                }
                is Operand.Constant -> {
                    // Para constantes, crear el número negativo de su valor
                    val negativeValue = -current.value
                    state = state.copy(number1 = Operand.Number(formatResult(negativeValue)))
                }
                is Operand.Error -> {
                    state = state.copy(number1 = Operand.Number("-"))
                }
                is Operand.Empty -> {
                    state = state.copy(number1 = Operand.Number("-"))
                }
            }
        } else {
            // Trabajando con number2
            when (val current = state.number2) {
                is Operand.Number -> {
                    when {
                        current.value.isNotBlank() && current.value != "0" && current.value != "-" -> {
                            val newValue = if (current.value.startsWith("-")) {
                                current.value.removePrefix("-")
                            } else {
                                "-" + current.value
                            }
                            state = state.copy(number2 = Operand.Number(newValue))
                        }
                        current.value.isBlank() || current.value == "0" -> {
                            state = state.copy(number2 = Operand.Number("-"))
                        }
                        current.value == "-" -> {
                            state = state.copy(number2 = Operand.Number("0"))
                        }
                    }
                }
                is Operand.Constant -> {
                    // Para constantes, crear el número negativo de su valor
                    val negativeValue = -current.value
                    state = state.copy(number2 = Operand.Number(formatResult(negativeValue)))
                }
                is Operand.Empty -> {
                    state = state.copy(number2 = Operand.Number("-"))
                }
                is Operand.Error -> {
                    state = state.copy(number2 = Operand.Number("-"))
                }
            }
        }
    }

    private fun performCalculation() {
        val number1 = state.number1.getNumericValue()
        val number2 = state.number2.getNumericValue()

        if (number1 == null || state.operation == null || number2 == null) {
            return
        }

        val result = when (state.operation) {
            is CalculatorOperation.Add -> number1 + number2
            is CalculatorOperation.Subtract -> number1 - number2
            is CalculatorOperation.Multiply -> number1 * number2
            is CalculatorOperation.Divide -> {
                if (number2 == 0.0) {
                    state = state.copy(
                        number1 = Operand.Error,
                        number2 = Operand.Empty,
                        operation = null,
                        result = "Error"
                    )
                    return
                }
                number1 / number2
            }
            is CalculatorOperation.Mod -> number1 % number2
            is CalculatorOperation.Root -> Math.pow(number1, 0.5)
            is CalculatorOperation.Power -> Math.pow(number1, number2)
            null -> return
        }

        state = state.copy(
            number1 = Operand.Number(formatResult(result)),
            number2 = Operand.Empty,
            result = formatResult(result),
            operation = null
        )
    }

    private fun enterOperation(operation: CalculatorOperation) {
        if (state.number1 is Operand.Error) return

        // Si ya tenemos una operación y un segundo operando, calculamos primero
        if (state.operation != null && state.number2 !is Operand.Empty) {
            performCalculation()
            state = state.copy(operation = operation)
            return
        }

        // Cambiar operador si ya hay uno pero no hay segundo operando
        if (state.operation != null && state.number2 is Operand.Empty) {
            state = state.copy(operation = operation)
            return
        }

        // Iniciar operación si tenemos un primer operando válido
        if (!state.number1.isEmpty() && state.number1 !is Operand.Error) {
            state = state.copy(operation = operation)
        }
    }

    private fun enterDecimal() {
        if (state.operation == null) {
            // Trabajando con number1
            when (val current = state.number1) {
                is Operand.Number -> {
                    if (current.value.contains('.')) return // Ya tiene decimal
                    if (current.value.isBlank()) {
                        state = state.copy(number1 = Operand.Number("0."))
                    } else {
                        state = state.copy(number1 = Operand.Number(current.value + "."))
                    }
                }
                is Operand.Error -> {
                    // Si había error, empezar con "0."
                    state = state.copy(number1 = Operand.Number("0."))
                }
                is Operand.Empty -> {
                    // Si estaba vacío, empezar con "0."
                    state = state.copy(number1 = Operand.Number("0."))
                }
                is Operand.Constant -> {
                    // Si había una constante, no podemos agregar decimal
                    // Opción 1: No hacer nada (ignorar el decimal)
                    return
                    // Opción 2: Reemplazar la constante con "0."
                    // state = state.copy(number1 = Operand.Number("0."))
                }
            }
        } else {
            // Trabajando con number2
            when (val current = state.number2) {
                is Operand.Number -> {
                    if (current.value.contains('.')) return // Ya tiene decimal
                    if (current.value.isBlank()) {
                        state = state.copy(number2 = Operand.Number("0."))
                    } else {
                        state = state.copy(number2 = Operand.Number(current.value + "."))
                    }
                }
                is Operand.Empty -> {
                    // Si number2 estaba vacío, empezar con "0."
                    state = state.copy(number2 = Operand.Number("0."))
                }
                is Operand.Error -> {
                    // Si había error, empezar con "0."
                    state = state.copy(number2 = Operand.Number("0."))
                }
                is Operand.Constant -> {
                    // Si había una constante, no podemos agregar decimal
                    // Opción 1: No hacer nada (ignorar el decimal)
                    return
                    // Opción 2: Reemplazar la constante con "0."
                    // state = state.copy(number2 = Operand.Number("0."))
                }
            }
        }
    }

    private fun enterNumber(number: Int) {
        if (state.operation == null) {
            when (val current = state.number1) {
                is Operand.Number -> {
                    if (current.value == "0" && number == 0) return
                    if (current.value.length >= MAX_NUM_LENGTH && current.value != "0") return

                    val newValue = if (current.value == "0") {
                        number.toString()
                    } else {
                        current.value + number
                    }
                    state = state.copy(number1 = Operand.Number(newValue))
                }
                is Operand.Error -> {
                    state = CalculatorState(number1 = Operand.Number(number.toString()))
                }
                is Operand.Constant -> {
                    // Si había una constante, la reemplazamos con el número
                    state = state.copy(number1 = Operand.Number(number.toString()))
                }
                is Operand.Empty -> {
                    state = state.copy(number1 = Operand.Number(number.toString()))
                }
            }
        } else {
            when (val current = state.number2) {
                is Operand.Number -> {
                    if (current.value == "0" && number == 0) return
                    if (current.value.length >= MAX_NUM_LENGTH) return

                    val newValue = if (current.value == "0") {
                        number.toString()
                    } else {
                        current.value + number
                    }
                    state = state.copy(number2 = Operand.Number(newValue))
                }
                is Operand.Empty -> {
                    state = state.copy(number2 = Operand.Number(number.toString()))
                }
                is Operand.Constant -> {
                    // Si había una constante, la reemplazamos con el número
                    state = state.copy(number2 = Operand.Number(number.toString()))
                }
                is Operand.Error -> {
                    state = state.copy(number2 = Operand.Number(number.toString()))
                }
            }
        }
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