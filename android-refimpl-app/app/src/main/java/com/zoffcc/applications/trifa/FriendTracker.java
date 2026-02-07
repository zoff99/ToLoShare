package com.zoffcc.applications.trifa;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zoffcc.applications.trifa.MainActivity.in_count_view;
import static com.zoffcc.applications.trifa.MainActivity.out_count_view;

public class FriendTracker {
    /** @noinspection unused*/
    final static String TAG = "FriendTracker";

    Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ConcurrentHashMap<String, Long> friendsMap_in = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> friendsMap_out = new ConcurrentHashMap<>();
    // Background thread pool with a single worker thread
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long EXPIRATION_MS = 10_000; // x seconds
    private static final long CLEANUP_INTERVAL_MS = 30_000; // x seconds

    public FriendTracker() {
        // Schedule a cleanup task
        scheduler.scheduleWithFixedDelay(this::cleanup, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void ping_incoming(String friend_key) {
        try
        {
            int old_friendsMap_in_size = friendsMap_in.size();
            friendsMap_in.put(friend_key, System.currentTimeMillis());
            do_paint_in(old_friendsMap_in_size);
        }
        catch(Exception ignored)
        {
        }
    }

    public void ping_outgoing(String friend_key) {
        try
        {
            int old_friendsMap_out_size = friendsMap_out.size();
            friendsMap_out.put(friend_key, System.currentTimeMillis());
            do_paint_out(old_friendsMap_out_size);
        }
        catch(Exception ignored)
        {
        }
    }

    /** @noinspection unused*/
    public boolean isActive_in(String friend_key) {
        Long lastPing = friendsMap_in.get(friend_key);
        if (lastPing == null) return false;

        if (System.currentTimeMillis() - lastPing > EXPIRATION_MS) {
            friendsMap_in.remove(friend_key);
            return false;
        }
        return true;
    }

    /** @noinspection unused*/
    public boolean isActive_out(String friend_key) {
        Long lastPing = friendsMap_out.get(friend_key);
        if (lastPing == null) return false;

        if (System.currentTimeMillis() - lastPing > EXPIRATION_MS) {
            friendsMap_out.remove(friend_key);
            return false;
        }
        return true;
    }

    /** @noinspection unused*/
    public int getActiveCount_in() {
        cleanup();
        return friendsMap_in.size();
    }

    /** @noinspection unused*/
    public int getActiveCount_out() {
        cleanup();
        return friendsMap_out.size();
    }

    /**
     * Periodically removes expired entries from memory.
     */
    synchronized public void cleanup() {
        try
        {
            int old_friendsMap_in_size = friendsMap_in.size();
            int old_friendsMap_out_size = friendsMap_out.size();
            long now = System.currentTimeMillis();
            friendsMap_out.entrySet().removeIf(entry -> (now - entry.getValue() > EXPIRATION_MS));
            friendsMap_in.entrySet().removeIf(entry -> (now - entry.getValue() > EXPIRATION_MS));

            // Log.i(TAG, "cleanup: out=" + friendsMap_out.size() + " in=" + friendsMap_in.size());
            do_paint_out(old_friendsMap_out_size);
            do_paint_in(old_friendsMap_in_size);
        }
        catch(Exception ignored)
        {
        }
    }

    private void do_paint_in(int old_size)
    {
        if (old_size != friendsMap_in.size())
        {
            try
            {
                mainHandler.post(() -> {
                    try
                    {
                        in_count_view.removeAll();
                        for (String key : friendsMap_in.keySet()) {
                            in_count_view.addKey(key);
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void do_paint_out(int old_size)
    {
        if (old_size != friendsMap_out.size())
        {
            try
            {
                mainHandler.post(() -> {
                    try
                    {
                        out_count_view.removeAll();
                        for (String key : friendsMap_out.keySet()) {
                            out_count_view.addKey(key);
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}

