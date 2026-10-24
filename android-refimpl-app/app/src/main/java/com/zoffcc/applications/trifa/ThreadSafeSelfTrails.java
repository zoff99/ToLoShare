package com.zoffcc.applications.trifa;

import android.location.Location;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.zoffcc.applications.trifa.MainActivity.MAX_AGE_MS;

public class ThreadSafeSelfTrails
{
    private final Deque<Location> selfLocations = new ConcurrentLinkedDeque<>();
    private static final int MAX_POSITIONS = 1000;

    public void updateLocation(Location newLoc)
    {
        synchronized (selfLocations)
        {
            selfLocations.addFirst(newLoc);
            long cutoff = System.currentTimeMillis() - MAX_AGE_MS;

            while (!selfLocations.isEmpty() && selfLocations.peekLast().getTime() < cutoff)
            {
                selfLocations.removeLast();
            }

            while (selfLocations.size() > MAX_POSITIONS)
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
