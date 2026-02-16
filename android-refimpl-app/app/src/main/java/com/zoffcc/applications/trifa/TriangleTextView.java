package com.zoffcc.applications.trifa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class TriangleTextView extends View
{
    private Paint trianglePaint;
    private Paint textPaint;
    private final Path trianglePath = new Path();

    private final PubKeyManager pkm = new PubKeyManager();

    public enum Direction { UP, DOWN, LEFT, RIGHT, CIRCLE }

    private int triangleSizeDp = 6;
    private int topPaddingDp = 8;
    private float textSizeSp = 18f;
    /** @noinspection FieldCanBeLocal*/
    private final int textBottomOffsetDp = 8;

    private int triangleColor = Color.RED;
    private Direction currentDirection = Direction.UP;

    public TriangleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public int getCount() {
        return this.pkm.getKeyCount();
    }

    public void addKey(String key) {
        this.pkm.addKey(key);
        invalidate();
    }

    public void removeKey(String key) {
        this.pkm.removeKey(key);
        invalidate();
    }

    public void removeAll() {
        this.pkm.removeAll();
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

        if ((pkm.getKeyCount() == 0) && (currentDirection != Direction.CIRCLE))
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

        switch (currentDirection) {
            case UP:
                trianglePath.reset();
                trianglePath.moveTo(centerX, paddingPx);
                trianglePath.lineTo(centerX - sizePx, paddingPx + (sizePx * 2));
                trianglePath.lineTo(centerX + sizePx, paddingPx + (sizePx * 2));
                trianglePath.close();
                break;
            case DOWN:
                trianglePath.reset();
                trianglePath.moveTo(centerX, paddingPx + (sizePx * 2));
                trianglePath.lineTo(centerX - sizePx, paddingPx);
                trianglePath.lineTo(centerX + sizePx, paddingPx);
                trianglePath.close();
                break;
            case LEFT:
                trianglePath.reset();
                trianglePath.moveTo(centerX - sizePx, paddingPx + sizePx);
                trianglePath.lineTo(centerX + sizePx, paddingPx);
                trianglePath.lineTo(centerX + sizePx, paddingPx + (sizePx * 2));
                trianglePath.close();
                break;
            case RIGHT:
                trianglePath.reset();
                trianglePath.moveTo(centerX + sizePx, paddingPx + sizePx);
                trianglePath.lineTo(centerX - sizePx, paddingPx);
                trianglePath.lineTo(centerX - sizePx, paddingPx + (sizePx * 2));
                trianglePath.close();
                break;
            case CIRCLE:
                break;
        }

        if (currentDirection == Direction.CIRCLE)
        {
            canvas.drawCircle(centerX, paddingPx + sizePx, sizePx, trianglePaint);
        }
        else
        {
            canvas.drawPath(trianglePath, trianglePaint);
        }

        // Draw Text at bottom with DP-based margin
        canvas.drawText("" + pkm.getKeyCount(), centerX, viewHeight - bottomOffsetPx, textPaint);
    }
}

