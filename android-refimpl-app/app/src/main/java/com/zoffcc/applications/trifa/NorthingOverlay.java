package com.zoffcc.applications.trifa;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class NorthingOverlay extends Overlay
{
    private static final String TAG = "NorthingOverlay";

    protected MapView mMapView;
    private final Display mDisplay;
    protected final float mScale;
    private Paint sSmoothPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final boolean mInCenter = false;
    private final float mCompassCenterX = 35.0f;
    private final float mCompassCenterY = 35.0f;
    private final float mCompassRadius = 20.0f;

    private boolean is_northed = false;

    protected Bitmap mCompassFrameBitmap;
    protected Bitmap mCompassRoseBitmap;
    private final Matrix mCompassMatrix = new Matrix();

    protected final float mCompassFrameCenterX;
    protected final float mCompassFrameCenterY;
    protected final float mCompassRoseCenterX;
    protected final float mCompassRoseCenterY;
    protected long mLastRender = 0;

    private final int mLastRenderLag = 500;

    public NorthingOverlay(Context context, MapView mapView)
    {
        super();
        mScale = context.getResources().getDisplayMetrics().density;

        mMapView = mapView;
        final WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();

        createCompassFramePicture();
        set_picture();

        mCompassFrameCenterX = mCompassFrameBitmap.getWidth() / 2f - 0.5f;
        mCompassFrameCenterY = mCompassFrameBitmap.getHeight() / 2f - 0.5f;
        mCompassRoseCenterX = mCompassRoseBitmap.getWidth() / 2f - 0.5f;
        mCompassRoseCenterY = mCompassRoseBitmap.getHeight() / 2f - 0.5f;

        invalidateCompass();

        Log.i(TAG, "NorthingOverlay");
    }

    public interface NorthingCallback
    {
        void update_is_northing(boolean value);
    }

    static NorthingCallback northing_callback_function = null;

    public static void set_northing_callback(NorthingCallback callback)
    {
        northing_callback_function = callback;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            this.invalidateCompass();
        }
        Log.i(TAG, "onResume");
    }

    private void set_picture()
    {
        if (is_northed)
        {
            createPointerPicture();
        }
        else
        {
            createCompassRosePicture();
        }
    }

    public void set_is_northed(boolean northed)
    {
        is_northed = northed;
        if (is_northed)
        {
            mMapView.setMapOrientation(0.0f);
        }

        set_picture();
        invalidateCompass();
        mMapView.invalidate();

        if (northing_callback_function != null) {
            northing_callback_function.update_is_northing(is_northed);
        }
    }

    @Override
    public void onDetach(MapView mapView) {
        this.mMapView = null;
        sSmoothPaint = null;
        mCompassFrameBitmap.recycle();
        mCompassRoseBitmap.recycle();
        super.onDetach(mapView);
        Log.i(TAG, "onDetach");
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e, final MapView mapView) {
        Log.i(TAG, "onSingleTapConfirmed " + e);
        set_is_northed(!is_northed);
        return true;
    }

    private void invalidateCompass() {
        Log.i(TAG, "invalidateCompass");

        if (mLastRender + mLastRenderLag > System.currentTimeMillis())
        {
            return;
        }
        mLastRender = System.currentTimeMillis();
        Rect screenRect = mMapView.getProjection().getScreenRect();
        int frameLeft;
        int frameRight;
        int frameTop;
        int frameBottom;
        if (mInCenter) {
            frameLeft = screenRect.left
                        + (int) Math.ceil(screenRect.exactCenterX() - mCompassFrameCenterX);
            frameTop = screenRect.top
                       + (int) Math.ceil(screenRect.exactCenterY() - mCompassFrameCenterY);
            frameRight = screenRect.left
                         + (int) Math.ceil(screenRect.exactCenterX() + mCompassFrameCenterX);
            frameBottom = screenRect.top
                          + (int) Math.ceil(screenRect.exactCenterY() + mCompassFrameCenterY);
        } else {
            frameLeft = screenRect.left
                        + (int) Math.ceil(mCompassCenterX * mScale - mCompassFrameCenterX);
            frameTop = screenRect.top
                       + (int) Math.ceil(mCompassCenterY * mScale - mCompassFrameCenterY);
            frameRight = screenRect.left
                         + (int) Math.ceil(mCompassCenterX * mScale + mCompassFrameCenterX);
            frameBottom = screenRect.top
                          + (int) Math.ceil(mCompassCenterY * mScale + mCompassFrameCenterY);
        }

        // Expand by 2 to cover stroke width
        mMapView.postInvalidateMapCoordinates(frameLeft - 2, frameTop - 2, frameRight + 2,
                                              frameBottom + 2);
    }

    private Point calculatePointOnCircle(final float centerX, final float centerY,
                                         final float radius, final float degrees) {
        // for trigonometry, 0 is pointing east, so subtract 90
        // compass degrees are the wrong way round
        final double dblRadians = Math.toRadians(-degrees + 90);

        final int intX = (int) (radius * Math.cos(dblRadians));
        final int intY = (int) (radius * Math.sin(dblRadians));

        return new Point((int) centerX + intX, (int) centerY - intY);
    }

    private void drawTriangle(final Canvas canvas, final float x, final float y,
                              final float radius, final float degrees, final Paint paint) {
        canvas.save();
        final Point point = this.calculatePointOnCircle(x, y, radius, degrees);
        canvas.rotate(degrees, point.x, point.y);
        final Path p = new Path();
        p.moveTo(point.x - 2 * mScale, point.y);
        p.lineTo(point.x + 2 * mScale, point.y);
        p.lineTo(point.x, point.y - 5 * mScale);
        p.close();
        canvas.drawPath(p, paint);
        canvas.restore();
    }

    private void createCompassFramePicture() {
        // The inside of the compass is white and transparent
        final Paint innerPaint = new Paint();
        innerPaint.setColor(Color.WHITE);
        innerPaint.setAntiAlias(true);
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setAlpha(200);

        // The outer part (circle and little triangles) is gray and transparent
        final Paint outerPaint = new Paint();
        outerPaint.setColor(Color.GRAY);
        outerPaint.setAntiAlias(true);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(2.0f);
        outerPaint.setAlpha(200);

        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 * mScale);
        final int center = picBorderWidthAndHeight / 2;
        if (mCompassFrameBitmap != null)
            mCompassFrameBitmap.recycle();
        mCompassFrameBitmap = Bitmap.createBitmap(picBorderWidthAndHeight, picBorderWidthAndHeight,
                                                  Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mCompassFrameBitmap);

        // draw compass inner circle and border
        canvas.drawCircle(center, center, mCompassRadius * mScale, innerPaint);
        canvas.drawCircle(center, center, mCompassRadius * mScale, outerPaint);

        // Draw little triangles north, south, west and east (don't move)
        // to make those move use "-bearing + 0" etc. (Note: that would mean to draw the triangles
        // in the onDraw() method)
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 0, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 90, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 180, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 270, outerPaint);
    }

    private void createCompassRosePicture() {
        // Paint design of north triangle (it's common to paint north in red color)
        final Paint northPaint = new Paint();
        northPaint.setColor(0xFFA00000);
        northPaint.setAntiAlias(true);
        northPaint.setStyle(Paint.Style.FILL);
        northPaint.setAlpha(220);

        // Paint design of south triangle (black)
        final Paint southPaint = new Paint();
        southPaint.setColor(Color.BLACK);
        southPaint.setAntiAlias(true);
        southPaint.setStyle(Paint.Style.FILL);
        southPaint.setAlpha(220);

        // Create a little white dot in the middle of the compass rose
        final Paint centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setAntiAlias(true);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setAlpha(220);

        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 * mScale);
        final int center = picBorderWidthAndHeight / 2;

        if (mCompassRoseBitmap != null)
        {
            mCompassRoseBitmap.recycle();
        }
        mCompassRoseBitmap = Bitmap.createBitmap(picBorderWidthAndHeight, picBorderWidthAndHeight,
                                                 Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mCompassRoseBitmap);

        // Triangle pointing north
        final Path pathNorth = new Path();
        pathNorth.moveTo(center, center - (mCompassRadius - 3) * mScale);
        pathNorth.lineTo(center + 4 * mScale, center);
        pathNorth.lineTo(center - 4 * mScale, center);
        pathNorth.lineTo(center, center - (mCompassRadius - 3) * mScale);
        pathNorth.close();
        canvas.drawPath(pathNorth, northPaint);

        // Triangle pointing south
        final Path pathSouth = new Path();
        pathSouth.moveTo(center, center + (mCompassRadius - 3) * mScale);
        pathSouth.lineTo(center + 4 * mScale, center);
        pathSouth.lineTo(center - 4 * mScale, center);
        pathSouth.lineTo(center, center + (mCompassRadius - 3) * mScale);
        pathSouth.close();
        canvas.drawPath(pathSouth, southPaint);

        // Draw a little white dot in the middle
        canvas.drawCircle(center, center, 2, centerPaint);
    }

    /**
     * A black pointer arrow.
     */
    private void createPointerPicture() {
        final Paint arrowPaint = new Paint();
        arrowPaint.setColor(Color.BLACK);
        arrowPaint.setAntiAlias(true);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAlpha(220);

        // Create a little white dot in the middle of the compass rose
        final Paint centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setAntiAlias(true);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setAlpha(220);

        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 * mScale);
        final int center = picBorderWidthAndHeight / 2;

        if (mCompassRoseBitmap != null)
        {
            mCompassRoseBitmap.recycle();
        }
        mCompassRoseBitmap = Bitmap.createBitmap(picBorderWidthAndHeight, picBorderWidthAndHeight,
                                                 Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mCompassRoseBitmap);

        // Arrow comprised of 2 triangles
        final Path pathArrow = new Path();
        pathArrow.moveTo(center, center - (mCompassRadius - 3) * mScale);
        pathArrow.lineTo(center + 4 * mScale, center + (mCompassRadius - 3) * mScale);
        pathArrow.lineTo(center, center + 0.5f * (mCompassRadius - 3) * mScale);
        pathArrow.lineTo(center - 4 * mScale, center + (mCompassRadius - 3) * mScale);
        pathArrow.lineTo(center, center - (mCompassRadius - 3) * mScale);
        pathArrow.close();
        canvas.drawPath(pathArrow, arrowPaint);

        // Draw a little white dot in the middle
        canvas.drawCircle(center, center, 2, centerPaint);
    }

    protected void drawCompass(final Canvas canvas, final float bearing, final Rect screenRect) {
        final Projection proj = mMapView.getProjection();

        float centerX;
        float centerY;
        if (mInCenter) {
            final Rect rect = proj.getScreenRect();
            centerX = rect.exactCenterX();
            centerY = rect.exactCenterY();
        } else {
            centerX = mCompassCenterX * mScale;
            centerY = mCompassCenterY * mScale;
        }

        mCompassMatrix.setTranslate(-mCompassFrameCenterX, -mCompassFrameCenterY);
        mCompassMatrix.postTranslate(centerX, centerY);

        proj.save(canvas, false, true);
        canvas.concat(mCompassMatrix);
        canvas.drawBitmap(mCompassFrameBitmap, 0, 0, sSmoothPaint);
        proj.restore(canvas, true);

        mCompassMatrix.setRotate(-bearing, mCompassRoseCenterX, mCompassRoseCenterY);
        mCompassMatrix.postTranslate(-mCompassRoseCenterX, -mCompassRoseCenterY);
        mCompassMatrix.postTranslate(centerX, centerY);

        proj.save(canvas, false, true);
        canvas.concat(mCompassMatrix);
        canvas.drawBitmap(mCompassRoseBitmap, 0, 0, sSmoothPaint);
        proj.restore(canvas, true);
    }

    @Override
    public void draw(Canvas c, Projection pProjection) {
        float map_orientation = mMapView.getMapOrientation();
        drawCompass(c, -map_orientation, pProjection.getScreenRect());
    }
}
