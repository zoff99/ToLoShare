@file:Suppress("PropertyName", "ConvertToStringTemplate", "FunctionName", "PrivatePropertyName",
    "LocalVariableName"
)

package com.zoffcc.applications.trifa

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage


class PushServiceImpl : PushService() {
    val TAG = "TRIFA_PUSH"

    private val context = this
    override fun onMessage(message: PushMessage, instance: String) {
        var msg = ""
        try {
            msg = message.content.toString(Charsets.UTF_8)
        } catch (_: Exception) {
        }

        Log.d(TAG, "onMessage(): " + msg + " instance: " + instance)

        try {
            // wake up trifa here ------------------
            val intent = Intent()
            intent.setAction("com.zoffcc.applications.toloshare.EXTERN_RECV")
            intent.putExtra("task", "wakeup")
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            intent.setComponent(
                ComponentName(
                    "com.zoffcc.applications.toloshare",
                    "com.zoffcc.applications.toloshare.MyExternReceiver"
                )
            )
            // context.sendBroadcast(intent)
            MyExternReceiver.onReceive(context, intent)
            // wake up trifa here ------------------
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // sendNotification(context, msg)
    }

    /*
    private fun sendNotification(context: Context, messageBody: String?) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, 0,  /* Request code */intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId: String? = context.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder = Builder(
            context,
            channelId
        ).setSmallIcon(
            R.drawable.ic_stat_ic_notification
        ).setContentTitle(
            context.getString(R.string.fcm_message)
        ).setContentText(messageBody).setAutoCancel(true).setSound(
            defaultSoundUri
        ).setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build)
    }
    */

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        Log.d(TAG, "New Endpoint: ${endpoint.url}")

        val intent = Intent()
        intent.setAction("com.zoffcc.applications.toloshare.TOKEN_CHANGED")
        intent.putExtra("token", endpoint.url)
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        intent.setComponent(
            ComponentName(
                "com.zoffcc.applications.toloshare",
                "com.zoffcc.applications.toloshare.MyTokenReceiver"
            )
        )
        /*
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        */
        token_changed(intent)
    }

    private var token_wakeup_lock: PowerManager.WakeLock? = null

    @SuppressLint("ObsoleteSdkInt")
    fun token_changed(intent2: Intent)
    {
        Log.i(TAG, "got intent: " + intent2)
        try
        {
            if (token_wakeup_lock == null)
            {
                val pm = context.getSystemService(POWER_SERVICE) as PowerManager
                token_wakeup_lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "trifa:trifa_token_wakeup_lock")
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        val t = Thread()
        {
            @Override
            run()
            {
                try
                {
                    token_wakeup_lock?.isHeld?.let {
                        if (!it) {
                            token_wakeup_lock?.acquire(10*1000L /*10 seconds*/)
                            Log.i(TAG, "acquiring wakelock")
                        }
                    }
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                }

                try
                {
                    if (isMyServiceRunning(TrifaToxService::class.java.getName(), context)) {
                        Log.i(TAG, "TrifaToxService running")
                    } else {
                        Log.i(TAG, "TrifaToxService NOT running")

                        if (Build.VERSION.SDK_INT < 29) {
                            // TODO: this is not working anymore starting with Android 10
                            // https://developer.android.com/guide/components/activities/background-starts
                            // thanks Google

                            val open_trifa_intent =
                                Intent(context, StartMainActivityWrapper::class.java)
                            open_trifa_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(open_trifa_intent)
                            Log.i(TAG, "activity started")
                        } else {
                            Log.i(TAG, "API:" + Build.VERSION.SDK_INT)
                            try {
                                Log.i(TAG, "notify")
                            } catch (e2: java.lang.Exception) {
                                e2.printStackTrace()
                                Log.i(TAG, "show_noti:EE02:" + e2.message)
                            }
                        }

                        try {
                            Thread.sleep((20 * 1000).toLong()) // wait for 20 seconds
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                    Log.i(TAG, "TrifaToxService startup:EE01:" + e.message)
                }

                var tokenReceived: String? = null
                try {
                    val extras = intent2.extras
                    if (extras == null) {
                        Log.i(TAG, "couldn't get the token")
                    } else {
                        tokenReceived = extras.getString("token")
                        if (tokenReceived != null) {
                            if (tokenReceived.length > TRIFAGlobals.NOTIFICATION_NTFY_PUSH_URL_PREFIX.length) {
                                if (tokenReceived.startsWith(TRIFAGlobals.NOTIFICATION_NTFY_PUSH_URL_PREFIX)) {
                                    if (tokenReceived.endsWith("?up=1")) {
                                        // HINT: remove old "?up=1" from ntfy.sh token
                                        tokenReceived = tokenReceived.substring(
                                            0,
                                            tokenReceived.length - ("?up=1").length
                                        )
                                    }
                                }
                            }

                            Log.i(TAG, "token received: " + "xxxxxxxxxxxxx")
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                val current_push_token = HelperGeneric.get_g_opts(TRIFAGlobals.NOTIFICATION_TOKEN_DB_KEY)
                // Log.i(TAG, "MyTokenReceiver:" + "current_push_token:" + current_push_token)
                // Log.i(TAG, "MyTokenReceiver:" + "tokenReceived:" + tokenReceived)
                try {
                    if (TrifaToxService.trifa_service_thread != null) {
                        if (
                            (current_push_token.isNullOrEmpty())
                            ||
                            ((!tokenReceived.isNullOrEmpty()) && (current_push_token.compareTo(tokenReceived!!) != 0))
                            ) {
                            Log.i(TAG, "MyTokenReceiver:" + "onReceive")
                            HelperGeneric.trigger_proper_wakeup_outside_tox_service_thread()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                if (!current_push_token.isNullOrEmpty()) {
                    if (current_push_token == tokenReceived) {
                        // push token has changed
                        set_new_push_token(tokenReceived)
                        send_push_token_to_all_friends()
                    }
                } else {
                    if (tokenReceived != null) {
                        // push token is new (we had no push token before)
                        set_new_push_token(tokenReceived)
                        send_push_token_to_all_friends()
                    }
                }

                try {
                    Thread.sleep((10 * 1000).toLong()) // keep wakelock for 10 seconds
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }


                try {

                    token_wakeup_lock?.isHeld?.let {
                        if (!it) {
                            Log.i(TAG, "releasing wakelock")
                            token_wakeup_lock?.release()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                token_wakeup_lock = null
            }
        }
        t.start()
    }

    private fun send_push_token_to_all_friends() {
        if (MainActivity.PREF__use_push_service) {
            // now send token to all friends
            HelperFriend.send_pushurl_to_all_friends()
        }
    }

    private fun set_new_push_token(tokenReceived: String) {
        try {
            HelperGeneric.set_g_opts(
                TRIFAGlobals.NOTIFICATION_TOKEN_DB_KEY,
                tokenReceived
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun isMyServiceRunning(serviceClassName: String, c: Context): Boolean {
        try {
            val manager = c.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.Companion.MAX_VALUE)) {
                if (serviceClassName == service.service.className) {
                    return true
                }
            }
        } catch(_: Exception) {
        }
        return false
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Toast.makeText(context, "Registration Failed: $reason", Toast.LENGTH_SHORT).show()
        UnifiedPush.removeDistributor(context)
    }

    override fun onUnregistered(instance: String) {
        // Remove the endpoint on the application server
        val appName = context.getString(R.string.app_name)
        Toast.makeText(context, "$appName is unregistered", Toast.LENGTH_SHORT).show()
    }
}
