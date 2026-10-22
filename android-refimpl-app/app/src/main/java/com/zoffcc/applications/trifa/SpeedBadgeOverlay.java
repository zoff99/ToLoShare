package com.zoffcc.applications.trifa;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class SpeedBadgeOverlay extends Overlay
{
    private GeoPoint position = null;
    private float speed_kmh = 0.0f;
    private boolean has_speed = false;

    private final float density;
    private final Paint badge_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF badge_rect = new RectF();
    private final Point screen_point = new Point();

    public SpeedBadgeOverlay(MapView mapView)
    {
        super();
        density = mapView.getContext().getResources().getDisplayMetrics().density;
        text_paint.setColor(Color.WHITE);
        text_paint.setTextAlign(Paint.Align.CENTER);
        text_paint.setFakeBoldText(true);
    }

    public void setPosition(GeoPoint pos)
    {
        position = pos;
    }

    public void setSpeed(float speed_kmh_value, boolean has_speed_value)
    {
        speed_kmh = speed_kmh_value;
        has_speed = has_speed_value;
    }

    @Override
    public void draw(Canvas c, Projection pProjection)
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
            color = Color.argb(200, 100, 116, 139);
        }
        else if (speed <= 1)
        {
            color = Color.argb(200, 100, 116, 139);
        }
        else if (speed >= 20)
        {
            color = Color.argb(220, 249, 115, 22);
        }
        else
        {
            color = Color.argb(220, 34, 197, 94);
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

        text_paint.setTextSize(12.0f * density);

        float text_width = text_paint.measureText(label);

        float badge_w = Math.max(28.0f * density, text_width + (12.0f * density));
        float badge_h = 16.0f * density;

        float left = screen_point.x - (badge_w / 2.0f);
        float top = screen_point.y - (40.0f * density) - badge_h;

        badge_rect.set(left, top, left + badge_w, top + badge_h);

        badge_paint.setColor(color);
        badge_paint.setStyle(Paint.Style.FILL);

        c.drawRoundRect(badge_rect, 6.0f * density, 6.0f * density, badge_paint);

        c.drawText(label, screen_point.x, top + badge_h - (4.2f * density), text_paint);
    }
}
