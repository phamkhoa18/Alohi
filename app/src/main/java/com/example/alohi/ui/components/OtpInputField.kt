package com.example.alohi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alohi.ui.theme.AloHiTheme

/**
 * AloHi OTP Input Field
 * Features:
 * - 6 separate digit boxes
 * - Auto-focus next box
 * - Cursor animation
 * - Apple-like clean design
 * - Auto-submit when complete
 */
@Composable
fun OtpInputField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpLength: Int = 6,
    onComplete: (String) -> Unit = {},
) {
    val colors = AloHiTheme.extendedColors
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(otpValue) {
        if (otpValue.length == otpLength) {
            onComplete(otpValue)
        }
    }

    // Hidden text field that captures input
    Box(modifier = modifier) {
        BasicTextField(
            value = otpValue,
            onValueChange = { newVal ->
                if (newVal.length <= otpLength && newVal.all { it.isDigit() }) {
                    onOtpChange(newVal)
                }
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(otpLength) { index ->
                        val char = otpValue.getOrNull(index)
                        val isFocused = otpValue.length == index

                        OtpDigitBox(
                            digit = char?.toString() ?: "",
                            isFocused = isFocused,
                            isFilled = char != null
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun OtpDigitBox(
    digit: String,
    isFocused: Boolean,
    isFilled: Boolean,
) {
    val colors = AloHiTheme.extendedColors
    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> colors.border
    }
    val bgColor = if (isFilled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 56.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (digit.isNotEmpty()) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        } else if (isFocused) {
            // Cursor line
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
