/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file DiscoveryException.kt
 * @Author Teledyne FLIR
 *
 * @brief A custom exception type that might occur during discovery phrase.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.utils

import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.live.CommunicationInterface

/**
 * A custom exception type that might occur during discovery phrase.
 */
class DiscoveryException(val communicationInterface: CommunicationInterface, val errc: ErrorCode) : Exception() {

    override fun toString(): String {
        return "DiscoveryException(communicationInterface=$communicationInterface, errc=$errc)"
    }
}