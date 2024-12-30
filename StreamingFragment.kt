/***********************************************************************
 * @title FLIR Atlas Android SDK
 * @file StreamingFragment.kt
 * @Author Teledyne FLIR
 *
 * @brief Fragment for live streaming.
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
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid
import com.flir.thermalsdk.androidsdk.live.discovery.ble.BleSignalScanner
import com.flir.thermalsdk.image.ColorDistributionSettings
import com.flir.thermalsdk.image.PlateauHistogramEqSettings
import com.flir.thermalsdk.image.TemperatureUnit
import com.flir.thermalsdk.image.ThermalValue
import com.flir.thermalsdk.image.fusion.FusionMode
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.live.*
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.flir.thermalsdk.live.remote.Battery
import com.flir.thermalsdk.live.remote.Calibration
import com.flir.thermalsdk.live.remote.OnReceived
import com.flir.thermalsdk.live.remote.OnRemoteError
import com.flir.thermalsdk.live.streaming.Stream
import com.flir.thermalsdk.live.streaming.Streamer
import com.flir.thermalsdk.live.streaming.ThermalStreamer
import com.flir.thermalsdk.log.ThermalLog
import com.teledyneflir.androidsdk.sample.flironewireless.MainActivity
import com.teledyneflir.androidsdk.sample.flironewireless.databinding.StreamingFragmentBinding
import com.teledyneflir.androidsdk.sample.flironewireless.helpers.*
import com.teledyneflir.androidsdk.sample.flironewireless.utils.WifiConnectionHelper
import com.teledyneflir.androidsdk.sample.flironewireless.viewmodels.StreamingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.io.File
import java.io.FileOutputStream

/**
 * Fragment for live streaming.
 */
class StreamingFragment(private val discoveredCamera: DiscoveredCamera) : Fragment() {

    companion object {
        private const val TAG = "StreamingFragment"
        fun newInstance(discoveredCamera: DiscoveredCamera) = StreamingFragment(discoveredCamera)
    }

    private data class CameraCredentials(var ssid: String, var password: String?)

    private lateinit var binding: StreamingFragmentBinding
    private lateinit var streamingViewModel: StreamingViewModel
    private var camera: Camera? = null
    private lateinit var streamer: Streamer
    private var cameraCredentials = CameraCredentials("N/A", "N/A")
    private var activeStreamT: Stream? = null

    // setup initial streaming options
    private var currentPalette = PaletteManager.getDefaultPalettes().find { it.name.lowercase() == "iron" }!!
    private var currentFusionMode = FusionMode.MSX
    private var spotActive = true
    private var spotTempValue = 0.0
    private var currentColorMode: ColorDistributionSettings = PlateauHistogramEqSettings()
    private var autoScale = true
    private var saveSnapshotRequestActive = false
    private var manualScaleRange = Pair(0.0, 20.0)
    private var currentEmissivity = 0.95

    // subscribable properties
    // use these default values until a proper value comes from a subscription callback
    private var nucState = Calibration.NucState.UNKNOWN
    private var batteryState = Battery.ChargingState.NO_CHARGING
    private var batteryPercentage = -1

    private var bleSignalScanner: BleSignalScanner? = null

    private lateinit var activeStreamOnReceivedCallback: OnReceived<Void>
    private lateinit var activeStreamOnRemoteErrorCallback: OnRemoteError

    class StreamingFragment(private val discoveredCamera: DiscoveredCamera) : Fragment() {

        private val handler = Handler(Looper.getMainLooper())
        private val interval = 20 * 60 * 1000L // 20分鐘間隔
        private var isCapturing = false

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            startAutoCapture() // 啟動自動擷取
        }

        override fun onDestroyView() {
            super.onDestroyView()
            stopAutoCapture() // 停止自動擷取
        }

        // 啟動自動擷取
        private fun startAutoCapture() {
            isCapturing = true
            handler.post(object : Runnable {
                override fun run() {
                    if (isCapturing) {
                        captureThermalImage()
                        handler.postDelayed(this, interval)
                    }
                }
            })
        }

        // 停止自動擷取
        private fun stopAutoCapture() {
            isCapturing = false
            handler.removeCallbacksAndMessages(null)
        }

        // 擷取熱影像
        private fun captureThermalImage() {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val streamer = getThermalStreamer() // 獲取熱影像流
                    val bitmap: Bitmap? = streamer?.image?.let { BitmapAndroid.createBitmap(it).bitMap }
                    if (bitmap != null) {
                        askForMedicalRecordNumber { medicalRecordNumber ->
                            saveImage(bitmap, "flir", medicalRecordNumber)
                            updateCaptureStatus("熱影像已成功保存")
                        }
                    } else {
                        updateCaptureStatus("熱影像擷取失敗")
                    }
                } catch (e: Exception) {
                    updateCaptureStatus("擷取錯誤: ${e.message}")
                }
            }
        }

        // 顯示對話框詢問病歷號
        private fun askForMedicalRecordNumber(onMedicalRecordNumberEntered: (String) -> Unit) {
            lifecycleScope.launch(Dispatchers.Main) {
                val inputField = EditText(requireContext()).apply {
                    hint = "輸入病歷號"
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("輸入病歷號")
                    .setView(inputField)
                    .setPositiveButton("確認") { _, _ ->
                        val medicalRecordNumber = inputField.text.toString()
                        if (medicalRecordNumber.isNotBlank()) {
                            onMedicalRecordNumberEntered(medicalRecordNumber)
                        } else {
                            Toast.makeText(requireContext(), "病歷號不可為空", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("取消") { _, _ ->
                        Toast.makeText(requireContext(), "操作已取消", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }

        // 儲存影像檔案
        private var activeStreamT: Stream? = null // 定義熱影像流變數

        private fun saveImage(bitmap: Bitmap, type: String, medicalRecordNumber: String) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${medicalRecordNumber}_${timestamp}_$type.jpg"
            val storageDir = File(requireContext().getExternalFilesDir(null), "CapturedImages")
            if (!storageDir.exists()) storageDir.mkdirs()

            val file = File(storageDir, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            updateCaptureStatus("檔案已保存至: ${file.absolutePath}")
        }

        private fun getThermalStreamer(): ThermalStreamer? {
            // 確保 activeStreamT 已初始化
            return activeStreamT?.let { ThermalStreamer(it) }
        }

        private fun updateCaptureStatus(message: String) {
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }









    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        streamingViewModel =
            ViewModelProvider(this).get(StreamingViewModel::class.java)

        binding = StreamingFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.status.text = "Trying to establish connection to: ${discoveredCamera.identity.deviceId}"
        // store camera SSID if available
        discoveredCamera.cameraDetails?.apply {
            cameraCredentials.ssid = ssid
            cameraCredentials.password = password
        }

        // update UI with error info
        streamingViewModel.statusInfoLiveData.observe(
            viewLifecycleOwner
        ) {
            ThermalLog.v(TAG, "statusInfoLiveData update with: $it")
            binding.status.text = it
        }
        // update UI with new rendered frame
        streamingViewModel.bitmapLiveData.observe(
            viewLifecycleOwner
        ) { binding.streamView.setImageBitmap(it) }

        streamingViewModel.bitmapScaleLiveData.observe(
            viewLifecycleOwner
        ) { binding.scaleView.setImageBitmap(it) }

        streamingViewModel.scaleRangeLiveData.observe(
            viewLifecycleOwner
        ) {
            binding.scaleTempMin.text = String.format("%.1f", it.first)
            binding.scaleTempMax.text = String.format("%.1f", it.second)
        }

        streamingViewModel.spotMeasurementLiveData.observe(
            viewLifecycleOwner
        ) {
            binding.spotInfo.text = String.format("Center spot: %.1f°C", it)
        }

        streamingViewModel.operationResultLiveData.observe(
            viewLifecycleOwner
        ) {
            ThermalLog.v(TAG, "operationResultLiveData update with: $it")
            binding.lastOperationResult.text = it
        }

        streamingViewModel.extendedStatusInfoLiveData.observe(
            viewLifecycleOwner
        ) {
            binding.extendedStatus.text = it
        }

        bindButtonsToActions()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (discoveredCamera.identity.communicationInterface == CommunicationInterface.FLIR_ONE_WIRELESS) {
            tryToEstablishWifiConnection()
        }
    }

    private fun bindButtonsToActions() {
        binding.iconFusionMode.setOnClickListener {
            FusionHandler.showFusionModes(requireContext(), currentFusionMode) { selectedMode ->
                currentFusionMode = selectedMode
            }
        }
        binding.iconPalette.setOnClickListener {
            PaletteHandler.showAvailablePalettes(requireContext(), currentPalette) { selectedPalette ->
                currentPalette = selectedPalette
            }
        }
        binding.iconEmissivity.setOnClickListener {
            EmissivityHandler.showEmissivityDialog(requireContext(), currentEmissivity) { newEmissivityValue ->
                currentEmissivity = newEmissivityValue
            }
        }
        binding.iconMeasurements.setOnClickListener {
            spotActive = !spotActive
        }
        binding.iconColorDistribution.setOnClickListener {
            ColorDistributionHandler.showColorModes(requireContext(), currentColorMode) { selectedColorMode ->
                currentColorMode = selectedColorMode
            }
        }
        binding.iconTempRange.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val index = TempRangeHandler.readSelectedIndex(camera)
                val tempRanges = TempRangeHandler.readAvailableRanges(camera)
                if (index != null && tempRanges != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        TempRangeHandler.showTempRanges(requireContext(), index, TempRangeHandler.mapToStringList(tempRanges)) { selectedTempRangeIndex ->
                            CoroutineScope(Dispatchers.IO).launch {
                                camera?.remoteControl?.temperatureRange?.selectedIndex()?.sync = selectedTempRangeIndex
                            }
                        }
                    }
                } else {
                    streamingViewModel.operationResultLiveData.postValue("Temperature range feature not supported")
                }
            }
        }
        binding.iconSave.setOnClickListener {
            saveSnapshotRequestActive = true
        }
        binding.scaleView.setOnClickListener {
            autoScale = !autoScale
        }
        binding.scaleTempMin.setOnClickListener {
            attemptToModifyScale()
        }
        binding.scaleTempMax.setOnClickListener {
            attemptToModifyScale()
        }
        binding.iconNuc.setOnClickListener {
            camera?.remoteControl?.calibration?.apply {
                nuc().execute(
                    { streamingViewModel.operationResultLiveData.postValue("NUC completed.") },
                    { streamingViewModel.operationResultLiveData.postValue("NUC request error: $it") }
                )
            }
        }
        binding.iconChangeSsid.setOnClickListener {
            DialogBuilder.getInstance()
                .createSsidChangeDialog(requireContext(), "Requesting to change F1 WiFi settings", cameraCredentials.ssid) { newSsid ->
                    camera?.remoteControl?.system?.apply {
                        if (newSsid != cameraCredentials.ssid && wifiSsid().isAvailable) {
                            wifiSsid().execute(
                                { streamingViewModel.operationResultLiveData.postValue("WiFi settings change <SSID> completed.") },
                                { streamingViewModel.operationResultLiveData.postValue("WiFi settings change <SSID> error: $it") },
                                newSsid
                            )
                        }
                    }
                }.show()
        }
    }

    override fun onStop() {
        ThermalLog.d(TAG, "onStop(): bleSignalScanner?.stopListening()")
        // stop BLE signal strength listening when streaming fragment is destroyed
        bleSignalScanner?.stopListening()
        ThermalLog.d(TAG, "onStop(): activeStreamT?.stop()")
        activeStreamT?.stop()
        ThermalLog.d(TAG, "onStop(): unsubscribeForBatteryAndCalibrationEvents()")
        unsubscribeForBatteryAndCalibrationEvents()
        ThermalLog.d(TAG, "onStop(): camera?.disconnect()")
        camera?.disconnect()
        super.onStop()
    }

    /**
     * Automatically try to connect to the given [DiscoveredCamera].
     */
    private fun tryToEstablishWifiConnection() {
        ThermalLog.d(MainActivity.TAG, "tryToEstablishWifiConnection()")
        if (discoveredCamera.cameraDetails != null) {
            val wifiSsid = discoveredCamera.cameraDetails!!.ssid
            streamingViewModel.statusInfoLiveData.postValue("Trying to establish connection to the camera over WiFi named: $wifiSsid")
            if (WifiConnectionHelper.isConnectedToNetwork(requireContext(), wifiSsid)) {
                ThermalLog.d(TAG, "Already connected to camera's WiFi: $wifiSsid")
                connectToCameraAndStartStreaming(discoveredCamera.identity)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    ThermalLog.d(TAG, "Establish connection to camera's WiFi: $wifiSsid")
                    WifiConnectionHelper().connectToWifi(
                        requireContext(),
                        discoveredCamera.cameraDetails.ssid,
                        obtainNetworkPassword(discoveredCamera.cameraDetails),
                        object : WifiConnectionHelper.WifiConnectionStatus {
                            override fun publishStatus(status: WifiConnectionHelper.StatusInfo) {
                                when (status.status) {
                                    WifiConnectionHelper.ConInfo.IN_PROGRESS -> {
                                        ThermalLog.d("DiscoveryHelper", "WiFi connections status: ${status.message}")
                                    }
                                    WifiConnectionHelper.ConInfo.ERROR -> {
                                        streamingViewModel.statusInfoLiveData.postValue("Connection to WiFi failed with error: ${status.message}")
                                    }
                                    WifiConnectionHelper.ConInfo.CONNECTED -> {
                                        connectToCameraAndStartStreaming(discoveredCamera.identity)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        } else {
            streamingViewModel.statusInfoLiveData.postValue("Trying to establish connection to emulated camera")
            connectToCameraAndStartStreaming(discoveredCamera.identity)
        }
    }

    /**
     * Based on the given [WirelessCameraDetails] obtains a password to F1 Edge camera's WiFi network.
     *
     * @return Returns a password to F1 Edge camera's WiFi network as a plain String.
     */
    private fun obtainNetworkPassword(cameraDetails: WirelessCameraDetails): String {
        return if (cameraDetails.passwordChangedFlag) {
            // use a new way of getting camera password based on camera's serial number
            WirelessCameraDetails.getPasswordForSerialNumber(cameraDetails.serialNumber)
        } else {
            // use old way - use default arbitrary password
            "12345678"
        }
    }

    /**
     * Show UI allowing to modify scale settings.
     */
    private fun attemptToModifyScale() {
        ScaleHandler.showScaleDialog(requireContext(), manualScaleRange) { isAutoScale, scaleRange ->
            autoScale = isAutoScale
            if (!isAutoScale) {
                manualScaleRange = Pair(scaleRange.first!!, scaleRange.second!!)
            }
        }
    }

    /**
     * Perform actual connection to the F1 wireless camera and starts the live stream.
     */
    private fun connectToCameraAndStartStreaming(identity: Identity) {
        CoroutineScope(Dispatchers.IO).launch {
            ThermalLog.d(TAG, "connectToCameraAndStartStreaming: Identity: $identity")
            // try to connect to the camera
            try {
                camera = Camera()
                camera!!.connect(
                    identity, // identity selected in the previous fragment
                    {
                        // error callback
                        ThermalLog.w(TAG, "Camera connection interrupted, error: $it")
                        streamingViewModel.statusInfoLiveData.postValue(it.toString())
                    },
                    ConnectParameters() // default connection parameters
                )
            } catch (e: IOException) {
                streamingViewModel.statusInfoLiveData.postValue("Failed to connect to the camera: ${e.message}")
                return@launch
            }

            subscribeForBatteryAndCalibrationEvents()

            val rci = camera!!.remoteControl?.cameraInformation()?.sync
            ThermalLog.i(TAG, "Remote camera info: $rci")
            ThermalLog.d(TAG, "S/N from remote camera info: ${rci?.serialNumber}")

            // try to use first found thermal stream
            activeStreamT = camera!!.streams.find { it.isThermal }
            if (activeStreamT != null) {
                // work with first found thermal stream
                streamer = ThermalStreamer(activeStreamT)

                if (discoveredCamera.identity.communicationInterface == CommunicationInterface.FLIR_ONE_WIRELESS) {
                    // when connection is established start BLE listener for signal strength
                    if (bleSignalScanner == null) {
                        bleSignalScanner = BleSignalScanner(requireContext(), discoveredCamera.cameraDetails.serialNumber)
                    }
                    bleSignalScanner?.startListening(object : BleSignalScanner.BleSignalListener {
                        @SuppressLint("SetTextI18n")
                        override fun onSignalReceived(signalStrength: SignalStrength?) {
                            ThermalLog.d(TAG, "Signal strength: ${signalStrength?.name}")
                            binding.bleSignal.text = "Signal strength: ${signalStrength?.name}"
                        }

                        override fun onDiscoveryError(error: ErrorCode?) {
                            streamingViewModel.statusInfoLiveData.postValue("SignalStrength listener error: $error")
                        }
                    })
                }

                activeStreamOnReceivedCallback = OnReceived {
                    ThermalLog.v(TAG, "Thermal Frame received")
                    // update data based on the received thermal image
                    // doAsync because Streamer.update() shouldn't be called directly on the streaming (OnReceived) thread
                    CoroutineScope(Dispatchers.IO).launch {
                        refreshThermalFrame()
                    }
                }
                activeStreamOnRemoteErrorCallback = OnRemoteError {
                    // error callback
                    ThermalLog.w(TAG, "Active thermal stream interrupted, error: $it")
                    streamingViewModel.statusInfoLiveData.postValue(it.toString())
                    // visualize that streaming is stopped via the button
                    connectButtonOff()
                }

                activeStreamT!!.start(activeStreamOnReceivedCallback, activeStreamOnRemoteErrorCallback)
                streamingViewModel.statusInfoLiveData.postValue("Thermal streaming started.")
                setupDisconnectButton()
            } else {
                // camera doesn't have any thermal streams
                streamingViewModel.statusInfoLiveData.postValue("No thermal stream available for this camera.")
            }
        }
    }

    private fun setupDisconnectButton() {
        CoroutineScope(Dispatchers.Main).launch {
            // remove the button listener, otherwise it will react to changing the state unnecessarily
            binding.switchConnection.setOnCheckedChangeListener(null)
            binding.switchConnection.isEnabled = true
            binding.switchConnection.isChecked = true
            // set button listener again, so it reacts to user interaction
            setupConnectButtonListener()
        }
    }

    private fun connectButtonOff() {
        CoroutineScope(Dispatchers.Main).launch {
            // remove the button listener, otherwise it will react to changing the state unnecessarily
            binding.switchConnection.setOnCheckedChangeListener(null)
            binding.switchConnection.isChecked = false
            // set button listener again, so it reacts to user interaction
            setupConnectButtonListener()
        }
    }

    private fun setupConnectButtonListener() {
        binding.switchConnection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (discoveredCamera.identity.communicationInterface == CommunicationInterface.FLIR_ONE_WIRELESS) {
                    ThermalLog.d(TAG, "Manual connection to the camera while on WiFi: ${discoveredCamera.cameraDetails!!.ssid}")
                    connectToCameraAndStartStreaming(discoveredCamera.identity)
                }
            } else {
                unsubscribeForBatteryAndCalibrationEvents()
                camera?.disconnect()
            }
        }
    }

    /**
     * Subscribe for NUC and Battery events.
     * Note that events are coming after streaming starts, usually with some delay. Until then Battery and calibration info uses a placeholder data.
     */
    private fun subscribeForBatteryAndCalibrationEvents() {
        camera!!.remoteControl?.calibration?.nucState()?.subscribe {
            if (it != nucState) {
                // update current state
                nucState = it
                // notify about change
                streamingViewModel.extendedStatusInfoLiveData.postValue("Battery: ${batteryState.name}, $batteryPercentage%, Calibration: $nucState")
            }
        }
        camera!!.remoteControl?.battery?.chargingState()?.subscribe {
            if (it != batteryState) {
                // update current state
                batteryState = it
                // notify about change
                streamingViewModel.extendedStatusInfoLiveData.postValue("Battery: ${batteryState.name}, $batteryPercentage%, Calibration: $nucState")
            }
        }
        camera!!.remoteControl?.battery?.percentage()?.subscribe {
            if (it != batteryPercentage) {
                // update current state
                batteryPercentage = it
                // notify about change
                streamingViewModel.extendedStatusInfoLiveData.postValue("Battery: ${batteryState.name}, $batteryPercentage%, Calibration: $nucState")
            }
        }
    }

    /**
     * Unsubscribe for NUC and Battery events.
     */
    private fun unsubscribeForBatteryAndCalibrationEvents() {
        camera!!.remoteControl?.calibration?.nucState()?.unsubscribe()
        camera!!.remoteControl?.battery?.chargingState()?.unsubscribe()
        camera!!.remoteControl?.battery?.percentage()?.unsubscribe()
    }

    /**
     * Redraws UI based on the received thermal frame.
     */
    @Synchronized
    private fun refreshThermalFrame() {
        // setup stream
        val tStreamer = streamer as ThermalStreamer
        tStreamer.isRenderScale = true
        tStreamer.isAutoScale = autoScale

        // update streamer in order to be able to render live view and scale bitmaps
        tStreamer.update()

        // safely access thermal image to set particular settings
        tStreamer.withThermalImage {
            ThermalLog.v(TAG, "CameraInformation from ThermalImage: ${it.cameraInformation}")
            // set some basic image parameters
            it.palette = currentPalette
            it.fusion?.setFusionMode(currentFusionMode)
            it.colorDistributionSettings = currentColorMode

            if (!autoScale) {
                streamingViewModel.scaleRangeLiveData.postValue(manualScaleRange)
                it.scale.rangeMin = ThermalValue(manualScaleRange.first, TemperatureUnit.CELSIUS)
                it.scale.rangeMax = ThermalValue(manualScaleRange.second, TemperatureUnit.CELSIUS)
            }

            if (spotActive && currentFusionMode != FusionMode.VISUAL_ONLY) {
                if (it.measurements.spots.isEmpty()) {
                    it.measurements.addSpot(it.width / 2, it.height / 2)
                }
                spotTempValue = it.measurements.spots[0].value.asCelsius().value
            }

            if (saveSnapshotRequestActive) {
                saveSnapshotRequestActive = false
                val success = SaveHandler.saveImage(requireContext(), it, null)
                streamingViewModel.operationResultLiveData.postValue((if (success) "Snapshot stored" else "Snapshot failed"))
            }

            it.imageParameters.emissivity = currentEmissivity
        }

        // get a buffer with the colorized image for live view and scale
        val imageBuffer = tStreamer.image
        val imageBufferScale = tStreamer.scaleImage

        if (autoScale) {
            streamingViewModel.scaleRangeLiveData.postValue(
                Pair(tStreamer.scaleRangeMin.asCelsius().value, tStreamer.scaleRangeMax.asCelsius().value)
            )
        }

        try {
            // this could throw IllegalArgumentException if javaBuffer width or height are <= 0
            if (imageBuffer != null) {
                val bmp = BitmapAndroid.createBitmap(imageBuffer).bitMap
                if (bmp != null) {
                    if (spotActive && currentFusionMode != FusionMode.VISUAL_ONLY) {
                        MeasurementHandler.drawCenterSpot(bmp)
                        streamingViewModel.bitmapLiveData.postValue(bmp)
                        streamingViewModel.spotMeasurementLiveData.postValue(spotTempValue)
                    } else {
                        streamingViewModel.bitmapLiveData.postValue(bmp)
                        streamingViewModel.spotMeasurementLiveData.postValue(0.0)
                    }
                }
            }
            if (imageBufferScale != null) {
                val bmpScale = BitmapAndroid.createBitmap(imageBufferScale).bitMap
                if (bmpScale != null) {
                    streamingViewModel.bitmapScaleLiveData.postValue(bmpScale)
                }
            }
        } catch (e: IllegalArgumentException) {
            // ignore in this sample, just log to console
            ThermalLog.e(TAG, "Exception while streaming: $e")
        }
    }

}
