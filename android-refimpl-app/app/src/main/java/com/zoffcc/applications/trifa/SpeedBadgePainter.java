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
    private static final float BADGE_TEXT_SIZE_DP      = 16.0f; // Kept at 16 as requested
    private static final float BADGE_MIN_WIDTH_DP      = 60.0f; // Increased to comfortably fit 12 chars
    private static final float BADGE_PADDING_DP        = 16.0f;
    private static final float BADGE_HEIGHT_DP         = 44.0f; // Increased to fit two lines of 16dp text
    private static final float BADGE_OFFSET_Y_DP       = 38.0f; // Kept close to the icon
    private static final float BADGE_CORNER_RADIUS_DP  = 6.0f;
    private static final float BADGE_LINE_GAP_DP       = 2.0f;  // Small gap between the two lines of text

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

    public void draw(Canvas c, Projection pProjection, GeoPoint position, float speed_kmh, boolean has_speed, String nickname)
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

        // Shorten nickname to max 12 characters
        String shortName = "Unknown";
        if (nickname != null && !nickname.isEmpty()) {
            shortName = nickname.length() > 12 ? nickname.substring(0, 12) : nickname;
        }

        String speedStr = has_speed ? String.valueOf(speed) : "--";

        text_paint.setTextSize(BADGE_TEXT_SIZE_DP * density);

        // Measure both lines to find the maximum width needed
        float name_width = text_paint.measureText(shortName);
        float speed_width = text_paint.measureText(speedStr);
        float max_text_width = Math.max(name_width, speed_width);

        float badge_w = Math.max(BADGE_MIN_WIDTH_DP * density, max_text_width + (BADGE_PADDING_DP * density));
        float badge_h = BADGE_HEIGHT_DP * density;

        float left = screen_point.x - (badge_w / 2.0f);
        float top = screen_point.y - (BADGE_OFFSET_Y_DP * density) - badge_h;

        badge_rect.set(left, top, left + badge_w, top + badge_h);

        int saveCount = c.save();
        try {
            // Counter-rotate the canvas around the exact screen point.
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

            // Draw two lines of text, perfectly centered vertically
            Paint.FontMetrics fm = text_paint.getFontMetrics();
            float text_height = fm.descent - fm.ascent;
            float line_gap = BADGE_LINE_GAP_DP * density;
            float total_text_height = (text_height * 2.0f) + line_gap;

            // Calculate the Y position for the first line so both lines are centered in the badge
            float start_y = top + (badge_h - total_text_height) / 2.0f - fm.ascent;

            c.drawText(shortName, screen_point.x, start_y, text_paint);
            c.drawText(speedStr, screen_point.x, start_y + text_height + line_gap, text_paint);

        } finally {
            c.restoreToCount(saveCount);
        }
    }
}
