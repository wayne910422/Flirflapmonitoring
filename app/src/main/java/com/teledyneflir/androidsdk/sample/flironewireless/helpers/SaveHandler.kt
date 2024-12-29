/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file SaveHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with save/snapshot feature.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import com.flir.thermalsdk.image.JavaImageBuffer
import com.flir.thermalsdk.image.ThermalImage
import com.flir.thermalsdk.log.ThermalLog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * A convenient handler used to work with save/snapshot feature.
 */
object SaveHandler {

    /**
     * Takes a snapshot of the ThermalImage.
     */
    fun saveImage(context: Context, thermalImage: ThermalImage, overlay: JavaImageBuffer? = null): Boolean {
        val absFilePath = prepareFilePath(context);
        try {
            ThermalLog.i("SaveHandler", "Snapshot saved under: $absFilePath")
            thermalImage.saveAs(absFilePath, overlay)
            // save done, run media scanner to make sure images thumbnails are created
            //AndroidFilesUtil.scanMedia(context, absFilePath)
        } catch (e: IOException) {
            ThermalLog.e("SaveHandler", "Error saving file: $absFilePath, message=${e.message}")
            return false
        }
        return true
    }

    @SuppressLint("SimpleDateFormat")
    private fun prepareFilePath(context: Context): String {
        val pattern = "yyyy-MM-dd_HH_mm_ss"
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val formattedTimestamp = now.format(formatter)

        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + File.separator + "F1W_" + formattedTimestamp + ".jpg"
    }

}