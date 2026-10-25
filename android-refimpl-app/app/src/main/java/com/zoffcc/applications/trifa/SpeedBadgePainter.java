package com.zoffcc.applications.trifa;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
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
    private static final float BADGE_MIN_WIDTH_DP      = 60.0f;
    private static final float BADGE_PADDING_DP        = 16.0f;
    private static final float BADGE_HEIGHT_DP         = 44.0f;
    private static final float BADGE_OFFSET_Y_DP       = 38.0f;
    private static final float BADGE_CORNER_RADIUS_DP  = 6.0f;
    private static final float BADGE_LINE_GAP_DP       = 2.0f;

    // speed thresholds (km/h)
    private static final float SPEED_STATIONARY_MAX_KMH = 1.0f;
    private static final float SPEED_DRIVING_MIN_KMH    = 20.0f;

    // Low-CPU Collision settings
    private static final long COLLISION_CHECK_INTERVAL_MS = 500;
    private static final float ICON_RADIUS_DP = 80.0f;

    private final float density;
    private final Paint badge_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF badge_rect = new RectF();
    private final Point screen_point = new Point();
    private final Point other_pt = new Point(); // Reusable point to avoid allocation

    // Cached collision state
    private boolean cached_is_overlapping = false;
    private long last_collision_check_time = 0;

    public SpeedBadgePainter(float density)
    {
        this.density = density;

        text_paint.setColor(BADGE_TEXT_COLOR);
        text_paint.setTextAlign(Paint.Align.CENTER);
        text_paint.setFakeBoldText(true);
    }

    // Note: 'skipOverlay' is passed so we don't check for collision against ourselves
    public void draw(Canvas c, Projection pProjection, GeoPoint position, float speed_kmh, boolean has_speed, String nickname, Object skipOverlay)
    {
        if (position == null) return;

        pProjection.toPixels(position, screen_point);

        int speed = Math.max(0, Math.round(speed_kmh));
        int color = BADGE_COLOR_NO_SPEED;
        if (has_speed) {
            if (speed <= SPEED_STATIONARY_MAX_KMH) color = BADGE_COLOR_STATIONARY;
            else if (speed >= SPEED_DRIVING_MIN_KMH) color = BADGE_COLOR_DRIVING;
            else color = BADGE_COLOR_WALKING;
        }

        // Shorten nickname to max 12 characters
        String shortName = "Unknown";
        if (nickname != null && !nickname.isEmpty()) {
            shortName = nickname.length() > 12 ? nickname.substring(0, 12) : nickname;
        }

        String speedStr = has_speed ? String.valueOf(speed) : "--";

        text_paint.setTextSize(BADGE_TEXT_SIZE_DP * density);
        float max_text_width = Math.max(text_paint.measureText(shortName), text_paint.measureText(speedStr));
        float badge_w = Math.max(BADGE_MIN_WIDTH_DP * density, max_text_width + (BADGE_PADDING_DP * density));
        float badge_h = BADGE_HEIGHT_DP * density;

        float left = screen_point.x - (badge_w / 2.0f);
        float top = screen_point.y - (BADGE_OFFSET_Y_DP * density) - badge_h;

        badge_rect.set(left, top, left + badge_w, top + badge_h);

        // --- THROTTLED LOW-CPU COLLISION DETECTION ---
        long now = System.currentTimeMillis();
        if (now - last_collision_check_time >= COLLISION_CHECK_INTERVAL_MS)
        {
            last_collision_check_time = now;
            cached_is_overlapping = false;

            float iconRadius = ICON_RADIUS_DP * density;
            Rect badgeRectInt = new Rect((int) badge_rect.left, (int) badge_rect.top, (int) badge_rect.right, (int) badge_rect.bottom);

            // 1. Check against self location (if this is a friend overlay)
            if (skipOverlay != null) {
                try {
                    java.util.List<android.location.Location> selfLocs = MainActivity.self_trails.getRecentPositions();
                    if (selfLocs != null && !selfLocs.isEmpty()) {
                        android.location.Location selfLoc = selfLocs.get(0);
                        pProjection.toPixels(new GeoPoint(selfLoc.getLatitude(), selfLoc.getLongitude()), other_pt);
                        if (checkIntersection(badgeRectInt, other_pt, iconRadius)) {
                            cached_is_overlapping = true;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 2. Check against all other friend overlays
            if (!cached_is_overlapping) {
                for (CaptureService.remote_location_overlay_entry entry : CaptureService.remote_location_overlays.values()) {
                    if (entry != null && entry.remote_location_overlay != null && entry.remote_location_overlay != skipOverlay) {
                        GeoPoint loc = entry.remote_location_overlay.getLocation();
                        if (loc != null) {
                            pProjection.toPixels(loc, other_pt);
                            if (checkIntersection(badgeRectInt, other_pt, iconRadius)) {
                                cached_is_overlapping = true;
                                break; // Found collision, stop checking
                            }
                        }
                    }
                }
            }
        }

        int saveCount = c.save();
        try {
            float mapRotation = MainActivity.map != null ? MainActivity.map.getMapOrientation() : 0f;
            if (mapRotation != 0f) {
                c.rotate(-mapRotation, screen_point.x, screen_point.y);
            }

            int originalBadgeAlpha = Color.alpha(color);
            int originalTextAlpha = text_paint.getAlpha();

            badge_paint.setColor(color);
            badge_paint.setStyle(Paint.Style.FILL);

            if (cached_is_overlapping) {
                badge_paint.setAlpha((int) (originalBadgeAlpha * 0.4f));
                text_paint.setAlpha((int) (originalTextAlpha * 0.4f));
            }

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

            // Restore original alphas
            badge_paint.setAlpha(originalBadgeAlpha);
            text_paint.setAlpha(originalTextAlpha);

        } finally {
            c.restoreToCount(saveCount);
        }
    }

    private boolean checkIntersection(Rect badgeRect, Point pt, float radius) {
        Rect otherRect = new Rect((int)(pt.x - radius), (int)(pt.y - radius), (int)(pt.x + radius), (int)(pt.y + radius));
        return Rect.intersects(badgeRect, otherRect);
    }
}
