/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file WifiConnectionHelper.kt
 * @Author Teledyne FLIR
 *
 * @brief A helper for WiFi connection handling.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

@file:Suppress("DEPRECATION")

package com.teledyneflir.androidsdk.sample.flironewireless.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.flir.thermalsdk.log.ThermalLog

/**
 * A helper for WiFi connection handling.
 */
class WifiConnectionHelper {

    companion object {
        private const val TAG = "WifiConnectionHelper"

        fun isConnectedToNetwork(context: Context, ssid: String): Boolean {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectedSsid = wifiManager.connectionInfo.ssid;
            return connectedSsid.contains(ssid)
        }
    }

    enum class ConInfo {
        IN_PROGRESS,
        CONNECTED,
        ERROR
    }

    class StatusInfo(val status: ConInfo, val message: String)

    interface WifiConnectionStatus {
        fun publishStatus(status: StatusInfo)
    }

    private var wifiBroadcastReceiver: BroadcastReceiver? = null

    /**
     * Performs a connection to the specified WiFi network.
     */
    fun connectToWifi(context: Context, ssid: String, pwd: String, statusListener: WifiConnectionStatus) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        statusListener.publishStatus(StatusInfo(ConInfo.IN_PROGRESS, "Connecting to WiFi: $ssid"))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // prepare WiFi change listener
            wifiBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent?) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    if (wifiManager.isWifiEnabled) {
                        // unregister receiver when we are connected and prior to establishing direct connection to the camera
                        cleanup(context)
                        statusListener.publishStatus(StatusInfo(ConInfo.CONNECTED, "Connected to WiFi: $ssid"))
                    } else {
                        cleanup(context)
                        statusListener.publishStatus(StatusInfo(ConInfo.ERROR, "WiFi disconnected"))
                    }
                }
            }

            // register listener
            context.registerReceiver(wifiBroadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = String.format("\"%s\"", ssid)
            wifiConfig.preSharedKey = String.format("\"%s\"", pwd)
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()

        } else {
            val ns = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pwd)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // F1 WiFi doesn't need/have internet access/capability
                .setNetworkSpecifier(ns)
                .build()

            connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    statusListener.publishStatus(StatusInfo(ConInfo.ERROR, "WiFi disconnected"))
                }

                override fun onAvailable(network: Network) {
                    setWifiAsPreferredNetwork(context)
                    statusListener.publishStatus(StatusInfo(ConInfo.CONNECTED, "Connected to WiFi: $ssid"))
                }
            })
        }
    }

    /**
     * Performs an internal cleanup.
     */
    private fun cleanup(context: Context) {
        if (wifiBroadcastReceiver != null) {
            context.unregisterReceiver(wifiBroadcastReceiver)
        }
        wifiBroadcastReceiver = null
    }

    /**
     * Makes sure Wifi network is a preferred one used to scan and connect to camera.
     * This is the case when both Mobile Data network (with Internet access) and WiFi network (without Internet access) are present.
     * Some Android phones (i.e. Google Pixel XL) in such scenario set Mobile network (with Internet access) as a preferred one, but we want WiFi.
     * The list of available networks looks similar to this:
     * [type: MOBILE['LTE'], state: CONNECTED/CONNECTED, reason: connected, extra: internet, failover: false, available: true, roaming: false]
     * [type: WIFI[], state: CONNECTED/CONNECTED, reason: (unspecified), extra: (none), failover: false, available: true, roaming: false]
     */
    private fun setWifiAsPreferredNetwork(context: Context) {
        ThermalLog.d(TAG, ">> setWifiAsPreferredNetwork()")
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // list all available networks, it may have both WiFi and Mobile connected
        val networks = connectivityManager.allNetworks
        var wifiNetwork: Network? = null
        var wifiNetworkInfo: NetworkInfo? = null
        ThermalLog.d(TAG, "Currently available networks:")
        for (net in networks) {
            val info = connectivityManager.getNetworkInfo(net)
            // print all available network info
            ThermalLog.d(TAG, "Available network:" + info.toString())
            if (info!!.type == ConnectivityManager.TYPE_WIFI && info.isConnected) {
                wifiNetwork = net
                wifiNetworkInfo = info
            }
        }
        if (wifiNetwork != null) {
            ThermalLog.d(TAG, "Setting preferred network to: $wifiNetworkInfo")
            // ensure WiFi is the preferred network type
            connectivityManager.bindProcessToNetwork(wifiNetwork)
        } else {
            ThermalLog.d(TAG, "Failed to set preferred WiFi network - no WiFi connection: $wifiNetworkInfo")
        }
        ThermalLog.d(TAG, "<< setWifiAsPreferredNetwork()")
    }

}