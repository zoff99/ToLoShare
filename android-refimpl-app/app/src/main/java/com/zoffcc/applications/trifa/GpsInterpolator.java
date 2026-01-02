package com.zoffcc.applications.trifa;

import android.location.Location;
import android.location.LocationManager;

import org.osmdroid.util.GeoPoint;

import static com.zoffcc.applications.trifa.CaptureService.remote_location_data;
import static com.zoffcc.applications.trifa.CaptureService.remote_location_overlays;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_friends;
import static com.zoffcc.applications.trifa.MainActivity.follow_friend_on_map;

public class GpsInterpolator
{

    final static String TAG = "GpsInterpolator";

    private double lastLat;
    private double lastLon;
    private double lastBearing;
    private boolean last_has_bearing;
    private long lastUpdateTime = 0;
    private boolean isFirstFix = true;

    void push_geo_pos(double newLat, double newLon, double newBearing, float acc, String f_pubkey)
    {
        try
        {
            // Log.i(TAG, "push_geo_pos: " + newLat + " " + newLon + " " + newBearing + " " + acc);
            CaptureService.remote_location_overlay_entry remote_ol = remote_location_overlays.get(f_pubkey);
            remote_ol.remote_location_overlay.setLocation(new GeoPoint(newLat, newLon));
            remote_ol.remote_location_overlay.setAccuracy(Math.round(acc));
            remote_ol.remote_location_overlay.setBearing((float)newBearing);
        }
        catch(Exception e)
        {
        }

        try
        {
            CaptureService.remote_location_entry re = remote_location_data.get(f_pubkey);
            if (re.remoteBestLocation == null)
            {
                re.remoteBestLocation = new Location(LocationManager.GPS_PROVIDER);
            }
            re.remoteBestLocation.setAccuracy(acc);
            re.remoteBestLocation.setLatitude(newLat);
            re.remoteBestLocation.setLongitude(newLon);
            if (re.has_bearing)
            {
                re.remoteBestLocation.setBearing((float) newBearing);
            }
            else
            {
                re.remoteBestLocation.removeBearing();
            }
        }
        catch(Exception e)
        {
        }

        try
        {
            follow_friend_on_map(f_pubkey);
        }
        catch(Exception e)
        {
        }
    }

    /**
     * Called on every new GPS update.
     * Calculates the time since the last update and sleeps between steps.
     */
    public void onGpsUpdate(double newLat, double newLon, double newBearing_, boolean has_bearing, float acc, int steps, String f_pubkey) {
        long currentTime = System.currentTimeMillis();

        // Calculate time elapsed since last GPS fix
        long timeDelta = currentTime - lastUpdateTime;

        double newBearing;
        if ((!PREF__gps_smooth_friends) || (isFirstFix) || (timeDelta > 3000) || (steps < 1) || (steps > 20)) {
            lastLat = newLat;
            lastLon = newLon;
            if (has_bearing)
            {
                newBearing = newBearing_;
            }
            else
            {
                newBearing = lastBearing;
            }
            lastBearing = newBearing;
            lastUpdateTime = currentTime;
            isFirstFix = false;
            push_geo_pos(newLat, newLon, newBearing, acc, f_pubkey);
            return;
        }

        if (has_bearing)
        {
            newBearing = newBearing_;
        }
        else
        {
            newBearing = lastBearing;
        }

        // Determine sleep time per step (total delta / number of steps)
        long sleepTimePerStep = timeDelta / (steps + 1);

        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / (steps + 1);

            // Interpolate Coordinates
            double interpolatedLat = lastLat + (newLat - lastLat) * fraction;
            double interpolatedLon = lastLon + (newLon - lastLon) * fraction;

            // Interpolate Bearing (Shortest path)
            double interpolatedBearing = interpolateBearing(lastBearing, newBearing, fraction);

            // Sleep to create smooth visual motion
            try {
                if (sleepTimePerStep > 0) {
                    Thread.sleep(sleepTimePerStep);
                }
                // System.out.printf("Step %d: Lat %.6f, Lon %.6f, Bearing %.2f%n",
                //                  i, interpolatedLat, interpolatedLon, interpolatedBearing);
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


