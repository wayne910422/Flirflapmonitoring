/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file DialogBuilder.kt
 * @Author Teledyne FLIR
 *
 * @brief Helper for creating different kind of dialogs.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import com.teledyneflir.androidsdk.sample.flironewireless.R
import java.lang.Double.parseDouble

/**
 * Helper for creating different kind of dialogs.
 */
class DialogBuilder {

    companion object {
        fun getInstance() = DialogBuilder()
    }

    /**
     * Creates a dialog with a list of items to choose from.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context  app context
     * @param title    dialog title
     * @param items    array of items to choose from
     * @param listener a positive button click listener
     * @return Returns a dialog instance.
     */
    fun createDialogSingleChoice(context: Context, title: String, items: Array<String>, listener: DialogInterface.OnClickListener): Dialog {
        return createDialogSingleChoice(context, title, items, -1, listener)
    }

    /**
     * Creates a dialog with a list of items to choose from.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context       app context
     * @param title         dialog title
     * @param items         array of items to choose from
     * @param selectedIndex initial index to be selected
     * @param listener      a positive button click listener
     * @return Returns a dialog instance.
     */
    fun createDialogSingleChoice(context: Context, title: String, items: Array<String>, selectedIndex: Int, listener: DialogInterface.OnClickListener): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setSingleChoiceItems(items, selectedIndex, listener)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    /**
     * Creates a dialog with a list of items to choose from.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context         app context
     * @param title           dialog title
     * @param items           array of items to choose from
     * @param selectedIndex   initial index to be selected
     * @param listener        a positive button click listener
     * @param cancelListener  a negative button click listener
     * @return Returns a dialog instance.
     */
    fun createDialogSingleChoice(
        context: Context, title: String, items: Array<String>, selectedIndex: Int, listener: DialogInterface.OnClickListener,
        cancelListener: DialogInterface.OnClickListener
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setSingleChoiceItems(items, selectedIndex, listener)
        builder.setNegativeButton("Cancel", cancelListener)
        return builder.create()
    }

    /**
     * Creates a simple OK/Cancel dialog.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context    app context
     * @param title      dialog title
     * @param message    dialog message
     * @param listenerOk a positive button click listener
     * @return Returns a dialog instance.
     */
    fun createDialogOkCancel(
        context: Context, title: String, message: String,
        listenerOk: DialogInterface.OnClickListener,
        // default cancel listener just closes the dialog
        listenerCancel: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, _ -> dialog.cancel() }
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok", listenerOk)
        builder.setNegativeButton("Cancel", listenerCancel)
        return builder.create()
    }

    /**
     * Creates a simple information dialog.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context app context
     * @param title   dialog title
     * @param message dialog message
     * @return Returns a dialog instance.
     */
    fun createDialogConfirmOk(context: Context, title: String, message: String): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok") { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    /**
     * Creates an input dialog with one edit field.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context       app context
     * @param title         dialog title
     * @param initialVal         initial value for the "value" edit field
     * @param callback      a callback used to inform about the text entered
     * @return Returns a dialog instance.
     */
    fun createSingleInputDialog(context: Context, title: String, initialVal: Double, callback: (value: Double) -> Unit): Dialog {
        return createSingleInputDialog(context, title, null, initialVal, callback)
    }

    /**
     * Creates an input dialog with one edit field.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context       app context
     * @param title         dialog title
     * @param message         dialog message
     * @param initialVal         initial value for the "value" edit field
     * @param callback      a callback used to inform about the text entered
     * @return Returns a dialog instance.
     */
    fun createSingleInputDialog(context: Context, title: String, message: String?, initialVal: Double, callback: (value: Double) -> Unit): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        val content = LayoutInflater.from(context).inflate(R.layout.dialog_one_edit_field, null)
        val editText1: EditText = content.findViewById(R.id.dialogEditText1)


        (content.findViewById(R.id.dialogLabel1) as TextView).text = message ?: "Value: "
        // initial values
        editText1.setText(String.format("%.2f", initialVal).replace(",", "."))

        builder.setView(content)
        builder.setPositiveButton("Ok") { dialog, _ ->
            // read the input and use the callback to pass it
            try {
                callback.invoke(parseDouble(editText1.text.toString()))
                dialog.dismiss()
            } catch (e: NumberFormatException) {
                toast(context, "Incorrect input - you have to provide valid double value.")
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        return builder.create()
    }

    /**
     * Creates an input dialog with two edit fields.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context       app context
     * @param title         dialog title
     * @param inputMin      initial value for the "min" edit field
     * @param inputMax      initial value for the "max" edit field
     * @param callback      a callback used to inform about the text entered
     * @return Returns a dialog instance.
     */
    fun createDoubleInputDialog(context: Context, title: String, inputMin: Double, inputMax: Double, callback: (min: Double, max: Double) -> Unit): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        val content = LayoutInflater.from(context).inflate(R.layout.dialog_two_edit_fields, null)
        val editText1: EditText = content.findViewById(R.id.dialogEditText1)
        val editText2: EditText = content.findViewById(R.id.dialogEditText2)

        (content.findViewById(R.id.dialogLabel1) as TextView).text = "Min: "
        (content.findViewById(R.id.dialogLabel2) as TextView).text = "Max: "
        // initial values
        editText1.setText(String.format("%.2f", inputMin).replace(",", "."))
        editText2.setText(String.format("%.2f", inputMax).replace(",", "."))

        builder.setView(content)
        builder.setPositiveButton("Ok") { _, _ ->
            // read the input and use the callback to pass it
            try {
                callback.invoke(parseDouble(editText1.text.toString()), parseDouble(editText2.text.toString()))
            } catch (e: NumberFormatException) {
                toast(context, "Incorrect input - you have to provide valid signed double values.")
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        return builder.create()
    }

    /**
     * Creates an input dialog for setting remote scale.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context       app context
     * @param title         dialog title
     * @param inputMin      initial value for the "min" edit field
     * @param inputMax      initial value for the "max" edit field
     * @param callback      a callback used to inform about the text entered
     * @return Returns a dialog instance.
     */
    fun createScaleRangeDialog(
        context: Context,
        title: String,
        inputMin: Double,
        inputMax: Double,
        callback: (autoScale: Boolean, min: Double?, max: Double?) -> Unit
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        val content = LayoutInflater.from(context).inflate(R.layout.dialog_remote_scale, null)
        val editText1: EditText = content.findViewById(R.id.dialogEditText1)
        val editText2: EditText = content.findViewById(R.id.dialogEditText2)

        (content.findViewById(R.id.dialogLabel1) as TextView).text = "Min: "
        (content.findViewById(R.id.dialogLabel2) as TextView).text = "Max: "
        val switchAutoAdjust: SwitchMaterial = content.findViewById(R.id.dialogSwitch1)

        // initial values
        editText1.setText(String.format("%.2f", inputMin).replace(",", "."))
        editText2.setText(String.format("%.2f", inputMax).replace(",", "."))

        switchAutoAdjust.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                editText1.isEnabled = false
                editText2.isEnabled = false
            } else {
                editText1.isEnabled = true
                editText2.isEnabled = true
            }
        }

        builder.setView(content)
        builder.setPositiveButton("Ok") { _, _ ->
            // read the input and use the callback to pass it
            try {
                val minStr = editText1.text.toString()
                val maxStr = editText2.text.toString()
                val autoScale = switchAutoAdjust.isChecked
                if (!autoScale) {
                    // verify input data in manual scale only
                    when {
                        minStr.isBlank() -> {
                            toast(context, "Incorrect input - you have to provide valid double MIN value (may be negative).")
                            return@setPositiveButton
                        }
                        maxStr.isBlank() -> {
                            toast(context, "Incorrect input - you have to provide valid double MAX value (may be negative).")
                            return@setPositiveButton
                        }
                    }
                    val minTempCelcius = parseDouble(editText1.text.toString())
                    val maxTempCelcius = parseDouble(editText2.text.toString())

                    // notify about the user selection
                    callback.invoke(switchAutoAdjust.isChecked, minTempCelcius, maxTempCelcius)
                } else {
                    // auto scale is true - min/max values are ignored
                    callback.invoke(autoScale, null, null)
                }

            } catch (e: NumberFormatException) {
                toast(context, "Incorrect input - you have to provide valid double values (may be negative).")
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        return builder.create()
    }

    /**
     * Creates an input dialog for setting new SSID and password.
     * Note that the dialog is NOT displayed automatically.
     *
     * @param context       app context
     * @param title         dialog title
     * @param inputSsid      initial value for the SSID edit field
     * @param callback      a callback used to inform about the details entered
     * @return Returns a dialog instance.
     */
    fun createSsidChangeDialog(
        context: Context,
        title: String,
        inputSsid: String,
        callback: (newSsid: String) -> Unit
    ): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        val content = LayoutInflater.from(context).inflate(R.layout.dialog_change_ssid, null)
        val editTextSsid: EditText = content.findViewById(R.id.dialogEditSsid)

        editTextSsid.setText(inputSsid)

        builder.setView(content)

        builder.setPositiveButton("Ok") { _, _ ->
            // read the input and use the callback to pass it
            val ssid = editTextSsid.text.toString()

            if (ssid.isBlank()) {
                toast(context, "You have to provide a valid, non-empty SSID.")
                return@setPositiveButton
            }

            callback.invoke(ssid)
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        return builder.create()
    }

    /**
     * Show toast message.
     */
    private fun toast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

}