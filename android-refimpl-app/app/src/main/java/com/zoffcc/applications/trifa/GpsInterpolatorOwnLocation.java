package com.zoffcc.applications.trifa;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import static com.zoffcc.applications.trifa.CaptureService.GPS_UPDATE_FREQ_MS_MAX;
import static com.zoffcc.applications.trifa.CaptureService.GPS_UPDATE_FREQ_MS_MIN;
import static com.zoffcc.applications.trifa.CaptureService.MAP_FOLLOW_MODE.MAP_FOLLOW_MODE_SELF;
import static com.zoffcc.applications.trifa.CaptureService.set_map_center_to_proxy_uithread;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;
import static com.zoffcc.applications.trifa.MainActivity.PREF__map_follow_mode;
import static com.zoffcc.applications.trifa.MainActivity.mIMyLocationProvider;

/** @noinspection FieldCanBeLocal, CommentedOutCode */
public class GpsInterpolatorOwnLocation
{

    /** @noinspection unused*/
    final static String TAG = "GpsInterpolatorOL";

    private double lastLat;
    private double lastLon;
    private double lastBearing;
    private boolean lastHasBearing = false;
    private double lastAcc;
    private long lastUpdateTime = 0;
    private boolean isFirstFix = true;

    void push_geo_pos(double newLat, double newLon, double newBearing, double acc, boolean has_bearing,
                      MyLocationNewOverlay2 myLocationNewOverlay2)
    {
        try
        {
            // Log.i(TAG, "push_geo_pos: " + newLat + " " + newLon + " " + newBearing + " " + acc);
            Location interpolated_location = new Location(LocationManager.GPS_PROVIDER);
            interpolated_location.setAccuracy((float)acc);
            interpolated_location.setLatitude(newLat);
            interpolated_location.setLongitude(newLon);
            if (has_bearing)
            {
                interpolated_location.setBearing((float) newBearing);
            }
            else
            {
                interpolated_location.removeBearing();
            }
            myLocationNewOverlay2.onLocationChanged_real(interpolated_location, mIMyLocationProvider);
            if (PREF__map_follow_mode == MAP_FOLLOW_MODE_SELF.value)
            {
                set_map_center_to_proxy_uithread(interpolated_location);
            }
        }
        catch(Exception ignored)
        {
        }
    }

    /**
     * Called on every new GPS update.
     * Calculates the time since the last update and sleeps between steps.
     */
    public void onGpsUpdate(Location location, int steps, MyLocationNewOverlay2 myLocationNewOverlay2) {

        // Log.i(TAG, "__* START __");
        long currentTime = System.currentTimeMillis();

        // Calculate time elapsed since last GPS fix
        long timeDelta = currentTime - lastUpdateTime;
        // Log.i(TAG, "timeDelta=" + timeDelta + " isFirstFix=" + isFirstFix);

        if ((!PREF__gps_smooth_own) || (isFirstFix) ||
            (timeDelta < GPS_UPDATE_FREQ_MS_MIN) ||
            (timeDelta > GPS_UPDATE_FREQ_MS_MAX) || (steps < 1) || (steps > 30)) {
            lastLat = location.getLatitude();
            lastLon = location.getLongitude();
            if (lastHasBearing != location.hasBearing())
            {
                lastBearing = location.getBearing();
            }
            else if (location.hasBearing())
            {
                lastBearing = location.getBearing();
            }
            lastAcc = location.getAccuracy();
            lastUpdateTime = currentTime;
            lastHasBearing = location.hasBearing();
            isFirstFix = false;
            push_geo_pos(lastLat, lastLon, lastBearing, lastAcc, location.hasBearing(), myLocationNewOverlay2);
            // Log.i(TAG, "** RETURN **");
            return;
        }

        if (lastHasBearing != location.hasBearing())
        {
            lastBearing = location.getBearing();
        }

        // Update state for next fix
        lastUpdateTime = currentTime;

        // Determine sleep time per step (total delta / number of steps)
        long sleepTimePerStep = timeDelta / steps;

        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;

            // Interpolate Coordinates
            double interpolatedLat = lastLat + (location.getLatitude() - lastLat) * fraction;
            double interpolatedLon = lastLon + (location.getLongitude() - lastLon) * fraction;

            // Interpolate Bearing (Shortest path)
            double interpolatedBearing;
            if (lastHasBearing != location.hasBearing())
            {
                interpolatedBearing = lastBearing;
            }
            else if (location.hasBearing())
            {
                interpolatedBearing = interpolateBearing(lastBearing, location.getBearing(), fraction);
            }
            else
            {
                interpolatedBearing = lastBearing;
            }

            // Sleep to create smooth visual motion
            try {
                if ((sleepTimePerStep > 0) && (i > 1)) {
                    Thread.sleep(sleepTimePerStep);
                }
                //Log.i(TAG, "Step "+i+": Lat "+interpolatedLat+
                //           ", Lon "+interpolatedLon+", Bearing "+interpolatedBearing+" delta_t " + timeDelta);
                push_geo_pos(interpolatedLat, interpolatedLon, interpolatedBearing, location.getAccuracy(),
                             location.hasBearing(), myLocationNewOverlay2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                // Log.i(TAG, "** BREAK **");
                break;
            }
        }

        lastLat = location.getLatitude();
        lastLon = location.getLongitude();
        lastBearing = location.getBearing();
        lastHasBearing = location.hasBearing();
        // Log.i(TAG, "** DONE **");
    }

    private double interpolateBearing(double start, double end, double fraction) {
        double diff = end - start;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        double result = start + diff * fraction;
        return (result + 360) % 360;
    }
}


