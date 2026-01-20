package com.zoffcc.applications.trifa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class TriangleTextView extends View
{
    private Paint trianglePaint;
    private Paint textPaint;
    private final Path trianglePath = new Path();

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private int triangleSizeDp = 6;
    private int topPaddingDp = 8;
    private float textSizeSp = 18f;
    private int textBottomOffsetDp = 8;

    private int currentCount = 0;
    private int triangleColor = Color.RED;
    private Direction currentDirection = Direction.UP;

    public TriangleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void updateCount(int newCount) {
        this.currentCount = newCount;
        invalidate();
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void init() {
        trianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trianglePaint.setColor(triangleColor);
        trianglePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(dpToPx(textSizeSp));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDirection(Direction direction) {
        this.currentDirection = direction;
        invalidate();
    }

    public void setTriangleColor(int color)
    {
        this.triangleColor = color;
        trianglePaint.setColor(color);
        invalidate();
    }

    public void setTriangleSizeDp(int dp) {
        this.triangleSizeDp = dp;
        invalidate();
    }

    public void setTopPaddingDp(int dp) {
        this.topPaddingDp = dp;
        invalidate();
    }

    public void setTextSizeSp(int sp) {
        this.textSizeSp = sp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentCount == 0)
        {
            canvas.drawColor(Color.TRANSPARENT);
            return;
        }

        // Convert DP values to Pixels for current screen density
        float sizePx = dpToPx(triangleSizeDp);
        float paddingPx = dpToPx(topPaddingDp);
        float bottomOffsetPx = dpToPx(textBottomOffsetDp);

        int centerX = getWidth() / 2;
        int viewHeight = getHeight();

        trianglePath.reset();

        switch (currentDirection) {
            case UP:
                trianglePath.moveTo(centerX, paddingPx);
                trianglePath.lineTo(centerX - sizePx, paddingPx + (sizePx * 2));
                trianglePath.lineTo(centerX + sizePx, paddingPx + (sizePx * 2));
                break;
            case DOWN:
                trianglePath.moveTo(centerX, paddingPx + (sizePx * 2));
                trianglePath.lineTo(centerX - sizePx, paddingPx);
                trianglePath.lineTo(centerX + sizePx, paddingPx);
                break;
            case LEFT:
                trianglePath.moveTo(centerX - sizePx, paddingPx + sizePx);
                trianglePath.lineTo(centerX + sizePx, paddingPx);
                trianglePath.lineTo(centerX + sizePx, paddingPx + (sizePx * 2));
                break;
            case RIGHT:
                trianglePath.moveTo(centerX + sizePx, paddingPx + sizePx);
                trianglePath.lineTo(centerX - sizePx, paddingPx);
                trianglePath.lineTo(centerX - sizePx, paddingPx + (sizePx * 2));
                break;
        }
        trianglePath.close();
        canvas.drawPath(trianglePath, trianglePaint);

        // Draw Text at bottom with DP-based margin
        canvas.drawText("" + currentCount, centerX, viewHeight - bottomOffsetPx, textPaint);
    }
}

