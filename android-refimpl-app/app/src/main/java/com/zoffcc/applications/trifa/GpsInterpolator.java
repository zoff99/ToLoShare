package com.zoffcc.applications.trifa;

import android.util.Log;

import org.osmdroid.util.GeoPoint;

import static com.zoffcc.applications.trifa.CaptureService.remote_location_overlays;

public class GpsInterpolator
{

    final static String TAG = "CaptureService";

    private double lastLat, lastLon, lastBearing;
    private long lastUpdateTime = 0;
    private boolean isFirstFix = true;

    void push_geo_pos(double newLat, double newLon, double newBearing, float acc, String f_pubkey)
    {
        try
        {
            Log.i(TAG, "push_geo_pos: " + newLat + " " + newLon + " " + newBearing + " " + acc);
            CaptureService.remote_location_overlay_entry remote_ol = remote_location_overlays.get(f_pubkey);
            remote_ol.remote_location_overlay.setLocation(new GeoPoint(newLat, newLon));
            remote_ol.remote_location_overlay.setAccuracy(Math.round(acc));
            remote_ol.remote_location_overlay.setBearing((float)newBearing);
        }
        catch(Exception e)
        {
        }
    }

    /**
     * Called on every new GPS update.
     * Calculates the time since the last update and sleeps between steps.
     */
    public void onGpsUpdate(double newLat, double newLon, double newBearing, float acc, int steps, String f_pubkey) {
        long currentTime = System.currentTimeMillis();

        // Calculate time elapsed since last GPS fix
        long timeDelta = currentTime - lastUpdateTime;

        if ((isFirstFix) || (timeDelta > 3000)) {
            lastLat = newLat;
            lastLon = newLon;
            lastBearing = newBearing;
            lastUpdateTime = currentTime;
            isFirstFix = false;
            push_geo_pos(newLat, newLon, newBearing, acc, f_pubkey);
            return;
        }

        // Determine sleep time per step (total delta / number of steps)
        long sleepTimePerStep = timeDelta / steps;

        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;

            // Interpolate Coordinates
            double interpolatedLat = lastLat + (newLat - lastLat) * fraction;
            double interpolatedLon = lastLon + (newLon - lastLon) * fraction;

            // Interpolate Bearing (Shortest path)
            double interpolatedBearing = interpolateBearing(lastBearing, newBearing, fraction);

            // Trigger UI update or move marker here
            System.out.printf("Step %d: Lat %.6f, Lon %.6f, Bearing %.2f%n",
                              i, interpolatedLat, interpolatedLon, interpolatedBearing);

            // Sleep to create smooth visual motion
            try {
                if (sleepTimePerStep > 0) {
                    Thread.sleep(sleepTimePerStep);
                }
                push_geo_pos(interpolatedLat, interpolatedLon, interpolatedBearing, acc, f_pubkey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                break;
            }
        }

        // Update state for next fix
        lastLat = newLat;
        lastLon = newLon;
        lastBearing = newBearing;
        lastUpdateTime = currentTime;
    }

    private double interpolateBearing(double start, double end, double fraction) {
        double diff = end - start;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        double result = start + diff * fraction;
        return (result + 360) % 360;
    }
}


