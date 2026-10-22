package com.zoffcc.applications.trifa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

public class FadeDirectedLocationOverlay extends DirectedLocationOverlay
{
    public long timestamp_ms = 0;

    private float speed_kmh = 0.0f;
    private boolean has_speed = false;
    private GeoPoint current_pos = null;

    private final float density;
    private final Paint badge_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF badge_rect = new RectF();
    private final Point screen_point = new Point();

    public FadeDirectedLocationOverlay(Context ctx)
    {
        super(ctx);

        density = ctx.getResources().getDisplayMetrics().density;

        text_paint.setColor(Color.WHITE);
        text_paint.setTextAlign(Paint.Align.CENTER);
        text_paint.setFakeBoldText(true);
    }

    @Override
    public void setLocation(GeoPoint location)
    {
        current_pos = location;
        super.setLocation(location);
    }

    public void setSpeedIndicator(float speed_kmh_value, boolean has_speed_value)
    {
        speed_kmh = speed_kmh_value;
        has_speed = has_speed_value;
    }

    @Override
    public void draw(Canvas c, Projection pProjection)
    {
        float alpha = TrailPlaybackState.alphaForLocationTime(System.currentTimeMillis(), timestamp_ms);
        int alpha_int = (int)(alpha * 255);

        c.save();
        try
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            {
                c.saveLayerAlpha(0, 0, c.getWidth(), c.getHeight(), alpha_int);
            }
            else
            {
                c.saveLayerAlpha(0, 0, c.getWidth(), c.getHeight(), alpha_int, Canvas.ALL_SAVE_FLAG);
            }

            super.draw(c, pProjection);

            if (current_pos != null)
            {
                drawSpeedIndicator(c, pProjection);
            }
        }
        finally
        {
            c.restore();
        }
    }

    private void drawSpeedIndicator(Canvas c, Projection pProjection)
    {
        pProjection.toPixels(current_pos, screen_point);

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
