package com.zoffcc.applications.trifa;

import android.location.Location;
import android.location.LocationManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.zoffcc.applications.trifa.MainActivity.MAX_AGE_MS;
import static com.zoffcc.applications.trifa.MainActivity.MAX_TRAIL_POSITIONS;

public class ThreadSafeFriendTrails
{
    private final Map<String, Deque<Location>> friendLocations = new ConcurrentHashMap<>();

    // --- NEW 1-LINER HELPER ---
    public void addLocation(String friend_pubkey, String provider, double lat, double lon, long time,
                            float acc, float bearing, boolean has_bearing, float speed)
    {
        if (friend_pubkey == null) return;

        Location loc = new Location(provider != null ? provider : LocationManager.GPS_PROVIDER);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setTime(time > 0 ? time : System.currentTimeMillis());
        loc.setAccuracy(acc);
        if (has_bearing) {
            loc.setBearing(bearing);
        }
        loc.setSpeed(speed);

        // Create a deep copy to prevent any external modifications
        Location copy = new Location(loc);
        updateLocation(friend_pubkey, copy);
    }

    public void updateLocation(String friend_pubkey, Location newLoc)
    {
        if (newLoc == null) return;
        Deque<Location> history = friendLocations.computeIfAbsent(friend_pubkey, k -> new ArrayDeque<>());
        synchronized (history) {
            history.addFirst(newLoc);
            long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
            history.removeIf(loc -> loc.getTime() < cutoff);
            while (history.size() > MAX_TRAIL_POSITIONS) {
                history.removeLast();
            }
        }
    }

    public List<Location> getRecentPositions(String friend_pubkey)
    {
        Deque<Location> history = friendLocations.get(friend_pubkey);
        if (history == null) return Collections.emptyList();

        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;

        synchronized (history) {
            return history.stream()
                    .filter(loc -> loc.getTime() >= cutoff)
                    .limit(MAX_TRAIL_POSITIONS)
                    .collect(Collectors.toList());
        }
    }
}
