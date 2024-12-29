/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file MainFragment.kt
 * @Author Teledyne FLIR
 *
 * @brief Main UI Fragment.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid
import com.flir.thermalsdk.live.CommunicationInterface
import com.flir.thermalsdk.live.WirelessCameraDetails
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.teledyneflir.androidsdk.sample.flironewireless.databinding.MainFragmentBinding
import com.teledyneflir.androidsdk.sample.flironewireless.helpers.CustomListItemAdapter
import com.teledyneflir.androidsdk.sample.flironewireless.viewmodels.DiscoveryViewModel

/**
 * Main UI Fragment.
 */
class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var binding: MainFragmentBinding
    private lateinit var discoveryViewModel: DiscoveryViewModel
    private lateinit var devicesListAdapter: CustomListItemAdapter

    private val devicesList: MutableList<DiscoveredCamera> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        discoveryViewModel =
            ViewModelProvider(requireActivity()).get(DiscoveryViewModel::class.java)

        binding = MainFragmentBinding.inflate(inflater, container, false)

        binding.startDiscovery.setOnClickListener {
            val interfacesToScan = mutableListOf(CommunicationInterface.FLIR_ONE_WIRELESS)
            if (devicesList.isNotEmpty()) {
                devicesList.clear()
                devicesListAdapter.notifyDataSetChanged()
            }
            discoveryViewModel.startDiscovery(requireContext(), interfacesToScan)
        }

        devicesListAdapter = CustomListItemAdapter(requireActivity(), devicesList)
        binding.devicesList.adapter = devicesListAdapter
        binding.devicesList.layoutManager = LinearLayoutManager(requireActivity())
        binding.devicesList.addItemDecoration(
            DividerItemDecoration(
                activity,
                DividerItemDecoration.VERTICAL
            )
        )

        // populate found devices list and notify recycler view
        discoveryViewModel.foundDeviceLiveData.observe(viewLifecycleOwner) {
            val dc = devicesList.find { discoveredCamera ->
                it.identity.communicationInterface == CommunicationInterface.FLIR_ONE_WIRELESS &&
                        discoveredCamera.identity.communicationInterface == CommunicationInterface.FLIR_ONE_WIRELESS &&
                        it.cameraDetails.ssid == discoveredCamera.cameraDetails.ssid
            }
            if (dc == null) {
                // add new item only if we don't have it on the list
                devicesList.add(it)
            } else {
                // we have the item on the list, so we want to update the values in case they changed (i.e. battery status or level, RSSI)
                dc.applyCameraDetailsFrom(WirelessCameraDetails(it.cameraDetails))
            }
            devicesListAdapter.notifyDataSetChanged()
        }

        // inform about error or status
        discoveryViewModel.statusInfoLiveData.observe(viewLifecycleOwner) {
            binding.discoveryStatus.text = it
        }

        binding.sdkVersionLabel.text = "SDK version: ${ThermalSdkAndroid.getVersion()}"
        binding.sdkHashLabel.text = "SDK hash: ${ThermalSdkAndroid.getCommitHash().substring(0, 10)}" // 10 chars for SHA is enough

        return binding.root
    }

}