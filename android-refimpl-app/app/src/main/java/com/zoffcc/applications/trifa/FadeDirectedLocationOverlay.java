package com.zoffcc.applications.trifa;

import android.content.Context;
import android.graphics.Canvas;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import java.util.ArrayList;
import java.util.List;

public class FadeDirectedLocationOverlay extends DirectedLocationOverlay
{
    public long timestamp_ms = 0;

    private float speed_kmh = 0.0f;
    private boolean has_speed = false;
    private GeoPoint current_pos = null;
    private String nickname = "";

    private final SpeedBadgePainter badge_painter;

    public FadeDirectedLocationOverlay(Context ctx)
    {
        super(ctx);

        float density = ctx.getResources().getDisplayMetrics().density;
        badge_painter = new SpeedBadgePainter(density);
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
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
                // Pass 'this' so the painter knows to skip checking collision against itself
                badge_painter.draw(c, pProjection, current_pos, speed_kmh, has_speed, this.nickname, this);
            }
        }
        finally
        {
            c.restore();
        }
    }
}
