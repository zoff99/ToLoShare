package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.location.LocationListenerCompat;

import static com.zoffcc.applications.trifa.CaptureService.MAP_FOLLOW_MODE.MAP_FOLLOW_MODE_SELF;
import static com.zoffcc.applications.trifa.HelperGeneric.bytes_to_hex;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_dead_reconing_own;
import static com.zoffcc.applications.trifa.MainActivity.PREF__gps_smooth_own;
import static com.zoffcc.applications.trifa.MainActivity.PREF__map_follow_mode;
import static com.zoffcc.applications.trifa.MainActivity.location_info_text;
import static com.zoffcc.applications.trifa.MainActivity.mLocationOverlay;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.MainActivity.mapController;
import static com.zoffcc.applications.trifa.MainActivity.own_location_last_ts_millis;
import static com.zoffcc.applications.trifa.MainActivity.own_location_txt;
import static com.zoffcc.applications.trifa.MainActivity.set_debug_text;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_send_lossless_packet;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GEO_COORDS_CUSTOM_LOSSLESS_ID;

/** @noinspection CommentedOutCode, CallToPrintStackTrace , Convert2Lambda */
public class CaptureService extends Service
{
    final static String TAG = "CaptureService";

    static final String INVALID_BEARING = "FFF";
    final static String LOC_PROVIDER_NAME_FUSEDDR = "fused-dr";

    static boolean GPS_SERVICE_STARTED = false;
    private static final int _30_SECONDS = 1000 * 30;
    static final int GPS_UPDATE_FREQ_MS = 1000;
    static final int GPS_UPDATE_FREQ_MS_MIN = 800;
    static final int GPS_UPDATE_FREQ_MS_MAX = 1200;
    Notification notification_gps = null;
    NotificationManager nmn_gps = null;
    NotificationChannel notification_channel_gpsservice = null;
    String channelId_gps = "chl_svc1";
    LocationManager locationManager = null;
    LocationListenerCompat mLocationListener = null;
    public static LocationFusionManager fusion_m = null;
    static int ONGOING_GPS_NOTIFICATION_ID = 1491;
    final static String GEO_COORD_PROTO_MAGIC = "TzGeo"; // must be exactly 5 char wide
    final static String GEO_COORD_PROTO_VERSION = "00"; // must be exactly 2 char wide

    static Location currentBestLocation = null;
    static long last_position_timestamp_ms = 0;
    static long last_real_position_timestamp_ms = 0;
    final static long UPDATE_FROM_FUSED_AFTER_GPS_STALE_SECONDS = 2;
    final static long USE_FUSED_FOR_MAX_SECONDS = 15;

    static HashMap<String, remote_location_entry> remote_location_data = new HashMap<>();
    public static class remote_location_entry {
        Location remoteBestLocation = null;
        boolean has_bearing = false;
        GpsInterpolator gps_i = null;
        String remote_location_txt = "";
        // String remote_location_time_txt = "";
        // long last_remote_location_ts_millis = 0;
        long remote_location_last_ts_millis = 0;
        String friend_name = "remote";
    }

    static HashMap<String, remote_location_overlay_entry> remote_location_overlays = new HashMap<>();
    public static class remote_location_overlay_entry {
        DirectedLocationOverlay remote_location_overlay = null;
    }

    public enum MAP_FOLLOW_MODE
    {
        MAP_FOLLOW_MODE_NONE(-2),
        MAP_FOLLOW_MODE_SELF(-1),
        MAP_FOLLOW_MODE_FRIEND_0(0),
        MAP_FOLLOW_MODE_FRIEND_1(1),
        MAP_FOLLOW_MODE_FRIEND_2(2);

        public final int value;

        MAP_FOLLOW_MODE(int value)
        {
            this.value = value;
        }
    }

    @Override
    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        // serivce is created ---
        super.onCreate();

        GPS_SERVICE_STARTED = true;
        start_me();
    }

    void start_me()
    {
        Log.i(TAG, "start_me");

        nmn_gps = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "Chl Svc1";
            notification_channel_gpsservice = new NotificationChannel(channelId_gps, name, NotificationManager.IMPORTANCE_DEFAULT);
            notification_channel_gpsservice.enableVibration(false);
            notification_channel_gpsservice.setSound(null, null);
            nmn_gps.createNotificationChannel(notification_channel_gpsservice);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.
                Builder(this, channelId_gps).
                setContentTitle("Active").
                setContentText("ToLoShare").
                setSmallIcon(R.mipmap.ic_launcher).
                setPriority(NotificationCompat.PRIORITY_DEFAULT).
                setSound(null).
                setCategory(NotificationCompat.CATEGORY_LOCATION_SHARING).
                setAutoCancel(false);

        notification_gps = notificationBuilder.build();
        nmn_gps.notify(ONGOING_GPS_NOTIFICATION_ID, notification_gps);

        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
        }
        ServiceCompat.startForeground(this,
                                      ONGOING_GPS_NOTIFICATION_ID,
                                      notification_gps,
                                      type);

        startLocationTracking();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(TAG, "onStartCommand");
        // this gets called all the time!
        return START_NOT_STICKY; // START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        // This is triggered when the app is swiped away from Recents
        // Reset the session lock state
        AppSessionManager.getInstance().lockApp();
    }

    @SuppressLint("MissingPermission")
    public void startLocationTracking()
    {
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mLocationListener = new LocationListenerCompat() {

            private final String TAG1 = "_location_";

            @Override
            public void onLocationChanged(@NonNull Location location)
            {
                // Log.i(TAG1, "onLocationChanged: " + location);

                try
                {
                    if (!isBetterLocation(location))
                    {
                        // HINT: ignore this location update
                        Log.i(TAG1, "onLocationChanged: " + "ignore this location update");
                        return;
                    }
                    currentBestLocation = new Location(location);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    last_real_position_timestamp_ms = System.currentTimeMillis();
                    update_gps_position(currentBestLocation, true);
                }
                catch (Exception e)
                {
                }
            }

            @Override
            public void onStatusChanged(@NonNull String provider, int status, Bundle extras)
            {
                Log.i(TAG1, "onStatusChanged: " + provider + " " +  status);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider)
            {
                Log.i(TAG1, "onProviderEnabled: " + provider);
                try
                {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastKnownLocation != null)
                    {
                        Log.i(TAG1, "onProviderEnabled: lastKnownLocation = " + lastKnownLocation);
                        if (PREF__map_follow_mode == MAP_FOLLOW_MODE_SELF.value)
                        {
                            set_map_center_to(lastKnownLocation);
                        }
                    }
                }
                catch(Exception ignored)
                {
                }
            }

            @Override
            public void onProviderDisabled(@NonNull String provider)
            {
                Log.i(TAG1, "onProviderDisabled: " + provider);
            }
        };

        try
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_FREQ_MS, 0, mLocationListener);
        }
        catch(Exception ignored)
        {
        }

        try
        {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_UPDATE_FREQ_MS, 0, mLocationListener);
        }
        catch(Exception ignored)
        {
        }

        if (PREF__gps_dead_reconing_own)
        {
            try
            {
                fusion_m = new LocationFusionManager(this);
            }
            catch(Exception e)
            {
            }

            try
            {
                if (PREF__gps_smooth_own)
                {
                    fusion_m.startSensorFusion();
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    static void broadcastFusedLocation(double currentLat, double currentLng, float currentMovementBearing)
    {
        try
        {
            if ((PREF__gps_dead_reconing_own) && (PREF__gps_smooth_own) && (currentBestLocation != null))
            {
                long now = System.currentTimeMillis();
                long delta = (now - last_position_timestamp_ms) / 1000;
                long delta_real = (now - last_real_position_timestamp_ms) / 1000;
                if (delta_real < USE_FUSED_FOR_MAX_SECONDS)
                {
                    currentBestLocation.setLatitude(currentLat);
                    currentBestLocation.setLongitude(currentLng);
                    currentBestLocation.setBearing(currentMovementBearing);
                    currentBestLocation.setProvider(LOC_PROVIDER_NAME_FUSEDDR);
                    mLocationOverlay.onLocationChanged_injection(currentBestLocation, null);
                    if (last_position_timestamp_ms != 0)
                    {
                        if (delta >= UPDATE_FROM_FUSED_AFTER_GPS_STALE_SECONDS)
                        {
                            update_gps_position(currentBestLocation, true);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void update_gps_position(@NonNull Location location, boolean update_map)
    {
        last_position_timestamp_ms = System.currentTimeMillis();

        try
        {
            if (PREF__map_follow_mode == MAP_FOLLOW_MODE_SELF.value)
            {
                if (update_map)
                {
                    set_map_center_to(location);
                }
            }
        }
        catch(Exception e)
        {
        }

        try
        {
            final byte[] data_bin = getGeoMsg(location);
            int data_bin_len = data_bin.length;
            data_bin[0] = (byte) GEO_COORDS_CUSTOM_LOSSLESS_ID;

            long[] friends = MainActivity.tox_self_get_friend_list();
            for (int fc = 0; fc < friends.length; fc++)
            {
                //noinspection unused
                final int res = tox_friend_send_lossless_packet(fc, data_bin, data_bin_len);
                // Log.i(TAG1, "fn=" + fc + " res=" + res + " " + bytes_to_hex(data_bin) + " len=" + data_bin_len);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            own_location_last_ts_millis = System.currentTimeMillis();
            own_location_txt = "provider: " + location.getProvider() + "\n" + "accur: " +
                               (Math.round(location.getAccuracy() * 10f) / 10) + " m\n";
            set_debug_text(location_info_text(own_location_last_ts_millis, own_location_txt));
        }
        catch(Exception e)
        {
        }
    }

    static void set_map_center_to_proxy_uithread(final Location location)
    {
        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    set_map_center_to(location);
                }
                catch (Exception e)
                {
                    Log.i(TAG, "set_map_center_to_proxy_uithread:EE:" + e.getMessage());
                }
            }
        };

        if (main_handler_s != null)
        {
            main_handler_s.post(myRunnable);
        }

    }

    static void set_map_center_to(Location location)
    {
        try
        {
            // HINT: follow own location on the map
            GeoPoint new_center = new GeoPoint(location.getLatitude(),
                                               location.getLongitude());
            mapController.animateTo(new_center);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /** @noinspection UnnecessaryLocalVariable*/
    static byte[] getGeoMsg(Location location)
    {
        String bearing = "" + location.getBearing();
        if (!location.hasBearing())
        {
            bearing = INVALID_BEARING;
        }

        String temp_string = "X" + // the pkt ID will be added here later. needs to be exactly 1 char!
                             GEO_COORD_PROTO_MAGIC +
                             GEO_COORD_PROTO_VERSION  + ":BEGINGEO:" +
                             location.getLatitude() + ":" +
                             location.getLongitude() + ":" +
                             location.getAltitude() + ":" +
                             location.getAccuracy() + ":" +
                             bearing + ":ENDGEO";
        // Log.i(TAG, "raw:" + temp_string);
        // Log.i(TAG, "rawlen:" + temp_string.length());

        byte[] data_bin = temp_string.getBytes(); // TODO: use specific characterset
        return data_bin;
    }

    /** @noinspection unused*/
    public void stopLocationTracking()
    {
        locationManager.removeUpdates(mLocationListener);
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     */
    protected boolean isBetterLocation(Location location)
    {
        if (currentBestLocation == null)
        {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > (_30_SECONDS);
        boolean isSignificantlyOlder = timeDelta < -(_30_SECONDS);
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer)
        {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if (isSignificantlyOlder)
        {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                                                    currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate)
        {
            return true;
        }
        else if (isNewer && !isLessAccurate)
        {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
        {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2)
    {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

}
