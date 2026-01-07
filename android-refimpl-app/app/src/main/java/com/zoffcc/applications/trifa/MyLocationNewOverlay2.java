package com.zoffcc.applications.trifa;

import android.location.Location;
import android.util.Log;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import static com.zoffcc.applications.trifa.CaptureService.fusion_m;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_dead_reconing_own;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;
import static com.zoffcc.applications.trifa.MainActivity.SMOOTH_POS_STEPS_OWN;
import static com.zoffcc.applications.trifa.MainActivity.gps_int_own;
import static com.zoffcc.applications.trifa.MainActivity.runTaskOwnLocation;

public class MyLocationNewOverlay2 extends MyLocationNewOverlay
{
    /** @noinspection unused*/
    final static String TAG = "MyLocNewOverlay2";

    public MyLocationNewOverlay2(IMyLocationProvider myLocationProvider, MapView mapView)
    {
        super(myLocationProvider, mapView);
    }

    @Override
    public void onLocationChanged(final Location location, IMyLocationProvider source) {
        if ((PREF__gps_dead_reconing_own) && (PREF__gps_smooth_own))
        {
            try
            {
                fusion_m.onGpsLocationChanged(location);
            }
            catch (Exception e)
            {
            }
        }
        else
        {
            // Log.i(TAG, "onLocationChanged:0:00000000000000000:" + source + " " + location);
            final MyLocationNewOverlay2 this_ = this;
            final Runnable process_own_gps_location = () -> {
                try
                {
                    gps_int_own.onGpsUpdate(location, SMOOTH_POS_STEPS_OWN, this_);
                }
                catch (Exception ignored)
                {
                }
            };
            runTaskOwnLocation(process_own_gps_location);
        }
    }

    public void onLocationChanged_injection(final Location location, IMyLocationProvider source) {
        if (PREF__gps_dead_reconing_own)
        {
            // Log.i(TAG, "onLocationChanged:0:00000000000000000:" + source + " " + location);
            super.onLocationChanged(location, source);
        }
    }

    public void onLocationChanged_real(final Location location, IMyLocationProvider source) {
        // Log.i(TAG, "onLocationChanged:1:XXXXXXXXXXXXXXXXX");
        super.onLocationChanged(location, source);
    }
}
