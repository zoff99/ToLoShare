package com.zoffcc.applications.trifa;

import android.animation.ValueAnimator;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import org.osmdroid.util.GeoPoint;

import static com.zoffcc.applications.trifa.CaptureService.GPS_UPDATE_FREQ_MS_MAX;
import static com.zoffcc.applications.trifa.CaptureService.GPS_UPDATE_FREQ_MS_MIN;
import static com.zoffcc.applications.trifa.CaptureService.remote_location_data;
import static com.zoffcc.applications.trifa.CaptureService.remote_location_overlays;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_friends;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;
import static com.zoffcc.applications.trifa.MainActivity.follow_friend_on_map;

/** @noinspection CommentedOutCode*/
public class GpsInterpolator
{

    /** @noinspection unused*/
    final static String TAG = "GpsInterpolator";

    private double lastLat;
    private double lastLon;
    private double lastBearing;
    private long lastUpdateTime = 0;
    private boolean isFirstFix = true;
    private Handler interpolationHandler = new Handler(Looper.getMainLooper());
    private Runnable interpolationRunnable;

    void push_geo_pos(double newLat, double newLon, double newBearing, float acc, boolean has_bearing, String f_pubkey)
    {
        try
        {
            // Log.i(TAG, "push_geo_pos: " + newLat + " " + newLon + " " + newBearing + " " + acc);
            CaptureService.remote_location_overlay_entry remote_ol = remote_location_overlays.get(f_pubkey);
            if (remote_ol != null)
            {
                remote_ol.remote_location_overlay.setLocation(new GeoPoint(newLat, newLon));
                remote_ol.remote_location_overlay.setAccuracy(Math.round(acc));
                remote_ol.remote_location_overlay.setBearing((float)newBearing);
            }
        }
        catch(Exception ignored)
        {
        }

        try
        {
            CaptureService.remote_location_entry re = remote_location_data.get(f_pubkey);
            if (re != null)
            {
                if (re.remoteBestLocation == null)
                {
                    re.remoteBestLocation = new Location(LocationManager.GPS_PROVIDER);
                }
                re.remoteBestLocation.setAccuracy(acc);
                re.remoteBestLocation.setLatitude(newLat);
                re.remoteBestLocation.setLongitude(newLon);
                if (has_bearing)
                {
                    re.remoteBestLocation.setBearing((float) newBearing);
                }
                else
                {
                    re.remoteBestLocation.removeBearing();
                }
            }
        }
        catch(Exception ignored)
        {
        }

        try
        {
            follow_friend_on_map(f_pubkey);
        }
        catch(Exception ignored)
        {
        }
    }

    public void onGpsUpdate(double newLat, double newLon, double newBearing_, boolean has_bearing,
                            boolean old_has_bearing, float acc, int steps, String f_pubkey) {
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastUpdateTime;

        // 1. Validation & Instant Update Logic
        if ((!PREF__gps_smooth_friends) || (isFirstFix) ||
            (timeDelta < GPS_UPDATE_FREQ_MS_MIN) || (timeDelta > GPS_UPDATE_FREQ_MS_MAX)) {

            stopAnimation();
            updateState(newLat, newLon, has_bearing ? newBearing_ : lastBearing, currentTime);
            push_geo_pos(lastLat, lastLon, lastBearing, acc, has_bearing, f_pubkey);
            isFirstFix = false;
            return;
        }

        // 2. Prepare Interpolation Constants
        final double startLat = lastLat;
        final double startLon = lastLon;
        final double startBearing = lastBearing;
        final double targetBearing = has_bearing ? newBearing_ : lastBearing;
        final long animationDuration = timeDelta;
        final long startTime = System.currentTimeMillis();
        final long frameDelay = 50; // 50ms = 20 FPS

        // 3. Stop any previous animation before starting a new one
        stopAnimation();

        // 4. Manual Animation Loop (20 FPS)
        interpolationRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                double fraction = (double) elapsed / animationDuration;

                if (fraction >= 1.0) {
                    // Ensure we hit the exact final coordinate
                    push_geo_pos(newLat, newLon, targetBearing, acc, has_bearing, f_pubkey);
                    return;
                }

                // Calculate current step
                double currLat = startLat + (newLat - startLat) * fraction;
                double currLon = startLon + (newLon - startLon) * fraction;
                double currBearing = (old_has_bearing == has_bearing && has_bearing)
                        ? interpolateBearing(startBearing, targetBearing, fraction)
                        : targetBearing;

                push_geo_pos(currLat, currLon, currBearing, acc, has_bearing, f_pubkey);

                // Schedule next frame at 20 FPS
                interpolationHandler.postDelayed(this, frameDelay);
            }
        };

        interpolationHandler.post(interpolationRunnable);

        // Update state so the next GPS update knows where to start from
        updateState(newLat, newLon, targetBearing, currentTime);
    }

    private void stopAnimation() {
        if (interpolationRunnable != null) {
            interpolationHandler.removeCallbacks(interpolationRunnable);
        }
    }

    private void updateState(double lat, double lon, double bearing, long time) {
        lastLat = lat;
        lastLon = lon;
        lastBearing = bearing;
        lastUpdateTime = time;
    }

    /**
     * Calculates the shortest path between two angles (0-360).
     * Prevents the "spinning" bug when crossing the 359 -> 0 degree threshold.
     */
    private double interpolateBearing(double start, double end, double fraction) {
        // Normalize angles just in case
        double startMod = (start % 360 + 360) % 360;
        double endMod = (end % 360 + 360) % 360;

        double diff = endMod - startMod;

        // If the difference is more than 180, the shortest path is across the 360/0 line
        if (diff > 180) {
            diff -= 360;
        } else if (diff < -180) {
            diff += 360;
        }

        double result = startMod + (diff * fraction);

        // Ensure the result stays within 0-360 range
        return (result + 360) % 360;
    }
}


