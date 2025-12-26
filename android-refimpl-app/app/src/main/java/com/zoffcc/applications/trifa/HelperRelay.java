/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2020 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.util.Log;

import com.zoffcc.applications.sorm.ConferenceDB;
import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.sorm.GroupDB;
import com.zoffcc.applications.sorm.RelayListDB;

import java.util.List;

import static com.zoffcc.applications.trifa.HelperFiletransfer.check_auto_accept_incoming_filetransfer;
import static com.zoffcc.applications.trifa.HelperFriend.is_friend_online_real;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.del_g_opts;
import static com.zoffcc.applications.trifa.HelperGeneric.get_g_opts;
import static com.zoffcc.applications.trifa.HelperGeneric.update_savedata_file_wrapper;
import static com.zoffcc.applications.trifa.MainActivity.PREF__allow_push_server_ntfy;
import static com.zoffcc.applications.trifa.MainActivity.tox_conference_invite;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_send_lossless_packet;
import static com.zoffcc.applications.trifa.MainActivity.tox_group_invite_friend;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_FRIEND_PUBKEY_FOR_PROXY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_GROUP_ID_FOR_PROXY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_NOTIFICATION_TOKEN;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_PROXY_PUBKEY_FOR_FRIEND;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_FCM_PUSH_URL_PREFIX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_FCM_PUSH_URL_PREFIX_OLD;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_NTFY_PUSH_URL_PREFIX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_PROVIDER_DB_KEY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_TOKEN_DB_KEY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_UP_PUSH_URL_PREFIX;
import static com.zoffcc.applications.trifa.ToxVars.TOX_GROUP_CHAT_ID_SIZE;
import static com.zoffcc.applications.trifa.ToxVars.TOX_PUBLIC_KEY_SIZE;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class HelperRelay
{
    private static final String TAG = "trifa.Hlp.Relay";

    static boolean is_any_relay(String friend_pubkey)
    {
        boolean ret = false;
        int num = orma.selectFromFriendList().tox_public_key_stringEq(friend_pubkey).is_relayEq(true).count();

        if (num > 0)
        {
            ret = true;
        }

        return ret;
    }

    static boolean have_own_relay()
    {
        boolean ret = false;
        int num = orma.selectFromRelayListDB().own_relayEq(true).count();

        if (num == 1)
        {
            ret = true;
        }

        return ret;
    }

    static void own_push_token_load()
    {
        if (TRIFAGlobals.global_notification_token == null)
        {
            if (get_g_opts(NOTIFICATION_TOKEN_DB_KEY) != null)
            {
                TRIFAGlobals.global_notification_token = get_g_opts(NOTIFICATION_TOKEN_DB_KEY);
            }
        }
    }

    static String push_token_to_push_url(final String push_token)
    {
        if (push_token != null)
        {
            // I dont have a relay, but i have a PUSH token
            String notification_push_url = push_token;
            if (push_token.startsWith("https://"))
            {
                // this must be a gotify/unifiedpush token
            }
            else
            {
                // this must be a google FCM token, add the porper HTTPS url here
                notification_push_url = NOTIFICATION_FCM_PUSH_URL_PREFIX + push_token;
            }

            if (notification_push_url.length() < 1000)
            {
                return notification_push_url;
            }
        }

        return null;
    }

    static boolean have_own_pushurl()
    {
        try
        {
            if (get_g_opts(NOTIFICATION_TOKEN_DB_KEY) != null)
            {
                final String tmp = get_g_opts(NOTIFICATION_TOKEN_DB_KEY);
                if (tmp.length() > 5)
                {
                    return true;
                }
            }
        }
        catch (Exception ignored)
        {
        }

        return false;
    }

    static boolean is_own_relay(String friend_pubkey)
    {
        boolean ret = false;

        try
        {
            String own_relay_pubkey = get_own_relay_pubkey();

            if (own_relay_pubkey != null)
            {
                if (friend_pubkey.equals(own_relay_pubkey) == true)
                {
                    ret = true;
                }
            }
        }
        catch (Exception e)
        {
        }

        return ret;
    }

    static String get_own_relay_pubkey()
    {
        String ret = null;
        return ret;
    }

    static int get_own_relay_connection_status_real()
    {
        int ret = 0;
        return ret;
    }

    static String get_pushurl_for_friend(String friend_pubkey)
    {
        String ret = null;

        try
        {
            ret = orma.selectFromFriendList().tox_public_key_stringEq(friend_pubkey).get(0).push_url;
        }
        catch (Exception e)
        {
        }

        return ret;
    }

    static boolean is_valid_pushurl_for_friend_with_whitelist(String push_url)
    {
        // whitelist google FCM gateway
        if (push_url.length() > NOTIFICATION_FCM_PUSH_URL_PREFIX.length())
        {
            if (push_url.startsWith(NOTIFICATION_FCM_PUSH_URL_PREFIX))
            {
                return true;
            }
        }

        // whitelist OLD google FCM gateway
        if (push_url.length() > NOTIFICATION_FCM_PUSH_URL_PREFIX_OLD.length())
        {
            if (push_url.startsWith(NOTIFICATION_FCM_PUSH_URL_PREFIX_OLD))
            {
                return true;
            }
        }

        // whitelist unified push demo server
        if (push_url.length() > NOTIFICATION_UP_PUSH_URL_PREFIX.length())
        {
            if (push_url.startsWith(NOTIFICATION_UP_PUSH_URL_PREFIX))
            {
                return true;
            }
        }

        // whitelist ntfy.sh server
        if (PREF__allow_push_server_ntfy)
        {
            if (push_url.length() > NOTIFICATION_NTFY_PUSH_URL_PREFIX.length())
            {
                if (push_url.startsWith(NOTIFICATION_NTFY_PUSH_URL_PREFIX))
                {
                    return true;
                }
            }
        }

        // anything else is not allowed at this time!
        return false;
    }

    static void remove_friend_pushurl_in_db(String friend_pubkey)
    {
        try
        {
            orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).push_url(null).execute();
        }
        catch (Exception e1)
        {
            Log.i(TAG, "remove_friend_pushurl_in_db:EE3:" + e1.getMessage());
        }
    }

    static void remove_own_pushurl_in_db()
    {
        del_g_opts(NOTIFICATION_TOKEN_DB_KEY);
        del_g_opts(NOTIFICATION_PROVIDER_DB_KEY);
    }
}
