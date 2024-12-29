/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file TempRangeHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with temperature range.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.content.Context
import com.flir.thermalsdk.image.ThermalValue
import com.flir.thermalsdk.live.Camera
import kotlin.math.roundToInt

/**
 * A convenient handler used to work with temperature range.
 */
object TempRangeHandler {

    suspend fun readSelectedIndex(camera: Camera?): Int? {
        return camera?.remoteControl?.temperatureRange?.selectedIndex()?.sync
    }

    suspend fun readAvailableRanges(camera: Camera?): java.util.ArrayList<com.flir.thermalsdk.utils.Pair<ThermalValue, ThermalValue>>? {
        return camera?.remoteControl?.temperatureRange?.ranges()?.sync
    }

    fun mapToStringList(tempRanges: java.util.ArrayList<com.flir.thermalsdk.utils.Pair<ThermalValue, ThermalValue>>): List<String> {
        return tempRanges.map { pair -> "${pair.first.asCelsius().value.roundToInt()}°C - ${pair.second.asCelsius().value.roundToInt()}°C" }
    }

    /**
     * Shows a dialog allowing to pick one of the available temperature ranges.
     */
    fun showTempRanges(context: Context, selectedIndex: Int, tempRangesList: List<String>, callback: (Int) -> Unit) {
        val tempRangesNamesArray = tempRangesList.toTypedArray()

        DialogBuilder().createDialogSingleChoice(
            context, "Select temperature range", tempRangesNamesArray, selectedIndex
        ) { dialog, which ->
            callback(which)
            dialog.dismiss()
        }.show()
    }

}
