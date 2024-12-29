/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file MainActivity.kt
 * @Author Teledyne FLIR
 *
 * @brief Main Activity.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.androidsdk.helpers.PermissionHandler
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.flir.thermalsdk.log.ThermalLog
import com.teledyneflir.androidsdk.sample.flironewireless.fragments.MainFragment
import com.teledyneflir.androidsdk.sample.flironewireless.fragments.StreamingFragment
import com.teledyneflir.androidsdk.sample.flironewireless.viewmodels.DiscoveryViewModel


/**
 * Main Activity.
 */
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        var sdkInitDone = false
        // use a random value for a request code, but keep in mind that we can only use lower 8 bits for requestCode (so this value must be less than 0xFF)
        const val F1_EDGE_PERMISSIONS_REQUEST_CODE = 7
    }

    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        verifyRequiredPermissions()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) {
            // close the app when closing startup fragment
            finish()
        } else {
            // simulate back pressed when no physical "back" arrow is available (i.e. Pixel 6)
            onBackPressed()
        }
        return true
    }

    private fun initializeApp() {
        if (sdkInitDone) {
            ThermalLog.d(TAG, "SDK init already done")
            return
        }
        ThermalSdkAndroid.init(applicationContext, ThermalLog.LogLevel.DEBUG)
        ThermalLog.d(TAG, "SDK init completed")
        ThermalLog.d(TAG, "Atlas Android SDK version: ${ThermalSdkAndroid.getVersion()}")
        sdkInitDone = true

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commitNow()
    }

    /**
     * Allows to switch to live view for the particular [DiscoveredCamera].
     */
    fun switchToLiveView(discoveredCamera: DiscoveredCamera) {
        // stop discovery when moving away to live view
        ViewModelProvider(this).get(DiscoveryViewModel::class.java).stopDiscovery()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, StreamingFragment.newInstance(discoveredCamera))
        transaction.addToBackStack(null)
        transaction.commit()
    }

    /**
     * Checks if the app has required permissions.
     * If the app does not have permissions then the user will be prompted to grant them.
     */
    private fun verifyRequiredPermissions() {
        // Check if we have permissions
        permissionHandler = PermissionHandler(this)
        if (permissionHandler.requestF1EdgeProPermission(F1_EDGE_PERMISSIONS_REQUEST_CODE)) {
            Log.d(TAG, "All permissions granted")
            // all permissions in place, initialize app
            initializeApp()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            F1_EDGE_PERMISSIONS_REQUEST_CODE -> {
                if (permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
                    // all permissions granted, initialize app
                    initializeApp()
                } else {
                    // permission denied, show warning Toast and finish app
                    toast("In order to use the app you have to grant permissions.")
                }
                return
            }
        }
    }

    override fun onDestroy() {
        // ensure discovery is stopped when closing the app
        ViewModelProvider(this).get(DiscoveryViewModel::class.java).stopDiscovery()
        super.onDestroy()
    }
}

fun Activity.toast(msg: String, longToast: Boolean = false) {
    Log.d(MainActivity.TAG, msg)
    Toast.makeText(this, msg, if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(msg: String, longToast: Boolean = false) {
    Log.d(MainActivity.TAG, msg)
    Toast.makeText(this.requireContext(), msg, if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}