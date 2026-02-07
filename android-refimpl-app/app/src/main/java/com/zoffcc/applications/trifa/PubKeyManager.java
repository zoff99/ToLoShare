package com.zoffcc.applications.trifa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PubKeyManager
{
    private final List<String> keys;

    public PubKeyManager() {
        // Use a synchronized list for thread safety
        this.keys = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Adds a new public key if it is not already present.
     * @param key the public key to add
     * @return true if added, false if the key already exists or on error
     */
    public boolean addKey(String key) {
        try
        {
            synchronized (keys)
            {
                if (!keys.contains(key))
                {
                    keys.add(key);
                    return true;
                }
                return false;
            }
        }
        catch(Exception ignored)
        {
        }
        return false;
    }

    /**
     * Removes a public key if it exists.
     * @param key the public key to remove
     * @return true if removed, false if the key was not present or on error
     */
    public boolean removeKey(String key) {
        try
        {
            synchronized (keys) {
                return keys.remove(key);
            }
        }
            catch(Exception ignored)
        {
        }
        return false;
    }

    /**
     * Removes all keys.
     */
    public void removeAll() {
        try
        {
            synchronized (keys) {
                keys.clear();
            }
        }
        catch(Exception ignored)
        {
        }
    }

    /**
     * Returns the current number of keys.
     * @return number of keys, or 0 on error
     */
    public int getKeyCount() {
        try
        {
            synchronized (keys) {
                return keys.size();
            }
        }
        catch(Exception ignored)
        {
        }
        return 0;
    }

    /**
     * Optionally, get a copy of all keys.
     * @return a copy of the list of keys, or null on error
     */
    public List<String> getAllKeys() {
        try
        {
            synchronized (keys) {
                return new ArrayList<>(keys);
            }
        }
            catch(Exception ignored)
        {
        }
        return null;
    }
}

