/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file ScaleHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with manual scale.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.content.Context

/**
 * A convenient handler used to work with manual scale.
 */
object ScaleHandler {

    /**
     * Shows a dialog allowing to set desired manual scale range.
     */
    fun showScaleDialog(
        context: Context,
        currentScaleRange: Pair<Double, Double>,
        callback: (newAutoScale: Boolean, newRange: Pair<Double?, Double?>) -> Unit
    ) {
        DialogBuilder.getInstance()
            .createScaleRangeDialog(context, "Scale range", currentScaleRange.first, currentScaleRange.second) { nAutoScale, nMin, nMax ->
                callback(nAutoScale, Pair(nMin, nMax))
            }.show()
    }

}