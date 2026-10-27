package com.zoffcc.applications.trifa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.zoffcc.applications.trifa.TRIFAGlobals.BATTERY_HISTORY_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.battery_history;
import static com.zoffcc.applications.trifa.TRIFAGlobals.battery_history_count;
import static com.zoffcc.applications.trifa.TRIFAGlobals.battery_history_index;
import static com.zoffcc.applications.trifa.TRIFAGlobals.battery_history_ts;

public class BatteryChartView extends View
{
    private static final long WINDOW_MS = 10L * 60L * 60L * 1000L; // 10 hours
    private static final int RIGHT_PAD_MINUTES = 5;
    private static final float MIN_DP_PER_MINUTE = 2.0f;
    private static final float MAX_DP_PER_MINUTE = 60.0f;

    private float currentDpPerMinute = 6.0f;

    // Matrix zoom state
    private boolean isZooming = false;
    private float liveScaleFactor = 1f;
    private float livePivotX = 0f;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint();

    private final SimpleDateFormat hourFmt = new SimpleDateFormat("HH:mm", Locale.US);
    private ScaleGestureDetector scaleGestureDetector;

    // --- SANITY CHECK CONSTANTS ---
    public static final float RATE_CHARGING = -1f;
    public static final float RATE_INVALID = 0f;

    public BatteryChartView(Context context) { super(context); init(); }
    public BatteryChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init()
    {
        linePaint.setColor(0xFF4CAF50);
        linePaint.setStrokeWidth(HelperGeneric.dp2px(1.5f));
        linePaint.setStyle(Paint.Style.STROKE);

        gridPaint.setColor(Color.parseColor("#444444"));
        gridPaint.setStrokeWidth(HelperGeneric.dp2px(0.5f));

        textPaint.setColor(Color.parseColor("#DDDDDD"));
        textPaint.setTextSize(HelperGeneric.dp2px(12f));

        axisPaint.setColor(Color.parseColor("#888888"));
        axisPaint.setStrokeWidth(HelperGeneric.dp2px(1f));

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    public static int getTotalSpanMinutes() { return 600 + RIGHT_PAD_MINUTES; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        float density = getResources().getDisplayMetrics().density;
        int totalSpanMinutes = getTotalSpanMinutes();
        int wantedWidthPx = (int) (totalSpanMinutes * currentDpPerMinute * density);
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(wantedWidthPx, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        if (ev.getPointerCount() > 1) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        private float startDp = 0f;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isZooming = true;
            startDp = currentDpPerMinute;
            liveScaleFactor = 1f;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = liveScaleFactor * detector.getScaleFactor();
            float projectedDp = startDp * newScale;
            if (projectedDp < MIN_DP_PER_MINUTE) newScale = MIN_DP_PER_MINUTE / startDp;
            else if (projectedDp > MAX_DP_PER_MINUTE) newScale = MAX_DP_PER_MINUTE / startDp;
            liveScaleFactor = newScale;
            livePivotX = detector.getFocusX();
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isZooming = false;
            float newDp = Math.max(MIN_DP_PER_MINUTE, Math.min(MAX_DP_PER_MINUTE, startDp * liveScaleFactor));
            final long anchorTs = getTimestampAtX(livePivotX);
            float scrollNow = 0f;
            if (getParent() instanceof HorizontalScrollView) scrollNow = ((HorizontalScrollView) getParent()).getScrollX();
            final float focusScreenX = livePivotX - scrollNow;

            liveScaleFactor = 1f;
            currentDpPerMinute = newDp;
            requestLayout();

            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (!(getParent() instanceof HorizontalScrollView)) return;
                    HorizontalScrollView sv = (HorizontalScrollView) getParent();
                    float newX = getXForTimestamp(anchorTs) - focusScreenX;
                    int maxScroll = Math.max(0, getWidth() - sv.getWidth());
                    int target = (int) newX;
                    if (target < 0) target = 0;
                    if (target > maxScroll) target = maxScroll;
                    sv.scrollTo(target, 0);
                }
            });
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.drawColor(0xFF121212);

        final int w = getWidth();
        final int h = getHeight();
        if (w == 0 || h == 0) return;

        boolean applyMatrix = isZooming && liveScaleFactor != 1f;
        if (applyMatrix) {
            canvas.save();
            canvas.scale(liveScaleFactor, 1f, livePivotX, 0f);
        }

        final float marginLeft = HelperGeneric.dp2px(40f);
        final float marginBottom = HelperGeneric.dp2px(40f);
        final float marginTop = HelperGeneric.dp2px(20f);
        final float marginRight = HelperGeneric.dp2px(20f);

        final float chartW = w - marginLeft - marginRight;
        final float chartH = h - marginTop - marginBottom;

        final long now = System.currentTimeMillis();
        final long startTs = now - WINDOW_MS;
        final int totalSpanMinutes = getTotalSpanMinutes();
        final float minuteWidthPx = chartW / totalSpanMinutes;

        // Draw axes
        canvas.drawLine(marginLeft, marginTop, marginLeft, h - marginBottom, axisPaint);
        canvas.drawLine(marginLeft, h - marginBottom, w - marginRight, h - marginBottom, axisPaint);

        // Y-axis labels (0% to 100%)
        for (int i = 0; i <= 100; i += 20) {
            float y = h - marginBottom - (i / 100f) * chartH;
            canvas.drawText(i + "%", HelperGeneric.dp2px(5f), y + HelperGeneric.dp2px(3f), textPaint);
            if (i > 0 && i < 100) canvas.drawLine(marginLeft, y, w - marginRight, y, gridPaint);
        }

        // 10% threshold line
        float y10 = h - marginBottom - (10f / 100f) * chartH;
        Paint thresholdPaint = new Paint(gridPaint);
        thresholdPaint.setColor(Color.RED);
        thresholdPaint.setTextSize(HelperGeneric.dp2px(15f));
        thresholdPaint.setFakeBoldText(true);
        canvas.drawLine(marginLeft, y10, w - marginRight, y10, thresholdPaint);
        canvas.drawText("10% threshold", marginLeft + HelperGeneric.dp2px(10f), y10 - HelperGeneric.dp2px(4f), thresholdPaint);

        // X-axis labels (hours)
        for (int hh = 0; hh <= 10; hh += 1) {
            long ts = now - (hh * 60L * 60L * 1000L);
            float x = marginLeft + ((ts - startTs) / 60000f) * minuteWidthPx;
            canvas.drawLine(x, h - marginBottom, x, h - marginBottom + HelperGeneric.dp2px(5f), axisPaint);
            canvas.drawText(hourFmt.format(new Date(ts)), x - HelperGeneric.dp2px(15f), h - marginBottom + HelperGeneric.dp2px(25f), textPaint);
            if (hh > 0 && hh < 10) canvas.drawLine(x, marginTop, x, h - marginBottom, gridPaint);
        }

        // Draw battery history line
        Path chartPath = new Path();
        boolean firstPoint = true;
        int count = battery_history_count;
        int index = battery_history_index;
        int size = BATTERY_HISTORY_SIZE;

        for (int offset = 0; offset < count; offset++) {
            int idx = (index - count + offset + size) % size;
            long ts = battery_history_ts[idx];
            float pct = battery_history[idx];
            if (ts <= 0 || ts < startTs) continue;
            if (ts > now) break;

            float x = marginLeft + ((ts - startTs) / 60000f) * minuteWidthPx;
            float y = h - marginBottom - (pct / 100f) * chartH;

            if (firstPoint) {
                chartPath.moveTo(x, y);
                firstPoint = false;
            } else {
                chartPath.lineTo(x, y);
            }
        }

        if (!firstPoint) {
            canvas.drawPath(chartPath, linePaint);
        } else {
            textPaint.setTextSize(HelperGeneric.dp2px(18f));
            canvas.drawText("Waiting for battery data...", marginLeft + HelperGeneric.dp2px(20f), h / 2f, textPaint);
            textPaint.setTextSize(HelperGeneric.dp2px(12f));
        }

        if (applyMatrix) canvas.restore();
    }

    public long getTimestampAtX(float x) {
        int w = getWidth();
        if (w <= 0) return System.currentTimeMillis();
        float marginLeft = HelperGeneric.dp2px(40f);
        float marginRight = HelperGeneric.dp2px(20f);
        float chartW = w - marginLeft - marginRight;
        float minuteWidthPx = chartW / getTotalSpanMinutes();
        long startTs = System.currentTimeMillis() - WINDOW_MS;
        float adjustedX = x - marginLeft;
        if (adjustedX < 0) adjustedX = 0;
        return startTs + (long) ((adjustedX / minuteWidthPx) * 60000f);
    }

    public float getXForTimestamp(long timestamp) {
        int w = getWidth();
        if (w <= 0) return 0f;
        float marginLeft = HelperGeneric.dp2px(40f);
        float marginRight = HelperGeneric.dp2px(20f);
        float chartW = w - marginLeft - marginRight;
        float minuteWidthPx = chartW / getTotalSpanMinutes();
        long startTs = System.currentTimeMillis() - WINDOW_MS;
        return marginLeft + ((timestamp - startTs) / 60000f) * minuteWidthPx;
    }

    public static float calculateBatteryDrainRate() {
        int count = battery_history_count;
        int index = battery_history_index;
        int size = BATTERY_HISTORY_SIZE;

        // Sanity Check: Need at least 2 data points (60 seconds) of history to make a prediction
        if (count < 2) return RATE_INVALID;

        int idxFirst = (index - count + size) % size;
        int idxLast = (index - 1 + size) % size;
        long tsFirst = battery_history_ts[idxFirst];
        float pctFirst = battery_history[idxFirst];
        long tsLast = battery_history_ts[idxLast];
        float pctLast = battery_history[idxLast];

        if (tsLast <= tsFirst) return RATE_INVALID;

        double hoursElapsed = (tsLast - tsFirst) / (1000.0 * 60.0 * 60.0);
        // Sanity Check: Need at least ~1 minute (0.016 hours) of elapsed time to avoid noise
        if (hoursElapsed < 0.016) return RATE_INVALID;

        float pctDrop = pctFirst - pctLast;

        // Calculate the hourly drain rate immediately so we can apply scale-invariant sanity checks
        float rate = (float) (pctDrop / hoursElapsed);

        // Sanity Check: Unrealistically high drain rate (e.g. > 50% per hour usually means a sensor glitch)
        if (rate > 50.0f) {
            return RATE_INVALID;
        }

        // Sanity Check: Phone is charging, idle, or hovering (drain rate < 1% per hour)
        if (rate < 1.0f) {
            return RATE_CHARGING;
        }

        return rate;
    }
}
