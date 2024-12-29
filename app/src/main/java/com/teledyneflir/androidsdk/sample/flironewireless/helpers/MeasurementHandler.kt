/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file MeasurementHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with measurement drawing.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * A convenient handler used to work with measurement drawing.
 */
object MeasurementHandler {

    private const val SPOT_SIZE: Int = 15
    private val SPOT_PAINT: Paint = Paint()

    init {
        // define drawing parameters
        //        SPOT_PAINT.color = mContext.resources.getColor(R.color.almostRed)
        SPOT_PAINT.strokeWidth = 5f
    }

    /**
     * Draws spot on the provided bitmap.
     */
    @SuppressLint("DefaultLocale")
    fun drawCenterSpot(mutableSource: Bitmap) {
        // read spot data and set the label text
        val x = mutableSource.width / 2
        val y = mutableSource.height / 2

        val canvas = Canvas(mutableSource)
        // draw a spot meter on the original bitmap as an overlay
        canvas.drawLine((x - SPOT_SIZE).toFloat(), y.toFloat(), (x + SPOT_SIZE + 1).toFloat(), y.toFloat(), SPOT_PAINT)
        canvas.drawLine(x.toFloat(), (y - SPOT_SIZE).toFloat(), x.toFloat(), (y + SPOT_SIZE + 1).toFloat(), SPOT_PAINT)
    }
}
