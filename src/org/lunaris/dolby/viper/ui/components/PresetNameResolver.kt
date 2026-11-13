package org.lunaris.dolby.viper.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.lunaris.dolby.viper.data.model.DsPreset
import org.lunaris.dolby.viper.data.model.EqPreset
import org.lunaris.dolby.viper.effect.BuiltinPresets

@Composable
fun resolvePresetName(preset: EqPreset): String {
    val resId = preset.nameKey?.let { BuiltinPresets.EQ_PRESET_NAME_RES[it] }
    return if (resId != null) stringResource(resId) else preset.name
}

@Composable
fun resolvePresetName(preset: DsPreset): String {
    val resId = preset.nameKey?.let { BuiltinPresets.DS_PRESET_NAME_RES[it] }
    return if (resId != null) stringResource(resId) else preset.name
}
