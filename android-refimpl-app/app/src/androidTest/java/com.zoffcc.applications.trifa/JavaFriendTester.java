package com.zoffcc.applications.trifa;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.screenshot.Screenshot;

import static androidx.core.content.ContextCompat.startActivity;
import static androidx.test.InstrumentationRegistry.getTargetContext;
import static androidx.test.core.graphics.BitmapStorage.writeToTestStorage;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.screenshot.ViewInteractionCapture.captureToBitmap;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.runner.lifecycle.Stage.RESUMED;
import static com.zoffcc.applications.trifa.HelperFriend.get_set_is_default_ft_contact;
import static com.zoffcc.applications.trifa.MainActivity.PREF__window_security;
import static com.zoffcc.applications.trifa.MainActivity.context_s;
import static com.zoffcc.applications.trifa.MainActivity.mLocationOverlay;
import static com.zoffcc.applications.trifa.MainActivity.main_gallery_container;
import static com.zoffcc.applications.trifa.MainActivity.main_handler_s;
import static com.zoffcc.applications.trifa.MainActivity.map;
import static com.zoffcc.applications.trifa.MainActivity.switch_normal_main_view;
import static com.zoffcc.applications.trifa.MainActivity.waiting_container;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class JavaFriendTester
{
    private static final String TAG = "TEST012";
    //
    private static final String MOCK_PASSWORD = "af672m$kesr93$§4w0984_5439wsl023%847";
    @Rule
    public ActivityScenarioRule<StartMainActivityWrapper> rule = new ActivityScenarioRule<>(
            StartMainActivityWrapper.class);
    //
    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO,
                                                                               Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                               Manifest.permission.ACCESS_COARSE_LOCATION,
                                                                               Manifest.permission.ACCESS_FINE_LOCATION);
    private static Activity currentActivity = null;

    @Test
    public void Test_Startup()
    {
        Log.i(TAG, "Test_Startup");
        ActivityScenario<StartMainActivityWrapper> scenario = rule.getScenario();

        String cur_act = getActivityInstance().getLocalClassName();
        Log.i(TAG, "ACT:" + cur_act);

        boolean showing_app = false;
        int app_showing_cycles = 0;
        while (!showing_app)
        {
            if (app_showing_cycles > 120)
            {
                Log.i(TAG, "App did not load");
                cause_error(1);
            }
            cur_act = getActivityInstance().getLocalClassName();
            // we need to full name here because app id and pkg name differ!!
            if (cur_act.equals("com.zoffcc.applications.trifa.CheckPasswordActivity"))
            {
                showing_app = true;
            }
            // we need to full name here because app id and pkg name differ!!
            else if (cur_act.equals("com.zoffcc.applications.trifa.SetPasswordActivity"))
            {
                showing_app = true;
            }
            else if (cur_act.equals("com.zoffcc.applications.trifa.CustomPinActivity"))
            {
                showing_app = true;
            }
            else
            {
                app_showing_cycles++;
            }
            wait_(1, "until app is showing");
        }

        setSharedPrefs();
        PREF__window_security = false;
        Log.i(TAG, "PREF__window_security:002=" + PREF__window_security);

        screenshot("001");
        wait_(2);

        cur_act = getActivityInstance().getLocalClassName();

        if (cur_act.equals("com.zoffcc.applications.trifa.CheckPasswordActivity"))
        {
            Log.i(TAG, "ACT:0a:" + cur_act);
            onView(withId(R.id.password_1_c)).perform(replaceText(MOCK_PASSWORD));
            screenshot("002a");
            onView(withId(R.id.set_button_2)).perform(click());
        }
        else if (cur_act.equals("com.zoffcc.applications.trifa.SetPasswordActivity"))
        {
            Log.i(TAG, "ACT:0b:" + cur_act);
            onView(withId(R.id.password_1)).perform(replaceText(MOCK_PASSWORD));
            onView(withId(R.id.password_2)).perform(replaceText(MOCK_PASSWORD));
            screenshot("002b");
            onView(withId(R.id.set_button)).perform(click());
        }
        else if (cur_act.equals("com.zoffcc.applications.trifa.CustomPinActivity"))
        {
            cause_error(12);
            Log.i(TAG, "ACT:2:" + cur_act);
            Log.i(TAG, "ACT:2:001");
            screenshot("002ap");
            Log.i(TAG, "ACT:2:002");
            // onView(withId(R.id.btn_unlock)).perform(click());
            Log.i(TAG, "ACT:2:003");
        }
        else
        {
            cause_error(2);
        }

        wait_(2);
        cur_act = getActivityInstance().getLocalClassName();

        if (cur_act.equals("com.zoffcc.applications.trifa.CheckPasswordActivity"))
        {
            Log.i(TAG, "ACT:0a:" + cur_act);
            onView(withId(R.id.password_1_c)).perform(replaceText(MOCK_PASSWORD));
            screenshot("002a");
            onView(withId(R.id.set_button_2)).perform(click());
        }
        else if (cur_act.equals("com.zoffcc.applications.trifa.SetPasswordActivity"))
        {
            Log.i(TAG, "ACT:0b:" + cur_act);
            onView(withId(R.id.password_1)).perform(replaceText(MOCK_PASSWORD));
            onView(withId(R.id.password_2)).perform(replaceText(MOCK_PASSWORD));
            screenshot("002b");
            onView(withId(R.id.set_button)).perform(click());
        }
        else if (cur_act.equals("com.zoffcc.applications.trifa.CustomPinActivity"))
        {
            // workaround -----------
            PinStorageUtil.savePin(getActivityInstance(), "");
            AppSessionManager.getInstance().setUnlocked(true);
            // workaround -----------

            Intent intent = new Intent(getActivityInstance(), StartMainActivityWrapper.class);
            // Flags ensure the user can't "back" into the protected content
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(getActivityInstance(), intent, null);

            // cause_error(12);
            getActivityInstance().finish();
            Log.i(TAG, "ACT:2:" + cur_act);
            Log.i(TAG, "ACT:2:001");
        }
        else
        {
            cause_error(2);
        }



        Log.i(TAG, "checking for AlertDialog");

        try
        {
            onView(withId(android.R.id.button2)).check(matches(isDisplayed()));

        /*
        For an AlertDialog, the id assigned for each button is:
        POSITIVE: android.R.id.button1
        NEGATIVE: android.R.id.button2
        NEUTRAL: android.R.id.button3
        */
            // click NO on Dialog asking to disable battery optimisations for app
            onView(withId(android.R.id.button2)).inRoot(isDialog()).check(matches(isDisplayed())).perform(click());
            Log.i(TAG, "AlertDialog: \"NO\" button clicked");

        }
        catch (NoMatchingViewException e)
        {
            //view not displayed logic
            Log.i(TAG, "checking for AlertDialog:View does not show, that is ok");
        }

        wait_(4);
        // switch to gallery mode by setting the switch to "OFF"
        // onView(withId(R.id.switch_normal_main_view)).check(matches(isChecked())).perform(click()).check(
        //        matches(isNotChecked()));

        screenshot("006");
        wait_(2);

        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ///********/// switch_normal_main_view.setChecked(false);

                    MainActivity.PREF__normal_main_view = false;

                    // the above does not trigger the "setOnCheckedChangeListener" for some reason

                    map.onResume();
                    mLocationOverlay.enableMyLocation();
                    main_gallery_container.bringToFront();

                    waiting_container.setVisibility(View.GONE);
                    main_gallery_container.setVisibility(View.VISIBLE);
                    main_gallery_container.bringToFront();
                    Log.i(TAG, "trigger setOnCheckedChangeListener");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "EE:002:" + e.getMessage());
                }
            }
        };
        try
        {
            if (main_handler_s != null)
            {
                main_handler_s.post(myRunnable);
            }
        }
        catch (Exception e)
        {
        }

        boolean tox_online = false;
        while (!tox_online)
        {
            tox_online = MainActivity.tox_self_get_connection_status() != 0;
            // HINT: wait for tox to get online
            wait_(1, "for tox to get online");
        }

        setSharedPrefs();
        PREF__window_security = false;
        Log.i(TAG, "PREF__window_security:001=" + PREF__window_security);

        // HINT: after we are online give it another 5 seconds
        wait_(5);

        // HINT: we are online here ----------------------
        final String mytoxid = MainActivity.get_my_toxid();
        Log.i(TAG, "my_toxid:" + mytoxid);
        testwrite("001", mytoxid);

        long loops = 0;
        final long max_loops = 120;
        int count_friends = orma.selectFromFriendList().count();
        /*
        while (count_friends < 1)
        {
            wait_(2);
            loops++;
            if (loops > max_loops)
            {
                Log.i(TAG, "ERROR: waiting too long for friend");
                break;
            }
            count_friends = orma.selectFromFriendList().count();
        }
         */

        // set first friend as default contact
        // final String def_friend_pubkey = orma.selectFromFriendList().get(0).tox_public_key_string;
        // Log.i(TAG, "def_friend_pubkey=" + def_friend_pubkey);
        // get_set_is_default_ft_contact(def_friend_pubkey, true);

        wait_(2);
        // friend should be fully added here
        screenshot("004a");

        wait_(1);
        screenshot("004b");

        wait_(12);
        screenshot("005");

        wait_(1);
        // Espresso.closeSoftKeyboard();

        wait_(2);
        // load_main_gallery_images();
        screenshot_full("007");

        wait_(40);
        Log.i(TAG, "taking last screenshot ...");
        screenshot_full("099");
        Log.i(TAG, "taking last screenshot ... DONE");
    }

    private static void testwrite(final String num, final String text)
    {
        final String file_with_path = currentActivity.getExternalFilesDir(null).getAbsolutePath() + "/" + num + ".txt";
        try
        {
            java.io.File file = new java.io.File(file_with_path);
            java.io.FileOutputStream fileOutput = new java.io.FileOutputStream(file);
            java.io.OutputStreamWriter outputStreamWriter = new java.io.OutputStreamWriter(fileOutput);
            outputStreamWriter.write(text);
            outputStreamWriter.flush();
            fileOutput.getFD().sync();
            outputStreamWriter.close();
            Log.i(TAG, "testwrite:write text: "+ file_with_path);
        }
        catch (Exception e)
        {
            Log.i(TAG, "testwrite:ERROR writing text: "+ file_with_path + " E:" + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void screenshot_full(final String num)
    {
        try
        {
            writeToTestStorage(Screenshot.capture().getBitmap(), "test_" + num);
            Log.i(TAG, "capture full screenshot: "+ "test_" + num + ".png");
        }
        catch (Exception e)
        {
            Log.i(TAG, "ERROR on capturing full screenshot: " + "test_" + num + ".png" + " E:" + e.getMessage());
            e.printStackTrace();

            // HINT: ok, then lets try the normal screenshot
            screenshot(num);
        }
    }

    private static void screenshot(final String num)
    {
        try
        {
            writeToTestStorage(captureToBitmap(onView(isRoot())), "test_" + num);
            Log.i(TAG, "capture screenshot: "+ "test_" + num + ".png");
        }
        catch (Exception e)
        {
            Log.i(TAG, "ERROR on ca pturing screenshot: "+ "test_" + num + ".png" + " E:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void wait_(final long seconds)
    {
        wait_(seconds, null);
    }

    private static void wait_(final long seconds, String custom_message_addon)
    {
        if (custom_message_addon != null)
        {
            Log.i(TAG, "sleeping " + seconds + " seconds " + custom_message_addon);
        }
        else
        {
            Log.i(TAG, "sleeping " + seconds + " seconds");
        }
        SystemClock.sleep(seconds * 1000);
        Log.i(TAG, "sleeping ended");
    }

    private static Matcher<View> getElementFromMatchAtPosition(final Matcher<View> matcher, final int position)
    {
        return new BaseMatcher<View>()
        {
            int counter = 0;

            @Override
            public boolean matches(final Object item)
            {
                if (matcher.matches(item))
                {
                    if (counter == position)
                    {
                        counter++;
                        return true;
                    }
                    counter++;
                }
                return false;
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText("Element at hierarchy position " + position);
            }
        };
    }

    private static void cause_error(int errnum)
    {
        Log.i(TAG, "___ERROR_at_TESTS___:" + errnum);
        if (errnum == 1)
        {
            onView(withId(R.id.bugButton1)).perform(replaceText(MOCK_PASSWORD));
        }
        else if (errnum == 2)
        {
            onView(withId(R.id.bugButton2)).perform(replaceText(MOCK_PASSWORD));
        }
        else if (errnum == 3)
        {
            onView(withId(R.id.bugButton3)).perform(replaceText(MOCK_PASSWORD));
        }
        else
        {
            onView(withId(R.id.bugButton)).perform(replaceText(MOCK_PASSWORD));
        }
    }

    public void setSharedPrefs()
    {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit() ;
        editor.putBoolean("window_security", false) ;
        editor.apply();
        editor.commit();
        Log.i(TAG ,"Setting up shared prefs");
    }

    @Before
    public void setUp() throws Exception
    {
        Log.i(TAG, "setUp");
    }

    @After
    public void tearDown() throws Exception
    {
        Log.i(TAG, "tearDown");
    }

    public Activity getActivityInstance()
    {
        getInstrumentation().runOnMainSync(new Runnable()
        {
            public void run()
            {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                        RESUMED);
                if (resumedActivities.iterator().hasNext())
                {
                    currentActivity = (Activity) resumedActivities.iterator().next();
                }
            }
        });

        return currentActivity;
    }

    public void grant_permissions()
    {
        // ----- persmission -----
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        for (int i = 0; i < permissions.size(); i++)
        {
            String command = String.format("pm grant %s %s", getTargetContext().getPackageName(), permissions.get(i));
            getInstrumentation().getUiAutomation().executeShellCommand(command);
            // wait a bit until the command is finished
            SystemClock.sleep(2000);
        }
        // ----- persmission -----
    }

    public String getViewInteractionText(ViewInteraction matcher)
    {
        final String[] text = new String[1];
        ViewAction va = new ViewAction()
        {
            @Override
            public Matcher<View> getConstraints()
            {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription()
            {
                return "Text of the view";
            }

            @Override
            public void perform(UiController uiController, View view)
            {
                TextView tv = (TextView) view;
                text[0] = tv.getText().toString();
            }
        };

        matcher.perform(va);
        return text[0];
    }
}
