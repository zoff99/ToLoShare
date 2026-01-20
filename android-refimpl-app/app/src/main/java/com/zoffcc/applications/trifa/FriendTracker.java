package com.zoffcc.applications.trifa;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FriendTracker {
    private final ConcurrentHashMap<String, Long> friendsMap_in = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> friendsMap_out = new ConcurrentHashMap<>();
    // Background thread pool with a single worker thread
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long EXPIRATION_MS = 30_000; // 30 seconds

    public FriendTracker() {
        // Schedule a cleanup task to run every 1 minute
        scheduler.scheduleWithFixedDelay(this::cleanup, EXPIRATION_MS, EXPIRATION_MS, TimeUnit.MILLISECONDS);
    }

    public void ping_incoming(String friend_key) {
        friendsMap_in.put(friend_key, System.currentTimeMillis());
    }

    public void ping_outgoing(String friend_key) {
        friendsMap_out.put(friend_key, System.currentTimeMillis());
    }

    public boolean isActive_in(String friend_key) {
        Long lastPing = friendsMap_in.get(friend_key);
        if (lastPing == null) return false;

        if (System.currentTimeMillis() - lastPing > EXPIRATION_MS) {
            friendsMap_in.remove(friend_key);
            return false;
        }
        return true;
    }

    public boolean isActive_out(String friend_key) {
        Long lastPing = friendsMap_out.get(friend_key);
        if (lastPing == null) return false;

        if (System.currentTimeMillis() - lastPing > EXPIRATION_MS) {
            friendsMap_out.remove(friend_key);
            return false;
        }
        return true;
    }

    public int getActiveCount_in() {
        cleanup();
        return friendsMap_in.size();
    }

    public int getActiveCount_out() {
        cleanup();
        return friendsMap_out.size();
    }


    /**
     * Periodically removes expired entries from memory.
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        friendsMap_out.entrySet().removeIf(entry -> (now - entry.getValue() > EXPIRATION_MS));
        friendsMap_in.entrySet().removeIf(entry -> (now - entry.getValue() > EXPIRATION_MS));
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}

