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

import java.util.Date;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.location.LocationListenerCompat;

import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.bytes_to_hex;
import static com.zoffcc.applications.trifa.MainActivity.debug_text;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.MainActivity.remote_location_overlay;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_send_lossless_packet;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_PROXY_PUBKEY_FOR_FRIEND;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_PUSH_URL_FOR_FRIEND;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GEO_COORDS_CUSTOM_LOSSLESS_ID;

public class CaptureService extends Service
{
    final static String TAG = "CaptureService";

    static boolean GPS_SERVICE_STARTED = false;

    Notification notification_gps = null;
    NotificationManager nmn_gps = null;
    NotificationChannel notification_channel_gpsservice = null;
    String channelId_gps = "toloshare_gps_location_service";
    LocationManager locationManager = null;
    LocationListenerCompat mLocationListener = null;
    static int ONGOING_GPS_NOTIFICATION_ID = 1491;
    final static String GEO_COORD_PROTO_MAGIC = "TzGeo"; // must be exactly 5 char wide
    final static String GEO_COORD_PROTO_VERSION = "00"; // must be exactly 2 char wide

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
            CharSequence name = "gps_service";
            notification_channel_gpsservice = new NotificationChannel(channelId_gps, name, NotificationManager.IMPORTANCE_DEFAULT);
            notification_channel_gpsservice.enableVibration(false);
            notification_channel_gpsservice.setSound(null, null);
            nmn_gps.createNotificationChannel(notification_channel_gpsservice);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.
                Builder(this, channelId_gps).
                setContentTitle("GPS").
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

    @SuppressLint("MissingPermission")
    public void startLocationTracking()
    {
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mLocationListener = new LocationListenerCompat() {
            @Override
            public void onLocationChanged(Location location)
            {
                Log.i(TAG, "onLocationChanged: " + location);
                try
                {
                    final byte[] data_bin = getGeoMsg(location);
                    int data_bin_len = data_bin.length;
                    data_bin[0] = (byte) CONTROL_PROXY_MESSAGE_TYPE_PROXY_PUBKEY_FOR_FRIEND.value; // GEO_COORDS_CUSTOM_LOSSLESS_ID;
                    final int res = tox_friend_send_lossless_packet(0, data_bin, data_bin_len);
                    Log.i(TAG, "res=" + res + " " + bytes_to_hex(data_bin) + " len=" + data_bin_len);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

                Runnable myRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            debug_text.setText("provider: " + location.getProvider() + "\n" +
                                               "lat: " + location.getLatitude() + "\n" +
                                               "lon: " + location.getLongitude() + "\n" +
                                               "accur: " + location.getAccuracy() + "\n" +
                                               "time: " + MainActivity.df_date_time_long.format(new Date(System.currentTimeMillis())));
                        }
                        catch (Exception e)
                        {
                            Log.i(TAG, "EE.b:" + e.getMessage());
                        }
                    }
                };

                if (main_handler_s != null)
                {
                    main_handler_s.post(myRunnable);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                Log.i(TAG, "onStatusChanged: " + provider + " " +  status);
            }

            @Override
            public void onProviderEnabled(String provider)
            {
                Log.i(TAG, "onProviderEnabled: " + provider);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null)
                {
                    Log.i(TAG, "onProviderEnabled: lastKnownLocation = " + lastKnownLocation);
                }
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                Log.i(TAG, "onProviderDisabled: " + provider);
            }
        };

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 500, 0, mLocationListener);
    }

    static byte[] getGeoMsg(Location location)
    {
        String temp_string = "X" + // the pkt ID will be added here later. needs to be exactly 1 char!
                             GEO_COORD_PROTO_MAGIC +
                             GEO_COORD_PROTO_VERSION  + ":BEGINGEO:" +
                             location.getLatitude() + ":" +
                             location.getLongitude() + ":" +
                             location.getAltitude() + ":" +
                             location.getAccuracy() + ":" +
                             location.getBearing() + ":ENDGEO";
        // Log.i(TAG, "raw:" + temp_string);
        // Log.i(TAG, "rawlen:" + temp_string.length());

        byte[] data_bin = temp_string.getBytes(); // TODO: use specific characterset
        return data_bin;
    }

    public void stopLocationTracking()
    {
        locationManager.removeUpdates(mLocationListener);
    }
}
