package com.zoffcc.applications.trifa;

import android.util.Log;

import static com.zoffcc.applications.trifa.MainActivity.map;

public class MapPaintWrapper
{
    final static String TAG = "MapPaintWrapper";

    public static void invalidate()
    {
        // Log.i(TAG, "MAPVIEW:invalidate");
        map.invalidate();
    }

    public static void postInvalidate() {
        // Log.i(TAG, "MAPVIEW:postInvalidate");
        map.postInvalidate();
    }
}
