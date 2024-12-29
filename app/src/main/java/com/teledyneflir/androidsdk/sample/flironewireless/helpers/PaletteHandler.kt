/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file PaletteHandler.kt
 * @Author Teledyne FLIR
 *
 * @brief A convenient handler used to work with palettes.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.content.Context
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager

/**
 * A convenient handler used to work with palettes.
 */
object PaletteHandler {

    /**
     * Shows a dialog allowing to pick one of the available palettes.
     */
    fun showAvailablePalettes(context: Context, selectedPalette: Palette, callback: (pal: Palette) -> Unit) {
        // first we need a list of default palettes, PaletteManager will provide us one
        val palettesList = PaletteManager.getDefaultPalettes()

        // extract palettes name as a String array
        val palettesStr = palettesList.map { it.name }.toTypedArray()
        val selectedIndex = palettesList.indexOfFirst { it.name == selectedPalette.name }

        DialogBuilder.getInstance().createDialogSingleChoice(context, "Select palette", palettesStr, selectedIndex) { dialog, which ->
            callback(palettesList[which])
            dialog.dismiss()
        }.show()
    }

}