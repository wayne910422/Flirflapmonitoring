/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file FusionHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with fusion modes.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.content.Context
import com.flir.thermalsdk.image.fusion.FusionMode

/**
 * A convenient handler used to work with fusion modes.
 */
object FusionHandler {

    /**
     * Shows a dialog allowing to pick one of the available fusion modes.
     */
    fun showFusionModes(context: Context, selectedMode: FusionMode, callback: (FusionMode) -> Unit) {
        // we need an array with fusion modes name - for now assume only available: IR, VISUAL and MSX
        val fusionModesList = listOf(FusionMode.THERMAL_ONLY, FusionMode.VISUAL_ONLY, FusionMode.MSX)
        val fusionModesNamesList = fusionModesList.map { it.name }
        val fusionModesNamesArray = fusionModesNamesList.toTypedArray()

        val selectedIndex = fusionModesList.indexOfFirst { it == selectedMode }

        DialogBuilder().createDialogSingleChoice(
            context, "Select fusion mode", fusionModesNamesArray, selectedIndex
        ) { dialog, which ->
            callback(fusionModesList[which])
            dialog.dismiss()
        }.show()
    }

}
