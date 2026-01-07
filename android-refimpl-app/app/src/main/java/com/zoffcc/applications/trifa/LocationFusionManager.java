package com.zoffcc.applications.trifa;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import static com.zoffcc.applications.trifa.CaptureService.broadcastFusedLocation;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_dead_reconing_own;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;

public class LocationFusionManager implements SensorEventListener
{
    final static String TAG = "LocationFsnMgr";

    private SensorManager sensorManager;
    private Sensor linearAccelSensor;
    private Sensor gravitySensor;
    private Sensor magneticSensor;

    private float[] gravityValues = new float[3];
    private float[] magneticValues = new float[3];
    private boolean hasGravity = false;
    private boolean hasMagnetic = false;

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
        // We use Gravity and Magnetometer to calculate orientation
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Log.i(TAG, "new LocationFusionManager");
    }

    // Call this whenever a real GPS update arrives (1Hz)
    public void onGpsLocationChanged(Location location) {
        this.currentLat = location.getLatitude();
        this.currentLng = location.getLongitude();
        this.lastLat = currentLat;
        this.lastLng = currentLng;
        // The GPS also provides a high-accuracy 1Hz bearing to re-sync
        if (location.hasBearing()) {
            this.currentMovementBearing = location.getBearing();
        }
        // Log.i(TAG, "onGpsLocationChanged:1: " + this.lastLat + " " + this.lastLng);
        Log.i(TAG, "onGpsLocationChanged:1: " + this.currentMovementBearing);
        this.velocityX = 0;
        this.velocityY = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 1. Handle Compass Data
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3);
            hasGravity = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticValues, 0, 3);
            hasMagnetic = true;
        }

        if (hasGravity && hasMagnetic) {
            updateCompassBearing();
        }

        // 2. Handle Dead Reckoning (Position)
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (lastSensorTimestamp != 0) {
                final float dT = (event.timestamp - lastSensorTimestamp) * NS2S;

                velocityX += event.values[0] * dT;
                velocityY += event.values[1] * dT;

                double deltaX = velocityX * dT;
                double deltaY = velocityY * dT;

                currentLat += (deltaY / 111111.0);
                currentLng += (deltaX / (111111.0 * Math.cos(Math.toRadians(currentLat))));

                if ((PREF__gps_dead_reconing_own) && (PREF__gps_smooth_own)) {
                    broadcastFusedLocation(currentLat, currentLng, currentMovementBearing);
                }
            }
            lastSensorTimestamp = event.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateCompassBearing() {
        float[] R = new float[9];
        float[] I = new float[9];
        if (SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);

            // orientation[0] is azimuth (bearing) in radians
            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);

            // Normalize to 0-360
            currentMovementBearing = (azimuthInDegrees + 360) % 360;
        }
    }

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
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
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
            hasGravity = false;
            hasMagnetic = false;
        }
    }
}
