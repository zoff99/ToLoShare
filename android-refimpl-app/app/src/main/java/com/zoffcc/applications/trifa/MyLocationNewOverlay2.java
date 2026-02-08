package com.zoffcc.applications.trifa;

import android.location.Location;
import android.util.Log;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import static com.zoffcc.applications.trifa.MainActivity.SMOOTH_POS_STEPS_OWN;
import static com.zoffcc.applications.trifa.MainActivity.gps_int_own;
import static com.zoffcc.applications.trifa.MainActivity.runTaskOwnLocation;

public class MyLocationNewOverlay2 extends MyLocationNewOverlayTweak
{
    /** @noinspection unused*/
    final static String TAG = "MyLocNewOverlay2";

    public MyLocationNewOverlay2(MapView mapView)
    {
        super(mapView);
        // Log.i(TAG, "L00:MyLocationNewOverlay2:create:" + myLocationProvider);
    }

    // HINT: function to inject location into the "GpsInterpolatorOwnLocation" (which will inject it into "onLocationChanged_real")
    public void onLocationChanged_interpolater(final Location location, IMyLocationProvider source) {
        // Log.i(TAG, "L00:onLocationChanged_real:1:XXXXXXXXXXXXXXXXX");
        // Log.i(TAG, "onLocationChanged:0:00000000000000000:" + source + " " + location);
        final MyLocationNewOverlay2 this_ = this;
        final Runnable process_own_gps_location = () -> {
            try
            {
                // Log.i(TAG, "onLocationChanged:0:11111111111");
                gps_int_own.onGpsUpdate(location, SMOOTH_POS_STEPS_OWN, this_);
                // Log.i(TAG, "onLocationChanged:0:22222222222");
            }
            catch (Exception ignored)
            {
            }
        };
        runTaskOwnLocation(process_own_gps_location);
    }

    // HINT: function to inject location into the "MyLocationNewOverlay" of osmdroid lib
    public void onLocationChanged_real(final Location location, IMyLocationProvider source) {
        // Log.i(TAG, "L00:onLocationChanged_real:1:XXXXXXXXXXXXXXXXX");
        super.onLocationChanged(location);
    }
}
