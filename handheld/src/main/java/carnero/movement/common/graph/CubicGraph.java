package carnero.movement.common.graph;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import carnero.movement.R;
import carnero.movement.ui.view.XY;

public class CubicGraph extends View {

    private final ArrayList<XY> mPoints = new ArrayList<XY>();
    private final ArrayList<DeltaPoint> mPixels = new ArrayList<DeltaPoint>();
    private Path mPath;
    private Paint mPaintBase;
    private Paint mPaintPath;
    private Bitmap mCacheBitmap;
    private Canvas mCacheCanvas;
    // config, TODO: move to XML attrs
    private int mStrokeColor = R.color.graph_distance_outline;
    private int mStrokeWidth = R.dimen.graph_stroke;

    @SuppressWarnings("unused")
    public CubicGraph(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("unused")
    public CubicGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused")
    public CubicGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialize graph (paint, paths...)
     */
    private void init() {
        final int strokeColor = getResources().getColor(mStrokeColor);
        final int strokeWidth = getResources().getDimensionPixelSize(mStrokeWidth);

        mPaintBase = new Paint(); // For the transparent "corridor" for line
        mPaintBase.setAntiAlias(true);
        mPaintBase.setColor(Color.TRANSPARENT);
        mPaintBase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaintBase.setStrokeWidth(strokeWidth * 2);
        mPaintBase.setStyle(Paint.Style.STROKE);
        mPaintBase.setStrokeJoin(Paint.Join.ROUND);
        mPaintBase.setStrokeCap(Paint.Cap.ROUND);

        mPaintPath = new Paint(); // For the line itself
        mPaintPath.setAntiAlias(true);
        mPaintPath.setColor(strokeColor);
        mPaintPath.setStrokeWidth(strokeWidth);
        mPaintPath.setStyle(Paint.Style.STROKE);
        mPaintPath.setStrokeJoin(Paint.Join.ROUND);
        mPaintPath.setStrokeCap(Paint.Cap.ROUND);

        mPath = new Path();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Clear path
        mPath.reset();

        // Calculate path
        // more: http://stackoverflow.com/questions/8287949/android-how-to-draw-a-smooth-line-following-your-finger
        // more: https://github.com/johncarl81/androiddraw/tree/master/src/main/java/org/example/androiddraw
        if (mPixels.size() > 1) {
            for (int i = mPixels.size() - 2; i < mPixels.size(); i++) {
                if (i >= 0) {
                    DeltaPoint point = mPixels.get(i);

                    if (i == 0) {
                        DeltaPoint next = mPixels.get(i + 1);
                        point.dx = ((next.x - point.x) / 3);
                        point.dy = ((next.y - point.y) / 3);
                    } else if (i == mPixels.size() - 1) {
                        DeltaPoint prev = mPixels.get(i - 1);
                        point.dx = ((point.x - prev.x) / 3);
                        point.dy = ((point.y - prev.y) / 3);
                    } else {
                        DeltaPoint next = mPixels.get(i + 1);
                        DeltaPoint prev = mPixels.get(i - 1);
                        point.dx = ((next.x - prev.x) / 3);
                        point.dy = ((next.y - prev.y) / 3);
                    }
                }
            }
        }

        boolean first = true;
        for (int i = 0; i < mPixels.size(); i++) {
            final DeltaPoint point = mPixels.get(i);
            if (first) {
                mPath.moveTo(
                    point.x,
                    point.y
                );

                first = false;
            } else {
                DeltaPoint prev = mPixels.get(i - 1);
                mPath.cubicTo(
                    prev.x + prev.dx,
                    prev.y + prev.dy,
                    point.x - point.dx,
                    point.y - point.dy,
                    point.x,
                    point.y
                );
            }
        }

        { // Canvas...
            if (mCacheBitmap == null) {
                mCacheBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                mCacheCanvas = new Canvas(mCacheBitmap);
            }

            // Clear canvas
            mCacheCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Draw path
            mCacheCanvas.drawPath(mPath, mPaintBase);
            mCacheCanvas.drawPath(mPath, mPaintPath);

            // Draw it out
            canvas.save();
            canvas.drawBitmap(mCacheBitmap, 0, 0, null);
            canvas.restore();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int wOld, int hOld) {
        super.onSizeChanged(w, h, wOld, hOld);

        alignPoints();
    }

    /**
     * Set points to be displayed
     *
     * @param XYs list of points
     */
    public void setData(ArrayList<XY> XYs) {
        synchronized (mPoints) {
            mPoints.clear();
            mPoints.addAll(XYs);
        }

        alignPoints();
        invalidate();
    }

    /**
     * Recalculate points to match available viewport
     */
    private void alignPoints() {
        final ArrayList<DeltaPoint> pixels = new ArrayList<DeltaPoint>();

        float xMin = Float.MAX_VALUE;
        float xMax = Float.MIN_VALUE;
        float yMin = Float.MAX_VALUE;
        float yMax = Float.MIN_VALUE;

        int width = getWidth();
        int height = getHeight();

        synchronized (mPoints) {
            for (XY point : mPoints) { // Find min/max
                xMin = Math.min(xMin, point.x);
                xMax = Math.max(xMax, point.x);
                yMin = Math.min(yMin, point.y);
                yMax = Math.max(yMax, point.y);
            }

            for (XY point : mPoints) { // Align pixels to available viewport
                float xPercent = (point.x - xMin) / (xMax - xMin);
                float yPercent = (point.y - yMin) / (yMax - yMin);

                int x = (int)(xPercent * width);
                int y = (int)(height - (height * yPercent));

                pixels.add(new DeltaPoint(x, y));
            }
        }

        synchronized (mPixels) {
            mPixels.clear();
            mPixels.addAll(pixels);
        }
    }
}
