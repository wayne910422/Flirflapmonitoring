/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file StreamingViewModel.kt
 * @Author Teledyne FLIR
 *
 * @brief ViewModel for streaming. Used for bitmaps, status, temperature range and errors.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for streaming. Used for bitmaps, status, temperature range and errors.
 */
class StreamingViewModel : ViewModel() {

    /**
     * Used for status and error reporting.
     */
    val statusInfoLiveData: MutableLiveData<String> = MutableLiveData()

    /**
     * Used for extended status reporting.
     */
    val extendedStatusInfoLiveData: MutableLiveData<String> = MutableLiveData()

    /**
     * Used for refreshing live view from the live stream feed.
     */
    val bitmapLiveData: MutableLiveData<Bitmap> = MutableLiveData()

    /**
     * Used for refreshing scale view from the live stream feed.
     */
    val bitmapScaleLiveData: MutableLiveData<Bitmap> = MutableLiveData()

    /**
     * Used for refreshing scale range view.
     */
    val scaleRangeLiveData: MutableLiveData<Pair<Double, Double>> = MutableLiveData()

    /**
     * Used for refreshing spot measurement temperature.
     */
    val spotMeasurementLiveData: MutableLiveData<Double> = MutableLiveData()

    /**
     * Used to notify about requested operation result.
     */
    val operationResultLiveData: MutableLiveData<String> = MutableLiveData()

}