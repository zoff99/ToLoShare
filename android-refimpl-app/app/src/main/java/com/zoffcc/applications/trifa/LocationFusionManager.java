package com.zoffcc.applications.trifa;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import static com.zoffcc.applications.trifa.CaptureService.broadcastFusedLocation;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;

public class LocationFusionManager implements SensorEventListener
{
    final static String TAG = "LocationFsnMgr";

    private SensorManager sensorManager;
    private Sensor linearAccelSensor;

    private double currentLat;
    private double currentLng;
    private double lastLat;
    private double lastLng;
    private float currentMovementBearing = 0.0f;
    private float velocityX = 0;
    private float velocityY = 0;
    private long lastSensorTimestamp = 0;
    private static final float NS2S = 1.0f / 1000000000.0f; // Nanoseconds to seconds

    public LocationFusionManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Log.i(TAG, "new LocationFusionManager");
    }

    // Call this whenever a real GPS update arrives (1Hz)
    public void onGpsLocationChanged(Location location) {
        this.currentLat = location.getLatitude();
        this.currentLng = location.getLongitude();
        this.lastLat = currentLat;
        this.lastLng = currentLng;
        // Log.i(TAG, "onGpsLocationChanged:1: " + this.lastLat + " " + this.lastLng);
        // The GPS also provides a high-accuracy 1Hz bearing to re-sync
        if (location.hasBearing()) {
            this.currentMovementBearing = location.getBearing();
        }
        this.velocityX = 0;
        this.velocityY = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (lastSensorTimestamp != 0) {
                // 1. Store the "old" fused position before moving
                lastLat = currentLat;
                lastLng = currentLng;

                final float dT = (event.timestamp - lastSensorTimestamp) * NS2S;

                // 1. Integrate acceleration to get velocity (v = v0 + a*t)
                velocityX += event.values[0] * dT;
                velocityY += event.values[1] * dT;

                // 2. Integrate velocity to get displacement (d = v*t)
                double deltaX = velocityX * dT;
                double deltaY = velocityY * dT;

                // 3. Convert displacement (meters) to Lat/Lng offsets
                // Approximate: 1 degree lat is ~111,111 meters
                currentLat += (deltaY / 111111.0);
                currentLng += (deltaX / (111111.0 * Math.cos(Math.toRadians(currentLat))));

                // Calculate bearing between the last two fused points
                currentMovementBearing = calculateMovementBearing(lastLat, lastLng, currentLat, currentLng);

                if (PREF__gps_smooth_own)
                {
                    // Now 'Lat/Lng/bearing' updates at the sensor rate (e.g., 50-100Hz)
                    // Log.i(TAG, "onGpsLocationChanged:2: " + currentLat + " " + currentLng);
                    broadcastFusedLocation(currentLat, currentLng, currentMovementBearing);
                }
            }
            lastSensorTimestamp = event.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * Calculates the high-frequency bearing based on dead reckoning.
     */
    public float calculateMovementBearing(double startLat, double startLng, double endLat, double endLng) {
        Location start = new Location("fused_provider");
        start.setLatitude(startLat);
        start.setLongitude(startLng);

        Location end = new Location("fused_provider");
        end.setLatitude(endLat);
        end.setLongitude(endLng);

        // Returns degrees East of True North (-180 to 180)
        float bearing = start.bearingTo(end);

        // Convert to 0-360 range for standard compass usage
        return (bearing + 360) % 360;
    }

    /**
     * Starts high-frequency sensor updates.
     * Use SENSOR_DELAY_FASTEST for maximum update frequency.
     */
    public void startSensorFusion() {
        if (sensorManager != null && linearAccelSensor != null) {
            Log.i(TAG, "startSensorFusion");
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    /**
     * Stops sensor updates to conserve battery.
     * Always call this when the app is paused or the feature is not in use.
     */
    public void stopSensorFusion() {
        if (sensorManager != null) {
            Log.i(TAG, "stopSensorFusion");
            sensorManager.unregisterListener(this);
            // Reset timestamps and velocity to prevent "jumps" when restarting
            lastSensorTimestamp = 0;
            velocityX = 0;
            velocityY = 0;
        }
    }
}
