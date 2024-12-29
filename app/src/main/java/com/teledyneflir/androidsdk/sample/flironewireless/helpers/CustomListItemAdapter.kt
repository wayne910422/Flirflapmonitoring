/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file CustomListItemAdapter.kt
 * @Author Teledyne FLIR
 *
 * @brief Adapter for a list of discovered DiscoveredCamera objects.
 *
 * Copyright 2023:    Teledyne FLIR
 ***********************************************************************/

package com.teledyneflir.androidsdk.sample.flironewireless.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.teledyneflir.androidsdk.sample.flironewireless.MainActivity
import com.teledyneflir.androidsdk.sample.flironewireless.databinding.ItemCustomListLayoutBinding

/**
 * Adapter for a list of discovered [DiscoveredCamera] objects.
 */
class CustomListItemAdapter(
    private val activity: Activity,
    private val listItems: List<DiscoveredCamera>
) :
    RecyclerView.Adapter<CustomListItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemCustomListLayoutBinding =
            ItemCustomListLayoutBinding.inflate(LayoutInflater.from(activity), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val discoveredCamera = listItems[position]
        val cameraDetailsStr = if (discoveredCamera.cameraDetails != null) {
            discoveredCamera.cameraDetails.toString()
        } else {
            "N/A"
        }
        holder.tvText.text = discoveredCamera.identity.deviceId + ": " + cameraDetailsStr

        holder.itemView.setOnClickListener {
            if (activity is MainActivity)
                activity.switchToLiveView(discoveredCamera)
        }
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    class ViewHolder(view: ItemCustomListLayoutBinding) : RecyclerView.ViewHolder(view.root) {
        val tvText = view.tvText
    }
}