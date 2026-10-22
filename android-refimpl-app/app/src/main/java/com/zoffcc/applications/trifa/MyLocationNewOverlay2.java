package com.zoffcc.applications.trifa;

import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import static com.zoffcc.applications.trifa.MainActivity.SMOOTH_POS_STEPS_OWN;
import static com.zoffcc.applications.trifa.MainActivity.gps_int_own;
import static com.zoffcc.applications.trifa.MainActivity.runTaskOwnLocation;

public class MyLocationNewOverlay2 extends MyLocationNewOverlayTweak
{
    /** @noinspection unused*/
    final static String TAG = "MyLocNewOverlay2";

    private boolean force_location_change = false;

    public MyLocationNewOverlay2(MapView mapView)
    {
        super(mapView);
    }

    public void onLocationChanged_interpolater(final Location location, IMyLocationProvider source) {
        final MyLocationNewOverlay2 this_ = this;
        final Runnable process_own_gps_location = () -> {
            try
            {
                gps_int_own.onGpsUpdate(location, SMOOTH_POS_STEPS_OWN, this_, location.hasSpeed() ? location.getSpeed() : 0.0f);
            }
            catch(Exception ignored)
            {
            }
        };
        runTaskOwnLocation(process_own_gps_location);
    }

    public void forceLocationChange(Location location)
    {
        force_location_change = true;
        onLocationChanged_real(location, null);
    }

    // HINT: function to inject location into the "MyLocationNewOverlay" of osmdroid lib
    public void onLocationChanged_real(final Location location, IMyLocationProvider source) {
        // Log.i(TAG, "L00:onLocationChanged_real:1:XXXXXXXXXXXXXXXXX");
        super.onLocationChanged(location);
    }
}
