package com.zoffcc.applications.trifa;

import android.location.Location;
import android.location.LocationManager;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.zoffcc.applications.trifa.MainActivity.MAX_AGE_MS;
import static com.zoffcc.applications.trifa.MainActivity.MAX_TRAIL_POSITIONS;

public class ThreadSafeSelfTrails
{
    private final Deque<Location> selfLocations = new ConcurrentLinkedDeque<>();

    // --- NEW 1-LINER HELPER (from primitives) ---
    public void addLocation(double lat, double lon, long time, float acc, float bearing, boolean has_bearing, float speed)
    {
        Location loc = new Location(LocationManager.GPS_PROVIDER);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setTime(time > 0 ? time : System.currentTimeMillis());
        loc.setAccuracy(acc);
        if (has_bearing) {
            loc.setBearing(bearing);
        }
        loc.setSpeed(speed);

        updateLocation(loc);
    }

    // --- NEW 1-LINER HELPER (from existing Location object) ---
    public void addLocation(Location loc)
    {
        if (loc == null) return;

        // Create a deep copy to prevent the original Location from being modified
        Location copy = new Location(loc);
        updateLocation(copy);
    }

    public void updateLocation(Location newLoc)
    {
        if (newLoc == null) return;
        synchronized (selfLocations)
        {
            selfLocations.addFirst(newLoc);
            long cutoff = System.currentTimeMillis() - MAX_AGE_MS;

            try
            {
                while (!selfLocations.isEmpty() && selfLocations.peekLast().getTime() < cutoff)
                {
                    selfLocations.removeLast();
                }
            }
            catch(Exception e)
            {
            }

            while (selfLocations.size() > MAX_TRAIL_POSITIONS)
            {
                selfLocations.removeLast();
            }
        }
    }

    public List<Location> getRecentPositions()
    {
        synchronized (selfLocations)
        {
            return new ArrayList<>(selfLocations);
        }
    }
}
