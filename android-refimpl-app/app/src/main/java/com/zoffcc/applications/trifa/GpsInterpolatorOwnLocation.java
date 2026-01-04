package com.zoffcc.applications.trifa;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import static com.zoffcc.applications.trifa.CaptureService.GPS_UPDATE_FREQ_MS_MAX;
import static com.zoffcc.applications.trifa.CaptureService.GPS_UPDATE_FREQ_MS_MIN;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;
import static com.zoffcc.applications.trifa.MainActivity.mIMyLocationProvider;

/** @noinspection FieldCanBeLocal, CommentedOutCode */
public class GpsInterpolatorOwnLocation
{

    /** @noinspection unused*/
    final static String TAG = "GpsInterpolatorOL";

    private double lastLat, lastLon, lastBearing;
    private double lastAcc;
    private long lastUpdateTime = 0;
    private boolean isFirstFix = true;

    void push_geo_pos(double newLat, double newLon, double newBearing, double acc, MyLocationNewOverlay2 myLocationNewOverlay2)
    {
        try
        {
            // Log.i(TAG, "push_geo_pos: " + newLat + " " + newLon + " " + newBearing + " " + acc);
            Location interpolated_location = new Location(LocationManager.GPS_PROVIDER);
            interpolated_location.setAccuracy((float)acc);
            interpolated_location.setLatitude(newLat);
            interpolated_location.setLongitude(newLon);
            interpolated_location.setBearing((float) newBearing);
            myLocationNewOverlay2.onLocationChanged_real(interpolated_location, mIMyLocationProvider);
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

        long currentTime = System.currentTimeMillis();

        // Calculate time elapsed since last GPS fix
        long timeDelta = currentTime - lastUpdateTime;

        if ((!PREF__gps_smooth_own) || (isFirstFix) ||
            (timeDelta < GPS_UPDATE_FREQ_MS_MIN) ||
            (timeDelta > GPS_UPDATE_FREQ_MS_MAX) || (steps < 1) || (steps > 30)) {
            lastLat = location.getLatitude();
            lastLon = location.getLongitude();
            lastBearing = location.getBearing();
            lastAcc = location.getAccuracy();
            lastUpdateTime = currentTime;
            // Log.i(TAG, "timeDelta=" + timeDelta);
            isFirstFix = false;
            push_geo_pos(lastLat, lastLon, lastBearing, lastAcc, myLocationNewOverlay2);
            return;
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
            double interpolatedBearing = interpolateBearing(lastBearing, location.getBearing(), fraction);

            // Sleep to create smooth visual motion
            try {
                if ((sleepTimePerStep > 0) && (i > 1)) {
                    Thread.sleep(sleepTimePerStep);
                }
                //Log.i(TAG, "Step "+i+": Lat "+interpolatedLat+
                //           ", Lon "+interpolatedLon+", Bearing "+interpolatedBearing+" delta_t " + timeDelta);
                push_geo_pos(interpolatedLat, interpolatedLon, interpolatedBearing, location.getAccuracy(), myLocationNewOverlay2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                break;
            }
        }

        lastLat = location.getLatitude();
        lastLon = location.getLongitude();
        lastBearing = location.getBearing();
    }

    private double interpolateBearing(double start, double end, double fraction) {
        double diff = end - start;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        double result = start + diff * fraction;
        return (result + 360) % 360;
    }
}


