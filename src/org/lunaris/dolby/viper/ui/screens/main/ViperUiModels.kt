package org.lunaris.dolby.viper.ui.screens.main

data class DriverStatus(
    val installed: Boolean = false,
    val versionCode: Int = -1,
    val versionName: String = "",
    val architecture: String = "",
    val streaming: Boolean = false,
    val samplingRate: Int = 0,
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
)
