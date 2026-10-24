package com.zoffcc.applications.trifa;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DynamicTextThumbDrawable extends Drawable
{
    private final float density;
    private final Paint circlePaint;
    private final Paint textPaint;
    private final Paint shadowPaint;

    private float thumbRadiusDp = 8.0f;
    private int circleColor = Color.parseColor("#22C55E");
    private String text = "";
    private boolean showText = false;

    public DynamicTextThumbDrawable(float density)
    {
        this.density = density;

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(circleColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(10.0f * density);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
    }

    public void updateState(int newColor, float newRadiusDp, String newText, boolean newShowText)
    {
        if (circleColor != newColor || thumbRadiusDp != newRadiusDp || !text.equals(newText) || showText != newShowText)
        {
            this.circleColor = newColor;
            this.thumbRadiusDp = newRadiusDp;
            this.text = newText;
            this.showText = newShowText;
            circlePaint.setColor(circleColor);
            invalidateSelf();
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas)
    {
        Rect bounds = getBounds();
        float centerX = bounds.exactCenterX();
        float centerY = bounds.exactCenterY();
        float radius = thumbRadiusDp * density;

        // Draw subtle shadow for depth
        canvas.drawCircle(centerX, centerY + (2.0f * density), radius, shadowPaint);

        // Draw main circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint);

        // Draw text if enabled
        if (showText && !text.isEmpty())
        {
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = centerY - ((fm.descent + fm.ascent) / 2.0f);
            canvas.drawText(text, centerX, textY, textPaint);
        }
    }

    @Override
    public void setAlpha(int alpha)
    {
        circlePaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter)
    {
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth()
    {
        return (int) (44.0f * density);
    }

    @Override
    public int getIntrinsicHeight()
    {
        return (int) (44.0f * density);
    }
}
