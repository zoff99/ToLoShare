package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public class MockLocationSimulator {
    private final LocationManager locationManager;
    private final String provider = "gps";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler actionHandler = new Handler(Looper.getMainLooper());

    private double currentLat = 48.20182007532241;
    private double currentLng = 16.34695813573697;
    private float currentSpeedMs = 10.0f; // Start at ~36 km/h
    private float internalBearing = 0.0f;
    private boolean isStopped = false;

    public MockLocationSimulator(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setupMockProvider();
    }

    @SuppressLint("WrongConstant")
    private void setupMockProvider() {
        try
        {
            if (locationManager.getProvider(provider) != null)
            {
                locationManager.removeTestProvider(provider);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            locationManager.addTestProvider(provider, false, false, false,
                                            false, true, true, true,
                                            2, 1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            locationManager.setTestProviderEnabled(provider, true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void startSimulation() {
        // Start the 1Hz location update loop
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateMockLocation();
                int baseDelay = 1000;
                final int JITTER = 300;

                // Simulate a "lost" update or network lag (e.g., 10% chance)
                // If triggered, the next update will happen in ~2000ms instead of ~1000ms
                if (new java.util.Random().nextDouble() < 0.10) {
                    baseDelay = 2000;
                }

                int jitter = new java.util.Random().nextInt(JITTER + 1) - (JITTER / 2);

                mainHandler.postDelayed(this, baseDelay + jitter);
            }
        });

        // Start the automated "Driving Script"
        runDrivingScript();
    }

    /**
     * Schedules a sequence of driving behaviors over time.
     */
    private void runDrivingScript() {
        // 0s: Start driving straight at 10m/s
        actionHandler.postDelayed(() -> setSpeed(10.0f), 0);

        // 5s: Speed up to 25m/s (~90 km/h)
        actionHandler.postDelayed(() -> setSpeed(25.0f), 5000);

        // 10s: Turn 90 degrees right (East)
        actionHandler.postDelayed(() -> turn(90), 10000);

        // 15s: Stop at a red light
        actionHandler.postDelayed(() -> setStopped(true), 15000);

        // 20s: Resume driving and turn 45 degrees left
        actionHandler.postDelayed(() -> {
            setStopped(false);
            setSpeed(15.0f);
            turn(-45);
        }, 20000);

        // 30s: Stop at a red light
        actionHandler.postDelayed(() -> setStopped(true), 30000);

        // 35s: Slow down
        actionHandler.postDelayed(() -> {
            setStopped(false);
            setSpeed(15.0f);
        }, 35000);
    }

    private void updateMockLocation() {
        if (!isStopped) {
            double distanceMoved = currentSpeedMs * 1.0;
            double latChange = (distanceMoved * Math.cos(Math.toRadians(internalBearing))) / 111111.0;
            double lngChange = (distanceMoved * Math.sin(Math.toRadians(internalBearing))) /
                               (111111.0 * Math.cos(Math.toRadians(currentLat)));

            currentLat += latChange;
            currentLng += lngChange;
        }

        Location mockLocation = new Location(provider);
        mockLocation.setLatitude(currentLat);
        mockLocation.setLongitude(currentLng);
        mockLocation.setSpeed(isStopped ? 0.0f : currentSpeedMs);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setTime(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        if (isStopped)
        {
            mockLocation.setBearing(0.0f);
            mockLocation.removeBearing();
        }
        else
        {
            mockLocation.setBearing(internalBearing);
        }

        locationManager.setTestProviderLocation(provider, mockLocation);
    }

    public void setStopped(boolean stopped) { this.isStopped = stopped; }
    public void turn(float degrees) { this.internalBearing = (this.internalBearing + degrees) % 360; }
    public void setSpeed(float speedMs) { this.currentSpeedMs = speedMs; }

    public void stopSimulation() {
        mainHandler.removeCallbacksAndMessages(null);
        actionHandler.removeCallbacksAndMessages(null);
        try {
            locationManager.removeTestProvider(provider);
        } catch (Exception ignored) {}
    }
}