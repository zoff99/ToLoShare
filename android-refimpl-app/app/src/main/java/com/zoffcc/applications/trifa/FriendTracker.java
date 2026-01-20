package com.zoffcc.applications.trifa;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zoffcc.applications.trifa.MainActivity.in_count_view;
import static com.zoffcc.applications.trifa.MainActivity.out_count_view;

public class FriendTracker {
    final static String TAG = "FriendTracker";

    Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ConcurrentHashMap<String, Long> friendsMap_in = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> friendsMap_out = new ConcurrentHashMap<>();
    // Background thread pool with a single worker thread
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long EXPIRATION_MS = 30_000; // x seconds

    public FriendTracker() {
        // Schedule a cleanup task to run every 1 minute
        scheduler.scheduleWithFixedDelay(this::cleanup, EXPIRATION_MS, EXPIRATION_MS, TimeUnit.MILLISECONDS);
    }

    public void ping_incoming(String friend_key) {
        int old_friendsMap_in_size = friendsMap_in.size();
        friendsMap_in.put(friend_key, System.currentTimeMillis());
        do_paint_in(old_friendsMap_in_size);
    }

    public void ping_outgoing(String friend_key) {
        int old_friendsMap_out_size = friendsMap_out.size();
        friendsMap_out.put(friend_key, System.currentTimeMillis());
        do_paint_out(old_friendsMap_out_size);
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
        int old_friendsMap_in_size = friendsMap_in.size();
        int old_friendsMap_out_size = friendsMap_out.size();
        long now = System.currentTimeMillis();
        friendsMap_out.entrySet().removeIf(entry -> (now - entry.getValue() > EXPIRATION_MS));
        friendsMap_in.entrySet().removeIf(entry -> (now - entry.getValue() > EXPIRATION_MS));

        // Log.i(TAG, "cleanup: out=" + friendsMap_out.size() + " in=" + friendsMap_in.size());
        do_paint_out(old_friendsMap_out_size);
        do_paint_in(old_friendsMap_in_size);
    }

    private void do_paint_in(int old_size)
    {
        if (old_size != friendsMap_in.size())
        {
            try
            {
                mainHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            in_count_view.updateCount(friendsMap_in.size());
                        }
                        catch (Exception e)
                        {
                        }
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
                mainHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            out_count_view.updateCount(friendsMap_out.size());
                        }
                        catch (Exception e)
                        {
                        }
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

