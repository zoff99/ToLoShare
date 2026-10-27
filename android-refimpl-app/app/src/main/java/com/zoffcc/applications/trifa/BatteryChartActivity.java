package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import static com.zoffcc.applications.trifa.TRIFAGlobals.BATTERY_PERCENT_UNKNOWN;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_battery_percent;

public class BatteryChartActivity extends AppCompatActivity
{
    private LockableHorizontalScrollView scrollView;
    private BatteryChartView chartView;
    private TextView remainingTimeText;

    // Track if user has scrolled away from live "now" edge
    private boolean userScrolledAway = false;
    private long targetTimestamp = 0;
    private boolean programmaticScroll = false;
    private boolean userIsTouching = false;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (chartView != null)
            {
                chartView.invalidate();
                updateRemainingTime();
                positionRemainingTimeText();

                if (!userScrolledAway)
                {
                    scroll_to_now();
                }
                else if (!userIsTouching)
                {
                    // Keep the viewport glued to targetTimestamp as the 10h window rolls
                    int newScrollX = (int) chartView.getXForTimestamp(targetTimestamp);
                    int maxScroll = Math.max(0, chartView.getWidth() - scrollView.getWidth());
                    if (newScrollX < 0) newScrollX = 0;
                    if (newScrollX > maxScroll) newScrollX = maxScroll;
                    scrollXTo(newScrollX);
                }
            }
            refreshHandler.postDelayed(this, 60000); // Update every 60 seconds
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("Battery History (10h)");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(0xFF121212);

        // Scrollable Chart (Added FIRST so it is at the bottom layer)
        scrollView = new LockableHorizontalScrollView(this);
        scrollView.setBackgroundColor(0xFF121212);

        chartView = new BatteryChartView(this);
        scrollView.addView(chartView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT));

        scrollView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                userIsTouching = true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                userIsTouching = false;
                scrollView.post(() -> {
                    if (chartView != null) {
                        int maxScroll = Math.max(0, chartView.getWidth() - scrollView.getWidth());
                        int currentScroll = scrollView.getScrollX();
                        if (currentScroll < maxScroll - 20) {
                            userScrolledAway = true;
                            targetTimestamp = chartView.getTimestampAtX(currentScroll);
                        } else {
                            userScrolledAway = false;
                        }
                    }
                });
            }
            return false;
        });

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (programmaticScroll || chartView == null) return;
            int maxScroll = Math.max(0, chartView.getWidth() - scrollView.getWidth());
            int currentScroll = scrollView.getScrollX();
            if (currentScroll < maxScroll - 20) {
                userScrolledAway = true;
                targetTimestamp = chartView.getTimestampAtX(currentScroll);
            } else {
                userScrolledAway = false;
            }
        });

        rootLayout.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Overlay for remaining time, positioned at exact midpoint between threshold line and X-axis
        remainingTimeText = new TextView(this);
        remainingTimeText.setTextColor(Color.WHITE);
        remainingTimeText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, HelperGeneric.dp2px(14f));
        remainingTimeText.setPadding((int) HelperGeneric.dp2px(16f), 0, (int) HelperGeneric.dp2px(16f), 0);
        remainingTimeText.setText("Estimating remaining time...");

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = Gravity.BOTTOM | Gravity.START;
        rootLayout.addView(remainingTimeText, textParams);

        setContentView(rootLayout);
        scrollView.post(() -> {
            scroll_to_now();
            positionRemainingTimeText();
        });
        updateRemainingTime();
    }

    private void positionRemainingTimeText()
    {
        if (chartView == null || remainingTimeText == null) return;

        int chartH = chartView.getHeight();
        if (chartH == 0) return;

        float marginTop = HelperGeneric.dp2px(20f);
        float marginBottom = HelperGeneric.dp2px(40f);
        float drawableChartH = chartH - marginTop - marginBottom;

        // Y position of the 15% threshold line (from top of view)
        float yThreshold = marginTop + ((100f - BatteryChartView.BATTERY_THRESHOLD_PERCENT) / 100f) * drawableChartH;

        // Y position of the X-axis (bottom of drawable area)
        float yAxis = chartH - marginBottom;

        // Exact midpoint between threshold line and X-axis
        float midY = (yThreshold + yAxis) / 2f;

        // Center the text vertically on that midpoint
        int textHeight = remainingTimeText.getHeight();
        float topMargin = midY - (textHeight / 2f);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) remainingTimeText.getLayoutParams();
        params.topMargin = Math.max(0, (int) topMargin);
        params.bottomMargin = 0;
        params.gravity = Gravity.START;
        remainingTimeText.setLayoutParams(params);
    }

    private void updateRemainingTime()
    {
        final int threshold = BatteryChartView.BATTERY_THRESHOLD_PERCENT;
        float current = global_battery_percent;

        if (current <= 0 || current == BATTERY_PERCENT_UNKNOWN) {
            remainingTimeText.setText("Waiting for battery data...");
            return;
        }

        float rate = BatteryChartView.calculateBatteryDrainRate();

        if (rate == BatteryChartView.RATE_CHARGING) {
            remainingTimeText.setText("Phone is charging or idle");
        } else if (rate == BatteryChartView.RATE_INVALID) {
            remainingTimeText.setText("Calculating battery drain rate...");
        } else if (current <= (float) threshold) {
            remainingTimeText.setText("Battery is already below " + threshold + "%");
        } else if (rate > 0f) {
            float percentToDrop = current - (float) threshold;
            float hoursRemaining = percentToDrop / rate;

            int totalHours = (int) hoursRemaining;
            int minutes = (int) ((hoursRemaining - totalHours) * 60);

            int days = totalHours / 24;
            int hours = totalHours % 24;

            String timeStr;
            if (days > 0) {
                timeStr = String.format(Locale.US, "Est. time until <" + threshold + "%%: %d days %d hours", days, hours);
            } else if (hours > 0) {
                timeStr = String.format(Locale.US, "Est. time until <" + threshold + "%%: %d hours %d minutes", hours, minutes);
            } else {
                timeStr = String.format(Locale.US, "Est. time until <" + threshold + "%%: %d minutes", minutes);
            }
            remainingTimeText.setText(timeStr);
        } else {
            remainingTimeText.setText("Calculating battery drain rate...");
        }
    }

    private void scrollXTo(int x) {
        programmaticScroll = true;
        scrollView.scrollTo(x, 0);
        programmaticScroll = false;
    }

    private void scroll_to_now() {
        int maxScroll = Math.max(0, chartView.getWidth() - scrollView.getWidth());
        scrollXTo(maxScroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        userScrolledAway = false;
        refreshHandler.postDelayed(refreshRunnable, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static class LockableHorizontalScrollView extends HorizontalScrollView {
        public LockableHorizontalScrollView(Context context) { super(context); }
        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (ev.getPointerCount() > 1) return false;
            return super.onInterceptTouchEvent(ev);
        }
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getPointerCount() > 1) return true;
            return super.onTouchEvent(ev);
        }
    }
}
