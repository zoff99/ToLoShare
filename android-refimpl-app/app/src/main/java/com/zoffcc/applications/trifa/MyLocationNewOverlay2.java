package com.zoffcc.applications.trifa;

import android.location.Location;
import android.util.Log;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import static com.zoffcc.applications.trifa.MainActivity.INTERPOLATE_POS_STEPS;
import static com.zoffcc.applications.trifa.MainActivity.gps_int_own;
import static com.zoffcc.applications.trifa.MainActivity.runTaskOwnLocation;

public class MyLocationNewOverlay2 extends MyLocationNewOverlay
{
    final static String TAG = "MyLocNewOverlay2";

    private boolean mIsLocationEnabled = false;

    public MyLocationNewOverlay2(IMyLocationProvider myLocationProvider, MapView mapView)
    {
        super(myLocationProvider, mapView);
    }

    @Override
    public void onLocationChanged(final Location location, IMyLocationProvider source) {
        Log.i(TAG, "onLocationChanged:0:00000000000000000:" + source + " " + location);
        final MyLocationNewOverlay2 this_ = this;
        final Runnable process_own_gps_location = () -> {
            try
            {
                gps_int_own.onGpsUpdate(location, INTERPOLATE_POS_STEPS, this_);
            }
            catch (Exception e)
            {
            }
        };
        runTaskOwnLocation(process_own_gps_location);
    }

    public void onLocationChanged_real(final Location location, IMyLocationProvider source) {
        Log.i(TAG, "onLocationChanged:1:XXXXXXXXXXXXXXXXX");
        super.onLocationChanged(location, source);
    }
}
