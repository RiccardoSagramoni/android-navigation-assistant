package it.unipi.dii.indoornavigatorassistant.scanners

import android.R
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import it.unipi.dii.indoornavigatorassistant.BLERegionManager
import it.unipi.dii.indoornavigatorassistant.NavigationActivity
import it.unipi.dii.indoornavigatorassistant.dao.BeaconInfoProvider
import it.unipi.dii.indoornavigatorassistant.databinding.ActivityNavigationBinding
import it.unipi.dii.indoornavigatorassistant.util.Constants
import java.lang.ref.WeakReference


class BeaconScanner(private val navigationActivity: WeakReference<NavigationActivity>,
                    private val binding: ActivityNavigationBinding) {
    
    private val proximityManager = ProximityManagerFactory.create(navigationActivity.get()!!)
    private val beaconInfoProvider = BeaconInfoProvider.getInstance(navigationActivity.get()!!)
    private val regionManager = BLERegionManager()

    fun startScanning() {
        Log.d(Constants.LOG_TAG, "BeaconScanner::startScanning - scanning started")
        proximityManager.setIBeaconListener(createIBeaconListener())
        proximityManager.connect { proximityManager.startScanning() }
    }

    private fun createIBeaconListener(): IBeaconListener {
        Log.d(Constants.LOG_TAG, "BeaconScanner::createIBeaconListener - beacon listener started")
        return object : SimpleIBeaconListener() {
            override fun onIBeaconDiscovered(ibeacon: IBeaconDevice, region: IBeaconRegion) {
                Log.d(Constants.LOG_TAG, "BeaconScanner::onIBeaconDiscovered - beacon discovered: $ibeacon")
            }
            override fun onIBeaconsUpdated(ibeacons: MutableList<IBeaconDevice>, region: IBeaconRegion) {
                // Sort beacons by signal strength
                val sortedBeacons = ibeacons.sortedByDescending { it.rssi }

                if (sortedBeacons.size >= 2) {
                    // Get the top 2 beacons
                    val top2Beacons = sortedBeacons.take(2)
                    // Print the top 2 beacon IDs
                    val top2BeaconIds = top2Beacons.map { it.uniqueId }
                    Log.d(Constants.LOG_TAG, "BeaconScanner::onIBeaconsUpdated " +
                            "- 2 nearest beacons: $top2BeaconIds")
                    // Get regionId from the top 2 beacons for rssi
                    val regionId = beaconInfoProvider.computeBLERegionId(top2BeaconIds[0], top2BeaconIds[1])
                    displayBeaconRegionInfo(regionId)

                    if (regionManager.isNewRegion(regionId)) {
                        // Get points of interest
                        val pointsOfInterest = beaconInfoProvider.getBLERegionInfo(regionId)
                        displayPointsOfInterestInfo(pointsOfInterest)
                    }
                }
            }
        }
    }

    /**
     * display on Logcat and Navigation activity page the current Beacon Region where the user is
     *
     * @param regionId id of current region
     */
    private fun displayBeaconRegionInfo(regionId : String){
        Log.d(Constants.LOG_TAG, "BeaconScanner::onIBeaconsUpdated " +
                "- Region scanned: $regionId")
        binding.textViewCurrentRegion.text = BEACON_INFO_MESSAGE + "$regionId"
    }

    /**
     * display on Logcat and Navigation activity page the Points of interest of the current region
     *
     * @param pointsOfInterest
     */
    private fun displayPointsOfInterestInfo(pointsOfInterest: List<String>?){
        Log.d(Constants.LOG_TAG, "BeaconScanner::onIBeaconsUpdated " +
                "- Points of interest: $pointsOfInterest")
        Toast.makeText(navigationActivity.get()!!, pointsOfInterest.toString(), Toast.LENGTH_SHORT).show()

        //display region points of interest
        if(pointsOfInterest != null) {
            val arrayAdapter: ArrayAdapter<*>
            val beaconListView = binding.POIBeacons
            arrayAdapter = ArrayAdapter(
                navigationActivity.get()!!,
                R.layout.simple_list_item_1,
                pointsOfInterest
            )
            beaconListView.adapter = arrayAdapter
        }
        else{
            binding.POIBeacons.adapter = null;
        }
    }


    fun stopScanning() {
        proximityManager.stopScanning()
        Log.d(Constants.LOG_TAG, "BeaconScanner::stopScanning - scanning stopped")
    }

    fun disconnect() {
        proximityManager.disconnect()
        Log.d(Constants.LOG_TAG, "BeaconScanner::disconnect - scanning service disconnected")
    }

    /**
     * Beacon Region Info Message
     */
    companion object{
        const val BEACON_INFO_MESSAGE = "Current Region Detected: "
    }

}
