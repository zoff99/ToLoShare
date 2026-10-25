package com.zoffcc.applications.trifa;

public class TrailPlaybackState
{
    public static final long PLAYBACK_OFF = 0L;
    public static volatile long playback_timestamp_ms = PLAYBACK_OFF;

    // Production VALUES
    public static final long STALE_START_MS = 5L * 60L * 1000L;
    public static final long STALE_GHOST_MS = 10L * 60L * 1000L;
    public static final long STALE_DEAD_MS = 20L * 60L * 1000L;

    // testing VALUES
    // public static final long STALE_START_MS = 5L * 1000L;       // 5 seconds
    // public static final long STALE_GHOST_MS = 15L * 1000L;      // 15 seconds
    // public static final long STALE_DEAD_MS = 30L * 1000L;       // 30 seconds

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
}
