/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file EmissivityHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with emissivity setting.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.content.Context

/**
 * A convenient handler used to work with emissivity setting.
 */
object EmissivityHandler {

    /**
     * Shows a dialog allowing to set desired emissivity value.
     */
    fun showEmissivityDialog(context: Context, currentEmissivity: Double, callback: (newEmissivity: Double) -> Unit) {
        DialogBuilder.getInstance().createSingleInputDialog(context, "Set emissivity", "Emissivity value in range: 0.0 - 1.0", currentEmissivity) { nVal ->
            callback(nVal)
        }.show()
    }

}