package org.neocities.djdiskmachine.lgpt_android

import android.content.Context
import android.hardware.Sensor
import android.util.Log
import org.libsdl.app.SDLSurface

/**
 * Custom SDL Surface that disables accelerometer-as-joystick spam
 * LGPT doesn't use accelerometer input, so we suppress it to reduce log noise
 */
class CustomSDLSurface(context: Context) : SDLSurface(context) {
    
    companion object {
        private const val TAG = "CustomSDLSurface"
    }

    override fun enableSensor(sensortype: Int, enabled: Boolean) {
        // Disable accelerometer-as-joystick; LGPT doesn't use it and it spams the event log
        if (sensortype == Sensor.TYPE_ACCELEROMETER) {
            Log.i(TAG, "Accelerometer sensor ignored (LGPT doesn't use it)")
            return
        }
        super.enableSensor(sensortype, enabled)
    }
}
