package com.zoffcc.applications.trifa;

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
    private final String provider = LocationManager.GPS_PROVIDER;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Starting coordinates (Example: San Francisco)
    private double currentLat = 37.7749;
    private double currentLng = -122.4194;
    private final double latIncrement = 0.00015; // Rough sim for driving speed

    public MockLocationSimulator(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        setupMockProvider();
    }

    private void setupMockProvider() {
        try {
            if (locationManager.getProvider(provider) != null) {
                locationManager.removeTestProvider(provider);
            }
            locationManager.addTestProvider(provider, false, false, false, false, true, true, true, ProviderProperties.POWER_USAGE_MEDIUM, ProviderProperties.ACCURACY_FINE);
            locationManager.setTestProviderEnabled(provider, true);
        } catch (SecurityException e) {
            // Requires 'Select mock location app' in Developer Options
        }
    }

    public void startSimulation() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateMockLocation();
                // Schedule next update in 1 second
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void updateMockLocation() {
        Location mockLocation = new Location(provider);

        // Simulate movement
        currentLat += latIncrement;

        mockLocation.setLatitude(currentLat);
        mockLocation.setLongitude(currentLng);
        mockLocation.setAltitude(0);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setSpeed(16.6f); // ~60 km/h
        mockLocation.setTime(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        locationManager.setTestProviderLocation(provider, mockLocation);
    }

    public void stopSimulation() {
        handler.removeCallbacksAndMessages(null);
        locationManager.removeTestProvider(provider);
    }
}
