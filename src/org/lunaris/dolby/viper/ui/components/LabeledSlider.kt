package org.lunaris.dolby.viper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.lunaris.dolby.R
import org.lunaris.dolby.utils.HapticFeedbackHelper
import org.lunaris.dolby.utils.rememberHapticFeedback
import java.util.Locale
import kotlin.math.roundToInt

data class SliderEdit(
    val displayValue: Double,
    val displayRange: ClosedFloatingPointRange<Double>,
    val decimals: Int,
    val onCommit: (Double) -> Unit,
    val unit: String = "",
)

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    valueLabel: String? = null,
    edit: SliderEdit? = null,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val canEdit = enabled && edit != null
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    fun stepIndex(currentValue: Float): Int {
        if (steps <= 0) return Int.MIN_VALUE
        val intervalCount = steps + 1
        val fraction =
            ((currentValue - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                .coerceIn(0f, 1f)
        return (fraction * intervalCount).roundToInt()
    }
    var lastHapticStep by remember { mutableIntStateOf(stepIndex(value)) }

    LaunchedEffect(value, steps, valueRange.start, valueRange.endInclusive) {
        lastHapticStep = stepIndex(value)
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = UiDimens.XSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            val pillContainerColor = if (enabled) {
                if (canEdit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val pillContentColor = if (enabled) {
                if (canEdit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val valueModifier =
                if (canEdit) {
                    Modifier
                        .clickable(role = Role.Button) { showEditDialog = true }
                } else {
                    Modifier
                }
            Surface(
                modifier = valueModifier,
                shape = CircleShape,
                color = pillContainerColor,
                contentColor = pillContentColor,
            ) {
                Text(
                    text = valueLabel ?: value.roundToInt().toString(),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = pillContentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        val hideTicks = steps == 0 || steps >= 10
        Slider(
            value = value,
            onValueChange = { newValue ->
                val newStep = stepIndex(newValue)
                if (enabled && newStep != Int.MIN_VALUE && newStep != lastHapticStep) {
                    scope.launch {
                        haptic.performHaptic(HapticFeedbackHelper.HapticIntensity.TEXTURE_TICK)
                    }
                    lastHapticStep = newStep
                }
                onValueChange(newValue)
            },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors =
                if (hideTicks) {
                    SliderDefaults.colors(
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        disabledActiveTickColor = Color.Transparent,
                        disabledInactiveTickColor = Color.Transparent,
                    )
                } else {
                    SliderDefaults.colors()
                },
        )
    }

    if (showEditDialog && edit != null) {
        NumberInputDialog(
            label = label,
            edit = edit,
            onDismiss = { showEditDialog = false },
        )
    }
}

@Composable
fun NumberInputDialog(
    label: String,
    edit: SliderEdit,
    onDismiss: () -> Unit,
) {
    val min = edit.displayRange.start
    val max = edit.displayRange.endInclusive

    fun fmt(v: Double): String =
        if (edit.decimals <= 0) {
            v.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.${edit.decimals}f", v)
        }

    var text by remember { mutableStateOf(fmt(edit.displayValue)) }
    val parsed = text.trim().toDoubleOrNull()
    val isValid = parsed != null && parsed in min..max

    val rangeHint =
        if (edit.unit.isNotEmpty()) {
            stringResource(
                R.string.dialog_value_range_unit,
                fmt(min),
                fmt(max),
                edit.unit,
            )
        } else {
            stringResource(R.string.dialog_value_range, fmt(min), fmt(max))
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(UiDimens.DialogCornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = UiDimens.Compact,
        ) {
            Column(modifier = Modifier.padding(UiDimens.IconLarge)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = !isValid,
                    suffix = if (edit.unit.isNotEmpty()) ({ Text(edit.unit) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text(rangeHint) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = UiDimens.Standard, bottom = UiDimens.IconLarge),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(UiDimens.Medium))
                    Button(
                        onClick = {
                            val clamped = parsed!!.coerceIn(min, max)
                            edit.onCommit(clamped)
                            onDismiss()
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}
