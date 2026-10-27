package com.zoffcc.applications.trifa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.zoffcc.applications.trifa.TRIFAGlobals.BATTERY_PERCENT_UNKNOWN;
import static com.zoffcc.applications.trifa.TRIFAGlobals.global_battery_percent;

public class BatteryChartActivity extends AppCompatActivity
{
    private static final int REQ_PICK_TREE = 0x61;
    private static final String PREF_TREE_URI_KEY = "battery_chart_export_tree_uri";
    private static final float EXPORT_DP_PER_MINUTE = 13.0f;
    private static final int MAX_EXPORT_WIDTH_PX = 70000;

    private LockableHorizontalScrollView scrollView;
    private BatteryChartView chartView;
    private TextView remainingTimeText;
    private DocumentExporter exporter;
    private volatile boolean exporting = false;

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
                positionExportFab();
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

        exporter = new DocumentExporter(this, PREF_TREE_URI_KEY, REQ_PICK_TREE);

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

        // Export FAB (bottom-right)
        FloatingActionButton fabExport = new FloatingActionButton(this);
        fabExport.setImageResource(R.drawable.ic_export);
        fabExport.setContentDescription("Export chart");
        fabExport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
        fabExport.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));

        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        fabParams.gravity = Gravity.END;
        fabParams.rightMargin = dp(16);
        fabExport.setLayoutParams(fabParams);

        fabExport.setId(R.id.fab_battery_export);
        fabExport.setOnClickListener(v -> onExportClicked());
        fabExport.setOnLongClickListener(v -> {
            onChangeFolderClicked();
            return true;
        });

        rootLayout.addView(fabExport, fabParams);

        setContentView(rootLayout);
        scrollView.post(() -> {
            scroll_to_now();
            positionRemainingTimeText();
            positionExportFab();
        });
        updateRemainingTime();
    }

    // -------------------------------------------------------------------------
    // Export logic
    // -------------------------------------------------------------------------

    private void onExportClicked()
    {
        if (exporting) return;
        exporting = true;

        exporter.ensureDirectory(() -> {
            final Bitmap bmp;
            try
            {
                float density = getResources().getDisplayMetrics().density;
                int totalSpan = BatteryChartView.getTotalSpanMinutes();

                int w = (int) (totalSpan * EXPORT_DP_PER_MINUTE * density);
                float effDp = EXPORT_DP_PER_MINUTE;
                if (w > MAX_EXPORT_WIDTH_PX)
                {
                    effDp *= (float) MAX_EXPORT_WIDTH_PX / (float) w;
                    w = MAX_EXPORT_WIDTH_PX;
                }
                if (w < 1) w = 1;

                // Create an off-screen chart view at export resolution
                BatteryChartView exportView = new BatteryChartView(this);
                exportView.setDpPerMinute(effDp);

                // Use the same height as the on-screen chart
                int contentH = chartView.getHeight();
                if (contentH < 1) contentH = (int) (600 * density);

                int wSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                int hSpec = View.MeasureSpec.makeMeasureSpec(contentH, View.MeasureSpec.EXACTLY);
                exportView.measure(wSpec, hSpec);
                exportView.layout(0, 0, w, contentH);

                bmp = Bitmap.createBitmap(w, contentH, Bitmap.Config.ARGB_8888);
                bmp.eraseColor(0xFF121212);
                Canvas c = new Canvas(bmp);
                exportView.draw(c);

                // --- Paint the remaining time text onto the exported bitmap ---
                paintExportRemainingTime(c, w, contentH, density);

            }
            catch (OutOfMemoryError oom)
            {
                exporting = false;
                toast("Export failed: not enough memory");
                return;
            }
            catch (Throwable t)
            {
                exporting = false;
                toast("Export failed: " + t.getMessage());
                return;
            }

            final String fileName = "battery_chart_" +
                                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";

            new Thread(() -> {
                try
                {
                    Uri fileUri = exporter.createFileUri(fileName, "image/png");
                    if (fileUri == null)
                    {
                        postUi(() -> toast("Export failed: cannot create file"));
                        return;
                    }

                    try (OutputStream os = getContentResolver().openOutputStream(fileUri, "w"))
                    {
                        if (os == null)
                        {
                            postUi(() -> toast("Export failed: cannot open output stream"));
                            return;
                        }
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.flush();
                    }

                    postUi(() -> toast("Saved: " + fileName));
                }
                catch (Throwable t)
                {
                    postUi(() -> toast("Export failed: " + t.getMessage()));
                }
                finally
                {
                    bmp.recycle();
                    exporting = false;
                }
            }, "battery-chart-export").start();

        }, error -> {
            exporting = false;
            toast(error);
        });
    }

    /**
     * Paints the remaining time text onto the export canvas at the exact vertical midpoint
     * between the threshold line and the X-axis, aligned to the bottom-right.
     */
    private void paintExportRemainingTime(Canvas canvas, int w, int h, float density)
    {
        String text = remainingTimeText.getText().toString();
        if (text == null || text.isEmpty()) return;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(HelperGeneric.dp2px(14f));
        paint.setTextAlign(Paint.Align.RIGHT);

        float yThreshold = BatteryChartView.getThresholdY(h, density);
        float yAxis = BatteryChartView.getXAxisY(h, density);
        float midY = (yThreshold + yAxis) / 2f;

        // Vertical center: adjust for font metrics so the text is truly centered on midY
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textCenterOffset = (fm.descent + fm.ascent) / 2f;
        float drawY = midY - textCenterOffset;

        // Horizontal: right-aligned with same padding as the on-screen text
        float drawX = w - HelperGeneric.dp2px(16f);

        canvas.drawText(text, drawX, drawY, paint);
    }

    private void onChangeFolderClicked()
    {
        exporter.changeDirectory(() -> toast("Export folder updated"), error -> toast(error));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (exporter.handleActivityResult(requestCode, resultCode, data)) return;
    }

    // -------------------------------------------------------------------------
    // Remaining time + positioning
    // -------------------------------------------------------------------------

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

    private void positionRemainingTimeText()
    {
        if (chartView == null || remainingTimeText == null) return;

        int chartH = chartView.getHeight();
        if (chartH == 0) return;

        float density = getResources().getDisplayMetrics().density;
        float yThreshold = BatteryChartView.getThresholdY(chartH, density);
        float yAxis = BatteryChartView.getXAxisY(chartH, density);
        float midY = (yThreshold + yAxis) / 2f;

        int textHeight = remainingTimeText.getHeight();
        float topMargin = midY - (textHeight / 2f);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) remainingTimeText.getLayoutParams();
        params.topMargin = Math.max(0, (int) topMargin);
        params.bottomMargin = 0;
        params.gravity = Gravity.START;
        remainingTimeText.setLayoutParams(params);
    }

    private void positionExportFab()
    {
        if (chartView == null) return;

        int chartH = chartView.getHeight();
        if (chartH == 0) return;

        float density = getResources().getDisplayMetrics().density;
        float yThreshold = BatteryChartView.getThresholdY(chartH, density);
        float yAxis = BatteryChartView.getXAxisY(chartH, density);
        float midY = (yThreshold + yAxis) / 2f;

        // Find the FAB from the root layout
        View fab = findViewById(R.id.fab_battery_export);
        if (fab == null) return;

        int fabHeight = fab.getHeight();
        if (fabHeight == 0) return;

        float topMargin = midY - (fabHeight / 2f);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fab.getLayoutParams();
        params.topMargin = Math.max(0, (int) topMargin);
        params.bottomMargin = 0;
        params.gravity = Gravity.END;
        fab.setLayoutParams(params);
    }

    // -------------------------------------------------------------------------
    // Scroll helpers
    // -------------------------------------------------------------------------

    private void scrollXTo(int x) {
        programmaticScroll = true;
        scrollView.scrollTo(x, 0);
        programmaticScroll = false;
    }

    private void scroll_to_now() {
        int maxScroll = Math.max(0, chartView.getWidth() - scrollView.getWidth());
        scrollXTo(maxScroll);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void postUi(Runnable r) { refreshHandler.post(r); }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }

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
