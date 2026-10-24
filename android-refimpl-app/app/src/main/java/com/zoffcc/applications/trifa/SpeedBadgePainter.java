package com.zoffcc.applications.trifa;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

public class SpeedBadgePainter
{
    // badge colors
    private static final int BADGE_COLOR_NO_SPEED   = Color.argb(200, 100, 116, 139);
    private static final int BADGE_COLOR_STATIONARY = Color.argb(200, 100, 116, 139);
    private static final int BADGE_COLOR_WALKING    = Color.argb(220, 34, 197, 94);
    private static final int BADGE_COLOR_DRIVING    = Color.argb(220, 249, 115, 22);

    // badge text color
    private static final int BADGE_TEXT_COLOR       = Color.WHITE;

    // badge size / layout values (in dp)
    private static final float BADGE_TEXT_SIZE_DP      = 16.0f;
    private static final float BADGE_MIN_WIDTH_DP      = 36.0f;
    private static final float BADGE_PADDING_DP        = 16.0f;
    private static final float BADGE_HEIGHT_DP         = 22.0f;
    private static final float BADGE_OFFSET_Y_DP       = 50.0f;
    private static final float BADGE_CORNER_RADIUS_DP  = 6.0f;
    private static final float BADGE_TEXT_BASELINE_DP  = 5.8f;

    // speed thresholds (km/h)
    private static final float SPEED_STATIONARY_MAX_KMH = 1.0f;
    private static final float SPEED_DRIVING_MIN_KMH    = 20.0f;

    private final float density;
    private final Paint badge_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF badge_rect = new RectF();
    private final Point screen_point = new Point();

    public SpeedBadgePainter(float density)
    {
        this.density = density;

        text_paint.setColor(BADGE_TEXT_COLOR);
        text_paint.setTextAlign(Paint.Align.CENTER);
        text_paint.setFakeBoldText(true);
    }

    public void draw(Canvas c, Projection pProjection, GeoPoint position, float speed_kmh, boolean has_speed)
    {
        if (position == null)
        {
            return;
        }

        pProjection.toPixels(position, screen_point);

        int speed = Math.max(0, Math.round(speed_kmh));

        int color;
        if (!has_speed)
        {
            color = BADGE_COLOR_NO_SPEED;
        }
        else if (speed <= SPEED_STATIONARY_MAX_KMH)
        {
            color = BADGE_COLOR_STATIONARY;
        }
        else if (speed >= SPEED_DRIVING_MIN_KMH)
        {
            color = BADGE_COLOR_DRIVING;
        }
        else
        {
            color = BADGE_COLOR_WALKING;
        }

        String label;
        if (!has_speed)
        {
            label = "--";
        }
        else
        {
            label = String.valueOf(speed);
        }

        text_paint.setTextSize(BADGE_TEXT_SIZE_DP * density);

        float text_width = text_paint.measureText(label);

        float badge_w = Math.max(BADGE_MIN_WIDTH_DP * density, text_width + (BADGE_PADDING_DP * density));
        float badge_h = BADGE_HEIGHT_DP * density;

        float left = screen_point.x - (badge_w / 2.0f);
        float top = screen_point.y - (BADGE_OFFSET_Y_DP * density) - badge_h;

        badge_rect.set(left, top, left + badge_w, top + badge_h);

        int saveCount = c.save();
        try {
            // Counter-rotate the canvas around the exact screen point.
            // This keeps the bubble's position perfectly locked to the icon,
            // but makes the drawing axes upright so the text/rect are not rotated.
            float mapRotation = 0f;
            if (MainActivity.map != null) {
                mapRotation = MainActivity.map.getMapOrientation();
            }

            if (mapRotation != 0f) {
                c.rotate(-mapRotation, screen_point.x, screen_point.y);
            }

            badge_paint.setColor(color);
            badge_paint.setStyle(Paint.Style.FILL);

            c.drawRoundRect(badge_rect, BADGE_CORNER_RADIUS_DP * density, BADGE_CORNER_RADIUS_DP * density, badge_paint);

            c.drawText(label, screen_point.x, top + badge_h - (BADGE_TEXT_BASELINE_DP * density), text_paint);
        } finally {
            c.restoreToCount(saveCount);
        }
    }
}
