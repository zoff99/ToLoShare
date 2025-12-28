/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 Zoff <zoff@zoff.cc>
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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.trifa.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.zoffcc.applications.trifa.HelperFriend.get_friend_capabilities_from_pubkey;
import static com.zoffcc.applications.trifa.HelperFriend.main_get_friend;
import static com.zoffcc.applications.trifa.HelperFriend.set_friend_avatar_update;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_get_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperGeneric.darkenColor;
import static com.zoffcc.applications.trifa.HelperGeneric.get_vfs_image_filename_friend_avatar;
import static com.zoffcc.applications.trifa.HelperGeneric.is_nightmode_active;
import static com.zoffcc.applications.trifa.HelperGeneric.put_vfs_image_on_imageview_real;
import static com.zoffcc.applications.trifa.HelperRelay.get_pushurl_for_friend;
import static com.zoffcc.applications.trifa.HelperRelay.is_valid_pushurl_for_friend_with_whitelist;
import static com.zoffcc.applications.trifa.HelperRelay.remove_friend_pushurl_in_db;
import static com.zoffcc.applications.trifa.Identicon.create_avatar_identicon_for_pubkey;
import static com.zoffcc.applications.trifa.MainActivity.friend_list_fragment;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.MainActivity.tox_friend_get_connection_ip;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_SYSTEM_MESSAGE_PEER_PUBKEY;
import static com.zoffcc.applications.trifa.ToxVars.TOX_CAPABILITY_DECODE;
import static com.zoffcc.applications.trifa.ToxVars.TOX_CAPABILITY_DECODE_TO_STRING;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

public class FriendInfoActivity extends AppCompatActivity
{
    static final String TAG = "trifa.FriendInfoActy";
    de.hdodenhof.circleimageview.CircleImageView profile_icon = null;
    TextView mytoxid = null;
    TextView mynick = null;
    TextView mystatus_message = null;
    EditText alias_text = null;
    TextView fi_relay_pubkey_textview = null;
    TextView fi_toxcapabilities_textview = null;
    TextView fi_relay_text = null;
    TextView fi_ipaddr_text = null;
    TextView friend_num_msgs_text = null;
    Button remove_friend_relay_button = null;
    TextView fi_pushurl_textview = null;
    TextView fi_pushurl_text = null;
    Button remove_friend_pushurl_button = null;
    String friend_pubkey = null;

    long friendnum = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friendinfo);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        Intent intent = getIntent();
        friendnum = intent.getLongExtra("friendnum", -1);
        friend_pubkey = tox_friend_get_public_key__wrapper(friendnum);

        profile_icon = (de.hdodenhof.circleimageview.CircleImageView) findViewById(R.id.fi_profile_icon);
        mytoxid = (TextView) findViewById(R.id.fi_toxprvkey_textview);
        mynick = (TextView) findViewById(R.id.fi_nick_text);
        mystatus_message = (TextView) findViewById(R.id.fi_status_message_text);
        alias_text = (EditText) findViewById(R.id.fi_alias_text);
        fi_relay_pubkey_textview = (TextView) findViewById(R.id.fi_relay_pubkey_textview);
        fi_relay_text = (TextView) findViewById(R.id.fi_relay_text);
        fi_ipaddr_text = (TextView) findViewById(R.id.fi_ipaddr_text);
        remove_friend_relay_button = (Button) findViewById(R.id.remove_friend_relay_button);
        fi_pushurl_textview = (TextView) findViewById(R.id.fi_pushurl_textview);
        fi_pushurl_text = (TextView) findViewById(R.id.fi_pushurl_text);
        remove_friend_pushurl_button = (Button) findViewById(R.id.remove_friend_pushurl_button);
        fi_toxcapabilities_textview = (TextView) findViewById(R.id.fi_toxcapabilities_textview);
        friend_num_msgs_text = (TextView) findViewById(R.id.friend_num_msgs_text);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }
}
