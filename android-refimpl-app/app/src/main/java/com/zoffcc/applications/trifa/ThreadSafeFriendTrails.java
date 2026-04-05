package com.zoffcc.applications.trifa;

import android.location.Location;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ThreadSafeFriendTrails
{
    private final Map<String, Deque<Location>> friendLocations = new ConcurrentHashMap<>();
    private static final long MAX_AGE_MS = 180 * 1000; // n seconds in milliseconds
    private static final int MAX_POSITIONS = 80;

    public void updateLocation(String friendId, Location newLoc)
    {
        Deque<Location> history = friendLocations.computeIfAbsent(friendId, k -> new ArrayDeque<>());
        synchronized (history) {
            history.addFirst(newLoc);
            long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
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
            return history.stream()
                    .filter(loc -> loc.getTime() >= cutoff)
                    .limit(MAX_POSITIONS) // is redundant here due to update logic, but safe to keep
                    .collect(Collectors.toList());
        }
    }
}
