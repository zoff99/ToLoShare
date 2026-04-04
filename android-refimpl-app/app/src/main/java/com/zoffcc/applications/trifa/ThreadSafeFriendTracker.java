package com.zoffcc.applications.trifa;

import android.location.Location;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ThreadSafeFriendTracker
{
    private final Map<String, Deque<Location>> friendLocations = new ConcurrentHashMap<>();
    private static final long MAX_AGE_MS = 180 * 1000; // n seconds in milliseconds
    private static final int MAX_POSITIONS = 80;

    public void updateLocation(String friendId, Location newLoc)
    {
        // computeIfAbsent is atomic for the map entry
        Deque<Location> history = friendLocations.computeIfAbsent(friendId, k -> new ArrayDeque<>());

        synchronized (history) {
            // 1. Add newest update to the front
            history.addFirst(newLoc);

            // 2. Cleanup: Remove if older than 120s or size > 10
            long cutoff = System.currentTimeMillis() - MAX_AGE_MS;

            // removeIf is available via Gradle desugaring
            history.removeIf(loc -> loc.getTime() < cutoff);

            while (history.size() > MAX_POSITIONS) {
                history.removeLast();
            }
        }
    }

    public List<Location> getRecentPositions(String friendId)
    {
        Deque<Location> history = friendLocations.get(friendId);
        if (history == null) return Collections.emptyList();

        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;

        synchronized (history) {
            // Using Stream API (supported by desugaring)
            return history.stream()
                    .filter(loc -> loc.getTime() >= cutoff)
                    // limit(10) is redundant here due to update logic, but safe to keep
                    .collect(Collectors.toList());
        }
    }
}
