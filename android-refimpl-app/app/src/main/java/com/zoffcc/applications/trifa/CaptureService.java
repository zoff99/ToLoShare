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
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.location.LocationListenerCompat;

import static android.location.LocationManager.FUSED_PROVIDER;
import static com.zoffcc.applications.trifa.CaptureService.MAP_FOLLOW_MODE.MAP_FOLLOW_MODE_SELF;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_get_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.bytes_to_hex;
import static com.zoffcc.applications.trifa.MainActivity.PREF__map_follow_mode;
import static com.zoffcc.applications.trifa.MainActivity.f_tracker;
import static com.zoffcc.applications.trifa.MainActivity.inject_own_location;
import static com.zoffcc.applications.trifa.MainActivity.location_info_text;
import static com.zoffcc.applications.trifa.MainActivity.mLocationOverlay;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.MainActivity.map;
import static com.zoffcc.applications.trifa.MainActivity.mapController;
import static com.zoffcc.applications.trifa.MainActivity.map_is_northed;
import static com.zoffcc.applications.trifa.MainActivity.own_location_last_ts_millis;
import static com.zoffcc.applications.trifa.MainActivity.own_location_txt;
import static com.zoffcc.applications.trifa.MainActivity.set_debug_text;
import static com.zoffcc.applications.trifa.MainActivity.set_found_loc_providers_text;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_send_lossless_packet;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GEO_COORDS_CUSTOM_LOSSLESS_ID;

/** @noinspection CommentedOutCode, CallToPrintStackTrace , Convert2Lambda */
public class CaptureService extends Service
{
    final static String TAG = "CaptureService";

    static final String INVALID_BEARING = "FFF";
    final static String LOC_PROVIDER_NAME_FUSEDDR = "fused-dr";

    static String found_location_providers = "";

    static boolean GPS_SERVICE_STARTED = false;
    private static final int LOCATION_TOO_OLD_MS = 1000 * 30;
    private static final int LOCATION_ACCURACY_DELTA_METERS = 100;
    static final int GPS_UPDATE_FREQ_MS = 1000;
    static final int JITTER_LOC_DELTA_MS = 300;
    static final int GPS_UPDATE_FREQ_MS_MIN = 1000 - JITTER_LOC_DELTA_MS;
    static final int GPS_UPDATE_FREQ_MS_MAX = 1000 + JITTER_LOC_DELTA_MS;
    Notification notification_gps = null;
    NotificationManager nmn_gps = null;
    NotificationChannel notification_channel_gpsservice = null;
    String channelId_gps = "chl_svc1";
    LocationManager locationManager = null;
    LocationListenerCompat mLocationListener = null;
    static int ONGOING_GPS_NOTIFICATION_ID = 1491;
    final static String GEO_COORD_PROTO_MAGIC = "TzGeo"; // must be exactly 5 char wide
    final static String GEO_COORD_PROTO_VERSION = "00"; // must be exactly 2 char wide
    final static String GEO_COORD_PROTO_VERSION_1 = "01"; // must be exactly 2 char wide

    static Location currentBestLocation = null;

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

    void update_location_function(@NonNull Location location)
    {
        try
        {
            if (!isBetterLocation(location, currentBestLocation))
            {
                // HINT: ignore this location update
                // Log.i(TAG, "update_location_function: " + "ignore this location update");
                return;
            }
            currentBestLocation = new Location(location);
            // Log.i(TAG, "update_location_function: currentBestLocation update -> " + location);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        inject_own_location();

        try
        {
            update_gps_position(currentBestLocation, false);
        }
        catch (Exception e)
        {
        }
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
                update_location_function(location);
            }

            @Override
            public void onStatusChanged(@NonNull String provider, int status, Bundle extras)
            {
                Log.i(TAG1, "onStatusChanged: " + provider + " " +  status);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider)
            {
                if (provider.equals(FUSED_PROVIDER))
                {
                    // Log.i(TAG, "isBetterLocation:ignoring FUSED provider!!");
                    return;
                }

                Log.i(TAG1, "onProviderEnabled: " + provider + " currentBestLocation=" + currentBestLocation);

                try
                {
                    mLocationOverlay.enableMyLocation();
                    boolean is_loc_enabled = mLocationOverlay.isMyLocationEnabled();
                    Log.i(TAG, "OOOOOOO:new:123:is_loc_enabled=" + is_loc_enabled);
                }
                catch(Exception e)
                {
                }

                try
                {
                    if (currentBestLocation == null)
                    {
                        try
                        {
                            Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                            if (lastKnownLocation != null)
                            {
                                Log.i(TAG1, "onProviderEnabled: provider = " + provider + " lastKnownLocation = " + lastKnownLocation);
                                update_location_function(lastKnownLocation);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
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
                try
                {
                }
                catch(Exception ignored)
                {
                }
            }
        };

        found_location_providers = "";
        try {
            List<String> providers = locationManager.getProviders(false);
            for (String provider : providers) {
                if (found_location_providers.isEmpty())
                {
                    found_location_providers = provider;
                }
                else
                {
                    found_location_providers += ", " + provider;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            set_found_loc_providers_text(found_location_providers);
        } catch (Exception ignored) {
        }

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
    }

    private static void update_gps_position(@NonNull Location location, boolean update_map)
    {
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

        send_location_update_to_all_friends(location);

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

    static void send_location_update_to_all_friends(@NonNull Location location)
    {
        try
        {
            final byte[] data_bin = getGeoMsg_proto_v1(location, own_location_last_ts_millis);
            int data_bin_len = data_bin.length;
            data_bin[0] = (byte) GEO_COORDS_CUSTOM_LOSSLESS_ID;

            long[] friends = MainActivity.tox_self_get_friend_list();
            for (int fc = 0; fc < friends.length; fc++)
            {
                //noinspection unused
                final int res = tox_friend_send_lossless_packet(fc, data_bin, data_bin_len);
                // Log.i(TAG, "fn=" + fc + " res=" + res + " " + bytes_to_hex(data_bin) + " len=" + data_bin_len);

                if (res == 1)
                {
                    try
                    {
                        String f_pubkey = tox_friend_get_public_key__wrapper(fc);
                        if ((f_pubkey != null) && (f_pubkey.length() > 10))
                        {
                            f_tracker.ping_outgoing(f_pubkey);
                        }
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    static void send_location_update_to_friend(@NonNull Location location, @NonNull String fpubkey)
    {
        try
        {
            final byte[] data_bin = getGeoMsg_proto_v1(location, own_location_last_ts_millis);
            int data_bin_len = data_bin.length;
            data_bin[0] = (byte) GEO_COORDS_CUSTOM_LOSSLESS_ID;

            long[] friends = MainActivity.tox_self_get_friend_list();
            for (int fc = 0; fc < friends.length; fc++)
            {
                //noinspection unused
                final int res = tox_friend_send_lossless_packet(fc, data_bin, data_bin_len);
                // Log.i(TAG, "fn=" + fc + " res=" + res + " " + bytes_to_hex(data_bin) + " len=" + data_bin_len);

                if (res == 1)
                {
                    try
                    {
                        String f_pubkey = tox_friend_get_public_key__wrapper(fc);
                        if ((f_pubkey != null) && (f_pubkey.length() > 10))
                        {
                            if (f_pubkey.equals(fpubkey))
                            {
                                f_tracker.ping_outgoing(f_pubkey);
                            }
                        }
                    }
                    catch(Exception e)
                    {
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
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

    static void set_map_center_to_animate(Location location)
    {
        try
        {
            // HINT: follow own location on the map
            GeoPoint new_center = new GeoPoint(location.getLatitude(),
                                               location.getLongitude());
            mapController.animateTo(new_center);
            // mapController.setCenter(new_center);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    static void set_map_center_to(Location location)
    {
        try
        {
            // HINT: follow own location on the map
            GeoPoint new_center = new GeoPoint(location.getLatitude(),
                                               location.getLongitude());
            // mapController.animateTo(new_center);
            mapController.setCenter(new_center);

            if (!map_is_northed)
            {
                if (location.hasBearing())
                {
                    map.setMapOrientation(-location.getBearing());
                }
            }
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

        byte[] data_bin = temp_string.getBytes();
        return data_bin;
    }

    /** @noinspection UnnecessaryLocalVariable*/
    static byte[] getGeoMsg_proto_v1(Location location, long timestamp)
    {
        String bearing = "" + location.getBearing();
        if (!location.hasBearing())
        {
            bearing = INVALID_BEARING;
        }

        String provider = "unknown";
        try
        {
            if (location.getProvider() != null)
            {
                provider = location.getProvider();
            }
        }
        catch(Exception e)
        {
        }

        String temp_string = "X" + // the pkt ID will be added here later. needs to be exactly 1 char!
                             GEO_COORD_PROTO_MAGIC +
                             GEO_COORD_PROTO_VERSION_1  + ":BEGINGEO:" +
                             location.getLatitude() + ":" +
                             location.getLongitude() + ":" +
                             location.getAltitude() + ":" +
                             location.getAccuracy() + ":" +
                             timestamp + ":" +
                             provider + ":" +
                             bearing + ":ENDGEO";
        // Log.i(TAG, "raw:" + temp_string);
        // Log.i(TAG, "rawlen:" + temp_string.length());

        byte[] data_bin = temp_string.getBytes();
        return data_bin;
    }

    /** @noinspection unused*/
    public void stopLocationTracking()
    {
        locationManager.removeUpdates(mLocationListener);
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param new_location  The new Location that you want to evaluate
     * @param current_best_location  The current Location to compare against
     */
    protected boolean isBetterLocation(Location new_location, Location current_best_location) {
        if (new_location == null) {
            // A "null" location is always bad
            return false;
        }

        String new_provider_nonnull = "unknown";
        String old_provider_nonnull = "unknown";

        try {
            if (new_location.getProvider() != null) {
                new_provider_nonnull = new_location.getProvider();
            }
            if (current_best_location != null && current_best_location.getProvider() != null) {
                old_provider_nonnull = current_best_location.getProvider();
            }
        } catch (Exception ignored) {
            // Ignore any exceptions
        }

        // Always prefer GPS provider if new location is from GPS
        if ("gps".equalsIgnoreCase(new_provider_nonnull)) {
            return true;
        }

        // If current best location is from GPS, do not switch unless new location is significantly better
        if ("gps".equalsIgnoreCase(old_provider_nonnull)) {
            // Only switch if new location is significantly better
            if (isMoreAccurate(new_location, current_best_location)) {
                return true;
            }
            // Avoid switching away from GPS unless new location is much better
            return false;
        }

        // If new location is newer by a certain threshold, consider switching
        long timeDelta = new_location.getTime() -
                         (current_best_location != null ? current_best_location.getTime() : 0);
        boolean isSignificantlyNewer = timeDelta > (LOCATION_TOO_OLD_MS);
        boolean isSignificantlyOlder = timeDelta < -(LOCATION_TOO_OLD_MS);

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        if (current_best_location == null) {
            return true;
        }

        boolean isMoreAccurate = isMoreAccurate(new_location, current_best_location);
        boolean isSameAccurate = isSameAccurate(new_location, current_best_location);

        // Prefer more accurate location
        if (isMoreAccurate) {
            return true;
        } else if (isSameAccurate && isSameProvider(new_provider_nonnull, old_provider_nonnull)) {
            // If accuracy is same and provider is same, accept the new location
            return true;
        }

        return false;
    }

    // Helper method to compare accuracy
    private boolean isMoreAccurate(Location location1, Location location2) {
        try
        {
            if (location2 == null)
            {
                return true;
            }
            return location1.getAccuracy() < location2.getAccuracy();
        }
        catch(Exception ignored)
        {
        }
        return true;
    }

    private boolean isSameAccurate(Location location1, Location location2) {
        try
        {
            if (location2 == null)
            {
                return false;
            }
            return location1.getAccuracy() == location2.getAccuracy();
        }
        catch(Exception ignored)
        {
        }
        return false;
    }



    // Helper method to compare provider names
    private boolean isSameProvider(String provider1, String provider2) {
        try
        {
            if (provider1 == null)
            {
                return false;
            }
            return provider1.equalsIgnoreCase(provider2);
        }
        catch(Exception ignored)
        {
        }
        return false;
    }

}
