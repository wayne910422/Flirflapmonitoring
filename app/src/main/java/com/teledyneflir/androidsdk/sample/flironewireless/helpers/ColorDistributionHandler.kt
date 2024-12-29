/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file ColorDistributionHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with color distribution settings.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.content.Context
import com.flir.thermalsdk.image.*

/**
 * A convenient handler used to work with color distribution settings.
 */
object ColorDistributionHandler {

    /**
     * Shows a dialog allowing to pick one of the available color distribution modes.
     */
    fun showColorModes(context: Context, selectedMode: ColorDistributionSettings, callback: (ColorDistributionSettings) -> Unit) {
        val colorModesList = listOf(
            HistogramEqualizationSettings(),
            TemperatureLinearSettings(),
            SignalLinearSettings(),
            PlateauHistogramEqSettings(),
            AdeSettings(),
            DdeSettings(),
            EntropySettings(),
        )
        val colorModesNamesList = colorModesList.map { it.javaClass.simpleName }
        val colorModesNamesArray = colorModesNamesList.toTypedArray()

        val selectedIndex = colorModesList.indexOfFirst { it.javaClass.simpleName == selectedMode.javaClass.simpleName }

        DialogBuilder().createDialogSingleChoice(
            context, "Select color mode", colorModesNamesArray, selectedIndex
        ) { dialog, which ->
            callback(colorModesList[which])
            dialog.dismiss()
        }.show()
    }

}
