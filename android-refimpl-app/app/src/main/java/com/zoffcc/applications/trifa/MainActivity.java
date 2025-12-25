/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 - 2020 Zoff <zoff@zoff.cc>
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.vanniktech.emoji.EmojiManager;
import com.yariksoffice.lingver.Lingver;
import com.zoffcc.applications.sorm.FileDB;
import com.zoffcc.applications.sorm.FriendList;
import com.zoffcc.applications.sorm.Message;
import com.zoffcc.applications.sorm.OrmaDatabase;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.unifiedpush.android.connector.UnifiedPush;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static com.zoffcc.applications.sorm.OrmaDatabase.run_multi_sql;
import static com.zoffcc.applications.sorm.OrmaDatabase.set_schema_upgrade_callback;
import static com.zoffcc.applications.trifa.CaptureService.getGeoMsg;
import static com.zoffcc.applications.trifa.FriendListFragment.fl_loading_progressbar;
import static com.zoffcc.applications.trifa.HelperFriend.get_friend_name_from_num;
import static com.zoffcc.applications.trifa.HelperFriend.get_set_is_default_ft_contact;
import static com.zoffcc.applications.trifa.HelperFriend.main_get_friend;
import static com.zoffcc.applications.trifa.HelperFriend.send_friend_msg_receipt_v2_wrapper;
import static com.zoffcc.applications.trifa.HelperFriend.send_pushurl_to_friend;
import static com.zoffcc.applications.trifa.HelperFriend.tox_friend_by_public_key__wrapper;
import static com.zoffcc.applications.trifa.HelperFriend.update_friend_in_db_capabilities;
import static com.zoffcc.applications.trifa.HelperFriend.update_friend_in_db_ip_addr_str;
import static com.zoffcc.applications.trifa.HelperGeneric.append_logger_msg;
import static com.zoffcc.applications.trifa.HelperGeneric.bytes_to_hex;
import static com.zoffcc.applications.trifa.HelperGeneric.display_toast;
import static com.zoffcc.applications.trifa.HelperGeneric.display_toast_with_context_custom_duration;
import static com.zoffcc.applications.trifa.HelperGeneric.draw_main_top_icon;
import static com.zoffcc.applications.trifa.HelperGeneric.get_battery_percent;
import static com.zoffcc.applications.trifa.HelperGeneric.initializeScreenshotSecurity;
import static com.zoffcc.applications.trifa.HelperGeneric.is_nightmode_active;
import static com.zoffcc.applications.trifa.HelperGeneric.update_savedata_file_wrapper_throttled_last_trigger_ts;
import static com.zoffcc.applications.trifa.HelperMsgNotification.change_msg_notification;
import static com.zoffcc.applications.trifa.HelperRelay.get_own_relay_connection_status_real;
import static com.zoffcc.applications.trifa.HelperRelay.have_own_relay;
import static com.zoffcc.applications.trifa.HelperRelay.is_any_relay;
import static com.zoffcc.applications.trifa.HelperRelay.own_push_token_load;
import static com.zoffcc.applications.trifa.HelperToxNotification.tox_notification_change_wrapper;
import static com.zoffcc.applications.trifa.MessageListActivity.ml_friend_typing;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONNECTION_STATUS_MANUAL_LOGOUT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_PROXY_PUBKEY_FOR_FRIEND;
import static com.zoffcc.applications.trifa.TRIFAGlobals.CONTROL_PROXY_MESSAGE_TYPE.CONTROL_PROXY_MESSAGE_TYPE_PUSH_URL_FOR_FRIEND;
import static com.zoffcc.applications.trifa.TRIFAGlobals.DELETE_SQL_AND_VFS_ON_ERROR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GEO_COORDS_CUSTOM_LOSSLESS_ID;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GLOBAL_INIT_PLAY_DELAY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.GLOBAL_VIDEO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.HIGHER_GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LOWER_GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LOWER_GLOBAL_VIDEO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LOWER_NGC_VIDEO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.LOWER_NGC_VIDEO_QUANTIZER;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NGC_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NORMAL_GLOBAL_AUDIO_BITRATE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_EDIT_ACTION.NOTIFICATION_EDIT_ACTION_REMOVE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.NOTIFICATION_PROVIDER_DB_KEY;
import static com.zoffcc.applications.trifa.TRIFAGlobals.ORBOT_PROXY_HOST;
import static com.zoffcc.applications.trifa.TRIFAGlobals.ORBOT_PROXY_PORT;
import static com.zoffcc.applications.trifa.TRIFAGlobals.PREF__DB_secrect_key__user_hash;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOX_PUSH_SETUP_HOWTO_URL;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_FT_DIRECTION.TRIFA_FT_DIRECTION_INCOMING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_MSG_TYPE.TRIFA_MSG_FILE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_PREFIX;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VIDEO_FRAME_RATE_INCOMING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VIDEO_FRAME_RATE_OUTGOING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.bootstrapping;
import static com.zoffcc.applications.trifa.TRIFAGlobals.count_video_frame_received;
import static com.zoffcc.applications.trifa.TRIFAGlobals.count_video_frame_sent;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_self_connection_status;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_self_last_went_offline_timestamp;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_self_last_went_online_timestamp;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_showing_mainview;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_showing_messageview;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_tox_self_status;
import static com.zoffcc.applications.trifa.TRIFAGlobals.last_video_frame_received;
import static com.zoffcc.applications.trifa.TRIFAGlobals.last_video_frame_sent;
import static com.zoffcc.applications.trifa.TRIFAGlobals.orbot_is_really_running;
import static com.zoffcc.applications.trifa.ToxVars.TOX_CONNECTION.TOX_CONNECTION_NONE;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_CANCEL;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_ID_LENGTH;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_DATA;
import static com.zoffcc.applications.trifa.ToxVars.TOX_HASH_LENGTH;
import static com.zoffcc.applications.trifa.ToxVars.TOX_PUBLIC_KEY_SIZE;
import static com.zoffcc.applications.trifa.ToxVars.TOX_USER_STATUS.TOX_USER_STATUS_AWAY;
import static com.zoffcc.applications.trifa.ToxVars.TOX_USER_STATUS.TOX_USER_STATUS_BUSY;
import static com.zoffcc.applications.trifa.ToxVars.TOX_USER_STATUS.TOX_USER_STATUS_NONE;
import static com.zoffcc.applications.trifa.TrifaToxService.TOX_SERVICE_STARTED;
import static com.zoffcc.applications.trifa.TrifaToxService.is_tox_started;
import static com.zoffcc.applications.trifa.TrifaToxService.manually_logged_out;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;
import static com.zoffcc.applications.trifa.TrifaToxService.vfs;

/*

first actually relayed message via ToxProxy

2019-08-28 22:20:43.286148 [D] friend_message_v2_cb:
fn=1 res=1 msg=🍔👍😜👍😜 @%\4äö ubnc Ovid n JB von in BK ni ubvzv8 ctcitccccccizzvvcvvv        u  tiigi gig i g35667u 6 66

 */

@SuppressWarnings({"UnusedReturnValue", "deprecation", "JniMissingFunction", "unused", "RedundantSuppression", "unchecked", "ConstantConditions", "RedundantCast", "Convert2Lambda", "EmptyCatchBlock", "PointlessBooleanExpression", "SimplifiableIfStatement", "ResultOfMethodCallIgnored"})
@SuppressLint({"StaticFieldLeak", "SimpleDateFormat", "SetTextI18n"})
@RuntimePermissions
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.MainActivity";
    // --------- global config ---------
    // --------- global config ---------
    // --------- global config ---------
    final static boolean CTOXCORE_NATIVE_LOGGING = false; // set "false" for release builds
    final static boolean NDK_STDOUT_LOGGING = false; // set "false" for release builds
    final static boolean DEBUG_BATTERY_OPTIMIZATION_LOGGING = false;  // set "false" for release builds
    final static boolean ORMA_TRACE = false; // set "false" for release builds
    final static int ORMA_CURRENT_DB_SCHEMA_VERSION = 10242; // increase for database schema changes
    final static boolean DB_ENCRYPT = true; // set "true" always!
    final static boolean VFS_ENCRYPT = true; // set "true" always!
    final static boolean AEC_DEBUG_DUMP = false; // set "false" for release builds
    final static boolean VFS_CUSTOM_WRITE_CACHE = true; // set "true" for release builds
    final static boolean DEBUG_USE_LOGFRIEND = false; // set "false" for release builds
    public final static boolean DEBUG_BSN_ON_PROFILE = false; // set "false" for release builds
    static boolean WANT_DEBUG_THREAD = false; // set "false" for release builds
    // --------- global config ---------
    // --------- global config ---------
    // --------- global config ---------

    static boolean DEBUG_THREAD_STARTED = false;

    static TextView mt = null;
    static ImageView top_imageview = null;
    static ImageView top_imageview2 = null;
    static ImageView top_imageview3 = null;
    static boolean native_lib_loaded = false;
    static boolean native_audio_lib_loaded = false;
    static String app_files_directory = "";
    // static boolean stop_me = false;
    // static Thread ToxServiceThread = null;
    static Semaphore semaphore_videoout_bitmap = new Semaphore(1);
    static Semaphore semaphore_tox_savedata = new Semaphore(1);
    Handler main_handler = null;
    static Handler main_handler_s = null;
    static Context context_s = null;
    static MainActivity main_activity_s = null;
    static AudioManager audio_manager_s = null;
    static Resources resources = null;
    static DisplayMetrics metrics = null;
    static SwitchCompat switch_normal_main_view = null;
    static ViewGroup waiting_container = null;
    static ViewGroup main_gallery_container = null;
    static TextView debug_text = null;
    static TextView debug_text_2 = null;
    static int main_gallery_lastScrollPosition = 0;
    static GridLayoutManager main_gallery_manager = null;
    static ArrayList<String> main_gallery_images = null;

    private static MapView map = null;
    static DirectedLocationOverlay remote_location_overlay = null;
    static MyLocationNewOverlay mLocationOverlay = null;
    static long last_remote_location_ts_millis = 0;

    static int AudioMode_old;
    static int RingerMode_old;
    static boolean isSpeakerPhoneOn_old;
    static boolean isWiredHeadsetOn_old;
    static boolean isBluetoothScoOn_old;
    static Notification notification = null;
    static NotificationManager nmn3 = null;
    static NotificationChannel notification_channel_toxservice = null;
    static NotificationChannel notification_channel_newmessage_sound_and_vibrate = null;
    static NotificationChannel notification_channel_newmessage_sound = null;
    static NotificationChannel notification_channel_newmessage_vibrate = null;
    static NotificationChannel notification_channel_newmessage_silent = null;
    static String channelId_toxservice = null;
    static String channelId_newmessage_sound_and_vibrate = null;
    static String channelId_newmessage_sound = null;
    static String channelId_newmessage_vibrate = null;
    static String channelId_newmessage_silent = null;
    static int NOTIFICATION_ID = 293821038;
    static int WATCHDOG_NOTIFICATION_ID = 696935351;
    static RemoteViews notification_view = null;
    static FriendListFragment friend_list_fragment = null;
    static MessageListFragment message_list_fragment = null;
    static MessageListActivity message_list_activity = null;
    final static String MAIN_DB_NAME = "main.db";
    final static String MAIN_VFS_NAME = "files.db";
    final static String DB_SHM_EXT = "-shm";
    final static String DB_WAL_EXT = "-wal";
    static String SD_CARD_TMP_DIR = "";
    static String SD_CARD_STATIC_DIR = "";
    static String SD_CARD_FILES_EXPORT_DIR = "";
    static String SD_CARD_FILES_DEBUG_DIR = "";
    static String SD_CARD_FILES_OUTGOING_WRAPPER_DIR = "";
    static String SD_CARD_ENC_FILES_EXPORT_DIR = "/unenc_files/";
    static String SD_CARD_ENC_CHATS_EXPORT_DIR = "/unenc_chats/";
    static String SD_CARD_FULL_FILES_EXPORT_DIR = "/fullexport/";
    static String SD_CARD_TMP_DUMMYFILE = null;
    final static int AddFriendActivity_ID = 10001;
    final static int CallingActivity_ID = 10002;
    final static int ProfileActivity_ID = 10003;
    final static int SettingsActivity_ID = 10004;
    final static int AboutpageActivity_ID = 10005;
    final static int MaintenanceActivity_ID = 10006;
    final static int WhiteListFromDozeActivity_ID = 10008;
    final static int SelectFriendSingleActivity_ID = 10009;
    final static int SelectLanguageActivity_ID = 10010;
    final static int CallingWaitingActivity_ID = 10011;
    final static int AddPrivateGroupActivity_ID = 10012;
    final static int AddPublicGroupActivity_ID = 10013;
    final static int JoinPublicGroupActivity_ID = 10014;
    final static int Notification_new_message_ID = 10023;
    final static int Notification_watchdog_trifa_stopped_ID = 10099;
    static long Notification_new_message_last_shown_timestamp = -1;
    final static long Notification_new_message_every_millis = 2000; // ~2 seconds between notifications
    final static long UPDATE_MESSAGES_WHILE_FT_ACTIVE_MILLIS = 30000; // ~30 seconds
    final static long UPDATE_MESSAGES_NORMAL_MILLIS = 500; // ~0.5 seconds
    static String temp_string_a = "";
    static ByteBuffer video_buffer_1 = null;
    static ByteBuffer video_buffer_2 = null;
    final static int audio_in_buffer_max_count = 2; // how many out play buffers? [we are now only using buffer "0" !!]
    public final static int audio_out_buffer_mult = 1;
    static ByteBuffer audio_buffer_2 = null; // given to JNI with set_JNI_audio_buffer2() for incoming audio (group and call)
    static long debug__audio_pkt_incoming = 0;
    static long debug__audio_frame_played = 0;
    static long debug__audio_play_buf_count_max = -1;
    static long debug__audio_play_buf01 = 0;
    static long debug__audio_play_buf02 = 0;
    static long debug__audio_play_buf03 = 0;
    static long debug__audio_play_buf04 = 0;
    static long debug__audio_play_buf05 = 0;
    static long debug__audio_play_buf06 = 0;
    static long debug__audio_play_factor = 0;
    static long debug__audio_play_iter = 0;
    // public static long[] audio_buffer_2_ts = new long[n_audio_in_buffer_max_count];
    // static ByteBuffer audio_buffer_play = null;
    static int audio_buffer_play_length = 0;
    static int[] audio_buffer_2_read_length = new int[audio_in_buffer_max_count];
    static TrifaToxService tox_service_fg = null;
    static long update_all_messages_global_timestamp = -1;
    final static SimpleDateFormat df_date_time_long = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final static SimpleDateFormat df_seconds_time = new SimpleDateFormat("mm:ss");
    final static SimpleDateFormat df_date_time_long_for_filename = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
    final static SimpleDateFormat df_date_only = new SimpleDateFormat("yyyy-MM-dd");
    static long last_updated_fps = -1;
    final static long update_fps_every_ms = 1500L; // update every 1.5 seconds
    //
    static long tox_self_capabilites = 0;
    //
    static boolean PREF__UV_reversed = true; // TODO: on older phones this needs to be "false"
    static boolean PREF__notification_sound = true;
    static boolean PREF__notification_vibrate = false;
    static boolean PREF__notification_show_content = false;
    static boolean PREF__notification = true;
    static final int MIN_AUDIO_SAMPLINGRATE_OUT = 48000;
    static final int SAMPLE_RATE_FIXED = 48000;
    static int PREF__min_audio_samplingrate_out = SAMPLE_RATE_FIXED;
    static String PREF__DB_secrect_key = "98rj93ßjw3j8j4vj9w8p9eüiü9aci092"; // this is just a dummy, this value is not used!
    static boolean PREF__DB_wal_mode = true;
    private static final String ALLOWED_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!§$%&()=?,.;:-_+";
    static boolean PREF__software_echo_cancel = false;
    static int PREF__higher_video_quality = 0;
    static int PREF__higher_audio_quality = 1;
    static int PREF__video_call_quality = 0;
    static int PREF__X_audio_play_buffer_custom = 0;
    static int PREF__udp_enabled = 0; // 0 -> Tox TCP mode, 1 -> Tox UDP mode
    static int PREF__audiosource = 2; // 1 -> VOICE_COMMUNICATION, 2 -> VOICE_RECOGNITION
    static boolean PREF__orbot_enabled = false;
    static boolean PREF__local_discovery_enabled = false;
    static boolean PREF__ipv6_enabled = true;
    static boolean PREF__audiorec_asynctask = true;
    static boolean PREF__cam_recording_hint = false; // careful with this paramter!! it can break camerapreview buffer size!!
    static boolean PREF__set_fps = false;
    static boolean PREF__fps_half = false;
    static boolean PREF__h264_encoder_use_intra_refresh = true;
    static boolean PREF__conference_show_system_messages = false;
    static boolean PREF__X_battery_saving_mode = false;
    static boolean PREF__X_misc_button_enabled = false;
    static String PREF__X_misc_button_msg = "t"; // TODO: hardcoded for now!
    static boolean PREF__U_keep_nospam = false;
    static boolean PREF__use_native_audio_play = true;
    static boolean PREF__tox_set_do_not_sync_av = false;
    static boolean PREF__use_audio_rec_effects = false;
    static boolean PREF__normal_main_view = true;
    static boolean PREF__window_security = true;
    public static int PREF__X_eac_delay_ms = 80;
    static boolean PREF__force_udp_only = false;
    static boolean PREF__use_incognito_keyboard = true;
    static boolean PREF__speakerphone_tweak = false;
    public static float PREF_mic_gain_factor = 1.0f;
    public static boolean PREF__mic_gain_factor_toggle = false;
    // from toxav/toxav.h -> valid values: 2.5, 5, 10, 20, 40 or 60 millseconds
    // 120 is also valid!!
    static int FRAME_SIZE_FIXED = 40; // this is only for recording audio!
    static int PREF__X_audio_recording_frame_size = FRAME_SIZE_FIXED; // !! 120 seems to work also !!
    static boolean PREF__X_zoom_incoming_video = false;
    static boolean PREF__use_software_aec = true;
    static boolean PREF__allow_screen_off_in_audio_call = true;
    static boolean PREF__use_H264_hw_encoding = true;
    static String PREF__camera_get_preview_format = "YV12"; // "YV12"; // "NV21";
    // static boolean PREF__use_camera_x = false;
    static boolean PREF__NO_RECYCLE_VIDEO_FRAME_BITMAP = true;
    static int PREF__audio_play_volume_percent = 100;
    static int PREF__video_play_delay_ms = GLOBAL_INIT_PLAY_DELAY;
    static int GLOBAL_AV_BUFFER_MS = 120;
    static int PREF__audio_group_play_volume_percent = 100;
    static boolean PREF__auto_accept_image = false;
    static boolean PREF__auto_accept_video = false;
    static boolean PREF__auto_accept_all_upto = false;
    static int PREF__video_cam_resolution = 0;
    static final int PREF_GLOBAL_FONT_SIZE_DEFAULT = 2;
    static int PREF__global_font_size = PREF_GLOBAL_FONT_SIZE_DEFAULT;
    static boolean PREF__allow_open_encrypted_file_via_intent = true;
    static boolean PREF__allow_file_sharing_to_trifa_via_intent = false;
    static boolean PREF__compact_friendlist = false;
    static boolean PREF__compact_chatlist = true;
    static boolean PREF__use_push_service = false;
    static String[] PREF__toxirc_muted_peers = {};
    static boolean PREF__show_friendnumber_on_friendlist = false;
    static int PREF__dark_mode_pref = 1;
    static boolean PREF__allow_push_server_ntfy = false;
    static boolean PREF__messageview_paging = true;
    static int PREF__message_paging_num_msgs_per_page = 50;
    static int PREF__ngc_video_bitrate = LOWER_NGC_VIDEO_BITRATE; // ~600 kbits/s -> ~60 kbytes/s
    static int PREF__ngc_video_frame_delta_ms = 120; // 120 ms -> 8.3 fps
    static int PREF__ngc_video_max_quantizer = LOWER_NGC_VIDEO_QUANTIZER; // 47 -> default, 51 -> lowest quality, 30 -> very high quality and lots of bandwidth!
    static int PREF__ngc_audio_bitrate = NGC_AUDIO_BITRATE;
    static int PREF__ngc_audio_samplerate = 48000;
    static int PREF__ngc_audio_channels = 1;
    static boolean PREF__gainprocessing_active = true;
    static boolean PREF__rnnoise_active = false;
    static boolean PREF__trust_all_webcerts = false; // HINT: !!be careful with this option!!

    final static String push_instance_name = "com.zoffcc.applications.push_toloshare";

    static String versionName = "";
    static int versionCode = -1;
    static PackageInfo packageInfo_s = null;
    static final float BATTERY_PERCENT_UNKNOWN = -99f;
    static float global_battery_percent = BATTERY_PERCENT_UNKNOWN;
    IntentFilter receiverFilter1 = null;
    IntentFilter receiverFilter2 = null;
    IntentFilter receiverFilter3 = null;
    IntentFilter receiverFilter4 = null;
    static TextView waiting_view = null;
    static ProgressBar waiting_image = null;
    static ViewGroup normal_container = null;
    static ClipboardManager clipboard;
    private ClipData clip;
    static List<Long> selected_messages = new ArrayList<Long>();
    static List<Long> selected_messages_text_only = new ArrayList<Long>();
    static List<Long> selected_messages_incoming_file = new ArrayList<Long>();
    //
    // YUV conversion -------
    static ScriptIntrinsicYuvToRGB yuvToRgb = null;
    static Allocation alloc_in = null;
    static Allocation alloc_out = null;
    static Bitmap video_frame_image = null;
    static boolean video_frame_image_valid = false;
    static int buffer_size_in_bytes = 0;
    // YUV conversion -------

    // ---- lookup cache ----
    static Map<String, Long> cache_pubkey_fnum = new HashMap<String, Long>();
    static Map<Long, String> cache_fnum_pubkey = new HashMap<Long, String>();
    // ---- lookup cache ----

    // main drawer ----------
    static Drawer main_drawer = null;
    AccountHeader main_drawer_header = null;
    ProfileDrawerItem profile_d_item = null;
    // main drawer ----------

    Spinner spinner_own_status = null;

    /** @noinspection CommentedOutCode*/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "M:STARTUP:super onCreate");
        super.onCreate(savedInstanceState);
        Log.i(TAG, "M:STARTUP:onCreate");
        Log.i(TAG, "onCreate");

        Log.i(TAG, "M:STARTUP:Lingver set");
        Log.d(TAG, "Lingver_Locale: " + Lingver.getInstance().getLocale());
        Log.d(TAG, "Lingver_Language: " + Lingver.getInstance().getLanguage());
        // Log.d(TAG, "Actual_Language: " + resources.configuration.getLocaleCompat());

        try
        {
            if (BuildConfig.DEBUG)
            {
                Log.i(TAG, "***** BuildConfig.DEBUG *****");
                //StrictMode.setVmPolicy(
                //        new StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog().build());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        resources = this.getResources();
        metrics = resources.getDisplayMetrics();
        global_showing_messageview = false;
        global_showing_mainview = false;

        Log.i(TAG, "is_nightmode_active:" + is_nightmode_active(this));

        try
        {
            Log.i(TAG, "M:STARTUP:Thread=" + Thread.currentThread().getName());
        }
        catch (Exception e)
        {
        }

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        org.osmdroid.config.Configuration.getInstance().
                load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string

        Log.i(TAG, "M:STARTUP:setContentView start");
        setContentView(R.layout.activity_main);
        Log.i(TAG, "M:STARTUP:setContentView end");

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setFlingEnabled(true);
        map.setTilesScaledToDpi(true);
        map.setMinZoomLevel(null);

        /*
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(this, map);
        mRotationGestureOverlay.setEnabled(true);
        map.setMultiTouchControls(true);
        map.getOverlays().add(mRotationGestureOverlay);
        */

        remote_location_overlay = new DirectedLocationOverlay(this);
        remote_location_overlay.setShowAccuracy(true);
        map.getOverlays().add(remote_location_overlay);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);

        // set a default starting point in the middle of europe
        IMapController mapController = map.getController();
        mapController.setZoom(12);
        GeoPoint startPoint = new GeoPoint(48.18164, 16.3661);
        mapController.setCenter(startPoint);

        switch_normal_main_view = this.findViewById(R.id.switch_normal_main_view);
        waiting_container = this.findViewById(R.id.waiting_container);
        main_gallery_container = this.findViewById(R.id.main_gallery_container);
        debug_text = this.findViewById(R.id.debug_text);
        debug_text_2 = this.findViewById(R.id.debug_text_2);

        mt = (TextView) this.findViewById(R.id.main_maintext);
        mt.setText("...");
        mt.setVisibility(View.VISIBLE);
        if (native_lib_loaded)
        {
            Log.i(TAG, "M:STARTUP:native_lib_loaded OK");
            mt.setText("successfully loaded native library");
        }
        else
        {
            Log.i(TAG, "M:STARTUP:native_lib_loaded failed");
            mt.setText("loadLibrary jni-c-toxcore failed!");
            show_wrong_credentials();
            finish();
            return;
        }

        /*
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ExactAlarmChagedReceiver ar = new ExactAlarmChagedReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED");
                this.registerReceiver(ar, filter);
                Log.i(TAG, "ExactAlarmChagedReceiver registered");
            }
            else
            {
                Log.i(TAG, "ExactAlarmChagedReceiver:below API S");
            }
        }
        catch(Exception e22)
        {
            Log.i(TAG, "ExactAlarmChagedReceiver:EE:" + e22.getMessage());
            e22.printStackTrace();
        }
        */

        Log.i(TAG, "M:STARTUP:toolbar");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Log.i(TAG, "M:STARTUP:EmojiManager install");
        // EmojiManager.install(new IosEmojiProvider());
        EmojiManager.install(new com.vanniktech.emoji.google.GoogleEmojiProvider());
        // EmojiManager.install(new com.vanniktech.emoji.twitter.TwitterEmojiProvider());


        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PREF__DB_secrect_key = settings.getString("DB_secrect_key", "");

        if (PREF__DB_secrect_key.isEmpty())
        {
            // ok, use hash of user entered password
            PREF__DB_secrect_key = PREF__DB_secrect_key__user_hash;
        }

        main_handler = new Handler(getMainLooper());
        main_handler_s = main_handler;
        context_s = this.getBaseContext();
        main_activity_s = this;
        TRIFAGlobals.CONFERENCE_CHAT_BG_CORNER_RADIUS_IN_PX = (int) HelperGeneric.dp2px(10);
        TRIFAGlobals.CONFERENCE_CHAT_DRAWER_ICON_CORNER_RADIUS_IN_PX = (int) HelperGeneric.dp2px(20);

        // put this after! context_s has been set
        global_battery_percent = get_battery_percent();

        if ((!TOX_SERVICE_STARTED) || (orma == null))
        {
            Log.i(TAG, "M:STARTUP:init DB");

            try
            {
                String dbs_path = getDir("dbs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_DB_NAME;
                // Log.i(TAG, "db:path=" + dbs_path);
                File database_dir = new File(new File(dbs_path).getParent());
                database_dir.mkdirs();

                if (DB_ENCRYPT)
                {
                    // builder = builder.provider(new OrmaDatabase.EncryptedDatabase.Provider(PREF__DB_secrect_key));
                    orma = OrmaDatabase_wrapper(dbs_path, PREF__DB_secrect_key, PREF__DB_wal_mode);
                }
                else
                {
                    orma = OrmaDatabase_wrapper(dbs_path, null, PREF__DB_wal_mode);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "M:STARTUP:init DB:EE1");
                Log.i(TAG, "db:EE1:" + e.getMessage());
                String dbs_path = getDir("dbs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_DB_NAME;

                if (DELETE_SQL_AND_VFS_ON_ERROR)
                {
                    try
                    {
                        // Log.i(TAG, "db:deleting database:" + dbs_path);
                        new File(dbs_path).delete();
                    }
                    catch (Exception e3)
                    {
                        e3.printStackTrace();
                        Log.i(TAG, "db:EE3:" + e3.getMessage());
                    }
                }

                try
                {
                    if (DB_ENCRYPT)
                    {
                        // builder = builder.provider(new OrmaDatabase.EncryptedDatabase.Provider(PREF__DB_secrect_key));
                        orma = OrmaDatabase_wrapper(dbs_path, PREF__DB_secrect_key, PREF__DB_wal_mode);
                    }
                    else
                    {
                        orma = OrmaDatabase_wrapper(dbs_path, null, PREF__DB_wal_mode);
                    }
                }
                catch (Exception e4)
                {
                    Log.i(TAG, "M:STARTUP:init DB:EE4");
                    Log.i(TAG, "db:EE4:" + e4.getMessage());
                    show_wrong_credentials();
                    finish();
                    return;
                }
                // Log.i(TAG, "db:open(2)=OK:path=" + dbs_path);
            }

            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
            // ** // ** // orma.deleteFromMessage().execute();
            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
            // ----- Clear all messages from DB -----
        }

        if (PREF__window_security)
        {
            // prevent screenshots and also dont show the window content in recent activity screen
            initializeScreenshotSecurity(this);
        }

        //        try
        //        {
        //            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        //        }
        //        catch (Exception e)
        //        {
        //            e.printStackTrace();
        //            Log.i(TAG, "onCreate:setThreadPriority:EE:" + e.getMessage());
        //        }

        Log.i(TAG, "M:STARTUP:getVersionInfo");
        getVersionInfo();

        try
        {
            packageInfo_s = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //        if (canceller == null)
        //        {
        //            canceller = new EchoCanceller();
        //        }


        //        try
        //        {
        //            ((Toolbar) getSupportActionBar().getCustomView().getParent()).setContentInsetsAbsolute(0, 0);
        //        }
        //        catch (Exception e)
        //        {
        //            e.printStackTrace();
        //        }
        bootstrapping = false;
        waiting_view = (TextView) findViewById(R.id.waiting_view);
        waiting_image = (ProgressBar) findViewById(R.id.waiting_image);
        normal_container = (ViewGroup) findViewById(R.id.normal_container);
        waiting_view.setVisibility(View.GONE);
        waiting_image.setVisibility(View.GONE);
        normal_container.setVisibility(View.VISIBLE);
        SD_CARD_TMP_DIR = getExternalFilesDir(null).getAbsolutePath() + "/tmpdir/";
        SD_CARD_STATIC_DIR = getExternalFilesDir(null).getAbsolutePath() + "/_staticdir/";
        SD_CARD_FILES_EXPORT_DIR = getExternalFilesDir(null).getAbsolutePath() + "/vfs_export/";
        SD_CARD_FILES_DEBUG_DIR = getExternalFilesDir(null).getAbsolutePath() + "/debug/";
        SD_CARD_FILES_OUTGOING_WRAPPER_DIR = getExternalFilesDir(null).getAbsolutePath() + "/outgoing/";
        // Log.i(TAG, "SD_CARD_FILES_EXPORT_DIR:" + SD_CARD_FILES_EXPORT_DIR);
        SD_CARD_TMP_DUMMYFILE = HelperGeneric.make_some_static_dummy_file(this.getBaseContext());
        audio_manager_s = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.i(TAG, "java.library.path:" + System.getProperty("java.library.path"));
        nmn3 = (NotificationManager) context_s.getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            Log.i(TAG, "M:STARTUP:notification channels");
            String channelName;
            // ---------------------
            channelId_newmessage_sound_and_vibrate = "trifa_new_message_sound_and_vibrate";
            channelName = "New Message Sound and Vibrate";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_sound_and_vibrate = new NotificationChannel(
                    channelId_newmessage_sound_and_vibrate, channelName, importance);
            notification_channel_newmessage_sound_and_vibrate.setDescription(channelId_newmessage_sound_and_vibrate);
            notification_channel_newmessage_sound_and_vibrate.enableVibration(true);
            nmn3.createNotificationChannel(notification_channel_newmessage_sound_and_vibrate);
            // ---------------------
            channelId_newmessage_sound = "trifa_new_message_sound";
            channelName = "New Message Sound";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_sound = new NotificationChannel(channelId_newmessage_sound, channelName,
                                                                            importance);
            notification_channel_newmessage_sound.setDescription(channelId_newmessage_sound);
            notification_channel_newmessage_sound.enableVibration(false);
            nmn3.createNotificationChannel(notification_channel_newmessage_sound);
            // ---------------------
            channelId_newmessage_vibrate = "trifa_new_message_vibrate";
            channelName = "New Message Vibrate";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_vibrate = new NotificationChannel(channelId_newmessage_vibrate, channelName,
                                                                              importance);
            notification_channel_newmessage_vibrate.setDescription(channelId_newmessage_vibrate);
            notification_channel_newmessage_vibrate.setSound(null, null);
            notification_channel_newmessage_vibrate.enableVibration(true);
            nmn3.createNotificationChannel(notification_channel_newmessage_vibrate);
            // ---------------------
            channelId_newmessage_silent = "trifa_new_message_silent";
            channelName = "New Message Silent";
            importance = NotificationManager.IMPORTANCE_DEFAULT;
            notification_channel_newmessage_silent = new NotificationChannel(channelId_newmessage_silent, channelName,
                                                                             importance);
            notification_channel_newmessage_silent.setDescription(channelId_newmessage_silent);
            notification_channel_newmessage_silent.setSound(null, null);
            notification_channel_newmessage_silent.enableVibration(false);
            nmn3.createNotificationChannel(notification_channel_newmessage_silent);
            // ---------------------
            channelId_toxservice = "trifa_tox_service";
            channelName = "Tox Service";
            importance = NotificationManager.IMPORTANCE_LOW;
            notification_channel_toxservice = new NotificationChannel(channelId_toxservice, channelName, importance);
            notification_channel_toxservice.setDescription(channelId_toxservice);
            notification_channel_toxservice.setSound(null, null);
            notification_channel_toxservice.enableVibration(false);
            nmn3.createNotificationChannel(notification_channel_toxservice);
        }

        // prefs ----------
        PREF__UV_reversed = settings.getBoolean("video_uv_reversed", true);
        PREF__notification_sound = settings.getBoolean("notifications_new_message_sound", true);
        PREF__notification_vibrate = settings.getBoolean("notifications_new_message_vibrate", false);
        PREF__notification_show_content = settings.getBoolean("notification_show_content", false);
        PREF__notification = settings.getBoolean("notifications_new_message", true);
        PREF__software_echo_cancel = settings.getBoolean("software_echo_cancel", false);
        PREF__fps_half = settings.getBoolean("fps_half", false);
        PREF__h264_encoder_use_intra_refresh = settings.getBoolean("h264_encoder_use_intra_refresh", true);
        PREF__U_keep_nospam = settings.getBoolean("U_keep_nospam", false);
        PREF__set_fps = settings.getBoolean("set_fps", false);
        PREF__conference_show_system_messages = settings.getBoolean("conference_show_system_messages", false);
        PREF__X_battery_saving_mode = settings.getBoolean("X_battery_saving_mode", false);
        PREF__X_misc_button_enabled = settings.getBoolean("X_misc_button_enabled", false);
        PREF__local_discovery_enabled = settings.getBoolean("local_discovery_enabled", false);
        PREF__force_udp_only = settings.getBoolean("force_udp_only", false);
        PREF__use_incognito_keyboard = settings.getBoolean("use_incognito_keyboard", true);
        PREF__speakerphone_tweak = settings.getBoolean("speakerphone_tweak", false);
        PREF__mic_gain_factor_toggle = settings.getBoolean("mic_gain_factor_toggle", false);
        PREF__window_security = settings.getBoolean("window_security", true);
        PREF__use_native_audio_play = settings.getBoolean("X_use_native_audio_play", true);

        boolean tmp1 = settings.getBoolean("udp_enabled", false);

        if (tmp1)
        {
            PREF__udp_enabled = 1;
        }
        else
        {
            PREF__udp_enabled = 0;
        }

        PREF__higher_video_quality = 0;
        GLOBAL_VIDEO_BITRATE = LOWER_GLOBAL_VIDEO_BITRATE;

        try
        {
            PREF__video_call_quality = Integer.parseInt(settings.getString("video_call_quality", "0"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_call_quality = 0;
        }


        try
        {
            PREF__higher_audio_quality = Integer.parseInt(settings.getString("higher_audio_quality", "1"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__higher_audio_quality = 1;
        }

        if (PREF__higher_audio_quality == 2)
        {
            GLOBAL_AUDIO_BITRATE = HIGHER_GLOBAL_AUDIO_BITRATE;
        }
        else if (PREF__higher_audio_quality == 1)
        {
            GLOBAL_AUDIO_BITRATE = NORMAL_GLOBAL_AUDIO_BITRATE;
        }
        else
        {
            GLOBAL_AUDIO_BITRATE = LOWER_GLOBAL_AUDIO_BITRATE;
        }

        // ------- access the clipboard -------
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        // ------- access the clipboard -------
        PREF__orbot_enabled = settings.getBoolean("orbot_enabled", false);

        if (PREF__orbot_enabled)
        {
            Log.i(TAG, "M:STARTUP:wait for orbot");
            boolean orbot_installed = OrbotHelper.isOrbotInstalled(this);

            //if (orbot_installed)
            {
                boolean orbot_running = orbot_is_really_running; // OrbotHelper.isOrbotRunning(this);
                Log.i(TAG, "waiting_for_orbot_info:orbot_running=" + orbot_running);

                if (orbot_running)
                {
                    Log.i(TAG, "waiting_for_orbot_info:F1");
                    HelperGeneric.waiting_for_orbot_info(false);
                    OrbotHelper.get(this).statusTimeout(120 * 1000).addStatusCallback(new StatusCallback()
                    {
                        @Override
                        public void onEnabled(Intent statusIntent)
                        {
                        }

                        @Override
                        public void onStarting()
                        {
                        }

                        @Override
                        public void onStopping()
                        {
                        }

                        @Override
                        public void onDisabled()
                        {
                            // we got a broadcast with a status of off, so keep waiting
                        }

                        @Override
                        public void onStatusTimeout()
                        {
                            // throw new RuntimeException("Orbot status request timed out");
                            Log.i(TAG, "waiting_for_orbot_info:EEO1:" + "Orbot status request timed out");
                        }

                        @Override
                        public void onNotYetInstalled()
                        {
                        }
                    }).init(); // allow 60 seconds to connect to Orbot
                }
                else
                {
                    orbot_is_really_running = false;

                    if (OrbotHelper.requestStartTor(this))
                    {
                        Log.i(TAG, "waiting_for_orbot_info:*T2");
                        HelperGeneric.waiting_for_orbot_info(true);
                    }
                    else
                    {
                        // should never get here
                        Log.i(TAG, "waiting_for_orbot_info:F3");
                        HelperGeneric.waiting_for_orbot_info(false);
                    }

                    OrbotHelper.get(this).statusTimeout(120 * 1000).addStatusCallback(new StatusCallback()
                    {
                        @Override
                        public void onEnabled(Intent statusIntent)
                        {
                        }

                        @Override
                        public void onStarting()
                        {
                        }

                        @Override
                        public void onStopping()
                        {
                        }

                        @Override
                        public void onDisabled()
                        {
                            // we got a broadcast with a status of off, so keep waiting
                        }

                        @Override
                        public void onStatusTimeout()
                        {
                            // throw new RuntimeException("Orbot status request timed out");
                            Log.i(TAG, "waiting_for_orbot_info:EEO2:" + "Orbot status request timed out");
                        }

                        @Override
                        public void onNotYetInstalled()
                        {
                        }
                    }).init(); // allow 60 seconds to connect to Orbot
                }
            }
            /*
            else
            {
                Log.i(TAG, "waiting_for_orbot_info:F4");
                HelperGeneric.waiting_for_orbot_info(false);
                Intent orbot_get = OrbotHelper.getOrbotInstallIntent(this);

                try
                {
                    startActivity(orbot_get);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
             */
        }
        else
        {
            Log.i(TAG, "waiting_for_orbot_info:F5");
            HelperGeneric.waiting_for_orbot_info(false);
        }

        Log.i(TAG, "PREF__UV_reversed:2=" + PREF__UV_reversed);
        Log.i(TAG, "PREF__notification_sound:2=" + PREF__notification_sound);
        Log.i(TAG, "PREF__notification_vibrate:2=" + PREF__notification_vibrate);

        try
        {
            if (settings.getString("min_audio_samplingrate_out", "8000").compareTo("Auto") == 0)
            {
                PREF__min_audio_samplingrate_out = 8000;
            }
            else
            {
                PREF__min_audio_samplingrate_out = Integer.parseInt(
                        settings.getString("min_audio_samplingrate_out", "" + MIN_AUDIO_SAMPLINGRATE_OUT));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__min_audio_samplingrate_out = MIN_AUDIO_SAMPLINGRATE_OUT;
        }

        // ------- FIXED -------
        PREF__min_audio_samplingrate_out = SAMPLE_RATE_FIXED;
        // ------- FIXED -------

        try
        {
            PREF__allow_screen_off_in_audio_call = settings.getBoolean("allow_screen_off_in_audio_call", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_screen_off_in_audio_call = true;
        }

        try
        {
            PREF__auto_accept_image = settings.getBoolean("auto_accept_image", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_image = false;
        }

        try
        {
            PREF__auto_accept_video = settings.getBoolean("auto_accept_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_video = false;
        }

        try
        {
            PREF__auto_accept_all_upto = settings.getBoolean("auto_accept_all_upto", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_all_upto = false;
        }

        try
        {
            PREF__X_zoom_incoming_video = settings.getBoolean("X_zoom_incoming_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_zoom_incoming_video = false;
        }

        try
        {
            PREF__X_audio_recording_frame_size = Integer.parseInt(
                    settings.getString("X_audio_recording_frame_size", "" + 40));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_audio_recording_frame_size = 40;
        }

        // ------- FIXED -------
        PREF__X_audio_recording_frame_size = FRAME_SIZE_FIXED;
        // ------- FIXED -------

        try
        {
            PREF__video_cam_resolution = Integer.parseInt(settings.getString("video_cam_resolution", "" + 0));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_cam_resolution = 0;
        }

        try
        {
            PREF__global_font_size = Integer.parseInt(
                    settings.getString("global_font_size", "" + PREF_GLOBAL_FONT_SIZE_DEFAULT));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__global_font_size = PREF_GLOBAL_FONT_SIZE_DEFAULT;
        }

        PREF__camera_get_preview_format = settings.getString("camera_get_preview_format", "YV12");


        try
        {
            PREF__compact_friendlist = settings.getBoolean("compact_friendlist", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__compact_friendlist = false;
        }

        try
        {
            PREF__allow_file_sharing_to_trifa_via_intent = settings.getBoolean("allow_file_sharing_to_trifa_via_intent",
                                                                               false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_file_sharing_to_trifa_via_intent = false;
        }

        try
        {
            PREF__allow_open_encrypted_file_via_intent = settings.getBoolean("allow_open_encrypted_file_via_intent",
                                                                             true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_open_encrypted_file_via_intent = true;
        }

        PREF__compact_chatlist = true;

        try
        {
            PREF__allow_push_server_ntfy = settings.getBoolean("allow_push_server_ntfy", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_push_server_ntfy = false;
        }

        try
        {
            PREF__use_push_service = settings.getBoolean("use_push_service", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__use_push_service = false;
        }

        try
        {
            PREF__gainprocessing_active = settings.getBoolean("gainprocessing_active", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__gainprocessing_active = true;
        }

        try
        {
            PREF__rnnoise_active = settings.getBoolean("rnnoise_active", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__rnnoise_active = false;
        }

        //    PREF__use_camera_x = false;

        // prefs ----------

        // TODO: remake this into something nicer ----------
        top_imageview = (ImageView) this.findViewById(R.id.main_maintopimage);
        top_imageview.setVisibility(View.GONE);

        top_imageview2 = (ImageView) this.findViewById(R.id.main_maintopimage2);
        top_imageview2.setVisibility(View.GONE);

        top_imageview3 = (ImageView) this.findViewById(R.id.main_maintopimage3);
        top_imageview3.setVisibility(View.GONE);

        if (PREF__U_keep_nospam == true)
        {
            top_imageview2.setBackgroundColor(Color.TRANSPARENT);
            // top_imageview.setBackgroundColor(Color.parseColor("#C62828"));
            final Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_exclamation_circle).paddingDp(
                    15).color(getResources().getColor(R.color.md_red_600)).sizeDp(100);
            top_imageview2.setImageDrawable(d1);
            fadeInAndShowImage(top_imageview2, 5000);
        }
        else
        {
            top_imageview2.setVisibility(View.GONE);
        }

        own_push_token_load();
        top_imageview3.setVisibility(View.GONE);

        fadeInAndShowImage(top_imageview, 5000);
        fadeOutAndHideImage(mt, 4000);
        // TODO: remake this into something nicer ----------
        // --------- status spinner ---------
        spinner_own_status = (Spinner) findViewById(R.id.spinner_own_status);
        ArrayList<String> own_online_status_string_values = new ArrayList<String>(
                Arrays.asList(getString(R.string.MainActivity_available), getString(R.string.MainActivity_away),
                              getString(R.string.MainActivity_busy)));
        ArrayAdapter<String> myAdapter = new OwnStatusSpinnerAdapter(this, R.layout.own_status_spinner_item,
                                                                     own_online_status_string_values);

        if (spinner_own_status != null)
        {
            spinner_own_status.setAdapter(myAdapter);
            spinner_own_status.setSelection(global_tox_self_status);
            spinner_own_status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View v, int position, long id)
                {
                    if (is_tox_started)
                    {
                        try
                        {
                            if (id == 0)
                            {
                                // status: available
                                tox_self_set_status(TOX_USER_STATUS_NONE.value);
                                global_tox_self_status = TOX_USER_STATUS_NONE.value;
                            }
                            else if (id == 1)
                            {
                                // status: away
                                tox_self_set_status(TOX_USER_STATUS_AWAY.value);
                                global_tox_self_status = TOX_USER_STATUS_AWAY.value;
                            }
                            else if (id == 2)
                            {
                                // status: busy
                                tox_self_set_status(TOX_USER_STATUS_BUSY.value);
                                global_tox_self_status = TOX_USER_STATUS_BUSY.value;
                            }
                        }
                        catch (Exception e2)
                        {
                            e2.printStackTrace();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView)
                {
                    // your code here
                }
            });
        }

        // --------- status spinner ---------
        // get permission ----------
        Log.i(TAG, "M:STARTUP:permissions");
        MainActivityPermissionsDispatcher.dummyForPermissions001WithPermissionCheck(this);
        // get permission ----------
        // -------- drawer ------------
        // -------- drawer ------------
        // -------- drawer ------------
        Log.i(TAG, "M:STARTUP:drawer");
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(
                R.string.MainActivity_profile).withIcon(GoogleMaterial.Icon.gmd_face);
        PrimaryDrawerItem item2 = new PrimaryDrawerItem().withIdentifier(2).withName(
                R.string.MainActivity_settings).withIcon(GoogleMaterial.Icon.gmd_settings);
        PrimaryDrawerItem item3;
        if (manually_logged_out)
        {
            item3 = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.MainActivity_manually_logged_out_true).withIcon(GoogleMaterial.Icon.gmd_refresh);
        }
        else
        {
            item3 = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.MainActivity_manually_logged_out_false).withIcon(GoogleMaterial.Icon.gmd_refresh);
        }
        PrimaryDrawerItem item4 = new PrimaryDrawerItem().withIdentifier(4).withName(
                R.string.MainActivity_maint).withIcon(GoogleMaterial.Icon.gmd_build);
        PrimaryDrawerItem item5 = new PrimaryDrawerItem().withIdentifier(5).withName(
                R.string.MainActivity_about).withIcon(GoogleMaterial.Icon.gmd_info);
        PrimaryDrawerItem item6 = new PrimaryDrawerItem().withIdentifier(6).withName("-----");
        PrimaryDrawerItem item7 = new PrimaryDrawerItem().withIdentifier(7).withName(
                R.string.MainActivity_exit).withIcon(GoogleMaterial.Icon.gmd_exit_to_app);
        final Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_lock).color(
                getResources().getColor(R.color.colorPrimaryDark)).sizeDp(100);
        profile_d_item = new ProfileDrawerItem().withName("me").withIcon(d1);
        // Create the AccountHeader
        main_drawer_header = new AccountHeaderBuilder().withSelectionListEnabledForSingleProfile(false).withActivity(
                this).withCompactStyle(true).addProfiles(profile_d_item).withOnAccountHeaderListener(
                new AccountHeader.OnAccountHeaderListener()
                {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile)
                    {
                        return false;
                    }
                }).build();
        // create the drawer and remember the `Drawer` result object
        main_drawer = new DrawerBuilder().withActivity(this).withInnerShadow(false).withRootView(
                R.id.drawer_container).withShowDrawerOnFirstLaunch(false).withActionBarDrawerToggleAnimated(
                true).withActionBarDrawerToggle(true).withToolbar(toolbar).addDrawerItems(item1,
                                                                                          new DividerDrawerItem(),
                                                                                          item2, item3, item4, item5,
                                                                                          new DividerDrawerItem(),
                                                                                          item6,
                                                                                          new DividerDrawerItem(),
                                                                                          item7).withTranslucentStatusBar(
                false).withAccountHeader(main_drawer_header).withOnDrawerItemClickListener(
                new Drawer.OnDrawerItemClickListener()
                {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem)
                    {
                        Log.i(TAG, "drawer:item=" + position);

                        if (position == 1)
                        {
                            // profile
                            try
                            {
                                if (Callstate.state == 0)
                                {
                                    Log.i(TAG, "start profile activity");
                                    Intent intent = new Intent(context_s, ProfileActivity.class);
                                    startActivityForResult(intent, ProfileActivity_ID);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 3)
                        {
                            // settings
                            try
                            {
                                if (Callstate.state == 0)
                                {
                                    Log.i(TAG, "start settings activity");
                                    Intent intent = new Intent(context_s, SettingsActivity.class);
                                    startActivityForResult(intent, SettingsActivity_ID);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 4)
                        {
                            // logout/login
                            try
                            {
                                if (is_tox_started)
                                {
                                    manually_log_out();
                                }
                                else
                                {
                                    manually_log_in();
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 6)
                        {
                            // About
                            try
                            {
                                Log.i(TAG, "start aboutpage activity");
                                Intent intent = new Intent(context_s, Aboutpage.class);
                                startActivityForResult(intent, AboutpageActivity_ID);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 5)
                        {
                            // Maintenance
                            try
                            {
                                Log.i(TAG, "start Maintenance activity");
                                Intent intent = new Intent(context_s, MaintenanceActivity.class);
                                startActivityForResult(intent, MaintenanceActivity_ID);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            // -- clear Glide cache --
                            // -- clear Glide cache --
                            // clearCache();
                            // -- clear Glide cache --
                            // -- clear Glide cache --
                        }
                        else if (position == 8)
                        {
                        }
                        else if (position == 10)
                        {
                            // Exit
                            try
                            {
                                if (is_tox_started)
                                {
                                    tox_service_fg.stop_tox_fg(true);
                                    tox_service_fg.stop_me(true);
                                }
                                else
                                {
                                    // just exit
                                    tox_service_fg.stop_me(true);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        return true;
                    }
                }).build();

        //        DrawerLayout drawer_layout = (DrawerLayout) findViewById(R.id.material_drawer_layout);
        //        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.faw_envelope_open, R.string.faw_envelope_open);
        //
        //        drawer_layout.setDrawerListener(drawerToggle);
        //
        //        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //        getSupportActionBar().setHomeButtonEnabled(true);
        //        drawerToggle.syncState();
        // show hambuger icon -------
        // getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        // main_drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        // show back icon -------
        // main_drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // -------- drawer ------------
        // -------- drawer ------------
        // -------- drawer ------------
        // reset calling state
        Callstate.state = 0;
        Callstate.tox_call_state = ToxVars.TOXAV_FRIEND_CALL_STATE.TOXAV_FRIEND_CALL_STATE_NONE.value;
        Callstate.call_first_video_frame_received = -1;
        Callstate.call_first_audio_frame_received = -1;
        VIDEO_FRAME_RATE_OUTGOING = 0;
        last_video_frame_sent = -1;
        VIDEO_FRAME_RATE_INCOMING = 0;
        last_video_frame_received = -1;
        count_video_frame_received = 0;
        count_video_frame_sent = 0;
        Log.i(TAG, "friend_pubkey:set:002");
        Callstate.friend_pubkey = "-1";
        Callstate.audio_speaker = true;
        Callstate.other_audio_enabled = 1;
        Callstate.other_video_enabled = 1;
        Callstate.my_audio_enabled = 1;
        Callstate.my_video_enabled = 1;

        String native_api = getNativeLibAPI();
        mt.setText(mt.getText() + "\n" + native_api);
        mt.setText(mt.getText() + "\n" + "c-toxcore:v" + tox_version_major() + "." + tox_version_minor() + "." +
                   tox_version_patch());
        mt.setText(mt.getText() + ", " + "jni-c-toxcore:v" + jnictoxcore_version());
        Log.i(TAG, "loaded:c-toxcore:v" + tox_version_major() + "." + tox_version_minor() + "." + tox_version_patch());
        Log.i(TAG, "loaded:jni-c-toxcore:v" + jnictoxcore_version());
        Log.i(TAG, "loaded:libavutil:v" + libavutil_version());

        if ((!TOX_SERVICE_STARTED) || (vfs == null))
        {
            Log.i(TAG, "M:STARTUP:init VFS");

            if (VFS_ENCRYPT)
            {
                try
                {
                    String dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_VFS_NAME;
                    File database_dir = new File(new File(dbFile).getParent());
                    database_dir.mkdirs();
                    // Log.i(TAG, "vfs:path=" + dbFile);
                    vfs = VirtualFileSystem.get();

                    try
                    {
                        if (!vfs.isMounted())
                        {
                            Log.i(TAG, "VFS:mount:[1]:start:" + Thread.currentThread().getId() + ":" +
                                       Thread.currentThread().getName());
                            vfs.mount(dbFile, PREF__DB_secrect_key);
                            Log.i(TAG, "VFS:mount:[1]:end");
                        }
                    }
                    catch (Exception ee)
                    {
                        Log.i(TAG, "vfs:EE1:" + ee.getMessage());
                        ee.printStackTrace();
                        Log.i(TAG, "VFS:mount:[2]:start:" + Thread.currentThread().getId() + ":" +
                                   Thread.currentThread().getName());
                        vfs.mount(dbFile, PREF__DB_secrect_key);
                        Log.i(TAG, "VFS:mount:[2]:end");
                    }

                    // Log.i(TAG, "vfs:open(1)=OK:path=" + dbFile);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "vfs:EE2:" + e.getMessage());
                    String dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_VFS_NAME;

                    if (DELETE_SQL_AND_VFS_ON_ERROR)
                    {
                        try
                        {
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**--------:" + dbFile);
                            new File(dbFile).delete();
                            Log.i(TAG, "vfs:**deleting database**--------:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                            Log.i(TAG, "vfs:**deleting database**:" + dbFile);
                        }
                        catch (Exception e3)
                        {
                            e3.printStackTrace();
                            Log.i(TAG, "vfs:EE3:" + e3.getMessage());
                        }
                    }

                    try
                    {
                        // Log.i(TAG, "vfs:path=" + dbFile);
                        vfs = VirtualFileSystem.get();
                        vfs.createNewContainer(dbFile, PREF__DB_secrect_key);
                        Log.i(TAG, "VFS:mount:[3]:start:" + Thread.currentThread().getId() + ":" +
                                   Thread.currentThread().getName());
                        vfs.mount(PREF__DB_secrect_key);
                        Log.i(TAG, "VFS:mount:[3]:end");
                        // Log.i(TAG, "vfs:open(2)=OK:path=" + dbFile);
                    }
                    catch (Exception e2)
                    {
                        e2.printStackTrace();
                        Log.i(TAG, "vfs:EE4:" + e.getMessage());
                    }
                }

                // Log.i(TAG, "vfs:encrypted:(1)prefix=" + VFS_PREFIX);
            }
            else
            {
                // VFS not encrypted -------------
                VFS_PREFIX = getExternalFilesDir(null).getAbsolutePath() + "/vfs/";
                // Log.i(TAG, "vfs:not_encrypted:(2)prefix=" + VFS_PREFIX);
                // VFS not encrypted -------------
            }
        }

        // cleanup temp dirs --------
        if (!TOX_SERVICE_STARTED)
        {
            Log.i(TAG, "M:STARTUP:cleanup_temp_dirs (background)");
            HelperGeneric.cleanup_temp_dirs();
        }

        // cleanup temp dirs --------
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        //        if (VFS_ENCRYPT)
        //        {
        //            if (vfs.isMounted())
        //            {
        //                vfs_listFilesAndFilesSubDirectories("/", 0, "");
        //            }
        //        }
        //        // ---------- DEBUG, just a test ----------
        //        // ---------- DEBUG, just a test ----------
        //        // ---------- DEBUG, just a test ----------
        app_files_directory = getFilesDir().getAbsolutePath();
        // --- forground service ---
        // --- forground service ---
        // --- forground service ---
        Intent i = new Intent(this, TrifaToxService.class);

        if (!TOX_SERVICE_STARTED)
        {
            Log.i(TAG, "M:STARTUP:start ToxService");
            startService(i);
        }

        if (!TOX_SERVICE_STARTED)
        {
            Log.i(TAG, "M:STARTUP:start ToxThread");
            tox_thread_start();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                Intent capture_i = new Intent(this, CaptureService.class);
                if (!TOX_SERVICE_STARTED)
                {
                    Log.i(TAG, "M:STARTUP:start CaptureService");
                    startService(capture_i);
                }
            }
        }
        else
        {
            Intent capture_i = new Intent(this, CaptureService.class);
            if (!TOX_SERVICE_STARTED)
            {
                Log.i(TAG, "M:STARTUP:start CaptureService");
                startService(capture_i);
            }
        }

        // --- forground service ---
        // --- forground service ---
        // --- forground service ---

        /*
        ////// WATCHDOG //////
        ////// WATCHDOG //////
        ////// WATCHDOG //////
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Context context_fg1 = getApplicationContext();
            Intent intent_fg1 = new Intent(this, WatchdogService.class);
            context_fg1.startForegroundService(intent_fg1);
        }
        ////// WATCHDOG //////
        ////// WATCHDOG //////
        ////// WATCHDOG //////
        */

        ///////******//////// register_for_push(this);



        PREF__normal_main_view = settings.getBoolean("normal_main_view", true);
        switch_normal_main_view.setChecked(PREF__normal_main_view);

        if (PREF__normal_main_view)
        {
            mLocationOverlay.disableMyLocation();
            map.onPause();
        }
        else
        {
            map.onResume();
            mLocationOverlay.enableMyLocation();
        }

        switch_normal_main_view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                Log.i(TAG, "switch_normal_main_view:" + isChecked);
                PREF__normal_main_view = isChecked;
                settings.edit().putBoolean("normal_main_view", isChecked).commit();

                if (!PREF__normal_main_view) {
                    waiting_container.setVisibility(View.GONE);
                    main_gallery_container.setVisibility(View.VISIBLE);
                    map.onResume();
                    mLocationOverlay.enableMyLocation();
                    // TODO: this hides the main drawer. how to fix?
                    main_gallery_container.bringToFront();
                } else {
                    waiting_container.setVisibility(View.VISIBLE);
                    main_gallery_container.setVisibility(View.GONE);
                    mLocationOverlay.disableMyLocation();
                    map.onPause();
                }
            }
        });

        if (!PREF__normal_main_view) {
            waiting_container.setVisibility(View.GONE);
            main_gallery_container.setVisibility(View.VISIBLE);
            // TODO: this hides the main drawer. how to fix?
            main_gallery_container.bringToFront();
        } else {
            waiting_container.setVisibility(View.VISIBLE);
            main_gallery_container.setVisibility(View.GONE);
        }

        Log.i(TAG, "M:STARTUP:-- DONE --");
    }

    private static void register_for_push(Context context)
    {
        List<String> distributors = UnifiedPush.getDistributors(context);
        if ((distributors != null) && (distributors.size() > 0))
        {
            if (distributors.size() == 1)
            {
                try
                {
                    String available_dist = "";
                    for (int i = 0; i < distributors.size(); i++)
                    {
                        available_dist = available_dist + distributors.get(i) + "\n";
                    }
                    Log.i(TAG, "PUSH:UnifiedPush:dists1=" + available_dist);
                }
                catch (Exception ignored)
                {
                }

                UnifiedPush.saveDistributor(context, distributors.get(0));
                UnifiedPush.registerApp(context, MainActivity.push_instance_name, "toloshare push", null);
                HelperGeneric.del_g_opts(NOTIFICATION_PROVIDER_DB_KEY);
                return;
            }

            try
            {
                String available_dist = "";
                for (int i = 0; i < distributors.size(); i++)
                {
                    available_dist = available_dist + distributors.get(i) + "\n";
                }
                Log.i(TAG, "PUSH:UnifiedPush:dists2=" + available_dist);
            }
            catch (Exception ignored)
            {
            }

            final String chosen_push_distributor = HelperGeneric.get_g_opts(NOTIFICATION_PROVIDER_DB_KEY);
            boolean found_chosen_distributor = false;

            Log.i(TAG, "PUSH:UnifiedPush:chosen_push_distributor=" + chosen_push_distributor);

            if ((chosen_push_distributor != null))
            {
                if (!distributors.isEmpty())
                {
                    try
                    {
                        for (int i = 0; i < distributors.size(); i++)
                        {
                            if (distributors.get(i).compareTo(chosen_push_distributor) == 0)
                            {
                                // the previously chosen distributor was found, register it again
                                try
                                {
                                    Log.i(TAG, "PUSH:UnifiedPush:found chosen_distributor=" + distributors.get(i));
                                }
                                catch(Exception e2)
                                {
                                }
                                found_chosen_distributor = true;
                                UnifiedPush.saveDistributor(context, distributors.get(i));
                                UnifiedPush.registerApp(context, MainActivity.push_instance_name, "toloshare push", null);
                                return;
                            }
                        }
                    }
                    catch (Exception ignored)
                    {
                        found_chosen_distributor = false;
                    }
                }
            }

            if (!found_chosen_distributor)
            {
                HelperGeneric.del_g_opts(NOTIFICATION_PROVIDER_DB_KEY);
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                if (distributors.isEmpty())
                {
                    Toast.makeText(context, "No UnifiedPush Distributors found", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    try
                    {
                        String available_dist = "";
                        for (int i = 0; i < distributors.size(); i++)
                        {
                            available_dist = available_dist + distributors.get(i) + "\n";
                        }
                        Log.i(TAG, "PUSH:UnifiedPush:dists2=" + available_dist);
                    }
                    catch (Exception ignored)
                    {
                    }

                    alert.setTitle("select_distributors");
                    String[] distributorsStr = distributors.toArray(new String[0]);
                    alert.setSingleChoiceItems(distributorsStr, -1, (dialog, item) -> {
                        String distributor = distributorsStr[item];
                        UnifiedPush.saveDistributor(context, distributor);
                        UnifiedPush.registerApp(context, MainActivity.push_instance_name, "toloshare push", null);
                        HelperGeneric.set_g_opts(NOTIFICATION_PROVIDER_DB_KEY, distributor);
                        dialog.dismiss();
                    });
                }
                alert.show();
            }
        }
    }

    void upgrade_db_schema_do(int old_version, int new_version)
    {
        if (new_version == 10015) {
            run_multi_sql("CREATE TABLE `BootstrapNodeEntryDB` (`num` INTEGER NOT NULL, `udp_node` BOOLEAN NOT NULL, `ip` TEXT NOT NULL, `port` INTEGER NOT NULL, `key_hex` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_num_on_BootstrapNodeEntryDB` ON `BootstrapNodeEntryDB` (`num`)");
            run_multi_sql("CREATE INDEX `index_udp_node_on_BootstrapNodeEntryDB` ON `BootstrapNodeEntryDB` (`udp_node`)");
            run_multi_sql("CREATE INDEX `index_ip_on_BootstrapNodeEntryDB` ON `BootstrapNodeEntryDB` (`ip`)");
            run_multi_sql("CREATE INDEX `index_port_on_BootstrapNodeEntryDB` ON `BootstrapNodeEntryDB` (`port`)");
            run_multi_sql("CREATE INDEX `index_key_hex_on_BootstrapNodeEntryDB` ON `BootstrapNodeEntryDB` (`key_hex`)");
            run_multi_sql("CREATE TABLE `ConferenceDB` (`who_invited__tox_public_key_string` TEXT NOT NULL, `name` TEXT , `peer_count` INTEGER NOT NULL DEFAULT -1, `own_peer_number` INTEGER NOT NULL DEFAULT -1, `kind` INTEGER NOT NULL DEFAULT 0, `tox_conference_number` INTEGER NOT NULL DEFAULT -1, `conference_active` BOOLEAN NOT NULL DEFAULT false, `notification_silent` BOOLEAN DEFAULT false, `conference_identifier` TEXT PRIMARY KEY)");
            run_multi_sql("CREATE INDEX `index_who_invited__tox_public_key_string_on_ConferenceDB` ON `ConferenceDB` (`who_invited__tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_name_on_ConferenceDB` ON `ConferenceDB` (`name`)");
            run_multi_sql("CREATE INDEX `index_peer_count_on_ConferenceDB` ON `ConferenceDB` (`peer_count`)");
            run_multi_sql("CREATE INDEX `index_own_peer_number_on_ConferenceDB` ON `ConferenceDB` (`own_peer_number`)");
            run_multi_sql("CREATE INDEX `index_kind_on_ConferenceDB` ON `ConferenceDB` (`kind`)");
            run_multi_sql("CREATE INDEX `index_tox_conference_number_on_ConferenceDB` ON `ConferenceDB` (`tox_conference_number`)");
            run_multi_sql("CREATE INDEX `index_conference_active_on_ConferenceDB` ON `ConferenceDB` (`conference_active`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_ConferenceDB` ON `ConferenceDB` (`notification_silent`)");
            run_multi_sql("CREATE TABLE `ConferenceMessage` (`conference_identifier` TEXT NOT NULL DEFAULT -1, `tox_peerpubkey` TEXT NOT NULL, `tox_peername` TEXT , `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER , `rcvd_timestamp` INTEGER , `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT , `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_conference_identifier_on_ConferenceMessage` ON `ConferenceMessage` (`conference_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_peerpubkey_on_ConferenceMessage` ON `ConferenceMessage` (`tox_peerpubkey`)");
            run_multi_sql("CREATE INDEX `index_tox_peername_on_ConferenceMessage` ON `ConferenceMessage` (`tox_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_ConferenceMessage` ON `ConferenceMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_ConferenceMessage` ON `ConferenceMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_ConferenceMessage` ON `ConferenceMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_ConferenceMessage` ON `ConferenceMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_ConferenceMessage` ON `ConferenceMessage` (`is_new`)");
            run_multi_sql("CREATE TABLE `ConferencePeerCacheDB` (`conference_identifier` TEXT NOT NULL, `peer_pubkey` TEXT NOT NULL, `peer_name` TEXT NOT NULL, `last_update_timestamp` INTEGER NOT NULL DEFAULT -1, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE UNIQUE INDEX `index_conference_identifier_peer_pubkey_on_ConferencePeerCacheDB` ON `ConferencePeerCacheDB` (`conference_identifier`, `peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_conference_identifier_on_ConferencePeerCacheDB` ON `ConferencePeerCacheDB` (`conference_identifier`)");
            run_multi_sql("CREATE INDEX `index_peer_pubkey_on_ConferencePeerCacheDB` ON `ConferencePeerCacheDB` (`peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_peer_name_on_ConferencePeerCacheDB` ON `ConferencePeerCacheDB` (`peer_name`)");
            run_multi_sql("CREATE INDEX `index_last_update_timestamp_on_ConferencePeerCacheDB` ON `ConferencePeerCacheDB` (`last_update_timestamp`)");
            run_multi_sql("CREATE TABLE `FileDB` (`kind` INTEGER NOT NULL, `direction` INTEGER NOT NULL, `tox_public_key_string` TEXT NOT NULL, `path_name` TEXT NOT NULL, `file_name` TEXT NOT NULL, `filesize` INTEGER NOT NULL DEFAULT -1, `is_in_VFS` BOOLEAN NOT NULL DEFAULT true, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_kind_on_FileDB` ON `FileDB` (`kind`)");
            run_multi_sql("CREATE INDEX `index_direction_on_FileDB` ON `FileDB` (`direction`)");
            run_multi_sql("CREATE INDEX `index_tox_public_key_string_on_FileDB` ON `FileDB` (`tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_FileDB` ON `FileDB` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_FileDB` ON `FileDB` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_FileDB` ON `FileDB` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_is_in_VFS_on_FileDB` ON `FileDB` (`is_in_VFS`)");
            run_multi_sql("CREATE TABLE `Filetransfer` (`tox_public_key_string` TEXT NOT NULL, `direction` INTEGER NOT NULL, `file_number` INTEGER NOT NULL, `kind` INTEGER NOT NULL, `state` INTEGER NOT NULL, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `path_name` TEXT NOT NULL, `file_name` TEXT NOT NULL, `fos_open` BOOLEAN NOT NULL DEFAULT false, `filesize` INTEGER NOT NULL DEFAULT -1, `current_position` INTEGER NOT NULL DEFAULT 0, `message_id` INTEGER NOT NULL DEFAULT -1, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_tox_public_key_string_on_Filetransfer` ON `Filetransfer` (`tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Filetransfer` ON `Filetransfer` (`direction`)");
            run_multi_sql("CREATE INDEX `index_file_number_on_Filetransfer` ON `Filetransfer` (`file_number`)");
            run_multi_sql("CREATE INDEX `index_kind_on_Filetransfer` ON `Filetransfer` (`kind`)");
            run_multi_sql("CREATE INDEX `index_state_on_Filetransfer` ON `Filetransfer` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Filetransfer` ON `Filetransfer` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Filetransfer` ON `Filetransfer` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_Filetransfer` ON `Filetransfer` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_Filetransfer` ON `Filetransfer` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_message_id_on_Filetransfer` ON `Filetransfer` (`message_id`)");
            run_multi_sql("CREATE TABLE `FriendList` (`name` TEXT , `alias_name` TEXT , `status_message` TEXT , `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT , `avatar_filename` TEXT , `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT -1, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE TABLE `Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT -1, `filetransfer_id` INTEGER NOT NULL DEFAULT -1, `sent_timestamp` INTEGER , `rcvd_timestamp` INTEGER , `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT , `filename_fullpath` TEXT , `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE TABLE `TRIFADatabaseGlobals` (`key` TEXT NOT NULL, `value` TEXT NOT NULL)");
            run_multi_sql("CREATE INDEX `index_key_on_TRIFADatabaseGlobals` ON `TRIFADatabaseGlobals` (`key`)");
            run_multi_sql("CREATE INDEX `index_value_on_TRIFADatabaseGlobals` ON `TRIFADatabaseGlobals` (`value`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `filename_fullpath`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `filename_fullpath`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
        }

        if (new_version == 10082) {
            run_multi_sql("CREATE TABLE `__temp_ConferenceMessage` (`conference_identifier` TEXT NOT NULL DEFAULT - 1, `tox_peerpubkey` TEXT NOT NULL, `tox_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_ConferenceMessage` (`conference_identifier`, `tox_peerpubkey`, `tox_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `id`) SELECT `conference_identifier`, `tox_peerpubkey`, `tox_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `id` FROM `ConferenceMessage`");
            run_multi_sql("DROP TABLE `ConferenceMessage`");
            run_multi_sql("ALTER TABLE `__temp_ConferenceMessage` RENAME TO `ConferenceMessage`");
            run_multi_sql("CREATE INDEX `index_conference_identifier_on_ConferenceMessage` ON `ConferenceMessage` (`conference_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_peerpubkey_on_ConferenceMessage` ON `ConferenceMessage` (`tox_peerpubkey`)");
            run_multi_sql("CREATE INDEX `index_tox_peername_on_ConferenceMessage` ON `ConferenceMessage` (`tox_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_ConferenceMessage` ON `ConferenceMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_ConferenceMessage` ON `ConferenceMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_ConferenceMessage` ON `ConferenceMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_ConferenceMessage` ON `ConferenceMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_ConferenceMessage` ON `ConferenceMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_ConferenceMessage` ON `ConferenceMessage` (`was_synced`)");
        }

        if (new_version == 10015) {
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
        }

        if (new_version == 10084) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `last_online_timestamp_real` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `added_timestamp`, `is_relay`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `added_timestamp`, `is_relay`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_real_on_FriendList` ON `FriendList` (`last_online_timestamp_real`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
        }

        if (new_version == 10086) {
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
        }

        if (new_version == 10025) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_on_off`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `notification_silent`, `sort`, `last_online_timestamp`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_on_off`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `notification_silent`, `sort`, `last_online_timestamp`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
            run_multi_sql("CREATE TABLE `RelayListDB` (`TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `own_relay` BOOLEAN NOT NULL DEFAULT false, `last_online_timestamp` INTEGER NOT NULL DEFAULT -1, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_RelayListDB` ON `RelayListDB` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_RelayListDB` ON `RelayListDB` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_own_relay_on_RelayListDB` ON `RelayListDB` (`own_relay`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_RelayListDB` ON `RelayListDB` (`last_online_timestamp`)");
        }

        if (new_version == 10093) {
            run_multi_sql("CREATE TABLE `__temp_ConferenceMessage` (`message_id_tox` TEXT, `conference_identifier` TEXT NOT NULL DEFAULT - 1, `tox_peerpubkey` TEXT NOT NULL, `tox_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_ConferenceMessage` (`conference_identifier`, `tox_peerpubkey`, `tox_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `id`) SELECT `conference_identifier`, `tox_peerpubkey`, `tox_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `id` FROM `ConferenceMessage`");
            run_multi_sql("DROP TABLE `ConferenceMessage`");
            run_multi_sql("ALTER TABLE `__temp_ConferenceMessage` RENAME TO `ConferenceMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_ConferenceMessage` ON `ConferenceMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_conference_identifier_on_ConferenceMessage` ON `ConferenceMessage` (`conference_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_peerpubkey_on_ConferenceMessage` ON `ConferenceMessage` (`tox_peerpubkey`)");
            run_multi_sql("CREATE INDEX `index_tox_peername_on_ConferenceMessage` ON `ConferenceMessage` (`tox_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_ConferenceMessage` ON `ConferenceMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_ConferenceMessage` ON `ConferenceMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_ConferenceMessage` ON `ConferenceMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_ConferenceMessage` ON `ConferenceMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_ConferenceMessage` ON `ConferenceMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_ConferenceMessage` ON `ConferenceMessage` (`was_synced`)");
        }

        if (new_version == 10025) {
            run_multi_sql("CREATE TABLE `__temp_RelayListDB` (`TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `own_relay` BOOLEAN NOT NULL DEFAULT false, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `tox_public_key_string_of_owner` TEXT, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_RelayListDB` (`TOX_CONNECTION`, `TOX_CONNECTION_on_off`, `own_relay`, `last_online_timestamp`, `tox_public_key_string`) SELECT `TOX_CONNECTION`, `TOX_CONNECTION_on_off`, `own_relay`, `last_online_timestamp`, `tox_public_key_string` FROM `RelayListDB`");
            run_multi_sql("DROP TABLE `RelayListDB`");
            run_multi_sql("ALTER TABLE `__temp_RelayListDB` RENAME TO `RelayListDB`");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_RelayListDB` ON `RelayListDB` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_RelayListDB` ON `RelayListDB` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_own_relay_on_RelayListDB` ON `RelayListDB` (`own_relay`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_RelayListDB` ON `RelayListDB` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_tox_public_key_string_of_owner_on_RelayListDB` ON `RelayListDB` (`tox_public_key_string_of_owner`)");
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_on_off`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_on_off`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
        }

        if (new_version == 10026) {
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
        }

        if (new_version == 10096) {
            run_multi_sql("CREATE TABLE `__temp_Filetransfer` (`tox_public_key_string` TEXT NOT NULL, `direction` INTEGER NOT NULL, `file_number` INTEGER NOT NULL, `kind` INTEGER NOT NULL, `state` INTEGER NOT NULL, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `path_name` TEXT NOT NULL, `file_name` TEXT NOT NULL, `fos_open` BOOLEAN NOT NULL DEFAULT false, `filesize` INTEGER NOT NULL DEFAULT - 1, `current_position` INTEGER NOT NULL DEFAULT 0, `message_id` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Filetransfer` (`tox_public_key_string`, `direction`, `file_number`, `kind`, `state`, `ft_accepted`, `ft_outgoing_started`, `path_name`, `file_name`, `fos_open`, `filesize`, `current_position`, `message_id`, `id`) SELECT `tox_public_key_string`, `direction`, `file_number`, `kind`, `state`, `ft_accepted`, `ft_outgoing_started`, `path_name`, `file_name`, `fos_open`, `filesize`, `current_position`, `message_id`, `id` FROM `Filetransfer`");
            run_multi_sql("DROP TABLE `Filetransfer`");
            run_multi_sql("ALTER TABLE `__temp_Filetransfer` RENAME TO `Filetransfer`");
            run_multi_sql("CREATE INDEX `index_tox_public_key_string_on_Filetransfer` ON `Filetransfer` (`tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Filetransfer` ON `Filetransfer` (`direction`)");
            run_multi_sql("CREATE INDEX `index_file_number_on_Filetransfer` ON `Filetransfer` (`file_number`)");
            run_multi_sql("CREATE INDEX `index_kind_on_Filetransfer` ON `Filetransfer` (`kind`)");
            run_multi_sql("CREATE INDEX `index_state_on_Filetransfer` ON `Filetransfer` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Filetransfer` ON `Filetransfer` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Filetransfer` ON `Filetransfer` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_Filetransfer` ON `Filetransfer` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_Filetransfer` ON `Filetransfer` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_message_id_on_Filetransfer` ON `Filetransfer` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Filetransfer` ON `Filetransfer` (`storage_frame_work`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
        }

        if (new_version == 10028) {
            run_multi_sql("CREATE TABLE `TRIFADatabaseGlobalsNew` (`value` TEXT NOT NULL, `key` TEXT PRIMARY KEY)");
            run_multi_sql("CREATE INDEX `index_value_on_TRIFADatabaseGlobalsNew` ON `TRIFADatabaseGlobalsNew` (`value`)");
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
        }

        if (new_version == 10099) {
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
        }

        if (new_version == 10028) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
        }

        if (new_version == 10101) {
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
        }

        if (new_version == 10079) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `is_relay`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
        }

        if (new_version == 10112) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `last_online_timestamp_real` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `push_url` TEXT, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_real_on_FriendList` ON `FriendList` (`last_online_timestamp_real`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
            run_multi_sql("CREATE INDEX `index_push_url_on_FriendList` ON `FriendList` (`push_url`)");
        }

        if (new_version == 10124) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `last_online_timestamp_real` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `push_url` TEXT, `capabilities` INTEGER NOT NULL DEFAULT 0, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_real_on_FriendList` ON `FriendList` (`last_online_timestamp_real`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
            run_multi_sql("CREATE INDEX `index_push_url_on_FriendList` ON `FriendList` (`push_url`)");
            run_multi_sql("CREATE INDEX `index_capabilities_on_FriendList` ON `FriendList` (`capabilities`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `sent_push` BOOLEAN, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `sent_push` INTEGER, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `last_online_timestamp_real` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `push_url` TEXT, `capabilities` INTEGER NOT NULL DEFAULT 0, `msgv3_capability` INTEGER NOT NULL DEFAULT 0, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `capabilities`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `capabilities`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_real_on_FriendList` ON `FriendList` (`last_online_timestamp_real`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
            run_multi_sql("CREATE INDEX `index_push_url_on_FriendList` ON `FriendList` (`push_url`)");
            run_multi_sql("CREATE INDEX `index_capabilities_on_FriendList` ON `FriendList` (`capabilities`)");
            run_multi_sql("CREATE INDEX `index_msgv3_capability_on_FriendList` ON `FriendList` (`msgv3_capability`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 5, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `sent_push` INTEGER, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 2, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `sent_push` INTEGER, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
        }

        if (new_version == 10127) {
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 4, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `sent_push` INTEGER, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
        }

        if (new_version == 10154) {
            run_multi_sql("CREATE TABLE `GroupDB` (`who_invited__tox_public_key_string` TEXT NOT NULL, `name` TEXT , `peer_count` INTEGER NOT NULL DEFAULT -1, `own_peer_number` INTEGER NOT NULL DEFAULT -1, `privacy_state` INTEGER NOT NULL DEFAULT 0, `tox_group_number` INTEGER NOT NULL DEFAULT -1, `notification_silent` BOOLEAN DEFAULT false, `group_identifier` TEXT PRIMARY KEY)");
            run_multi_sql("CREATE INDEX `index_who_invited__tox_public_key_string_on_GroupDB` ON `GroupDB` (`who_invited__tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_name_on_GroupDB` ON `GroupDB` (`name`)");
            run_multi_sql("CREATE INDEX `index_peer_count_on_GroupDB` ON `GroupDB` (`peer_count`)");
            run_multi_sql("CREATE INDEX `index_own_peer_number_on_GroupDB` ON `GroupDB` (`own_peer_number`)");
            run_multi_sql("CREATE INDEX `index_privacy_state_on_GroupDB` ON `GroupDB` (`privacy_state`)");
            run_multi_sql("CREATE INDEX `index_tox_group_number_on_GroupDB` ON `GroupDB` (`tox_group_number`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_GroupDB` ON `GroupDB` (`notification_silent`)");
            run_multi_sql("CREATE TABLE `GroupMessage` (`message_id_tox` TEXT , `group_identifier` TEXT NOT NULL DEFAULT -1, `tox_group_peer_pubkey` TEXT NOT NULL, `tox_group_peername` TEXT , `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER , `rcvd_timestamp` INTEGER , `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT , `was_synced` BOOLEAN , `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE TABLE `__temp_GroupDB` (`who_invited__tox_public_key_string` TEXT NOT NULL, `name` TEXT, `peer_count` INTEGER NOT NULL DEFAULT - 1, `own_peer_number` INTEGER NOT NULL DEFAULT - 1, `privacy_state` INTEGER NOT NULL DEFAULT 0, `tox_group_number` INTEGER NOT NULL DEFAULT - 1, `group_active` BOOLEAN NOT NULL DEFAULT false, `notification_silent` BOOLEAN DEFAULT false, `group_identifier` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_GroupDB` (`who_invited__tox_public_key_string`, `name`, `peer_count`, `own_peer_number`, `privacy_state`, `tox_group_number`, `notification_silent`, `group_identifier`) SELECT `who_invited__tox_public_key_string`, `name`, `peer_count`, `own_peer_number`, `privacy_state`, `tox_group_number`, `notification_silent`, `group_identifier` FROM `GroupDB`");
            run_multi_sql("DROP TABLE `GroupDB`");
            run_multi_sql("ALTER TABLE `__temp_GroupDB` RENAME TO `GroupDB`");
            run_multi_sql("CREATE INDEX `index_who_invited__tox_public_key_string_on_GroupDB` ON `GroupDB` (`who_invited__tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_name_on_GroupDB` ON `GroupDB` (`name`)");
            run_multi_sql("CREATE INDEX `index_peer_count_on_GroupDB` ON `GroupDB` (`peer_count`)");
            run_multi_sql("CREATE INDEX `index_own_peer_number_on_GroupDB` ON `GroupDB` (`own_peer_number`)");
            run_multi_sql("CREATE INDEX `index_privacy_state_on_GroupDB` ON `GroupDB` (`privacy_state`)");
            run_multi_sql("CREATE INDEX `index_tox_group_number_on_GroupDB` ON `GroupDB` (`tox_group_number`)");
            run_multi_sql("CREATE INDEX `index_group_active_on_GroupDB` ON `GroupDB` (`group_active`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_GroupDB` ON `GroupDB` (`notification_silent`)");
            run_multi_sql("CREATE TABLE `__temp_GroupDB` (`who_invited__tox_public_key_string` TEXT NOT NULL, `name` TEXT, `topic` TEXT, `peer_count` INTEGER NOT NULL DEFAULT - 1, `own_peer_number` INTEGER NOT NULL DEFAULT - 1, `privacy_state` INTEGER NOT NULL DEFAULT 0, `tox_group_number` INTEGER NOT NULL DEFAULT - 1, `group_active` BOOLEAN NOT NULL DEFAULT false, `notification_silent` BOOLEAN DEFAULT false, `group_identifier` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_GroupDB` (`who_invited__tox_public_key_string`, `name`, `peer_count`, `own_peer_number`, `privacy_state`, `tox_group_number`, `group_active`, `notification_silent`, `group_identifier`) SELECT `who_invited__tox_public_key_string`, `name`, `peer_count`, `own_peer_number`, `privacy_state`, `tox_group_number`, `group_active`, `notification_silent`, `group_identifier` FROM `GroupDB`");
            run_multi_sql("DROP TABLE `GroupDB`");
            run_multi_sql("ALTER TABLE `__temp_GroupDB` RENAME TO `GroupDB`");
            run_multi_sql("CREATE INDEX `index_who_invited__tox_public_key_string_on_GroupDB` ON `GroupDB` (`who_invited__tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_name_on_GroupDB` ON `GroupDB` (`name`)");
            run_multi_sql("CREATE INDEX `index_topic_on_GroupDB` ON `GroupDB` (`topic`)");
            run_multi_sql("CREATE INDEX `index_peer_count_on_GroupDB` ON `GroupDB` (`peer_count`)");
            run_multi_sql("CREATE INDEX `index_own_peer_number_on_GroupDB` ON `GroupDB` (`own_peer_number`)");
            run_multi_sql("CREATE INDEX `index_privacy_state_on_GroupDB` ON `GroupDB` (`privacy_state`)");
            run_multi_sql("CREATE INDEX `index_tox_group_number_on_GroupDB` ON `GroupDB` (`tox_group_number`)");
            run_multi_sql("CREATE INDEX `index_group_active_on_GroupDB` ON `GroupDB` (`group_active`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_GroupDB` ON `GroupDB` (`notification_silent`)");
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `msg_id_hash` TEXT, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
        }

        if (new_version == 10157) {
            run_multi_sql("CREATE TABLE `__temp_Filetransfer` (`tox_public_key_string` TEXT NOT NULL, `direction` INTEGER NOT NULL, `file_number` INTEGER NOT NULL, `kind` INTEGER NOT NULL, `state` INTEGER NOT NULL, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `path_name` TEXT NOT NULL, `file_name` TEXT NOT NULL, `fos_open` BOOLEAN NOT NULL DEFAULT false, `filesize` INTEGER NOT NULL DEFAULT - 1, `current_position` INTEGER NOT NULL DEFAULT 0, `message_id` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `tox_file_id_hex` TEXT, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Filetransfer` (`tox_public_key_string`, `direction`, `file_number`, `kind`, `state`, `ft_accepted`, `ft_outgoing_started`, `path_name`, `file_name`, `fos_open`, `filesize`, `current_position`, `message_id`, `storage_frame_work`, `id`) SELECT `tox_public_key_string`, `direction`, `file_number`, `kind`, `state`, `ft_accepted`, `ft_outgoing_started`, `path_name`, `file_name`, `fos_open`, `filesize`, `current_position`, `message_id`, `storage_frame_work`, `id` FROM `Filetransfer`");
            run_multi_sql("DROP TABLE `Filetransfer`");
            run_multi_sql("ALTER TABLE `__temp_Filetransfer` RENAME TO `Filetransfer`");
            run_multi_sql("CREATE INDEX `index_tox_public_key_string_on_Filetransfer` ON `Filetransfer` (`tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Filetransfer` ON `Filetransfer` (`direction`)");
            run_multi_sql("CREATE INDEX `index_file_number_on_Filetransfer` ON `Filetransfer` (`file_number`)");
            run_multi_sql("CREATE INDEX `index_kind_on_Filetransfer` ON `Filetransfer` (`kind`)");
            run_multi_sql("CREATE INDEX `index_state_on_Filetransfer` ON `Filetransfer` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Filetransfer` ON `Filetransfer` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Filetransfer` ON `Filetransfer` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_Filetransfer` ON `Filetransfer` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_Filetransfer` ON `Filetransfer` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_message_id_on_Filetransfer` ON `Filetransfer` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Filetransfer` ON `Filetransfer` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_tox_file_id_hex_on_Filetransfer` ON `Filetransfer` (`tox_file_id_hex`)");
            run_multi_sql("CREATE TABLE `__temp_Message` (`message_id` INTEGER NOT NULL, `tox_friendpubkey` TEXT NOT NULL, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `state` INTEGER NOT NULL DEFAULT 1, `ft_accepted` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_started` BOOLEAN NOT NULL DEFAULT false, `filedb_id` INTEGER NOT NULL DEFAULT - 1, `filetransfer_id` INTEGER NOT NULL DEFAULT - 1, `sent_timestamp` INTEGER DEFAULT 0, `sent_timestamp_ms` INTEGER DEFAULT 0, `rcvd_timestamp` INTEGER DEFAULT 0, `rcvd_timestamp_ms` INTEGER DEFAULT 0, `read` BOOLEAN NOT NULL, `send_retries` INTEGER NOT NULL DEFAULT 0, `is_new` BOOLEAN NOT NULL, `text` TEXT, `filename_fullpath` TEXT, `msg_id_hash` TEXT, `raw_msgv2_bytes` TEXT, `msg_version` INTEGER NOT NULL DEFAULT 0, `resend_count` INTEGER NOT NULL DEFAULT 4, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `ft_outgoing_queued` BOOLEAN NOT NULL DEFAULT false, `msg_at_relay` BOOLEAN NOT NULL DEFAULT false, `msg_idv3_hash` TEXT, `sent_push` INTEGER, `filetransfer_kind` INTEGER DEFAULT 0, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_Message` (`message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id`) SELECT `message_id`, `tox_friendpubkey`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `state`, `ft_accepted`, `ft_outgoing_started`, `filedb_id`, `filetransfer_id`, `sent_timestamp`, `sent_timestamp_ms`, `rcvd_timestamp`, `rcvd_timestamp_ms`, `read`, `send_retries`, `is_new`, `text`, `filename_fullpath`, `msg_id_hash`, `raw_msgv2_bytes`, `msg_version`, `resend_count`, `storage_frame_work`, `ft_outgoing_queued`, `msg_at_relay`, `msg_idv3_hash`, `sent_push`, `id` FROM `Message`");
            run_multi_sql("DROP TABLE `Message`");
            run_multi_sql("ALTER TABLE `__temp_Message` RENAME TO `Message`");
            run_multi_sql("CREATE INDEX `index_message_id_on_Message` ON `Message` (`message_id`)");
            run_multi_sql("CREATE INDEX `index_tox_friendpubkey_on_Message` ON `Message` (`tox_friendpubkey`)");
            run_multi_sql("CREATE INDEX `index_direction_on_Message` ON `Message` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_Message` ON `Message` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_Message` ON `Message` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_state_on_Message` ON `Message` (`state`)");
            run_multi_sql("CREATE INDEX `index_ft_accepted_on_Message` ON `Message` (`ft_accepted`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_started_on_Message` ON `Message` (`ft_outgoing_started`)");
            run_multi_sql("CREATE INDEX `index_filedb_id_on_Message` ON `Message` (`filedb_id`)");
            run_multi_sql("CREATE INDEX `index_filetransfer_id_on_Message` ON `Message` (`filetransfer_id`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_Message` ON `Message` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_ms_on_Message` ON `Message` (`rcvd_timestamp_ms`)");
            run_multi_sql("CREATE INDEX `index_send_retries_on_Message` ON `Message` (`send_retries`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_Message` ON `Message` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_text_on_Message` ON `Message` (`text`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_Message` ON `Message` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_raw_msgv2_bytes_on_Message` ON `Message` (`raw_msgv2_bytes`)");
            run_multi_sql("CREATE INDEX `index_msg_version_on_Message` ON `Message` (`msg_version`)");
            run_multi_sql("CREATE INDEX `index_resend_count_on_Message` ON `Message` (`resend_count`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_Message` ON `Message` (`storage_frame_work`)");
            run_multi_sql("CREATE INDEX `index_ft_outgoing_queued_on_Message` ON `Message` (`ft_outgoing_queued`)");
            run_multi_sql("CREATE INDEX `index_msg_at_relay_on_Message` ON `Message` (`msg_at_relay`)");
            run_multi_sql("CREATE INDEX `index_msg_idv3_hash_on_Message` ON `Message` (`msg_idv3_hash`)");
        }

        if (new_version == 10172) {
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `msg_id_hash`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `msg_id_hash`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
        }

        if (new_version == 10189) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_ftid_hex` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `last_online_timestamp_real` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `push_url` TEXT, `capabilities` INTEGER NOT NULL DEFAULT 0, `msgv3_capability` INTEGER NOT NULL DEFAULT 0, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `capabilities`, `msgv3_capability`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `capabilities`, `msgv3_capability`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_ftid_hex_on_FriendList` ON `FriendList` (`avatar_ftid_hex`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_real_on_FriendList` ON `FriendList` (`last_online_timestamp_real`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
            run_multi_sql("CREATE INDEX `index_push_url_on_FriendList` ON `FriendList` (`push_url`)");
            run_multi_sql("CREATE INDEX `index_capabilities_on_FriendList` ON `FriendList` (`capabilities`)");
            run_multi_sql("CREATE INDEX `index_msgv3_capability_on_FriendList` ON `FriendList` (`msgv3_capability`)");
        }

        if (new_version == 10195) {
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `path_name` TEXT, `file_name` TEXT, `filename_fullpath` TEXT, `filesize` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_GroupMessage` ON `GroupMessage` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_GroupMessage` ON `GroupMessage` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_GroupMessage` ON `GroupMessage` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_GroupMessage` ON `GroupMessage` (`storage_frame_work`)");
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `TRIFA_SYNC_TYPE` INTEGER, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `path_name` TEXT, `file_name` TEXT, `filename_fullpath` TEXT, `filesize` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_SYNC_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_SYNC_TYPE`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_GroupMessage` ON `GroupMessage` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_GroupMessage` ON `GroupMessage` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_GroupMessage` ON `GroupMessage` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_GroupMessage` ON `GroupMessage` (`storage_frame_work`)");
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `TRIFA_SYNC_TYPE` INTEGER, `sync_confirmations` INTEGER NOT NULL DEFAULT 0, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `path_name` TEXT, `file_name` TEXT, `filename_fullpath` TEXT, `filesize` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_SYNC_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_SYNC_TYPE`)");
            run_multi_sql("CREATE INDEX `index_sync_confirmations_on_GroupMessage` ON `GroupMessage` (`sync_confirmations`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_GroupMessage` ON `GroupMessage` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_GroupMessage` ON `GroupMessage` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_GroupMessage` ON `GroupMessage` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_GroupMessage` ON `GroupMessage` (`storage_frame_work`)");
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `TRIFA_SYNC_TYPE` INTEGER, `sync_confirmations` INTEGER NOT NULL DEFAULT 0, `tox_group_peer_pubkey_syncer_01` TEXT, `tox_group_peer_pubkey_syncer_02` TEXT, `tox_group_peer_pubkey_syncer_03` TEXT, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `path_name` TEXT, `file_name` TEXT, `filename_fullpath` TEXT, `filesize` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `sync_confirmations`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `sync_confirmations`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_SYNC_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_SYNC_TYPE`)");
            run_multi_sql("CREATE INDEX `index_sync_confirmations_on_GroupMessage` ON `GroupMessage` (`sync_confirmations`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_01_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_01`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_02_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_02`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_03_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_03`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_GroupMessage` ON `GroupMessage` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_GroupMessage` ON `GroupMessage` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_GroupMessage` ON `GroupMessage` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_GroupMessage` ON `GroupMessage` (`storage_frame_work`)");
        }

        if (new_version == 10203) {
            run_multi_sql("CREATE TABLE `GroupPeerDB` (`group_identifier` TEXT NOT NULL, `tox_group_peer_pubkey` TEXT NOT NULL, `peer_name` TEXT , `last_update_timestamp` INTEGER NOT NULL DEFAULT -1, `first_join_timestamp` INTEGER NOT NULL DEFAULT -1, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupPeerDB` ON `GroupPeerDB` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupPeerDB` ON `GroupPeerDB` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_peer_name_on_GroupPeerDB` ON `GroupPeerDB` (`peer_name`)");
            run_multi_sql("CREATE INDEX `index_last_update_timestamp_on_GroupPeerDB` ON `GroupPeerDB` (`last_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_first_join_timestamp_on_GroupPeerDB` ON `GroupPeerDB` (`first_join_timestamp`)");
            run_multi_sql("CREATE UNIQUE INDEX `index_group_identifier_tox_group_peer_pubkey_on_GroupPeerDB` ON `GroupPeerDB` (`group_identifier`, `tox_group_peer_pubkey`)");
        }

        if (new_version == 10206) {
            run_multi_sql("CREATE TABLE `__temp_GroupPeerDB` (`group_identifier` TEXT NOT NULL, `tox_group_peer_pubkey` TEXT NOT NULL, `peer_name` TEXT, `last_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `first_join_timestamp` INTEGER NOT NULL DEFAULT - 1, `Tox_Group_Role` INTEGER NOT NULL DEFAULT 2, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupPeerDB` (`group_identifier`, `tox_group_peer_pubkey`, `peer_name`, `last_update_timestamp`, `first_join_timestamp`, `id`) SELECT `group_identifier`, `tox_group_peer_pubkey`, `peer_name`, `last_update_timestamp`, `first_join_timestamp`, `id` FROM `GroupPeerDB`");
            run_multi_sql("DROP TABLE `GroupPeerDB`");
            run_multi_sql("ALTER TABLE `__temp_GroupPeerDB` RENAME TO `GroupPeerDB`");
            run_multi_sql("CREATE UNIQUE INDEX `index_group_identifier_tox_group_peer_pubkey_on_GroupPeerDB` ON `GroupPeerDB` (`group_identifier`, `tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupPeerDB` ON `GroupPeerDB` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupPeerDB` ON `GroupPeerDB` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_peer_name_on_GroupPeerDB` ON `GroupPeerDB` (`peer_name`)");
            run_multi_sql("CREATE INDEX `index_last_update_timestamp_on_GroupPeerDB` ON `GroupPeerDB` (`last_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_first_join_timestamp_on_GroupPeerDB` ON `GroupPeerDB` (`first_join_timestamp`)");
            run_multi_sql("CREATE INDEX `index_Tox_Group_Role_on_GroupPeerDB` ON `GroupPeerDB` (`Tox_Group_Role`)");
        }

        if (new_version == 10221) {
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `TRIFA_SYNC_TYPE` INTEGER, `sync_confirmations` INTEGER NOT NULL DEFAULT 0, `tox_group_peer_pubkey_syncer_01` TEXT, `tox_group_peer_pubkey_syncer_02` TEXT, `tox_group_peer_pubkey_syncer_03` TEXT, `tox_group_peer_pubkey_syncer_01_sent_timestamp` INTEGER, `tox_group_peer_pubkey_syncer_02_sent_timestamp` INTEGER, `tox_group_peer_pubkey_syncer_03_sent_timestamp` INTEGER, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `path_name` TEXT, `file_name` TEXT, `filename_fullpath` TEXT, `filesize` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `sync_confirmations`, `tox_group_peer_pubkey_syncer_01`, `tox_group_peer_pubkey_syncer_02`, `tox_group_peer_pubkey_syncer_03`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `sync_confirmations`, `tox_group_peer_pubkey_syncer_01`, `tox_group_peer_pubkey_syncer_02`, `tox_group_peer_pubkey_syncer_03`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_SYNC_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_SYNC_TYPE`)");
            run_multi_sql("CREATE INDEX `index_sync_confirmations_on_GroupMessage` ON `GroupMessage` (`sync_confirmations`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_01_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_01`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_02_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_02`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_03_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_03`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_01_sent_timestamp_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_01_sent_timestamp`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_02_sent_timestamp_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_02_sent_timestamp`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_03_sent_timestamp_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_03_sent_timestamp`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_GroupMessage` ON `GroupMessage` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_GroupMessage` ON `GroupMessage` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_GroupMessage` ON `GroupMessage` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_GroupMessage` ON `GroupMessage` (`storage_frame_work`)");
        }

        if (new_version == 10226) {
            run_multi_sql("CREATE TABLE `__temp_FriendList` (`name` TEXT, `alias_name` TEXT, `status_message` TEXT, `TOX_CONNECTION` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_real` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off` INTEGER NOT NULL DEFAULT 0, `TOX_CONNECTION_on_off_real` INTEGER NOT NULL DEFAULT 0, `TOX_USER_STATUS` INTEGER NOT NULL DEFAULT 0, `avatar_pathname` TEXT, `avatar_filename` TEXT, `avatar_ftid_hex` TEXT, `avatar_update` BOOLEAN DEFAULT false, `avatar_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `notification_silent` BOOLEAN DEFAULT false, `sort` INTEGER NOT NULL DEFAULT 0, `last_online_timestamp` INTEGER NOT NULL DEFAULT - 1, `last_online_timestamp_real` INTEGER NOT NULL DEFAULT - 1, `added_timestamp` INTEGER NOT NULL DEFAULT - 1, `is_relay` BOOLEAN DEFAULT false, `push_url` TEXT, `ip_addr_str` TEXT, `capabilities` INTEGER NOT NULL DEFAULT 0, `msgv3_capability` INTEGER NOT NULL DEFAULT 0, `tox_public_key_string` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_FriendList` (`name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_ftid_hex`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `capabilities`, `msgv3_capability`, `tox_public_key_string`) SELECT `name`, `alias_name`, `status_message`, `TOX_CONNECTION`, `TOX_CONNECTION_real`, `TOX_CONNECTION_on_off`, `TOX_CONNECTION_on_off_real`, `TOX_USER_STATUS`, `avatar_pathname`, `avatar_filename`, `avatar_ftid_hex`, `avatar_update`, `avatar_update_timestamp`, `notification_silent`, `sort`, `last_online_timestamp`, `last_online_timestamp_real`, `added_timestamp`, `is_relay`, `push_url`, `capabilities`, `msgv3_capability`, `tox_public_key_string` FROM `FriendList`");
            run_multi_sql("DROP TABLE `FriendList`");
            run_multi_sql("ALTER TABLE `__temp_FriendList` RENAME TO `FriendList`");
            run_multi_sql("CREATE INDEX `index_alias_name_on_FriendList` ON `FriendList` (`alias_name`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_FriendList` ON `FriendList` (`TOX_CONNECTION`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off`)");
            run_multi_sql("CREATE INDEX `index_TOX_CONNECTION_on_off_real_on_FriendList` ON `FriendList` (`TOX_CONNECTION_on_off_real`)");
            run_multi_sql("CREATE INDEX `index_TOX_USER_STATUS_on_FriendList` ON `FriendList` (`TOX_USER_STATUS`)");
            run_multi_sql("CREATE INDEX `index_avatar_ftid_hex_on_FriendList` ON `FriendList` (`avatar_ftid_hex`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_on_FriendList` ON `FriendList` (`avatar_update`)");
            run_multi_sql("CREATE INDEX `index_avatar_update_timestamp_on_FriendList` ON `FriendList` (`avatar_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_FriendList` ON `FriendList` (`notification_silent`)");
            run_multi_sql("CREATE INDEX `index_sort_on_FriendList` ON `FriendList` (`sort`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_on_FriendList` ON `FriendList` (`last_online_timestamp`)");
            run_multi_sql("CREATE INDEX `index_last_online_timestamp_real_on_FriendList` ON `FriendList` (`last_online_timestamp_real`)");
            run_multi_sql("CREATE INDEX `index_added_timestamp_on_FriendList` ON `FriendList` (`added_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_relay_on_FriendList` ON `FriendList` (`is_relay`)");
            run_multi_sql("CREATE INDEX `index_push_url_on_FriendList` ON `FriendList` (`push_url`)");
            run_multi_sql("CREATE INDEX `index_ip_addr_str_on_FriendList` ON `FriendList` (`ip_addr_str`)");
            run_multi_sql("CREATE INDEX `index_capabilities_on_FriendList` ON `FriendList` (`capabilities`)");
            run_multi_sql("CREATE INDEX `index_msgv3_capability_on_FriendList` ON `FriendList` (`msgv3_capability`)");
        }

        if (new_version == 10240) {
            run_multi_sql("CREATE TABLE `__temp_GroupDB` (`who_invited__tox_public_key_string` TEXT NOT NULL, `name` TEXT, `topic` TEXT, `peer_count` INTEGER NOT NULL DEFAULT - 1, `own_peer_number` INTEGER NOT NULL DEFAULT - 1, `privacy_state` INTEGER NOT NULL DEFAULT 0, `tox_group_number` INTEGER NOT NULL DEFAULT - 1, `group_active` BOOLEAN NOT NULL DEFAULT false, `group_we_left` BOOLEAN NOT NULL DEFAULT false, `notification_silent` BOOLEAN DEFAULT false, `group_identifier` TEXT PRIMARY KEY)");
            run_multi_sql("INSERT INTO `__temp_GroupDB` (`who_invited__tox_public_key_string`, `name`, `topic`, `peer_count`, `own_peer_number`, `privacy_state`, `tox_group_number`, `group_active`, `notification_silent`, `group_identifier`) SELECT `who_invited__tox_public_key_string`, `name`, `topic`, `peer_count`, `own_peer_number`, `privacy_state`, `tox_group_number`, `group_active`, `notification_silent`, `group_identifier` FROM `GroupDB`");
            run_multi_sql("DROP TABLE `GroupDB`");
            run_multi_sql("ALTER TABLE `__temp_GroupDB` RENAME TO `GroupDB`");
            run_multi_sql("CREATE INDEX `index_who_invited__tox_public_key_string_on_GroupDB` ON `GroupDB` (`who_invited__tox_public_key_string`)");
            run_multi_sql("CREATE INDEX `index_name_on_GroupDB` ON `GroupDB` (`name`)");
            run_multi_sql("CREATE INDEX `index_topic_on_GroupDB` ON `GroupDB` (`topic`)");
            run_multi_sql("CREATE INDEX `index_peer_count_on_GroupDB` ON `GroupDB` (`peer_count`)");
            run_multi_sql("CREATE INDEX `index_own_peer_number_on_GroupDB` ON `GroupDB` (`own_peer_number`)");
            run_multi_sql("CREATE INDEX `index_privacy_state_on_GroupDB` ON `GroupDB` (`privacy_state`)");
            run_multi_sql("CREATE INDEX `index_tox_group_number_on_GroupDB` ON `GroupDB` (`tox_group_number`)");
            run_multi_sql("CREATE INDEX `index_group_active_on_GroupDB` ON `GroupDB` (`group_active`)");
            run_multi_sql("CREATE INDEX `index_group_we_left_on_GroupDB` ON `GroupDB` (`group_we_left`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_GroupDB` ON `GroupDB` (`notification_silent`)");
        }

        if (new_version == 10241) {
            run_multi_sql("CREATE TABLE `__temp_GroupMessage` (`message_id_tox` TEXT, `group_identifier` TEXT NOT NULL DEFAULT - 1, `tox_group_peer_pubkey` TEXT NOT NULL, `tox_group_peer_role` INTEGER NOT NULL DEFAULT - 1, `private_message` INTEGER, `tox_group_peername` TEXT, `direction` INTEGER NOT NULL, `TOX_MESSAGE_TYPE` INTEGER NOT NULL, `TRIFA_MESSAGE_TYPE` INTEGER NOT NULL DEFAULT 0, `sent_timestamp` INTEGER, `rcvd_timestamp` INTEGER, `read` BOOLEAN NOT NULL, `is_new` BOOLEAN NOT NULL, `text` TEXT, `was_synced` BOOLEAN, `TRIFA_SYNC_TYPE` INTEGER, `sync_confirmations` INTEGER NOT NULL DEFAULT 0, `tox_group_peer_pubkey_syncer_01` TEXT, `tox_group_peer_pubkey_syncer_02` TEXT, `tox_group_peer_pubkey_syncer_03` TEXT, `tox_group_peer_pubkey_syncer_01_sent_timestamp` INTEGER, `tox_group_peer_pubkey_syncer_02_sent_timestamp` INTEGER, `tox_group_peer_pubkey_syncer_03_sent_timestamp` INTEGER, `msg_id_hash` TEXT, `sent_privately_to_tox_group_peer_pubkey` TEXT, `path_name` TEXT, `file_name` TEXT, `filename_fullpath` TEXT, `filesize` INTEGER NOT NULL DEFAULT - 1, `storage_frame_work` BOOLEAN NOT NULL DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupMessage` (`message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `sync_confirmations`, `tox_group_peer_pubkey_syncer_01`, `tox_group_peer_pubkey_syncer_02`, `tox_group_peer_pubkey_syncer_03`, `tox_group_peer_pubkey_syncer_01_sent_timestamp`, `tox_group_peer_pubkey_syncer_02_sent_timestamp`, `tox_group_peer_pubkey_syncer_03_sent_timestamp`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id`) SELECT `message_id_tox`, `group_identifier`, `tox_group_peer_pubkey`, `private_message`, `tox_group_peername`, `direction`, `TOX_MESSAGE_TYPE`, `TRIFA_MESSAGE_TYPE`, `sent_timestamp`, `rcvd_timestamp`, `read`, `is_new`, `text`, `was_synced`, `TRIFA_SYNC_TYPE`, `sync_confirmations`, `tox_group_peer_pubkey_syncer_01`, `tox_group_peer_pubkey_syncer_02`, `tox_group_peer_pubkey_syncer_03`, `tox_group_peer_pubkey_syncer_01_sent_timestamp`, `tox_group_peer_pubkey_syncer_02_sent_timestamp`, `tox_group_peer_pubkey_syncer_03_sent_timestamp`, `msg_id_hash`, `sent_privately_to_tox_group_peer_pubkey`, `path_name`, `file_name`, `filename_fullpath`, `filesize`, `storage_frame_work`, `id` FROM `GroupMessage`");
            run_multi_sql("DROP TABLE `GroupMessage`");
            run_multi_sql("ALTER TABLE `__temp_GroupMessage` RENAME TO `GroupMessage`");
            run_multi_sql("CREATE INDEX `index_message_id_tox_on_GroupMessage` ON `GroupMessage` (`message_id_tox`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupMessage` ON `GroupMessage` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_role_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_role`)");
            run_multi_sql("CREATE INDEX `index_private_message_on_GroupMessage` ON `GroupMessage` (`private_message`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peername_on_GroupMessage` ON `GroupMessage` (`tox_group_peername`)");
            run_multi_sql("CREATE INDEX `index_direction_on_GroupMessage` ON `GroupMessage` (`direction`)");
            run_multi_sql("CREATE INDEX `index_TOX_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TOX_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_MESSAGE_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_MESSAGE_TYPE`)");
            run_multi_sql("CREATE INDEX `index_rcvd_timestamp_on_GroupMessage` ON `GroupMessage` (`rcvd_timestamp`)");
            run_multi_sql("CREATE INDEX `index_is_new_on_GroupMessage` ON `GroupMessage` (`is_new`)");
            run_multi_sql("CREATE INDEX `index_was_synced_on_GroupMessage` ON `GroupMessage` (`was_synced`)");
            run_multi_sql("CREATE INDEX `index_TRIFA_SYNC_TYPE_on_GroupMessage` ON `GroupMessage` (`TRIFA_SYNC_TYPE`)");
            run_multi_sql("CREATE INDEX `index_sync_confirmations_on_GroupMessage` ON `GroupMessage` (`sync_confirmations`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_01_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_01`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_02_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_02`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_03_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_03`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_01_sent_timestamp_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_01_sent_timestamp`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_02_sent_timestamp_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_02_sent_timestamp`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_syncer_03_sent_timestamp_on_GroupMessage` ON `GroupMessage` (`tox_group_peer_pubkey_syncer_03_sent_timestamp`)");
            run_multi_sql("CREATE INDEX `index_msg_id_hash_on_GroupMessage` ON `GroupMessage` (`msg_id_hash`)");
            run_multi_sql("CREATE INDEX `index_sent_privately_to_tox_group_peer_pubkey_on_GroupMessage` ON `GroupMessage` (`sent_privately_to_tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_path_name_on_GroupMessage` ON `GroupMessage` (`path_name`)");
            run_multi_sql("CREATE INDEX `index_file_name_on_GroupMessage` ON `GroupMessage` (`file_name`)");
            run_multi_sql("CREATE INDEX `index_filesize_on_GroupMessage` ON `GroupMessage` (`filesize`)");
            run_multi_sql("CREATE INDEX `index_storage_frame_work_on_GroupMessage` ON `GroupMessage` (`storage_frame_work`)");
            run_multi_sql("CREATE TABLE `__temp_GroupPeerDB` (`group_identifier` TEXT NOT NULL, `tox_group_peer_pubkey` TEXT NOT NULL, `peer_name` TEXT, `last_update_timestamp` INTEGER NOT NULL DEFAULT - 1, `first_join_timestamp` INTEGER NOT NULL DEFAULT - 1, `Tox_Group_Role` INTEGER NOT NULL DEFAULT 2, `notification_silent` BOOLEAN DEFAULT false, `id` INTEGER PRIMARY KEY AUTOINCREMENT)");
            run_multi_sql("INSERT INTO `__temp_GroupPeerDB` (`group_identifier`, `tox_group_peer_pubkey`, `peer_name`, `last_update_timestamp`, `first_join_timestamp`, `Tox_Group_Role`, `id`) SELECT `group_identifier`, `tox_group_peer_pubkey`, `peer_name`, `last_update_timestamp`, `first_join_timestamp`, `Tox_Group_Role`, `id` FROM `GroupPeerDB`");
            run_multi_sql("DROP TABLE `GroupPeerDB`");
            run_multi_sql("ALTER TABLE `__temp_GroupPeerDB` RENAME TO `GroupPeerDB`");
            run_multi_sql("CREATE UNIQUE INDEX `index_group_identifier_tox_group_peer_pubkey_on_GroupPeerDB` ON `GroupPeerDB` (`group_identifier`, `tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_group_identifier_on_GroupPeerDB` ON `GroupPeerDB` (`group_identifier`)");
            run_multi_sql("CREATE INDEX `index_tox_group_peer_pubkey_on_GroupPeerDB` ON `GroupPeerDB` (`tox_group_peer_pubkey`)");
            run_multi_sql("CREATE INDEX `index_peer_name_on_GroupPeerDB` ON `GroupPeerDB` (`peer_name`)");
            run_multi_sql("CREATE INDEX `index_last_update_timestamp_on_GroupPeerDB` ON `GroupPeerDB` (`last_update_timestamp`)");
            run_multi_sql("CREATE INDEX `index_first_join_timestamp_on_GroupPeerDB` ON `GroupPeerDB` (`first_join_timestamp`)");
            run_multi_sql("CREATE INDEX `index_Tox_Group_Role_on_GroupPeerDB` ON `GroupPeerDB` (`Tox_Group_Role`)");
            run_multi_sql("CREATE INDEX `index_notification_silent_on_GroupPeerDB` ON `GroupPeerDB` (`notification_silent`)");
        }

        if (new_version == 10242)
        {
            run_multi_sql("ALTER TABLE `FriendList` ADD COLUMN is_default_ft_contact BOOLEAN NOT NULL DEFAULT false");
            run_multi_sql("CREATE INDEX `index_is_default_ft_contact_on_FriendList` ON `FriendList` (`is_default_ft_contact`)");
        }
    }

    private OrmaDatabase OrmaDatabase_wrapper(String dbs_path, String pref__db_secrect_key, boolean pref__db_wal_mode)
    {
        set_schema_upgrade_callback(new OrmaDatabase.schema_upgrade_callback()
        {
            @Override
            public void upgrade(int old_version, int new_version)
            {
                Log.i(TAG, "trying to upgrade schema from " + old_version + " to " + new_version);
                upgrade_db_schema_do(old_version, new_version);
            }
        });

        OrmaDatabase orma = new OrmaDatabase(dbs_path, pref__db_secrect_key, pref__db_wal_mode);
        try
        {
            OrmaDatabase.init(ORMA_CURRENT_DB_SCHEMA_VERSION);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return orma;
    }

    private void manually_log_in()
    {
        global_start_tox();
        manually_logged_out = false;
        try
        {
            final Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(50);
                        Log.i(TAG, "connection_status: manual activate");
                        tox_notification_change_wrapper(tox_self_get_connection_status(), "");
                    }
                    catch (Exception e)
                    {
                    }
                }
            };
            t.start();
        }
        catch(Exception e)
        {
        }

        try
        {
            PrimaryDrawerItem manual_logout_item = new PrimaryDrawerItem().withIdentifier(3).
                    withName(R.string.MainActivity_manually_logged_out_false).
                    withIcon(GoogleMaterial.Icon.gmd_refresh);
            main_drawer.updateItemAtPosition(manual_logout_item, 4);
        }
        catch(Exception e)
        {
        }
    }

    static void manually_log_out()
    {
        global_stop_tox();
        manually_logged_out = true;
        try
        {
            final Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(50);
                        Log.i(TAG, "connection_status: manual logout");
                        tox_notification_change_wrapper(CONNECTION_STATUS_MANUAL_LOGOUT, "");
                    }
                    catch (Exception e)
                    {
                    }
                }
            };
            t.start();
        }
        catch(Exception e)
        {
        }

        try
        {
            PrimaryDrawerItem manual_logout_item = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.MainActivity_manually_logged_out_true).withIcon(
                    GoogleMaterial.Icon.gmd_refresh);
            main_drawer.updateItemAtPosition(manual_logout_item, 4);
        }
        catch(Exception e)
        {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    void remove_all_progressDialogs()
    {
        try
        {
            fl_loading_progressbar.setVisibility(View.GONE);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.item_addfriend:
                final Intent intent = new Intent(this, AddFriendActivity.class);
                startActivityForResult(intent, AddFriendActivity_ID);
                break;
        }
        return true;
    }

    public void vfs_listFilesAndFilesSubDirectories(String directoryName, int depth, String parent)
    {
        if (VFS_ENCRYPT)
        {
            info.guardianproject.iocipher.File directory1 = new info.guardianproject.iocipher.File(directoryName);
            info.guardianproject.iocipher.File[] fList1 = directory1.listFiles();

            for (info.guardianproject.iocipher.File file : fList1)
            {
                if (file.isFile())
                {
                    // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // final String human_datetime = df.format(new Date(file.lastModified()));
                    Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
                }
                else if (file.isDirectory())
                {
                    Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                    vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1,
                                                        parent + "/" + file.getName());
                }
            }
        }
        else
        {
            java.io.File directory1 = new java.io.File(directoryName);
            java.io.File[] fList1 = directory1.listFiles();

            for (File file : fList1)
            {
                if (file.isFile())
                {
                    // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // final String human_datetime = df.format(new Date(file.lastModified()));
                    Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
                }
                else if (file.isDirectory())
                {
                    Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                    vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1,
                                                        parent + "/" + file.getName());
                }
            }
        }
    }


    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    @NeedsPermission({Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION })
    void dummyForPermissions001()
    {
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------

    // this is NOT a crpytographically secure random string generator!!
    // it should only be used to generate status messages or tox user strings to be sort of unique
    static String getRandomString(final int sizeOfRandomString)
    {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);

        for (int i = 0; i < sizeOfRandomString; ++i)
        {
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }

        return sb.toString();
    }

    void tox_thread_start()
    {
        try
        {
            Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    long counter = 0;

                    while (tox_service_fg == null)
                    {
                        counter++;

                        if (counter > 100)
                        {
                            break;
                        }

                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (Exception e)
                        {
                            // e.printStackTrace();
                        }
                    }

                    try
                    {
                        // [TODO: move this also to Service.]
                        // HINT: seems to work pretty ok now.
                        if (!is_tox_started)
                        {
                            int PREF__orbot_enabled_to_int = 0;

                            if (PREF__orbot_enabled)
                            {
                                PREF__orbot_enabled_to_int = 1;
                                // need to wait for Orbot to be active ...
                                // max 20 seconds!
                                int max_sleep_iterations = 40;
                                int sleep_iteration = 0;

                                while (!OrbotHelper.isOrbotRunning(context_s))
                                {
                                    // sleep 0.5 seconds
                                    sleep_iteration++;

                                    try
                                    {
                                        Thread.sleep(500);
                                    }
                                    catch (Exception e)
                                    {
                                        e.printStackTrace();
                                    }

                                    if (sleep_iteration > max_sleep_iterations)
                                    {
                                        // giving up
                                        break;
                                    }
                                }

                                try
                                {
                                    Thread.sleep(1000);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }

                                // remove "waiting for orbot view"
                                Log.i(TAG, "waiting_for_orbot_info:+F99");
                                orbot_is_really_running = true;
                                HelperGeneric.waiting_for_orbot_info(false);
                            }

                            int PREF__local_discovery_enabled_to_int = 0;

                            if (PREF__local_discovery_enabled)
                            {
                                PREF__local_discovery_enabled_to_int = 1;
                            }

                            int PREF__ipv6_enabled_to_int = 0;
                            if (PREF__ipv6_enabled)
                            {
                                PREF__ipv6_enabled_to_int = 1;
                            }

                            int PREF__force_udp_only_to_int = 0;
                            if (PREF__force_udp_only)
                            {
                                PREF__force_udp_only_to_int = 1;
                            }

                            init(app_files_directory, PREF__udp_enabled, PREF__local_discovery_enabled_to_int,
                                 PREF__orbot_enabled_to_int, ORBOT_PROXY_HOST, ORBOT_PROXY_PORT,
                                 TrifaSetPatternActivity.bytesToString(TrifaSetPatternActivity.sha256(
                                         TrifaSetPatternActivity.StringToBytes2(PREF__DB_secrect_key))),
                                 PREF__ipv6_enabled_to_int, PREF__force_udp_only_to_int, PREF__ngc_video_bitrate,
                                 PREF__ngc_video_max_quantizer,
                                 PREF__ngc_audio_bitrate, PREF__ngc_audio_samplerate, PREF__ngc_audio_channels);

                            //zzzzzzz// tox_callback_friend_lossless_packet_per_pktid(GEO_COORDS_CUSTOM_LOSSLESS_ID);
                        }

                        tox_service_fg.tox_thread_start_fg();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "tox_thread_start:EE:" + e.getMessage());
        }
    }

    //    static void stop_tox()
    //    {
    //        try
    //        {
    //            Thread t = new Thread()
    //            {
    //                @Override
    //                public void run()
    //                {
    //                    long counter = 0;
    //                    while (tox_service_fg == null)
    //                    {
    //                        counter++;
    //                        if (counter > 100)
    //                        {
    //                            break;
    //                        }
    //
    //                        try
    //                        {
    //                            Thread.sleep(100);
    //                        }
    //                        catch (Exception e)
    //                        {
    //                            e.printStackTrace();
    //                        }
    //                    }
    //
    //                    try
    //                    {
    //
    //                        tox_service_fg.stop_tox_fg();
    //                    }
    //                    catch (Exception e)
    //                    {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            };
    //            t.start();
    //        }
    //        catch (Exception e)
    //        {
    //            e.printStackTrace();
    //            Log.i(TAG, "stop_tox:EE:" + e.getMessage());
    //        }
    //    }

    static void global_stop_tox()
    {
        try
        {
            if (is_tox_started)
            {
                tox_service_fg.stop_tox_fg(false);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void global_start_tox()
    {
        try
        {
            if (!is_tox_started)
            {
                int PREF__orbot_enabled_to_int = 0;

                if (PREF__orbot_enabled)
                {
                    PREF__orbot_enabled_to_int = 1;
                }

                int PREF__local_discovery_enabled_to_int = 0;

                if (PREF__local_discovery_enabled)
                {
                    PREF__local_discovery_enabled_to_int = 1;
                }

                int PREF__ipv6_enabled_to_int = 0;
                if (PREF__ipv6_enabled)
                {
                    PREF__ipv6_enabled_to_int = 1;
                }

                int PREF__force_udp_only_to_int = 0;
                if (PREF__force_udp_only)
                {
                    PREF__force_udp_only_to_int = 1;
                }

                init(app_files_directory, PREF__udp_enabled, PREF__local_discovery_enabled_to_int,
                     PREF__orbot_enabled_to_int, ORBOT_PROXY_HOST, ORBOT_PROXY_PORT,
                     TrifaSetPatternActivity.bytesToString(TrifaSetPatternActivity.sha256(
                             TrifaSetPatternActivity.StringToBytes2(PREF__DB_secrect_key))), PREF__ipv6_enabled_to_int,
                     PREF__force_udp_only_to_int, PREF__ngc_video_bitrate, PREF__ngc_video_max_quantizer,
                     PREF__ngc_audio_bitrate, PREF__ngc_audio_samplerate, PREF__ngc_audio_channels);

                //zzzzzzz// tox_callback_friend_lossless_packet_per_pktid(GEO_COORDS_CUSTOM_LOSSLESS_ID);

                tox_service_fg.tox_thread_start_fg();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // just in case, update own activity pointer!
        main_activity_s = this;
    }

    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause");
        super.onPause();

        global_showing_mainview = false;
        mLocationOverlay.disableMyLocation();
        map.onPause();

        MainActivity.friend_list_fragment = null;
    }

    @Override
    protected void onResume()
    {
        Log.i(TAG, "onResume");
        super.onResume();

        global_showing_mainview = true;
        if (!PREF__normal_main_view)
        {
            map.onResume();
            mLocationOverlay.enableMyLocation();
        }


        if (WANT_DEBUG_THREAD)
        {
            if (!DEBUG_THREAD_STARTED)
            {
                DEBUG_THREAD_STARTED = true;
                final Thread t_debug_location = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            while (true)
                            {
                                float lat = 48.22194736160127f - (float) (Math.random() * 0.02f);
                                float lon = 16.39440515983681f - (float) (Math.random() * 0.02f);
                                float acc = 20.5f - (float) (Math.random() * 19.0f);
                                float bearing = (float) (Math.random() * 360.0f);
                        /*
                        remote_location_overlay.setLocation(new GeoPoint(lat, lon));
                        remote_location_overlay.setAccuracy(Math.round(acc));
                        remote_location_overlay.setBearing(bearing);
                        map.invalidate();
                        */

                                try
                                {
                                    Location location = new Location(LocationManager.GPS_PROVIDER);
                                    location.setLatitude(lat);
                                    location.setLongitude(lon);
                                    location.setBearing(bearing);
                                    location.setAccuracy(acc);
                                    final byte[] data_bin = getGeoMsg(location);
                                    int data_bin_len = data_bin.length;
                                    data_bin[0] = (byte) CONTROL_PROXY_MESSAGE_TYPE_PROXY_PUBKEY_FOR_FRIEND.value; // GEO_COORDS_CUSTOM_LOSSLESS_ID;
                                    String friend_pubkey = orma.selectFromFriendList().orderByTox_public_key_stringAsc().get(
                                            0).tox_public_key_string;
                                    // Log.i(TAG, "toxpubkey=" + friend_pubkey + " " + bytes_to_hex(data_bin));
                                    final int res = tox_friend_send_lossless_packet(
                                            tox_friend_by_public_key__wrapper(friend_pubkey), data_bin, data_bin_len);
                                    // Log.i(TAG, "res=" + res);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }

                                Thread.sleep(2500);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                t_debug_location.start();
            }
        }


        /*
         // **************************************
         // **************************************
         // **************************************
        VFS_Append_test_001();
         // **************************************
         // **************************************
         // **************************************
         */

        // prefs ----------
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PREF__UV_reversed = settings.getBoolean("video_uv_reversed", true);
        PREF__notification_sound = settings.getBoolean("notifications_new_message_sound", true);
        PREF__notification_vibrate = settings.getBoolean("notifications_new_message_vibrate", true);
        PREF__notification_show_content = settings.getBoolean("notification_show_content", false);
        PREF__notification = settings.getBoolean("notifications_new_message", true);
        PREF__software_echo_cancel = settings.getBoolean("software_echo_cancel", false);
        PREF__fps_half = settings.getBoolean("fps_half", false);
        PREF__h264_encoder_use_intra_refresh = settings.getBoolean("h264_encoder_use_intra_refresh", true);
        PREF__U_keep_nospam = settings.getBoolean("U_keep_nospam", false);
        PREF__set_fps = settings.getBoolean("set_fps", false);
        PREF__conference_show_system_messages = settings.getBoolean("conference_show_system_messages", false);
        PREF__X_battery_saving_mode = settings.getBoolean("X_battery_saving_mode", false);
        PREF__X_misc_button_enabled = settings.getBoolean("X_misc_button_enabled", false);
        PREF__local_discovery_enabled = settings.getBoolean("local_discovery_enabled", false);
        PREF__force_udp_only = settings.getBoolean("force_udp_only", false);
        PREF__use_incognito_keyboard = settings.getBoolean("use_incognito_keyboard", true);
        PREF__speakerphone_tweak = settings.getBoolean("speakerphone_tweak", false);
        PREF__mic_gain_factor_toggle = settings.getBoolean("mic_gain_factor_toggle", false);
        PREF__window_security = settings.getBoolean("window_security", true);
        PREF__use_native_audio_play = settings.getBoolean("X_use_native_audio_play", true);
        PREF__tox_set_do_not_sync_av = settings.getBoolean("X_tox_set_do_not_sync_av", false);


    // reset trigger for throttled saving
        update_savedata_file_wrapper_throttled_last_trigger_ts = 0;

        remove_all_progressDialogs();

        try
        {
            PREF__X_eac_delay_ms = Integer.parseInt(settings.getString("X_eac_delay_ms_2", "80"));
        }
        catch (Exception e)
        {
            PREF__X_eac_delay_ms = 80;
            e.printStackTrace();
        }

        try
        {
            int temp = Integer.parseInt(settings.getString("message_paging_num_msgs_per_page", "50"));

            // HINT: sanity check pref value
            if ((temp < 0) || (temp > 999))
            {
                temp = 50;
            }

            PREF__message_paging_num_msgs_per_page = temp;

            if (PREF__message_paging_num_msgs_per_page == 0)
            {
                PREF__messageview_paging = false;
            }
            else
            {
                PREF__messageview_paging = true;
            }
        }
        catch (Exception e)
        {
            PREF__message_paging_num_msgs_per_page = 50;
            PREF__messageview_paging = true;
            e.printStackTrace();
        }

        // HINT: disable paging for now!!!!!!!!!!!!
        // HINT: disable paging for now!!!!!!!!!!!!
        // HINT: disable paging for now!!!!!!!!!!!!
        PREF__message_paging_num_msgs_per_page = 0;
        PREF__messageview_paging = false;
        // HINT: disable paging for now!!!!!!!!!!!!
        // HINT: disable paging for now!!!!!!!!!!!!
        // HINT: disable paging for now!!!!!!!!!!!!

        try
        {
            PREF__X_audio_play_buffer_custom = Integer.parseInt(settings.getString("X_audio_play_buffer_custom", "0"));
        }
        catch (Exception e)
        {
            PREF__X_audio_play_buffer_custom = 0;
            e.printStackTrace();
        }


        try
        {
            PREF_mic_gain_factor = (float) (settings.getInt("mic_gain_factor", 1));
            Log.i(TAG, "PREF_mic_gain_factor:1=" + PREF_mic_gain_factor);
            // PREF_mic_gain_factor = PREF_mic_gain_factor + 1.0f;

            if (PREF_mic_gain_factor < 1.0f)
            {
                PREF_mic_gain_factor = 1.0f;
            }
            else if (PREF_mic_gain_factor > 30.0f)
            {
                PREF_mic_gain_factor = 30.0f;
            }
            Log.i(TAG, "PREF_mic_gain_factor:2=" + PREF_mic_gain_factor);
        }
        catch (Exception e)
        {
            PREF_mic_gain_factor = 1.0f;
            Log.i(TAG, "PREF_mic_gain_factor:E=" + PREF_mic_gain_factor);
            e.printStackTrace();
        }

        if (PREF__U_keep_nospam == true)
        {
            top_imageview2.setBackgroundColor(Color.TRANSPARENT);
            // top_imageview.setBackgroundColor(Color.parseColor("#C62828"));
            final Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_exclamation_circle).paddingDp(
                    15).color(getResources().getColor(R.color.md_red_600)).sizeDp(100);
            top_imageview2.setImageDrawable(d1);
            top_imageview2.setVisibility(View.VISIBLE);
        }
        else
        {
            top_imageview2.setVisibility(View.GONE);
        }

        own_push_token_load();
        top_imageview3.setVisibility(View.GONE);

        top_imageview3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                try
                {
                    AlertDialog ad = new AlertDialog.Builder(view.getContext()).setNegativeButton(
                            R.string.MainActivity_no_button, new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                }
                            }).setPositiveButton(R.string.MainActivity_ok_take_me_there_button,
                                                 new DialogInterface.OnClickListener()
                                                 {
                                                     public void onClick(DialogInterface dialog, int id)
                                                     {
                                                         try
                                                         {
                                                             Intent i = new Intent(Intent.ACTION_VIEW);
                                                             i.setData(Uri.parse(TOX_PUSH_SETUP_HOWTO_URL));
                                                             startActivity(i);
                                                         }
                                                         catch (Exception e)
                                                         {
                                                         }
                                                     }
                                                 }).create();
                    ad.setTitle(getString(R.string.MainActivity_setup_push_tip_title));
                    ad.setMessage(getString(R.string.MainActivity_setup_push_tip_text));
                    ad.setCancelable(false);
                    ad.setCanceledOnTouchOutside(false);
                    ad.show();
                }
                catch (Exception ee2)
                {
                }
            }
        });

        try
        {
            if (have_own_relay())
            {
                int relay_connection_status_real = get_own_relay_connection_status_real();

                if (relay_connection_status_real == 2)
                {
                    draw_main_top_icon(top_imageview, context_s, Color.parseColor("#04b431"), true);
                }
                else if (relay_connection_status_real == 1)
                {
                    draw_main_top_icon(top_imageview, context_s, Color.parseColor("#ffce00"), true);
                }
                else
                {
                    draw_main_top_icon(top_imageview, context_s, Color.parseColor("#ff0000"), true);
                }
            }
            else
            {
                draw_main_top_icon(top_imageview, this, Color.GRAY, true);
            }
        }
        catch (Exception e)
        {
        }


        boolean tmp1 = settings.getBoolean("udp_enabled", false);

        if (tmp1)
        {
            PREF__udp_enabled = 1;
        }
        else
        {
            PREF__udp_enabled = 0;
        }

        PREF__higher_video_quality = 0;
        GLOBAL_VIDEO_BITRATE = LOWER_GLOBAL_VIDEO_BITRATE;

        try
        {
            PREF__video_call_quality = Integer.parseInt(settings.getString("video_call_quality", "0"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_call_quality = 0;
        }

        try
        {
            PREF__higher_audio_quality = Integer.parseInt(settings.getString("higher_audio_quality", "1"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__higher_audio_quality = 1;
        }

        if (PREF__higher_audio_quality == 2)
        {
            GLOBAL_AUDIO_BITRATE = HIGHER_GLOBAL_AUDIO_BITRATE;
        }
        else if (PREF__higher_audio_quality == 1)
        {
            GLOBAL_AUDIO_BITRATE = NORMAL_GLOBAL_AUDIO_BITRATE;
        }
        else
        {
            GLOBAL_AUDIO_BITRATE = LOWER_GLOBAL_AUDIO_BITRATE;
        }

        try
        {
            if (settings.getString("min_audio_samplingrate_out", "8000").compareTo("Auto") == 0)
            {
                PREF__min_audio_samplingrate_out = 8000;
            }
            else
            {
                PREF__min_audio_samplingrate_out = Integer.parseInt(
                        settings.getString("min_audio_samplingrate_out", "" + MIN_AUDIO_SAMPLINGRATE_OUT));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__min_audio_samplingrate_out = MIN_AUDIO_SAMPLINGRATE_OUT;
        }

        // ------- FIXED -------
        PREF__min_audio_samplingrate_out = SAMPLE_RATE_FIXED;
        // ------- FIXED -------


        Log.i(TAG, "PREF__UV_reversed:2=" + PREF__UV_reversed);
        Log.i(TAG, "PREF__min_audio_samplingrate_out:2=" + PREF__min_audio_samplingrate_out);

        try
        {
            PREF__allow_screen_off_in_audio_call = settings.getBoolean("allow_screen_off_in_audio_call", true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_screen_off_in_audio_call = true;
        }

        try
        {
            PREF__auto_accept_image = settings.getBoolean("auto_accept_image", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_image = false;
        }

        try
        {
            PREF__auto_accept_video = settings.getBoolean("auto_accept_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_video = false;
        }

        try
        {
            PREF__auto_accept_all_upto = settings.getBoolean("auto_accept_all_upto", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__auto_accept_all_upto = false;
        }

        try
        {
            PREF__X_zoom_incoming_video = settings.getBoolean("X_zoom_incoming_video", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_zoom_incoming_video = false;
        }

        try
        {
            PREF__X_audio_recording_frame_size = Integer.parseInt(
                    settings.getString("X_audio_recording_frame_size", "" + 40));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__X_audio_recording_frame_size = 40;
        }

        // ------- FIXED -------
        PREF__X_audio_recording_frame_size = FRAME_SIZE_FIXED;
        // ------- FIXED -------

        try
        {
            PREF__video_cam_resolution = Integer.parseInt(settings.getString("video_cam_resolution", "" + 0));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__video_cam_resolution = 0;
        }

        try
        {
            PREF__dark_mode_pref = Integer.parseInt(settings.getString("dark_mode_pref", "" + 1));
        }
        catch (Exception e)
        {
            PREF__dark_mode_pref = 1;
        }

        if (PREF__dark_mode_pref == 0)
        {
            // follow system. try to guess if we actually have dark mode active now
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switch (currentNightMode)
            {
                case Configuration.UI_MODE_NIGHT_NO:// Night mode is not active, we're in day time
                    PREF__dark_mode_pref = 2;
                    break;
                case Configuration.UI_MODE_NIGHT_YES:// Night mode is active, we're at night!
                    PREF__dark_mode_pref = 1;
                    break;
                case Configuration.UI_MODE_NIGHT_UNDEFINED:// We don't know what mode we're in, assume notnight
                    PREF__dark_mode_pref = 1;
                    break;
            }
        }

        try
        {
            PREF__global_font_size = Integer.parseInt(
                    settings.getString("global_font_size", "" + PREF_GLOBAL_FONT_SIZE_DEFAULT));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__global_font_size = PREF_GLOBAL_FONT_SIZE_DEFAULT;
        }

        PREF__camera_get_preview_format = settings.getString("camera_get_preview_format", "YV12");

        try
        {
            PREF__compact_friendlist = settings.getBoolean("compact_friendlist", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__compact_friendlist = false;
        }

        try
        {
            PREF__allow_file_sharing_to_trifa_via_intent = settings.getBoolean("allow_file_sharing_to_trifa_via_intent",
                                                                               false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_file_sharing_to_trifa_via_intent = false;
        }

        try
        {
            PREF__allow_open_encrypted_file_via_intent = settings.getBoolean("allow_open_encrypted_file_via_intent",
                                                                             true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_open_encrypted_file_via_intent = true;
        }

        PREF__compact_chatlist = true;

        try
        {
            PREF__allow_push_server_ntfy = settings.getBoolean("allow_push_server_ntfy", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__allow_push_server_ntfy = false;
        }

        try
        {
            PREF__use_push_service = settings.getBoolean("use_push_service", false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__use_push_service = false;
        }

        try
        {
            final String muted_peers_01 = settings.getString("toxirc_muted_peers_01", "");
            final String muted_peers_02 = settings.getString("toxirc_muted_peers_02", "");
            final String muted_peers_03 = settings.getString("toxirc_muted_peers_03", "");

            int count_muted = 0;
            if (muted_peers_01.length() > 0)
            {
                count_muted++;
            }
            if (muted_peers_02.length() > 0)
            {
                count_muted++;
            }
            if (muted_peers_03.length() > 0)
            {
                count_muted++;
            }

            if (count_muted > 0)
            {
                PREF__toxirc_muted_peers = new String[count_muted];
                int i = 0;
                if (muted_peers_01.length() > 0)
                {
                    PREF__toxirc_muted_peers[i] = muted_peers_01;
                    i++;
                }

                if (muted_peers_02.length() > 0)
                {
                    PREF__toxirc_muted_peers[i] = muted_peers_02;
                    i++;
                }

                if (muted_peers_03.length() > 0)
                {
                    PREF__toxirc_muted_peers[i] = muted_peers_03;
                    i++;
                }
            }
            else
            {
                PREF__toxirc_muted_peers = new String[]{};
            }
        }
        catch (Exception e)
        {
            PREF__toxirc_muted_peers = new String[]{};
        }

        // prefs ----------

        try
        {
            profile_d_item.withIcon(
                    HelperGeneric.get_drawable_from_vfs_image(HelperGeneric.get_vfs_image_filename_own_avatar()));
            main_drawer_header.updateProfile(profile_d_item);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "onResume:EE1:" + e.getMessage());

            try
            {
                final Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_lock).color(
                        getResources().getColor(R.color.colorPrimaryDark)).sizeDp(50);
                profile_d_item.withIcon(d1);
                main_drawer_header.updateProfile(profile_d_item);
            }
            catch (Exception e2)
            {
                Log.i(TAG, "onResume:EE2:" + e2.getMessage());
                e2.printStackTrace();
            }
        }

        spinner_own_status.setSelection(global_tox_self_status);
        // just in case, update own activity pointer!
        main_activity_s = this;

        try
        {
            // ask user to whitelist app from DozeMode/BatteryOptimizations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                SharedPreferences settings2 = PreferenceManager.getDefaultSharedPreferences(this);
                boolean asked_for_whitelist_doze_already = settings2.getBoolean("asked_whitelist_doze", false);

                if (!asked_for_whitelist_doze_already)
                {
                    settings2.edit().putBoolean("asked_whitelist_doze", true).commit();
                    final Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    ResolveInfo resolve_activity = getPackageManager().resolveActivity(intent, 0);

                    if (resolve_activity != null)
                    {
                        AlertDialog ad = new AlertDialog.Builder(this).setNegativeButton(
                                R.string.MainActivity_no_button, new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int id)
                                    {
                                    }
                                }).setPositiveButton(R.string.MainActivity_ok_take_me_there_button,
                                                     new DialogInterface.OnClickListener()
                                                     {
                                                         public void onClick(DialogInterface dialog, int id)
                                                         {
                                                             startActivity(intent);
                                                         }
                                                     }).create();
                        ad.setTitle(getString(R.string.MainActivity_info_dialog_title));
                        ad.setMessage(getString(R.string.MainActivity_add_to_batt_opt));
                        ad.setCancelable(false);
                        ad.setCanceledOnTouchOutside(false);
                        ad.show();
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        if (!PREF__normal_main_view)
        {
            try
            {
                final String default_friend_pubkey = get_set_is_default_ft_contact(null, false);
                if (default_friend_pubkey != null)
                {
                    change_msg_notification(NOTIFICATION_EDIT_ACTION_REMOVE.value,
                                            default_friend_pubkey, null, null);
                }
            }
            catch(Exception e)
            {
            }
        }
    }

    @Override
    protected void onNewIntent(Intent i)
    {
        Log.i(TAG, "onNewIntent:i=" + i);
        super.onNewIntent(i);
    }

    @Override
    public void onBackPressed()
    {
        if (main_drawer.isDrawerOpen())
        {
            main_drawer.closeDrawer();
        }
        else
        {
            super.onBackPressed();
        }
    }

    // -- this is for incoming video --
    // -- this is for incoming video --
    static void allocate_video_buffer_1(int frame_width_px1, int frame_height_px1, long ystride, long ustride, long vstride)
    {
        try
        {
            //Log.i("semaphore_01","acquire:01");
            semaphore_videoout_bitmap.acquire();
            //Log.i("semaphore_01","acquire:01:OK");
        }
        catch (InterruptedException e)
        {
            //Log.i("semaphore_01","release:01");
            semaphore_videoout_bitmap.release();
            //Log.i("semaphore_01","release:01:OK");
            return;
        }

        if (video_buffer_1 != null)
        {
            video_buffer_1 = null;
        }

        if (video_frame_image != null)
        {
            video_frame_image_valid = false;

            if (!video_frame_image.isRecycled())
            {
                if (!PREF__NO_RECYCLE_VIDEO_FRAME_BITMAP)
                {
                    Log.i(TAG, "video_frame_image.recycle:start");
                    video_frame_image.recycle();
                    Log.i(TAG, "video_frame_image.recycle:end");
                }
            }

            video_frame_image = null;
        }

        /*
         * YUV420 frame with width * height
         *
         * @param y Luminosity plane. Size = MAX(width, abs(ystride)) * height.
         * @param u U chroma plane. Size = MAX(width/2, abs(ustride)) * (height/2).
         * @param v V chroma plane. Size = MAX(width/2, abs(vstride)) * (height/2).
         */
        int y_layer_size = (int) Math.max(frame_width_px1, Math.abs(ystride)) * frame_height_px1;
        int u_layer_size = (int) Math.max((frame_width_px1 / 2), Math.abs(ustride)) * (frame_height_px1 / 2);
        int v_layer_size = (int) Math.max((frame_width_px1 / 2), Math.abs(vstride)) * (frame_height_px1 / 2);
        int frame_width_px = (int) Math.max(frame_width_px1, Math.abs(ystride));
        int frame_height_px = (int) frame_height_px1;
        buffer_size_in_bytes = y_layer_size + v_layer_size + u_layer_size;
        Log.i(TAG, "YUV420 frame w1=" + frame_width_px1 + " h1=" + frame_height_px1 + " bytes=" + buffer_size_in_bytes);
        Log.i(TAG, "YUV420 frame w=" + frame_width_px + " h=" + frame_height_px + " bytes=" + buffer_size_in_bytes);
        Log.i(TAG, "YUV420 frame ystride=" + ystride + " ustride=" + ustride + " vstride=" + vstride);
        video_buffer_1 = ByteBuffer.allocateDirect(buffer_size_in_bytes);
        set_JNI_video_buffer(video_buffer_1, frame_width_px, frame_height_px);
        RenderScript rs = RenderScript.create(context_s);
        yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(frame_width_px).setY(frame_height_px);
        yuvType.setYuvFormat(ImageFormat.YV12);
        alloc_in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(frame_width_px).setY(frame_height_px);
        alloc_out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        video_frame_image = Bitmap.createBitmap(frame_width_px, frame_height_px, Bitmap.Config.ARGB_8888);

        if (video_frame_image == null)
        {
            video_frame_image_valid = false;
            video_buffer_1 = null;
        }
        else
        {
            video_frame_image_valid = true;
        }

        //Log.i("semaphore_01","release:02");
        semaphore_videoout_bitmap.release();
        //Log.i("semaphore_01","relase:02:OK");
    }
    // -- this is for incoming video --
    // -- this is for incoming video --

    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    // -------- native methods --------
    // -------- native methods --------
    // -------- native methods --------
    public native void init(@NonNull String data_dir, int udp_enabled, int local_discovery_enabled, int orbot_enabled, String orbot_host, long orbot_port, String tox_encrypt_passphrase_hash, int enable_ipv6, int force_udp_only_mode, int ngc_video_bitrate, int max_quantizer, int ngc_audio_bitrate, int ngc_audio_sampling_rate, int ngc_audio_channel_count);

    public native String getNativeLibAPI();

    public static native String getNativeLibGITHASH();

    public static native String getNativeLibTOXGITHASH();

    public static native void update_savedata_file(String tox_encrypt_passphrase_hash);

    public static native void export_savedata_file_unsecure(String tox_encrypt_passphrase_hash, String export_full_path_of_file);

    public static native String get_my_toxid();

    public static native String tox_get_all_tcp_relays();

    public static native String tox_get_all_udp_connections();

    // *UNUSED* public static native void bootstrap();

    public static native int add_tcp_relay_single(String ip, String key_hex, long port);

    public static native int bootstrap_single(String ip, String key_hex, long port);

    public static native int tox_self_get_connection_status();

    public static native void init_tox_callbacks();

    public static native int tox_callback_friend_lossless_packet_per_pktid(int pktid);

    public static native long tox_iteration_interval();

    public static native long tox_iterate();

    // ----------- TRIfA internal -----------
    public static native int jni_iterate_group_audio(int delta_new, int want_ms_output);

    public static native int jni_iterate_videocall_audio(int delta_new, int want_ms_output, int channels, int sample_rate, int send_emtpy_buffer);

    public static native void tox_set_do_not_sync_av(int do_not_sync_av);

    public static native void tox_set_onion_active(int active);
    // ----------- TRIfA internal -----------

    public static native long tox_kill();

    public static native void exit();

    public static native long tox_friend_send_message(long friendnum, int a_TOX_MESSAGE_TYPE, @NonNull String message);

    public static native long tox_version_major();

    public static native long tox_version_minor();

    public static native long tox_version_patch();

    public static native String jnictoxcore_version();

    public static native String libavutil_version();

    public static native String libopus_version();

    public static native String libvpx_version();

    public static native String libsodium_version();

    public static native long tox_max_filename_length();

    public static native long tox_file_id_length();

    public static native long tox_max_message_length();

    public static native long tox_friend_add(@NonNull String toxid_str, @NonNull String message);

    public static native long tox_friend_add_norequest(@NonNull String public_key_str);

    public static native long tox_self_get_friend_list_size();

    public static native void tox_self_set_nospam(long nospam); // this actually needs an "uint32_t" which is an unsigned 32bit integer value

    public static native long tox_self_get_nospam(); // this actually returns an "uint32_t" which is an unsigned 32bit integer value

    public static native long tox_friend_by_public_key(@NonNull String friend_public_key_string);

    public static native String tox_friend_get_name(long friend_number);

    public static native String tox_friend_get_public_key(long friend_number);

    public static native long tox_friend_get_capabilities(long friend_number);

    public static native long[] tox_self_get_friend_list();

    public static native int tox_self_set_name(@NonNull String name);

    public static native int tox_self_set_status_message(@NonNull String status_message);

    public static native void tox_self_set_status(int a_TOX_USER_STATUS);

    public static native int tox_self_set_typing(long friend_number, int typing);

    public static native int tox_friend_get_connection_status(long friend_number);

    public static native String tox_friend_get_connection_ip(long friend_number);

    public static native int tox_friend_delete(long friend_number);

    public static native String tox_self_get_name();

    public static native long tox_self_get_name_size();

    public static native long tox_self_get_status_message_size();

    public static native String tox_self_get_status_message();

    public static native long tox_self_get_capabilities();

    public static native int tox_friend_send_lossless_packet(long friend_number, @NonNull byte[] data, int data_length);

    public static native int tox_file_control(long friend_number, long file_number, int a_TOX_FILE_CONTROL);

    public static native int tox_hash(ByteBuffer hash_buffer, ByteBuffer data_buffer, long data_length);

    public static native int tox_file_seek(long friend_number, long file_number, long position);

    public static native int tox_file_get_file_id(long friend_number, long file_number, ByteBuffer file_id_buffer);

    // public static native long tox_file_sending_active(long friend_number);

    // public static native long tox_file_receiving_active(long friend_number);

    public static native long tox_file_send(long friend_number, long kind, long file_size, ByteBuffer file_id_buffer, String file_name, long filename_length);

    public static native int tox_file_send_chunk(long friend_number, long file_number, long position, ByteBuffer data_buffer, long data_length);


    // --------------- Message V2 -------------
    // --------------- Message V2 -------------
    // --------------- Message V2 -------------
    public static native long tox_messagev2_size(long text_length, long type, long alter_type);

    public static native int tox_messagev2_wrap(long text_length, long type, long alter_type, ByteBuffer message_text_buffer, long ts_sec, long ts_ms, ByteBuffer raw_message_buffer, ByteBuffer msgid_buffer);

    public static native int tox_messagev2_get_message_id(ByteBuffer raw_message_buffer, ByteBuffer msgid_buffer);

    public static native long tox_messagev2_get_ts_sec(ByteBuffer raw_message_buffer);

    public static native long tox_messagev2_get_ts_ms(ByteBuffer raw_message_buffer);

    public static native long tox_messagev2_get_message_text(ByteBuffer raw_message_buffer, long raw_message_len, int is_alter_msg, long alter_type, ByteBuffer message_text_buffer);

    public static native String tox_messagev2_get_sync_message_pubkey(ByteBuffer raw_message_buffer);

    public static native long tox_messagev2_get_sync_message_type(ByteBuffer raw_message_buffer);

    public static native int tox_util_friend_send_msg_receipt_v2(long friend_number, long ts_sec, ByteBuffer msgid_buffer);

    public static native long tox_util_friend_send_message_v2(long friend_number, int type, long ts_sec, String message, long length, ByteBuffer raw_message_back_buffer, ByteBuffer raw_message_back_buffer_length, ByteBuffer msgid_back_buffer);

    public static native int tox_util_friend_resend_message_v2(long friend_number, ByteBuffer raw_message_buffer, long raw_msg_len);
    // --------------- Message V2 -------------
    // --------------- Message V2 -------------
    // --------------- Message V2 -------------

    // --------------- Message V3 -------------
    // --------------- Message V3 -------------
    // --------------- Message V3 -------------
    public static native int tox_messagev3_get_new_message_id(ByteBuffer hash_buffer);

    public static native long tox_messagev3_friend_send_message(long friendnum, int a_TOX_MESSAGE_TYPE, @NonNull String message, @NonNull ByteBuffer mag_hash, long timestamp);
    // --------------- Message V3 -------------
    // --------------- Message V3 -------------
    // --------------- Message V3 -------------


    // --------------- Conference -------------
    // --------------- Conference -------------
    // --------------- Conference -------------
    public static native long tox_conference_join(long friend_number, ByteBuffer cookie_buffer, long cookie_length);

    public static native long tox_conference_peer_count(long conference_number);

    public static native long tox_conference_peer_get_name_size(long conference_number, long peer_number);

    public static native String tox_conference_peer_get_name(long conference_number, long peer_number);

    public static native String tox_conference_peer_get_public_key(long conference_number, long peer_number);

    public static native long tox_conference_offline_peer_count(long conference_number);

    public static native long tox_conference_offline_peer_get_name_size(long conference_number, long offline_peer_number);

    public static native String tox_conference_offline_peer_get_name(long conference_number, long offline_peer_number);

    public static native String tox_conference_offline_peer_get_public_key(long conference_number, long offline_peer_number);

    public static native long tox_conference_offline_peer_get_last_active(long conference_number, long offline_peer_number);

    public static native int tox_conference_peer_number_is_ours(long conference_number, long peer_number);

    public static native long tox_conference_get_title_size(long conference_number);

    public static native String tox_conference_get_title(long conference_number);

    public static native int tox_conference_get_type(long conference_number);

    public static native int tox_conference_send_message(long conference_number, int a_TOX_MESSAGE_TYPE, @NonNull String message);

    public static native int tox_conference_delete(long conference_number);

    public static native long tox_conference_get_chatlist_size();

    public static native long[] tox_conference_get_chatlist();

    public static native int tox_conference_get_id(long conference_number, ByteBuffer cookie_buffer);

    public static native int tox_conference_new();

    public static native int tox_conference_invite(long friend_number, long conference_number);

    public static native int tox_conference_set_title(long conference_number, String title);

    // --------------- Conference -------------
    // --------------- Conference -------------
    // --------------- Conference -------------

    // --------------- new Groups -------------
    // --------------- new Groups -------------
    // --------------- new Groups -------------

    /**
     * Creates a new group chat.
     * <p>
     * This function creates a new group chat object and adds it to the chats array.
     * <p>
     * The caller of this function has Founder role privileges.
     * <p>
     * The client should initiate its peer list with self info after calling this function, as
     * the peer_join callback will not be triggered.
     *
     * @param a_TOX_GROUP_PRIVACY_STATE The privacy state of the group. If this is set to TOX_GROUP_PRIVACY_STATE_PUBLIC,
     *                                  the group will attempt to announce itself to the DHT and anyone with the Chat ID may join.
     *                                  Otherwise a friend invite will be required to join the group.
     * @param group_name                The name of the group. The name must be non-NULL.
     * @param my_peer_name              The name of the peer creating the group.
     * @return group_number on success, UINT32_MAX on failure.
     */
    public static native long tox_group_new(int a_TOX_GROUP_PRIVACY_STATE, @NonNull String group_name, @NonNull String my_peer_name);

    /**
     * Joins a group chat with specified Chat ID.
     * <p>
     * This function creates a new group chat object, adds it to the chats array, and sends
     * a DHT announcement to find peers in the group associated with chat_id. Once a peer has been
     * found a join attempt will be initiated.
     *
     * @param chat_id_buffer The Chat ID of the group you wish to join. This must be TOX_GROUP_CHAT_ID_SIZE bytes.
     * @param password       The password required to join the group. Set to NULL if no password is required.
     * @param my_peer_name   The name of the peer joining the group.
     * @return group_number on success, UINT32_MAX on failure.
     */
    public static native long tox_group_join(@NonNull ByteBuffer chat_id_buffer, long chat_id_length, @NonNull String my_peer_name, String password);

    public static native int tox_group_leave(long group_number, String part_message);

    public static native int tox_group_disconnect(long group_number);

    public static native long tox_group_self_get_peer_id(long group_number);

    public static native int tox_group_self_set_name(long group_number, @NonNull String my_peer_name);

    public static native String tox_group_self_get_public_key(long group_number);

    public static native int tox_group_self_get_role(long group_number);

    public static native int tox_group_peer_get_role(long group_number, long peer_id);

    public static native int tox_group_get_chat_id(long group_number, @NonNull ByteBuffer chat_id_buffer);

    public static native long tox_group_get_number_groups();

    public static native long[] tox_group_get_grouplist();

    public static native long tox_group_peer_count(long group_number);

    public static native int tox_group_get_peer_limit(long group_number);

    public static native int tox_group_founder_set_peer_limit(long group_number, int max_peers);

    public static native long tox_group_offline_peer_count(long group_number);

    public static native long[] tox_group_get_peerlist(long group_number);

    public static native long tox_group_by_chat_id(@NonNull ByteBuffer chat_id_buffer);

    public static native int tox_group_get_privacy_state(long group_number);

    public static native int tox_group_mod_kick_peer(long group_number, long peer_id);

    public static native int tox_group_mod_set_role(long group_number, long peer_id, int a_Tox_Group_Role);

    public static native int tox_group_founder_set_voice_state(long group_number, int a_Tox_Group_Voice_State);

    public static native int tox_group_get_voice_state(long group_number);

    public static native String tox_group_peer_get_public_key(long group_number, long peer_id);

    public static native String tox_group_savedpeer_get_public_key(long group_number, long slot_num);

    public static native long tox_group_peer_by_public_key(long group_number, @NonNull String peer_public_key_string);

    public static native String tox_group_peer_get_name(long group_number, long peer_id);

    public static native String tox_group_get_name(long group_number);

    public static native String tox_group_get_topic(long group_number);

    public static native int tox_group_peer_get_connection_status(long group_number, long peer_id);

    public static native int tox_group_invite_friend(long group_number, long friend_number);

    public static native int tox_group_is_connected(long group_number);

    public static native int tox_group_reconnect(long group_number);

    public static native int tox_group_send_custom_packet(long group_number, int lossless, @NonNull byte[] data, int data_length);

    public static native int tox_group_send_custom_private_packet(long group_number, long peer_id, int lossless, @NonNull byte[] data, int data_length);

    /**
     * Send a text chat message to the group.
     * <p>
     * This function creates a group message packet and pushes it into the send
     * queue.
     * <p>
     * The message length may not exceed TOX_MAX_MESSAGE_LENGTH. Larger messages
     * must be split by the client and sent as separate messages. Other clients can
     * then reassemble the fragments. Messages may not be empty.
     *
     * @param group_number       The group number of the group the message is intended for.
     * @param a_TOX_MESSAGE_TYPE Message type (normal, action, ...).
     * @param message            A non-NULL pointer to the first element of a byte array
     *                           containing the message text.
     * @return message_id on success. return < 0 on error.
     */
    public static native long tox_group_send_message(long group_number, int a_TOX_MESSAGE_TYPE, @NonNull String message);

    /**
     * Send a text chat message to the specified peer in the specified group.
     * <p>
     * This function creates a group private message packet and pushes it into the send
     * queue.
     * <p>
     * The message length may not exceed TOX_MAX_MESSAGE_LENGTH. Larger messages
     * must be split by the client and sent as separate messages. Other clients can
     * then reassemble the fragments. Messages may not be empty.
     *
     * @param group_number The group number of the group the message is intended for.
     * @param peer_id      The ID of the peer the message is intended for.
     * @param message      A non-NULL pointer to the first element of a byte array
     *                     containing the message text.
     * @return 0 on success. return < 0 on error.
     */
    public static native int tox_group_send_private_message(long group_number, long peer_id, int a_TOX_MESSAGE_TYPE, @NonNull String message);

    /**
     * Send a text chat message to the specified peer in the specified group.
     * <p>
     * This function creates a group private message packet and pushes it into the send
     * queue.
     * <p>
     * The message length may not exceed TOX_MAX_MESSAGE_LENGTH. Larger messages
     * must be split by the client and sent as separate messages. Other clients can
     * then reassemble the fragments. Messages may not be empty.
     *
     * @param group_number           The group number of the group the message is intended for.
     * @param peer_public_key_string A memory region of at least TOX_PUBLIC_KEY_SIZE bytes of the peer the
     *                               message is intended for. If this parameter is NULL, this function will return false.
     * @param message                A non-NULL pointer to the first element of a byte array
     *                               containing the message text.
     * @return 0 on success. return < 0 on error.
     */
    public static native int tox_group_send_private_message_by_peerpubkey(long group_number, @NonNull String peer_public_key_string, int a_TOX_MESSAGE_TYPE, @NonNull String message);

    /**
     * Accept an invite to a group chat that the client previously received from a friend. The invite
     * is only valid while the inviter is present in the group.
     *
     * @param invite_data_buffer The invite data received from the `group_invite` event.
     * @param my_peer_name       The name of the peer joining the group.
     * @param password           The password required to join the group. Set to NULL if no password is required.
     * @return the group_number on success, UINT32_MAX on failure.
     */
    public static native long tox_group_invite_accept(long friend_number, @NonNull ByteBuffer invite_data_buffer, long invite_data_length, @NonNull String my_peer_name, String password);

    public static native int toxav_ngc_video_encode(int vbitrate, int max_quantizer, int width, int height, byte[] y, int y_bytes, byte[] u, int u_bytes, byte[] v, int v_bytes, byte[] encoded_frame_bytes);

    public static native int toxav_ngc_video_decode(byte[] encoded_frame_bytes, int encoded_frame_size_bytes, int width, int height, byte[] y, byte[] u, byte[] v, int flush_decoder);

    public static native int toxav_ngc_audio_encode(byte[] pcm, int sample_count_per_frame, byte[] encoded_frame_bytes);

    public static native int toxav_ngc_audio_decode(byte[] encoded_frame_bytes, int encoded_frame_size_bytes, byte[] pcm_decoded);
    // --------------- new Groups -------------
    // --------------- new Groups -------------
    // --------------- new Groups -------------


    // --------------- AV - Conference --------
    // --------------- AV - Conference --------
    // --------------- AV - Conference --------
    public static native long toxav_join_av_groupchat(long friend_number, ByteBuffer cookie_buffer, long cookie_length);

    public static native long toxav_add_av_groupchat();

    public static native long toxav_groupchat_enable_av(long conference_number);

    public static native long toxav_groupchat_disable_av(long conference_number);

    public static native int toxav_groupchat_av_enabled(long conference_number);

    public static native int toxav_group_send_audio(long groupnumber, long sample_count, int channels, long sampling_rate);

    // --------------- AV - Conference --------
    // --------------- AV - Conference --------
    // --------------- AV - Conference --------

    // --------------- AV -------------
    // --------------- AV -------------
    // --------------- AV -------------
    public static native int toxav_answer(long friendnum, long audio_bit_rate, long video_bit_rate);

    public static native long toxav_iteration_interval();

    public static native int toxav_call(long friendnum, long audio_bit_rate, long video_bit_rate);

    public static native int toxav_bit_rate_set(long friendnum, long audio_bit_rate, long video_bit_rate);

    public static native int toxav_call_control(long friendnum, int a_TOXAV_CALL_CONTROL);

    public static native int toxav_video_send_frame_uv_reversed(long friendnum, int frame_width_px, int frame_height_px);

    public static native int toxav_video_send_frame(long friendnum, int frame_width_px, int frame_height_px);

    public static native int toxav_video_send_frame_h264(long friendnum, int frame_width_px, int frame_height_px, long data_len);

    public static native int toxav_video_send_frame_h264_age(long friendnum, int frame_width_px, int frame_height_px, long data_len, int age_ms);

    public static native int toxav_option_set(long friendnum, long a_TOXAV_OPTIONS_OPTION, long value);

    public static native void set_av_call_status(int status);

    public static native void set_audio_play_volume_percent(int volume_percent);

    // ----------- TRIfA internal -----------
    // buffer is for incoming video (call)
    public static native long set_JNI_video_buffer(ByteBuffer buffer, int frame_width_px, int frame_height_px);

    // buffer2 is for sending video (call)
    public static native void set_JNI_video_buffer2(ByteBuffer buffer2, int frame_width_px, int frame_height_px);

    // audio_buffer is for sending audio (group and call)
    public static native void set_JNI_audio_buffer(ByteBuffer audio_buffer);

    // audio_buffer2 is for incoming audio (group and call)
    public static native void set_JNI_audio_buffer2(ByteBuffer audio_buffer2);
    // ----------- TRIfA internal -----------

    /**
     * Send an audio frame to a friend.
     * <p>
     * The expected format of the PCM data is: [s1c1][s1c2][...][s2c1][s2c2][...]...
     * Meaning: sample 1 for channel 1, sample 1 for channel 2, ...
     * For mono audio, this has no meaning, every sample is subsequent. For stereo,
     * this means the expected format is LRLRLR... with samples for left and right
     * alternating.
     *
     * @param friend_number The friend number of the friend to which to send an
     *                      audio frame.
     * @param sample_count  Number of samples in this frame. Valid numbers here are
     *                      ((sample rate) * (audio length) / 1000), where audio length can be
     *                      2.5, 5, 10, 20, 40 or 60 millseconds.
     * @param channels      Number of audio channels. Supported values are 1 and 2.
     * @param sampling_rate Audio sampling rate used in this frame. Valid sampling
     *                      rates are 8000, 12000, 16000, 24000, or 48000.
     */
    public static native int toxav_audio_send_frame(long friend_number, long sample_count, int channels, long sampling_rate);
    // --------------- AV -------------
    // --------------- AV -------------
    // --------------- AV -------------

    // -------- native methods --------
    // -------- native methods --------
    // -------- native methods --------

    // -------- called by AV native methods --------
    // -------- called by AV native methods --------
    // -------- called by AV native methods --------

    static void android_toxav_callback_call_cb_method(long friend_number, int audio_enabled, int video_enabled)
    {}

    static void android_toxav_callback_video_receive_frame_pts_cb_method(long friend_number, long frame_width_px, long frame_height_px, long ystride, long ustride, long vstride, long pts)
    {}

    static void android_toxav_callback_video_receive_frame_cb_method(long friend_number, long frame_width_px, long frame_height_px, long ystride, long ustride, long vstride)
    {}

    static void android_toxav_callback_video_receive_frame_h264_cb_method(long friend_number, long buf_size)
    {}

    static void android_toxav_callback_call_state_cb_method(long friend_number, int a_TOXAV_FRIEND_CALL_STATE)
    {}

    static void android_toxav_callback_bit_rate_status_cb_method(long friend_number, long audio_bit_rate, long video_bit_rate)
    {}

    static void android_toxav_callback_call_comm_cb_method(long friend_number, long a_TOXAV_CALL_COMM_INFO, long comm_number)
    {}

    static long global_last_audio_ts_no_correction = 0;
    static long global_last_audio_pts = 0;
    static long global_last_audio_ts = 0;

    static long global_last_video_ts_no_correction = 0;
    static long global_last_video_pts = 0;
    static long global_last_video_ts = 0;

    static void android_toxav_callback_audio_receive_frame_pts_cb_method(long friend_number, long sample_count, int channels, long sampling_rate, long pts)
    {}

    static void android_toxav_callback_audio_receive_frame_cb_method(long friend_number, long sample_count, int channels, long sampling_rate)
    {}

    static void android_toxav_callback_group_audio_receive_frame_cb_method(long conference_number, long peer_number, long sample_count, int channels, long sampling_rate)
    {}

    // -------- called by AV native methods --------
    // -------- called by AV native methods --------
    // -------- called by AV native methods --------


    // -------- called by native methods --------
    // -------- called by native methods --------
    // -------- called by native methods --------

    static void android_tox_callback_self_connection_status_cb_method(int a_TOX_CONNECTION)
    {
        final int connection_status_prev = global_self_connection_status;
        Log.i(TAG, "self_connection_status:" + a_TOX_CONNECTION);
        global_self_connection_status = a_TOX_CONNECTION;
        TrifaToxService.write_debug_file("CB_SELF_CONN_STATUS__cstatus:" + a_TOX_CONNECTION + "_b:" + bootstrapping);

        if ((connection_status_prev == TOX_CONNECTION_NONE.value) && (a_TOX_CONNECTION != TOX_CONNECTION_NONE.value))
        {
            // we just went online
            append_logger_msg(TAG + "::" + "went online:self connection status=" + a_TOX_CONNECTION);
        }
        else if ((connection_status_prev != TOX_CONNECTION_NONE.value) && (a_TOX_CONNECTION == TOX_CONNECTION_NONE.value))
        {
            // we just went OFFLINE
            append_logger_msg(TAG + "::" + "went OFFLINE:self connection status=" + a_TOX_CONNECTION);
        }

        if (bootstrapping)
        {
            Log.i(TAG, "self_connection_status:bootstrapping=true");

            // we just went online
            if (a_TOX_CONNECTION != 0)
            {
                Log.i(TAG, "self_connection_status:bootstrapping set to false");
                bootstrapping = false;
                global_self_last_went_online_timestamp = System.currentTimeMillis();
                global_self_last_went_offline_timestamp = -1;
            }
            else
            {
                global_self_last_went_offline_timestamp = System.currentTimeMillis();
            }
        }
        else
        {
            if (a_TOX_CONNECTION != 0)
            {
                global_self_last_went_online_timestamp = System.currentTimeMillis();
                global_self_last_went_offline_timestamp = -1;

                Log.i(TAG, "self_connection_status:went_online");
                // TODO: stop any active calls
            }
            else
            {
                global_self_last_went_offline_timestamp = System.currentTimeMillis();
            }
        }

        // -- notification ------------------
        // -- notification ------------------
        tox_notification_change_wrapper(a_TOX_CONNECTION, "");
        // -- notification ------------------
        // -- notification ------------------
    }

    static void android_tox_callback_friend_name_cb_method(long friend_number, String friend_name, long length)
    {
        // Log.i(TAG, "friend_alias_name:friend:" + friend_number + " name:" + friend_alias_name);
        FriendList f = main_get_friend(friend_number);

        // Log.i(TAG, "friend_alias_name:002:" + f);
        if (f != null)
        {
            f.name = friend_name;
            HelperFriend.update_friend_in_db_name(f);
            HelperFriend.update_single_friend_in_friendlist_view(f);
        }
    }

    static void android_tox_callback_friend_status_message_cb_method(long friend_number, String status_message, long length)
    {
        // Log.i(TAG, "friend_status_message:friend:" + friend_number + " status message:" + status_message);
        FriendList f = main_get_friend(friend_number);

        if (f != null)
        {
            f.status_message = status_message;
            HelperFriend.update_friend_in_db_status_message(f);
            HelperFriend.update_single_friend_in_friendlist_view(f);
        }
    }

    static void android_tox_callback_friend_status_cb_method(long friend_number, int a_TOX_USER_STATUS)
    {
        // Log.i(TAG, "friend_status:friend:" + friend_number + " status:" + a_TOX_USER_STATUS);
        FriendList f = main_get_friend(friend_number);

        if (f != null)
        {
            f.TOX_USER_STATUS = a_TOX_USER_STATUS;
            // Log.i(TAG, "friend_status:2:f.TOX_USER_STATUS=" + f.TOX_USER_STATUS);
            HelperFriend.update_friend_in_db_status(f);

            try
            {
                if (message_list_activity != null)
                {
                    // Log.i(TAG, "friend_status:002");
                    message_list_activity.set_friend_status_icon();
                    // Log.i(TAG, "friend_status:003");
                }
            }
            catch (Exception e)
            {
                // e.printStackTrace();
                Log.i(TAG, "friend_status:EE1:" + e.getMessage());
            }

            HelperFriend.update_single_friend_in_friendlist_view(f);
        }
    }

    static void android_tox_callback_friend_connection_status_cb_method(long friend_number, int a_TOX_CONNECTION)
    {
        FriendList f = main_get_friend(friend_number);
        // Log.i(TAG, "friend_connection_status:pubkey=" + f.tox_public_key_string + " friend:" +
        //           get_friend_name_from_pubkey(f.tox_public_key_string) + " connection status:" + a_TOX_CONNECTION);

        if (f != null)
        {
            if (f.TOX_CONNECTION_real != a_TOX_CONNECTION)
            {
                if (f.TOX_CONNECTION_real == TOX_CONNECTION_NONE.value)
                {
                    // ******** friend just came online ********
                    // update and save this friends TOX CAPABILITIES
                    long friend_capabilities = tox_friend_get_capabilities(friend_number);
                    // Log.i(TAG, "" + get_friend_name_from_num(friend_number) + " friend_capabilities: " + friend_capabilities + " decoded:" + TOX_CAPABILITY_DECODE_TO_STRING(TOX_CAPABILITY_DECODE(friend_capabilities)) + " " + (1L << 63L));
                    f.capabilities = friend_capabilities;
                    update_friend_in_db_capabilities(f);
                }
            }

            if (f.TOX_CONNECTION_real != a_TOX_CONNECTION)
            {
                if (f.TOX_CONNECTION_real == TOX_CONNECTION_NONE.value)
                {
                    // TODO: check when we have actual FTs that are stale
                    // ******** friend just came online ********
                    // check for stale filetransfers
                    try
                    {
                        // Log.i(TAG, "check_for_stale_ft:001:friend=" + f);
                        List<com.zoffcc.applications.sorm.Filetransfer> fts_active = orma.selectFromFiletransfer().file_numberNotEq(-1).kindEq(
                                TOX_FILE_KIND_DATA.value).tox_public_key_stringEq(f.tox_public_key_string).toList();
                        for (com.zoffcc.applications.sorm.Filetransfer ft : fts_active)
                        {
                            // Log.i(TAG, "check_for_stale_ft:002:ft=" + ft);

                            ByteBuffer file_id_buffer = ByteBuffer.allocateDirect(TOX_FILE_ID_LENGTH);
                            tox_file_get_file_id(friend_number, ft.file_number, file_id_buffer);
                            final String file_id_buffer_hex = HelperGeneric.bytesToHex(file_id_buffer.array(),
                                                                                       file_id_buffer.arrayOffset(),
                                                                                       file_id_buffer.limit()).toUpperCase();
                            //Log.i(TAG, "check_for_stale_ft:003:ft_hex_from_db=" + ft.tox_file_id_hex +
                            //           " file_id_buffer_hex=" + file_id_buffer_hex);

                        }
                    }
                    catch (Exception e)
                    {
                        // e.printStackTrace();
                        // Log.i(TAG, "check_for_stale_ft:088:EE:" + e.getMessage());
                    }
                }
            }

            if (a_TOX_CONNECTION != TOX_CONNECTION_NONE.value)
            {
                try
                {
                    final String ip_str = tox_friend_get_connection_ip(tox_friend_by_public_key__wrapper(f.tox_public_key_string));
                    f.ip_addr_str = ip_str.replaceAll("\0", "");
                    update_friend_in_db_ip_addr_str(f);
                }
                catch(Exception e)
                {
                    f.ip_addr_str = "";
                    update_friend_in_db_ip_addr_str(f);
                }
            }
            else
            {
                f.ip_addr_str = "";
                update_friend_in_db_ip_addr_str(f);
            }

            if (f.TOX_CONNECTION_real != a_TOX_CONNECTION)
            {
                if (f.TOX_CONNECTION_real == TOX_CONNECTION_NONE.value)
                {
                    // ******** friend just came online ********
                    if (PREF__use_push_service)
                    {
                        if (!is_any_relay(f.tox_public_key_string))
                        {
                            send_pushurl_to_friend(f.tox_public_key_string);
                        }
                    }
                }
            }

            if (f.TOX_CONNECTION_real != a_TOX_CONNECTION)
            {
                if (a_TOX_CONNECTION == 0)
                {
                    // Log.i(TAG, "friend_connection_status:friend:" + friend_number + ":went offline");
                    // TODO: stop any active calls to/from this friend
                    try
                    {
                        if (tox_friend_by_public_key__wrapper(Callstate.friend_pubkey) == friend_number)
                        {
                        }
                    }
                    catch (Exception e2)
                    {
                        e2.printStackTrace();
                    }
                }

                f.TOX_CONNECTION_real = a_TOX_CONNECTION;
                f.TOX_CONNECTION_on_off_real = HelperGeneric.get_toxconnection_wrapper(f.TOX_CONNECTION);
                HelperFriend.update_friend_in_db_connection_status_real(f);
            }

            HelperGeneric.update_friend_connection_status_helper(a_TOX_CONNECTION, f, false);

            // update connection status bar color on calling activity
            if (friend_number == tox_friend_by_public_key__wrapper(Callstate.friend_pubkey))
            {
                try
                {
                }
                catch (Exception e)
                {
                }
            }
        }
    }

    static void android_tox_callback_friend_typing_cb_method(long friend_number, final int typing)
    {
        // Log.i(TAG, "friend_typing_cb:fn=" + friend_number + " typing=" + typing);
        final long friend_number_ = friend_number;
        Runnable myRunnable = new Runnable()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void run()
            {
                try
                {
                    if (message_list_activity != null)
                    {
                        if (ml_friend_typing != null)
                        {
                            if (message_list_activity.get_current_friendnum() == friend_number_)
                            {
                                if (typing == 1)
                                {
                                    ml_friend_typing.setText(R.string.MainActivity_friend_is_typing);
                                }
                                else
                                {
                                    ml_friend_typing.setText("");
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    // e.printStackTrace();
                    Log.i(TAG, "friend_typing_cb:EE:" + e.getMessage());
                }
            }
        };

        if (main_handler_s != null)
        {
            main_handler_s.post(myRunnable);
        }
    }

    static void android_tox_callback_friend_read_receipt_message_v2_cb_method(final long friend_number, long ts_sec, byte[] msg_id)
    {
    }

    static void android_tox_callback_friend_read_receipt_cb_method(long friend_number, long message_id)
    {
    }

    static void android_tox_callback_friend_request_cb_method(String friend_public_key, String friend_request_message, long length)
    {
        // Log.i(TAG, "friend_request:friend:" + friend_public_key + " friend request message:" + friend_request_message);
        // Log.i(TAG, "friend_request:friend:" + friend_public_key.substring(0, TOX_PUBLIC_KEY_SIZE * 2) +
        //            " friend request message:" + friend_request_message);
        String friend_public_key__ = friend_public_key.substring(0, TOX_PUBLIC_KEY_SIZE * 2);
        HelperFriend.add_friend_to_system(friend_public_key__.toUpperCase(), false, null);

        display_toast(context_s.getString(R.string.invite_friend_success), false, 300);
    }

    static void android_tox_callback_friend_message_v2_cb_method(long friend_number, String friend_message, long length, long ts_sec, long ts_ms, byte[] raw_message, long raw_message_length)
    {
        int msg_type = 1;
        long pin_timestamp = System.currentTimeMillis();

        ByteBuffer raw_message_buf = ByteBuffer.allocateDirect((int) raw_message_length);
        raw_message_buf.put(raw_message, 0, (int) raw_message_length);

        ByteBuffer msg_id_buffer = ByteBuffer.allocateDirect(TOX_HASH_LENGTH);
        MainActivity.tox_messagev2_get_message_id(raw_message_buf, msg_id_buffer);

        HelperFriend.send_friend_msg_receipt_v2_wrapper(friend_number, msg_type, msg_id_buffer,
                                                        (pin_timestamp / 1000));
    }

    static void android_tox_callback_friend_lossless_packet_cb_method(long friend_number, byte[] data, long length)
    {
        // Log.i(TAG, "friend_lossless_packet_cb::IN:fn=" + get_friend_name_from_num(friend_number) + " len=" + length +
        //           " data=" + bytes_to_hex(data));

        // Log.i(TAG, "GEO: " + data[0]);
        // Log.i(TAG, "GEO: " + data);

        try
        {
            if (length > 0)
            {
                if (data[0] ==
                    (byte) CONTROL_PROXY_MESSAGE_TYPE_PROXY_PUBKEY_FOR_FRIEND.value) //GEO_COORDS_CUSTOM_LOSSLESS_ID)
                {
                    final String geo_data_raw = new String(Arrays.copyOfRange(data, 1, data.length),
                                                           StandardCharsets.UTF_8);
                    // Log.i(TAG, "GEO: " + geo_data_raw);

                    // example data: TzGeo00:BEGINGEO:48.13:16.45:0.0:22.03:124.1:ENDGEO

                    String[] separated = geo_data_raw.split(":");
                    if (separated[0].equals("TzGeo00"))
                    {
                        if (separated[1].equals("BEGINGEO"))
                        {
                            long current_ts_millis = System.currentTimeMillis();

                            float lat = Float.parseFloat(separated[2]);
                            float lon = Float.parseFloat(separated[3]);
                            // float alt = Float.parseFloat(separated[4]); // not used
                            float acc = Float.parseFloat(separated[5]);
                            float bearing = Float.parseFloat(separated[6]);
                            remote_location_overlay.setLocation(new GeoPoint(lat, lon));
                            remote_location_overlay.setAccuracy(Math.round(acc));
                            remote_location_overlay.setBearing(bearing);
                            map.invalidate();

                            Runnable myRunnable = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {

                                        long diff = new Date(current_ts_millis).getTime() - new Date(last_remote_location_ts_millis).getTime();

                                        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
                                        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

                                        if (minutes > 0)
                                        {
                                            debug_text_2.setText("remote location: " + "\n" +
                                                                 "lat: " + lat + "\n" +
                                                                 "lon: " + lon + "\n" +
                                                                 "accur: " + acc + "\n" +
                                                                 minutes + " minutes ago");
                                        }
                                        else if (seconds < 1)
                                        {
                                            debug_text_2.setText("remote location: " + "\n" +
                                                                 "lat: " + lat + "\n" +
                                                                 "lon: " + lon + "\n" +
                                                                 "accur: " + acc + "\n" +
                                                                 "time: " + MainActivity.df_date_time_long.format(new Date(System.currentTimeMillis())));
                                        }
                                        else
                                        {
                                            debug_text_2.setText("remote location: " + "\n" +
                                                                 "lat: " + lat + "\n" +
                                                                 "lon: " + lon + "\n" +
                                                                 "accur: " + acc + "\n" +
                                                                 seconds + " seconds ago");
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        Log.i(TAG, "EE.b:" + e.getMessage());
                                    }
                                }
                            };

                            if (main_handler_s != null)
                            {
                                main_handler_s.post(myRunnable);
                            }

                            last_remote_location_ts_millis = current_ts_millis;
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
        }
    }

    static void android_tox_callback_friend_sync_message_v2_cb_method(long friend_number, long ts_sec, long ts_ms, byte[] raw_message, long raw_message_length, byte[] raw_data, long raw_data_length)
    {
        final ByteBuffer raw_message_buf_wrapped = ByteBuffer.allocateDirect((int) raw_data_length);
        raw_message_buf_wrapped.put(raw_data, 0, (int) raw_data_length);

        ByteBuffer msg_id_buffer_wrapped = ByteBuffer.allocateDirect(TOX_HASH_LENGTH);
        tox_messagev2_get_message_id(raw_message_buf_wrapped, msg_id_buffer_wrapped);

        send_friend_msg_receipt_v2_wrapper(friend_number, 4, msg_id_buffer_wrapped,
                                           (System.currentTimeMillis() / 1000));
    }

    // --- incoming message ---
    // --- incoming message ---
    // --- incoming message ---
    static void android_tox_callback_friend_message_cb_method(long friend_number, int message_type, String friend_message, long length, byte[] msgV3hash_bin, long message_timestamp)
    {
        if (PREF__X_battery_saving_mode)
        {
            Log.i(TAG, "global_last_activity_for_battery_savings_ts:007:*PING*");
        }

        // Log.i(TAG, "android_tox_callback_friend_message_cb_method: " + friend_message);

        String msgV3hash_hex_string = null;
        if (msgV3hash_bin != null)
        {
            msgV3hash_hex_string = HelperGeneric.bytesToHex(msgV3hash_bin, 0, msgV3hash_bin.length);

            int got_messages = orma.selectFromMessage().tox_friendpubkeyEq(
                    HelperFriend.tox_friend_get_public_key__wrapper(friend_number)).directionEq(0).msg_idv3_hashEq(
                    msgV3hash_hex_string).count();

            if (got_messages > 0)
            {
                HelperMessage.send_msgv3_high_level_ack(friend_number, msgV3hash_hex_string);
                return;
            }
        }
    }
    // --- incoming message ---
    // --- incoming message ---
    // --- incoming message ---

    static void android_tox_callback_file_recv_control_cb_method(long friend_number, long file_number, int a_TOX_FILE_CONTROL)
    {
        if (a_TOX_FILE_CONTROL == TOX_FILE_CONTROL_CANCEL.value)
        {
        }
        else
        {
            try
            {
                tox_file_control(friend_number, file_number, TOX_FILE_CONTROL_CANCEL.value);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    static void android_tox_callback_file_chunk_request_cb_method(long friend_number, long file_number, long position, long length)
    {
    }

    static void android_tox_callback_file_recv_cb_method(long friend_number, long file_number, int a_TOX_FILE_KIND, long file_size, String filename, long filename_length)
    {
        try
        {
            tox_file_control(friend_number, file_number, TOX_FILE_CONTROL_CANCEL.value);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /** @noinspection ExtractMethodRecommender*/
    static void android_tox_callback_file_recv_chunk_cb_method(long friend_number, long file_number, long position, byte[] data, long length)
    {
    }

    // void test(int i)
    // {
    //    Log.i(TAG, "test:" + i);
    // }

    static void android_tox_log_cb_method(int a_TOX_LOG_LEVEL, String file, long line, String function, String message)
    {
        if (CTOXCORE_NATIVE_LOGGING)
        {
            Log.i(TAG, "C-TOXCORE:" + ToxVars.TOX_LOG_LEVEL.value_str(a_TOX_LOG_LEVEL) + ":file=" + file + ":linenum=" +
                       line + ":func=" + function + ":msg=" + message);
        }
    }

    static void logger_XX(int level, String text)
    {
        Log.i(TAG, text);
    }
    // -------- called by native methods --------
    // -------- called by native methods --------
    // -------- called by native methods --------

    // -------- called by native Conference methods --------
    // -------- called by native Conference methods --------
    // -------- called by native Conference methods --------

    static void android_tox_callback_conference_connected_cb_method(long conference_number)
    {}

    static void android_tox_callback_conference_invite_cb_method(final long friend_number, final int a_TOX_CONFERENCE_TYPE, final byte[] cookie_buffer, final long cookie_length)
    {}

    static void android_tox_callback_conference_message_cb_method(long conference_number, long peer_number, int a_TOX_MESSAGE_TYPE, String message_orig, long length)
    {}

    static void android_tox_callback_conference_title_cb_method(long conference_number, long peer_number, String title, long title_length)
    {}

    static void android_tox_callback_conference_peer_name_cb_method(long conference_number, long peer_number, String name, long name_length)
    {}

    static void android_tox_callback_conference_peer_list_changed_cb_method(long conference_number)
    {}

    // ------------------------
    // this is an old toxcore 0.1.x callback
    // not called anymore!!
    // android_tox_callback_conference_peer_list_changed_cb_method --> is used now instead
    static void android_tox_callback_conference_namelist_change_cb_method(long conference_number, long peer_number, int a_TOX_CONFERENCE_STATE_CHANGE)
    {}
    // ------------------------
    // this is an old toxcore 0.1.x callback
    // not called anymore!!

    // -------- called by native Conference methods --------
    // -------- called by native Conference methods --------
    // -------- called by native Conference methods --------

    // -------- called by native new Group methods --------
    // -------- called by native new Group methods --------
    // -------- called by native new Group methods --------

    static void android_tox_callback_group_message_cb_method(long group_number, long peer_id, int a_TOX_MESSAGE_TYPE, String message_orig, long length, long message_id)
    {}

    static void android_tox_callback_group_private_message_cb_method(long group_number, long peer_id, int a_TOX_MESSAGE_TYPE, String message_orig, long length)
    {}

    static void android_tox_callback_group_privacy_state_cb_method(long group_number, final int a_TOX_GROUP_PRIVACY_STATE)
    {}

    static void android_tox_callback_group_invite_cb_method(long friend_number, final byte[] invite_data, final long invite_data_length, String group_name)
    {}

    static void android_tox_callback_group_peer_join_cb_method(long group_number, long peer_id)
    {}

    static void android_tox_callback_group_peer_exit_cb_method(long group_number, long peer_id, int a_Tox_Group_Exit_Type)
    {}

    static void android_tox_callback_group_peer_name_cb_method(long group_number, long peer_id)
    {}

    static void android_tox_callback_group_join_fail_cb_method(long group_number, int a_Tox_Group_Join_Fail)
    {}

    static void android_tox_callback_group_self_join_cb_method(long group_number)
    {}

    static void android_tox_callback_group_moderation_cb_method(long group_number, long source_peer_id, long target_peer_id, int a_Tox_Group_Mod_Event)
    {}

    static void android_tox_callback_group_connection_status_cb_method(long group_number, int a_TOX_GROUP_CONNECTION_STATUS)
    {}

    static void android_tox_callback_group_topic_cb_method(long group_number, long peer_id, String topic, long topic_length)
    {}

    static void android_tox_callback_group_custom_packet_cb_method(long group_number, long peer_id, final byte[] data, long length)
    {}

    static void android_tox_callback_group_custom_private_packet_cb_method(long group_number, long peer_id, final byte[] data, long length)
    {}

    // -------- called by native new Group methods --------
    // -------- called by native new Group methods --------
    // -------- called by native new Group methods --------

    /*
     * this is used to load the native library on
     * application startup. The library has already been unpacked at
     * installation time by the package manager.
     */
    static
    {
        try
        {
            System.loadLibrary("jni-c-toxcore");
            native_lib_loaded = true;
            Log.i(TAG, "successfully loaded jni-c-toxcore library");
        }
        catch (java.lang.UnsatisfiedLinkError e)
        {
            native_lib_loaded = false;
            Log.i(TAG, "loadLibrary jni-c-toxcore failed!");
            e.printStackTrace();
        }
    }

    public void show_add_friend(View view)
    {
        Intent intent = new Intent(this, AddFriendActivity.class);
        // intent.putExtra("key", value);
        startActivityForResult(intent, AddFriendActivity_ID);
    }

    public void show_wrong_credentials()
    {
        Intent intent = new Intent(this, WrongCredentials.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AddFriendActivity_ID)
        {
            if (resultCode == RESULT_OK)
            {
                String friend_tox_id1 = data.getStringExtra("toxid");
                String friend_tox_id = "";
                friend_tox_id = friend_tox_id1.toUpperCase().replace(" ", "").replaceFirst("tox:", "").replaceFirst(
                        "TOX:", "").replaceFirst("Tox:", "");
                HelperFriend.add_friend_real(friend_tox_id);
            }
            else
            {
                // (resultCode == RESULT_CANCELED)
            }
        }
        else if (requestCode == AddPrivateGroupActivity_ID)
        {
        }
        else if (requestCode == AddPublicGroupActivity_ID)
        {
        }
        else if (requestCode == JoinPublicGroupActivity_ID)
        {
        }
    }

    void sendEmailWithAttachment(Context c, final String recipient, final String subject, final String message, final String full_file_name, final String full_file_name_suppl)
    {
        try
        {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", recipient, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(Uri.parse("file://" + full_file_name));
            Log.i(TAG, "email:full_file_name=" + full_file_name);
            File ff = new File(full_file_name);
            Log.i(TAG, "email:full_file_name exists:" + ff.exists());

            try
            {
                if (new File(full_file_name_suppl).length() > 0)
                {
                    uris.add(Uri.parse("file://" + full_file_name_suppl));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "email:EE1:" + e.getMessage());
            }

            List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(emailIntent, 0);
            List<LabeledIntent> intents = new ArrayList<>();

            if (resolveInfos.size() != 0)
            {
                for (ResolveInfo info : resolveInfos)
                {
                    Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    Log.i(TAG, "email:" + "comp=" + info.activityInfo.packageName + " " + info.activityInfo.name);
                    intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});

                    if (subject != null)
                    {
                        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    }

                    if (message != null)
                    {
                        intent.putExtra(Intent.EXTRA_TEXT, message);
                        // ArrayList<String> extra_text = new ArrayList<String>();
                        // extra_text.add(message);
                        // intent.putStringArrayListExtra(android.content.Intent.EXTRA_TEXT, extra_text);
                        // Log.i(TAG, "email:" + "message=" + message);
                        // Log.i(TAG, "email:" + "intent extra_text=" + extra_text);
                    }

                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    intents.add(new LabeledIntent(intent, info.activityInfo.packageName,
                                                  info.loadLabel(getPackageManager()), info.icon));
                }

                try
                {
                    Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1),
                                                          "Send email with attachments");
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
                    startActivity(chooser);
                }
                catch (Exception email_app)
                {
                    email_app.printStackTrace();
                    Log.i(TAG, "email:" + "Error starting Email App");
                    new AlertDialog.Builder(c).setMessage(
                            R.string.MainActivity_error_starting_email_app).setPositiveButton(
                            R.string.MainActivity_button_ok, null).show();
                }
            }
            else
            {
                Log.i(TAG, "email:" + "No Email App found");
                new AlertDialog.Builder(c).setMessage(R.string.MainActivity_no_email_app_found).setPositiveButton(
                        R.string.MainActivity_button_ok, null).show();
            }
        }
        catch (ActivityNotFoundException e)
        {
            // cannot send email for some reason
            e.printStackTrace();
            Log.i(TAG, "email:EE2:" + e.getMessage());
        }
    }

    static String safe_string_XX(byte[] in)
    {
        Log.i(TAG, "safe_string:in=" + in);
        String out = "";

        try
        {
            out = new String(in, "UTF-8");  // Best way to decode using "UTF-8"
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "safe_string:EE:" + e.getMessage());

            try
            {
                out = new String(in);
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
                Log.i(TAG, "safe_string:EE2:" + e2.getMessage());
            }
        }

        Log.i(TAG, "safe_string:out=" + out);
        return out;
    }

    void getVersionInfo()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    static class delete_selected_messages_asynchtask extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private WeakReference<Context> weakContext;
        boolean update_message_list = false;
        boolean update_friend_list = false;
        String dialog_text = "";

        delete_selected_messages_asynchtask(Context c, ProgressDialog progressDialog2, boolean update_message_list, boolean update_friend_list, String dialog_text)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
            this.update_message_list = update_message_list;
            this.update_friend_list = update_friend_list;
            this.dialog_text = dialog_text;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            // sort ascending (lowest ID on top)
            Collections.sort(selected_messages, new Comparator<Long>()
            {
                public int compare(Long o1, Long o2)
                {
                    return o1.compareTo(o2);
                }
            });
            Iterator i = selected_messages.iterator();

            while (i.hasNext())
            {
                try
                {
                    long mid = (Long) i.next();
                    final Message m_to_delete = (Message) orma.selectFromMessage().idEq(mid).get(0);

                    // ---------- delete fileDB if this message is an outgoing file ----------
                    if (m_to_delete.TRIFA_MESSAGE_TYPE == TRIFA_MSG_FILE.value)
                    {
                        if (m_to_delete.direction == 1)
                        {
                            try
                            {
                                // cleanup duplicated outgoing files from provider here ************
                                if (m_to_delete.storage_frame_work == false)
                                {
                                    if (m_to_delete.filename_fullpath.startsWith(SD_CARD_FILES_OUTGOING_WRAPPER_DIR))
                                    {
                                        // HINT: real file (no storage framework) and correct directory, delete the file now
                                        try
                                        {
                                            new File(m_to_delete.filename_fullpath).delete();
                                        }
                                        catch (Exception e)
                                        {
                                        }
                                    }
                                }
                                // FileDB file_ = orma.selectFromFileDB().idEq(m_to_delete.filedb_id).get(0);
                                orma.deleteFromFileDB().idEq(m_to_delete.filedb_id).execute();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                Log.i(TAG, "delete_selected_messages_asynchtask:EE4:" + e.getMessage());
                            }
                        }
                    }
                    // ---------- delete fileDB if this message is an outgoing file ----------

                    // ---------- delete fileDB and VFS file if this message is an incoming file ----------
                    if (m_to_delete.TRIFA_MESSAGE_TYPE == TRIFA_MSG_FILE.value)
                    {
                        if (m_to_delete.direction == 0)
                        {
                            try
                            {
                                FileDB file_ = (FileDB) orma.selectFromFileDB().idEq(m_to_delete.filedb_id).get(0);

                                try
                                {
                                    info.guardianproject.iocipher.File f_vfs = new info.guardianproject.iocipher.File(
                                            file_.path_name + "/" + file_.file_name);

                                    if (f_vfs.exists())
                                    {
                                        f_vfs.delete();
                                    }
                                }
                                catch (Exception e6)
                                {
                                    e6.printStackTrace();
                                    Log.i(TAG, "delete_selected_messages_asynchtask:EE5:" + e6.getMessage());
                                }

                                orma.deleteFromFileDB().idEq(m_to_delete.filedb_id).execute();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                Log.i(TAG, "delete_selected_messages_asynchtask:EE4:" + e.getMessage());
                            }
                        }
                    }

                    // ---------- delete fileDB and VFS file if this message is an incoming file ----------

                    // ---------- delete the message itself ----------
                    try
                    {
                        long message_id_to_delete = m_to_delete.id;

                        try
                        {
                            if (update_message_list)
                            {
                                Runnable myRunnable = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            MainActivity.message_list_fragment.adapter.remove_item(m_to_delete);
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                };

                                if (main_handler_s != null)
                                {
                                    main_handler_s.post(myRunnable);
                                }
                            }

                            // let message delete animation finish (maybe use yet another asynctask here?) ------------
                            try
                            {
                                if (update_message_list)
                                {
                                    Thread.sleep(50);
                                }
                            }
                            catch (Exception sleep_ex)
                            {
                                sleep_ex.printStackTrace();
                            }

                            // let message delete animation finish (maybe use yet another asynctask here?) ------------
                            orma.deleteFromMessage().idEq(message_id_to_delete).execute();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            Log.i(TAG, "delete_selected_messages_asynchtask:EE1:" + e.getMessage());
                        }
                    }
                    catch (Exception e2)
                    {
                        e2.printStackTrace();
                        Log.i(TAG, "delete_selected_messages_asynchtask:EE2:" + e2.getMessage());
                    }

                    // ---------- delete the message itself ----------
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                    Log.i(TAG, "delete_selected_messages_asynchtask:EE3:" + e2.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            selected_messages.clear();
            selected_messages_incoming_file.clear();
            selected_messages_text_only.clear();

            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
                Toast.makeText(c, R.string.MainActivity_toast_msg_deleted, Toast.LENGTH_SHORT).show();
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "save_selected_messages_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            if (this.progressDialog2 == null)
            {
                try
                {
                    Context c = weakContext.get();
                    progressDialog2 = ProgressDialog.show(c, "", dialog_text);
                    progressDialog2.setCanceledOnTouchOutside(false);
                    progressDialog2.setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "onPreExecute:start:EE:" + e.getMessage());
                }
            }
        }
    }

    static class save_selected_messages_asynchtask extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private WeakReference<Context> weakContext;
        private String export_directory = "";

        save_selected_messages_asynchtask(Context c, ProgressDialog progressDialog2)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            Iterator i = selected_messages_incoming_file.iterator();

            while (i.hasNext())
            {
                try
                {
                    long mid = (Long) i.next();
                    Message m = (Message) orma.selectFromMessage().idEq(mid).get(0);
                    FileDB file_ = (FileDB) orma.selectFromFileDB().idEq(m.filedb_id).get(0);
                    HelperGeneric.export_vfs_file_to_real_file(file_.path_name, file_.file_name,
                                                               SD_CARD_FILES_EXPORT_DIR + "/" + m.tox_friendpubkey +
                                                               "/", file_.file_name);

                    export_directory = SD_CARD_FILES_EXPORT_DIR + "/" + m.tox_friendpubkey + "/";
                }
                catch (Exception e2)
                {
                    e2.printStackTrace();
                    Log.i(TAG, "save_selected_messages_asynchtask:EE1:" + e2.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            selected_messages.clear();
            selected_messages_incoming_file.clear();
            selected_messages_text_only.clear();

            try
            {
                // need to redraw all items again here, to remove the selections
                MainActivity.message_list_fragment.adapter.redraw_all_items();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "save_selected_messages_asynchtask:EE2:" + e.getMessage());
            }

            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
                Toast.makeText(c, "Files exported to:" + "\n" + export_directory, Toast.LENGTH_SHORT).show();
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "save_selected_messages_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
        }
    }

    static class save_selected_message_custom_asynchtask extends AsyncTask<Void, Void, String>
    {
        ProgressDialog progressDialog2;
        private final WeakReference<Context> weakContext;
        private final String export_directory = "";
        private final FileDB f;
        private final String fname;
        private final View v;
        private final RecyclerView.Adapter<? extends RecyclerView.ViewHolder> a;
        private final int pos;

        save_selected_message_custom_asynchtask(Context c, ProgressDialog progressDialog2, FileDB file_, String export_filename, RecyclerView.Adapter<? extends RecyclerView.ViewHolder> a, int pos_in_adaper, View v)
        {
            this.weakContext = new WeakReference<>(c);
            this.progressDialog2 = progressDialog2;
            this.f = file_;
            this.fname = export_filename;
            this.v = v;
            this.a = a;
            this.pos = pos_in_adaper;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                HelperGeneric.export_vfs_file_to_real_file(f.path_name, f.file_name, fname, f.file_name);
            }
            catch (Exception e)
            {
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            try
            {
                a.notifyItemChanged(pos);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                progressDialog2.dismiss();
                Context c = weakContext.get();
                display_toast_with_context_custom_duration(c, "File exported to:" + "\n" + fname + f.file_name, 400, 0);
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "save_selected_message_custom_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
        }
    }

    static class save_selected_group_message_custom_asynchtask extends AsyncTask<Void, Void, String>
    {
        private final WeakReference<Context> weakContext;
        private final String export_directory = "";
        private final String saved_path;
        private final String saved_file;
        private final String fname;
        private final View v;

        save_selected_group_message_custom_asynchtask(Context c, String saved_path, String saved_file,
                                                      String export_dst_pathname, View v)
        {
            this.weakContext = new WeakReference<>(c);
            this.saved_path = saved_path;
            this.saved_file = saved_file;
            this.fname = export_dst_pathname;
            this.v = v;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                HelperGeneric.export_vfs_file_to_real_file(saved_path, saved_file, fname, saved_file);
            }
            catch (Exception e)
            {
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            try
            {
                Context c = weakContext.get();
                display_toast_with_context_custom_duration(c, "File exported to:" + "\n" + fname + saved_file, 400, 0);
            }
            catch (Exception e4)
            {
                e4.printStackTrace();
                Log.i(TAG, "save_selected_group_message_custom_asynchtask:EE3:" + e4.getMessage());
            }
        }

        @Override
        protected void onPreExecute()
        {
        }
    }

    static class send_message_result
    {
        long msg_num;
        boolean msg_v2;
        String msg_hash_hex;
        String msg_hash_v3_hex;
        String raw_message_buf_hex;
        long error_num;
    }

    /*************************************************************************/
    /* this function now really sends a 1:1 to a friend (or a friends relay) */
    private void fadeInAndShowImage(final View img, long start_after_millis)
    {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(1000);
        fadeIn.setStartOffset(start_after_millis);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
            }

            public void onAnimationRepeat(Animation animation)
            {
            }

            public void onAnimationStart(Animation animation)
            {
                img.setVisibility(View.VISIBLE);
            }
        });
        img.startAnimation(fadeIn);
    }

    private void fadeOutAndHideImage(final View img, long start_after_millis)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(1000);
        fadeOut.setStartOffset(start_after_millis);
        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                img.setVisibility(View.GONE);
            }

            public void onAnimationRepeat(Animation animation)
            {
            }

            public void onAnimationStart(Animation animation)
            {
            }
        });
        img.startAnimation(fadeOut);
    }


    // --------- make app crash ---------
    // --------- make app crash ---------
    // --------- make app crash ---------
    public static void crash_app_java(int type)
    {
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======================+++++");
        System.out.println("+++++======= TYPE:J =======+++++");
        System.out.println("+++++======================+++++");

        if (type == 1)
        {
            Java_Crash_001();
        }
        else if (type == 2)
        {
            Java_Crash_002();
        }
        else
        {
            stackOverflow();
        }
    }

    public static void Java_Crash_001()
    {
        Integer i = null;
        i.byteValue();
    }

    public static void Java_Crash_002()
    {
        View v = null;
        v.bringToFront();
    }

    static byte[] cipherWriteRandomByteReturnBuf(int bytes, String filename) {
        byte[] random_buf;
        try {
            info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(filename);
            info.guardianproject.iocipher.FileOutputStream out = new info.guardianproject.iocipher.FileOutputStream(f);

            Random prng = new Random();
            random_buf = new byte[bytes];
            prng.nextBytes(random_buf);

            out.write(random_buf);
            out.close();

        } catch (IOException e) {
            Log.e(TAG, e.getCause().toString());
            return null;
        }
        return random_buf;
    }

    public static void VFS_Append_test_001()
    {
        try {
            for(int i=0;i<3;i++)
            {
                String name = "/testFileManySizes_" + i;
                info.guardianproject.iocipher.File f = new info.guardianproject.iocipher.File(name);
                byte[] bufrandom = cipherWriteRandomByteReturnBuf(i, name);
                Log.v(TAG, "write: bytes=" + i);

                f = new info.guardianproject.iocipher.File(name);
                FileOutputStream out = new FileOutputStream(f, true);
                Log.v(TAG, "append: length:before=" + f.length());
                for(int k=0;k<2;k++)
                {
                    out.write(19);
                    Log.v(TAG, "append: length:+k=" + f.length());
                }
                out.close();

                f = new info.guardianproject.iocipher.File(name);
                byte[] orig_in = new byte[i];
                FileInputStream in = new FileInputStream(f);
                in.read(orig_in, 0, i);
                //*****//assertEquals(i + 2, f.length());
                for(int j=0;j<i;j++)
                {
                    //*****//assertEquals(bufrandom[j], orig_in[j]);
                }
                Log.v(TAG, "CMP: " + bytes_to_hex(bufrandom) + " <--> " + bytes_to_hex(orig_in));
                Log.v(TAG, "read: bytes=" + i + " OK");
                in.close();

                f = new info.guardianproject.iocipher.File(name);
                f.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getCause().toString());
        }
    }

    public static void stackOverflow()
    {
        stackOverflow();
    }

    public static void crash_app_C()
    {
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======================+++++");
        System.out.println("+++++======= TYPE:C =======+++++");
        System.out.println("+++++======================+++++");
        AppCrashC();
    }

    public static native void AppCrashC();
    // --------- make app crash ---------
    // --------- make app crash ---------
    // --------- make app crash ---------

}

