/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file DiscoveryViewModel.kt
 * @Author Teledyne FLIR
 *
 * @brief ViewModel for discovery events.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.live.discovery.ble.BluetoothErrorCategory
import com.flir.thermalsdk.live.CommunicationInterface
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener
import com.flir.thermalsdk.live.discovery.DiscoveryFactory
import com.teledyneflir.androidsdk.sample.flironewireless.utils.DiscoveryException
import kotlinx.coroutines.launch

/**
 * ViewModel for discovery events.
 */
class DiscoveryViewModel : ViewModel() {

    val foundDeviceLiveData: MutableLiveData<DiscoveredCamera> = MutableLiveData()
    val statusInfoLiveData: MutableLiveData<String> = MutableLiveData()

    /**
     * Starts the discovery process, looks for BLE devices.
     */
    @Suppress("UNUSED_PARAMETER")
    fun startDiscovery(context: Context, interfaces: List<CommunicationInterface>) {
        statusInfoLiveData.postValue("Discovery in progress")
        viewModelScope.launch {
            try {
                DiscoveryFactory.getInstance().scan(object : DiscoveryEventListener {
                    override fun onCameraFound(discoveredCamera: DiscoveredCamera?) {
                        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                        if (discoveredCamera != null) {
                            // push the found device to the observers
                            foundDeviceLiveData.postValue(discoveredCamera!!)
                        }
                    }

                    override fun onDiscoveryError(communicationInterface: CommunicationInterface?, error: ErrorCode?) {
                        // let's handle this particular error type separately so we can verify if this way works as expected
                        if (BluetoothErrorCategory.Errc.BLUETOOTH_TURNED_OFF.compare(error)) {
                            statusInfoLiveData.postValue("Please turn on Bluetooth adapter prior to starting discovery.")
                        } else {
                            statusInfoLiveData.postValue(DiscoveryException(communicationInterface!!, error!!).toString())
                        }
                    }

                    override fun onDiscoveryFinished(communicationInterface: CommunicationInterface?) {
                        statusInfoLiveData.postValue("Discovery finished")
                    }
                }, *interfaces.toTypedArray())

            } catch (e: DiscoveryException) {
                statusInfoLiveData.postValue(e.toString())
            }
        }
    }

    /**
     * Stops the discovery process.
     */
    fun stopDiscovery() {
        viewModelScope.launch {
            DiscoveryFactory.getInstance().stop()
        }
    }

}