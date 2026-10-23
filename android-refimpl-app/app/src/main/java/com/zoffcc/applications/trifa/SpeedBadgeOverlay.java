package com.zoffcc.applications.trifa;

import android.graphics.Canvas;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class SpeedBadgeOverlay extends Overlay
{
    private GeoPoint position = null;
    private float speed_kmh = 0.0f;
    private boolean has_speed = false;

    private final SpeedBadgePainter badge_painter;

    public SpeedBadgeOverlay(MapView mapView)
    {
        super();

        float density = mapView.getContext().getResources().getDisplayMetrics().density;
        badge_painter = new SpeedBadgePainter(density);
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
        badge_painter.draw(c, pProjection, position, speed_kmh, has_speed);
    }
}
