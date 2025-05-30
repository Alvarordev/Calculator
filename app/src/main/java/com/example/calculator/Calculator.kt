package com.example.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.ui.theme.BackgroundBlue
import com.example.calculator.ui.theme.SpecialButtonWhite

@Composable
fun Calculator(
    state: CalculatorState, onAction: (CalculatorAction) -> Unit
) {
    val displayText = buildString {
        append(state.number1.getDisplayValue())
        state.operation?.let {
            append(it.symbol)
        }
        if (state.number2 !is Operand.Empty) {
            append(state.number2.getDisplayValue())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpecialButtonWhite),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomEnd = 32.dp, bottomStart = 32.dp))
                .background(BackgroundBlue)
                .padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.result,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalculatorOperationButton("√", onClick = {
                onAction(CalculatorAction.Operation(CalculatorOperation.Root))
            })
            CalculatorOperationButton("π", onClick = {
                onAction(CalculatorAction.Constant(MathConstants.PI))
            })
            CalculatorOperationButton("e", onClick = {
                onAction(CalculatorAction.Constant(MathConstants.E))
            })
            CalculatorOperationButton("xʸ", onClick = {
                onAction(CalculatorAction.Operation(CalculatorOperation.Power))
            })
        }

        val buttonItems = listOf(
            CalculatorButton.Clear,
            CalculatorButton.Parentheses,
            CalculatorButton.Percentage,
            CalculatorButton.Divide,
            CalculatorButton.Number(7),
            CalculatorButton.Number(8),
            CalculatorButton.Number(9),
            CalculatorButton.Multiply,
            CalculatorButton.Number(4),
            CalculatorButton.Number(5),
            CalculatorButton.Number(6),
            CalculatorButton.Minus,
            CalculatorButton.Number(1),
            CalculatorButton.Number(2),
            CalculatorButton.Number(3),
            CalculatorButton.Plus,
            CalculatorButton.Number(0),
            CalculatorButton.Decimal,
            CalculatorButton.Delete,
            CalculatorButton.Equals
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(buttonItems, span = { item ->
                GridItemSpan(1)
            }) { item ->
                CalculatorButton(button = item, onClick = {
                    when (item) {
                        is CalculatorButton.Number -> {
                            onAction(CalculatorAction.Number(item.num))
                        }

                        is CalculatorButton.Clear -> {
                            onAction(CalculatorAction.Clear)
                        }

                        is CalculatorButton.Decimal -> {
                            onAction(CalculatorAction.Decimal)
                        }

                        is CalculatorButton.Plus -> {
                            onAction(CalculatorAction.Operation(CalculatorOperation.Add))
                        }

                        is CalculatorButton.Minus -> {
                            if ((state.operation == CalculatorOperation.Multiply ||
                                        state.operation == CalculatorOperation.Divide ||
                                        state.operation == CalculatorOperation.Mod) &&
                                (state.number2.isEmpty() || state.number2.getDisplayValue() == "-")
                            ) {
                                onAction(CalculatorAction.ToggleSign)
                            }
                            else {
                                onAction(CalculatorAction.Operation(CalculatorOperation.Subtract))
                            }
                        }

                        is CalculatorButton.Multiply -> {
                            onAction(CalculatorAction.Operation(CalculatorOperation.Multiply))
                        }

                        is CalculatorButton.Divide -> {
                            onAction(CalculatorAction.Operation(CalculatorOperation.Divide))
                        }

                        is CalculatorButton.Delete -> {
                            onAction(CalculatorAction.Delete)
                        }

                        is CalculatorButton.Equals -> {
                            onAction(CalculatorAction.Calculate)
                        }

                        is CalculatorButton.Percentage -> {
                            onAction(CalculatorAction.Operation(CalculatorOperation.Mod))
                        }

                        else -> {
                            println("Operation clicked: ${item.symbol}")
                        }
                    }
                })

            }
        }
    }
}