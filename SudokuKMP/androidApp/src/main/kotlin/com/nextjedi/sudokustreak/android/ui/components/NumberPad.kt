package com.nextjedi.sudokustreak.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextjedi.sudokustreak.android.ui.theme.Blue500
import com.nextjedi.sudokustreak.android.ui.theme.ErrorRed

@Composable
fun NumberPad(
    onNumberPress: (Int) -> Unit,
    onErase: () -> Unit,
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1-5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..5).forEach { num ->
                NumberButton(
                    num = num.toString(),
                    onClick = { onNumberPress(num) },
                    disabled = disabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Row 6-9 + Erase
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (6..9).forEach { num ->
                NumberButton(
                    num = num.toString(),
                    onClick = { onNumberPress(num) },
                    disabled = disabled,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(
                onClick = onErase,
                enabled = !disabled,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ErrorRed
                )
            ) {
                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NumberButton(
    num: String,
    onClick: () -> Unit,
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !disabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Blue500,
            contentColor = Color.White,
            disabledContainerColor = Color.LightGray
        )
    ) {
        Text(num, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
