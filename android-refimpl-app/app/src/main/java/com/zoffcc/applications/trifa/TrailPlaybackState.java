package com.zoffcc.applications.trifa;

import android.graphics.Color;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class TrailPlaybackState
{
    public static final long PLAYBACK_OFF = 0L;
    public static volatile long playback_timestamp_ms = PLAYBACK_OFF;

    public static final long STALE_START_MS = 5L * 60L * 1000L;
    public static final long STALE_GHOST_MS = 15L * 60L * 1000L;
    public static final long STALE_DEAD_MS = 60L * 60L * 1000L;

    public static final int CONTEXT_STATIONARY = 0;
    public static final int CONTEXT_WALKING = 1;
    public static final int CONTEXT_DRIVING = 2;

    public static final float STATIONARY_SPEED_KMH = 1.0f;
    public static final float WALK_MAX_KMH = 6.0f;
    public static final float DRIVE_MIN_KMH = 20.0f;

    public static boolean isPlaybackActive()
    {
        return playback_timestamp_ms != PLAYBACK_OFF;
    }

    public static void setPlaybackTimestamp(long timestamp_ms)
    {
        playback_timestamp_ms = timestamp_ms;
    }

    public static void setLive()
    {
        playback_timestamp_ms = PLAYBACK_OFF;
    }

    public static float alphaForLocationTime(long now_ms, long location_timestamp_ms)
    {
        if (location_timestamp_ms <= 0L) return 1.0f;

        long reference_ms = isPlaybackActive() ? playback_timestamp_ms : now_ms;
        long age_ms = reference_ms - location_timestamp_ms;
        if (age_ms < 0L) age_ms = 0L;

        if (age_ms <= STALE_START_MS) return 1.0f;
        if (age_ms <= STALE_GHOST_MS)
        {
            float t = (float)(age_ms - STALE_START_MS) / (float)(STALE_GHOST_MS - STALE_START_MS);
            return 1.0f - (0.45f * t);
        }
        if (age_ms <= STALE_DEAD_MS)
        {
            float t = (float)(age_ms - STALE_GHOST_MS) / (float)(STALE_DEAD_MS - STALE_GHOST_MS);
            return 0.55f - (0.30f * t);
        }
        return 0.25f;
    }

    public static int contextForMovement(float speed_kmh, long stationary_ms)
    {
        if (speed_kmh < 0.0f) speed_kmh = 0.0f;
        if (speed_kmh <= STATIONARY_SPEED_KMH) return CONTEXT_STATIONARY;
        if (speed_kmh >= DRIVE_MIN_KMH) return CONTEXT_DRIVING;
        return CONTEXT_WALKING;
    }

    public static int haloColorForContext(int context)
    {
        switch (context)
        {
            case CONTEXT_STATIONARY: return Color.argb(60, 148, 163, 184);
            case CONTEXT_WALKING: return Color.argb(70, 34, 197, 94);
            case CONTEXT_DRIVING: default: return Color.argb(70, 249, 115, 22);
        }
    }

    public static double haloRadiusMetersForContext(int context)
    {
        switch (context)
        {
            case CONTEXT_STATIONARY: return 28.0;
            case CONTEXT_WALKING: return 16.0;
            case CONTEXT_DRIVING: default: return 42.0;
        }
    }

    public static List<GeoPoint> generateCirclePoints(GeoPoint center, double radiusInMeters, int numPoints)
    {
        List<GeoPoint> points = new ArrayList<>();
        double earthRadius = 6371000;
        for (int i = 0; i <= numPoints; i++)
        {
            double angle = Math.toRadians(i * 360.0 / numPoints);
            double dLat = (radiusInMeters / earthRadius) * (180.0 / Math.PI);
            double dLon = (radiusInMeters / earthRadius) * (180.0 / Math.PI) / Math.cos(Math.toRadians(center.getLatitude()));
            points.add(new GeoPoint(center.getLatitude() + dLat * Math.sin(angle),
                                    center.getLongitude() + dLon * Math.cos(angle)));
        }
        return points;
    }
}
