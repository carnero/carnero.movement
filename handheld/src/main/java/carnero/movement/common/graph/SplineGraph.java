package carnero.movement.common.graph;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import carnero.movement.R;
import carnero.movement.ui.view.XY;

public class SplineGraph extends View {

    private final ArrayList<XY> mPoints = new ArrayList<XY>();
    private final ArrayList<DeltaPoint> mPointsAligned = new ArrayList<DeltaPoint>();
    private final ArrayList<DeltaPoint> mPixels = new ArrayList<DeltaPoint>();
    private Spline mSpline;
    private Path mPath;
    private Paint mPaintFill;
    private Paint mPaintPathBase;
    private Paint mPaintPathStroke;
    private Paint mPaintPointBase;
    private Paint mPaintPointStroke;
    private Bitmap mCacheBitmap;
    private Canvas mCacheCanvas;
    private int[] mPadding = new int[]{0, 0, 0, 0}; // top, right, bottom, left
    // config, TODO: move to XML attrs
    private int[] mPaddingRes = new int[]{R.dimen.graph_padding_top, 0, 0, 0};
    private boolean mFillPath = true;
    private int mFillColor1Res = R.color.graph_distance_fill;
    private int mFillColor2Res = R.color.none;
    private boolean mFillGradient = true;
    private int mStrokeWidthRes = R.dimen.graph_stroke;
    private int mStrokeColor1Res = R.color.graph_distance_outline_start;
    private int mStrokeColor2Res = R.color.graph_distance_outline_end;
    private boolean mStrokeGradient = true;
    private int mPathWidthRes = R.dimen.graph_path;
    private boolean mShowPoints = true;
    private int mPointColorRes = R.color.graph_distance_point;
    private int mPointSizeRes = R.dimen.graph_point;
    private int mPointPaddingRes = R.dimen.graph_point_padding;

    @SuppressWarnings("unused")
    public SplineGraph(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("unused")
    public SplineGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused")
    public SplineGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initialize graph (paint, paths...)
     */
    private void init() {
        // Load resources
        final int fillColor1 = getResources().getColor(mFillColor1Res);
        final int fillColor2 = getResources().getColor(mFillColor2Res);
        final int pointColor = getResources().getColor(mPointColorRes);
        final int strokeColor1 = getResources().getColor(mStrokeColor1Res);
        final int strokeColor2 = getResources().getColor(mStrokeColor2Res);
        final int strokeWidth = getResources().getDimensionPixelSize(mStrokeWidthRes);
        final int pathWidth = getResources().getDimensionPixelSize(mPathWidthRes);
        final int pointSize;
        if (mPointSizeRes == 0) {
            pointSize = strokeWidth;
        } else {
            pointSize = getResources().getDimensionPixelSize(mPointSizeRes);
        }
        final int pointPadding;
        if (mPointPaddingRes == 0) {
            pointPadding = 0;
        } else {
            pointPadding = getResources().getDimensionPixelSize(mPointPaddingRes);
        }

        for (int i = 0; i < 4; i++) {
            if (mPaddingRes[i] == 0) {
                mPadding[i] = 0;
                continue;
            }

            mPadding[i] = getResources().getDimensionPixelSize(mPaddingRes[i]);
        }

        // Initialize paints
        mPaintFill = new Paint();
        mPaintFill.setColor(fillColor1);
        mPaintFill.setStrokeWidth(1);
        mPaintFill.setStyle(Paint.Style.STROKE);
        if (mFillGradient) {
            final Shader shader = new LinearGradient(
                0,
                0,
                0,
                600, // TODO: move to place where we have View height
                fillColor1,
                fillColor2,
                Shader.TileMode.CLAMP
            );

            mPaintFill.setShader(shader);
        }

        mPaintPathBase = new Paint(); // For the transparent "corridor" for line
        mPaintPathBase.setAntiAlias(true);
        mPaintPathBase.setColor(Color.TRANSPARENT);
        mPaintPathBase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaintPathBase.setStrokeWidth(pathWidth);
        mPaintPathBase.setStyle(Paint.Style.STROKE);
        mPaintPathBase.setStrokeJoin(Paint.Join.ROUND);
        mPaintPathBase.setStrokeCap(Paint.Cap.ROUND);

        mPaintPathStroke = new Paint(); // For the line itself
        mPaintPathStroke.setAntiAlias(true);
        mPaintPathStroke.setColor(strokeColor2);
        mPaintPathStroke.setStrokeWidth(strokeWidth);
        mPaintPathStroke.setStyle(Paint.Style.STROKE);
        mPaintPathStroke.setStrokeJoin(Paint.Join.ROUND);
        mPaintPathStroke.setStrokeCap(Paint.Cap.ROUND);
        if (mStrokeGradient) {
            final Shader shader = new LinearGradient(
                0,
                0,
                800, // TODO: move to place where we have View width
                0,
                strokeColor1,
                strokeColor2,
                Shader.TileMode.CLAMP
            );

            mPaintPathStroke.setShader(shader);
        }

        mPaintPointBase = new Paint();
        mPaintPointBase.setAntiAlias(true);
        mPaintPointBase.setColor(Color.TRANSPARENT);
        mPaintPointBase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaintPointBase.setStrokeWidth(pointSize + pointPadding);
        mPaintPointBase.setStyle(Paint.Style.FILL_AND_STROKE);

        mPaintPointStroke = new Paint();
        mPaintPointStroke.setAntiAlias(true);
        mPaintPointStroke.setColor(pointColor);
        mPaintPointStroke.setStrokeWidth(pointSize);
        mPaintPointStroke.setStyle(Paint.Style.FILL_AND_STROKE);
        
        // Initialize path
        mPath = new Path();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Clear path
        mPath.reset();

        if (mSpline == null) {
            return;
        }

        // Create path, smooth point-to-point segments
        synchronized (mPixels) {
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
                        point.x + mPadding[3],
                        point.y + mPadding[0]
                    );

                    first = false;
                } else {
                    DeltaPoint prev = mPixels.get(i - 1);
                    mPath.cubicTo(
                        prev.x + prev.dx + mPadding[3],
                        prev.y + prev.dy + mPadding[0],
                        point.x - point.dx + mPadding[3],
                        point.y - point.dy + mPadding[0],
                        point.x + mPadding[3],
                        point.y + mPadding[0]
                    );
                }
            }
        }

        // Create canvas if necessary
        if (mCacheBitmap == null) {
            mCacheBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            mCacheCanvas = new Canvas(mCacheBitmap);
        }

        // Clear canvas
        mCacheCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Draw curve "corridor"
        mCacheCanvas.drawPath(mPath, mPaintPathBase);

        // Fill path
        if (mFillPath) {
            synchronized (mPixels) {
                for (DeltaPoint pixel : mPixels) {
                    mCacheCanvas.drawLine(
                        pixel.x + mPadding[3],
                        pixel.y + mPadding[0],
                        pixel.x + mPadding[3],
                        getHeight() - mPadding[2],
                        mPaintFill
                    );
                }
            }
        }

        // Draw curve
        mCacheCanvas.drawPath(mPath, mPaintPathStroke);

        // Draw points
        if (mShowPoints) {
            synchronized (mPointsAligned) {
                for (DeltaPoint point : mPointsAligned) {
                    if (point.y + mPadding[0] >= getHeight() - mPadding[2]) {
                        continue;
                    }

                    mCacheCanvas.drawCircle(
                        point.x + mPadding[3],
                        point.y + mPadding[0],
                        mPaintPointBase.getStrokeWidth() / 2,
                        mPaintPointBase
                    );
                    mCacheCanvas.drawCircle(
                        point.x + mPadding[3],
                        point.y + mPadding[0],
                        mPaintPointStroke.getStrokeWidth() / 2,
                        mPaintPointStroke
                    );
                }
            }
        }

        // Draw it out
        canvas.save();
        canvas.drawBitmap(mCacheBitmap, 0, 0, null);
        canvas.restore();
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
        if (mPoints.isEmpty()) {
            return;
        }

        final ArrayList<DeltaPoint> pixels = new ArrayList<DeltaPoint>();

        float xMin = Float.MAX_VALUE;
        float xMax = Float.MIN_VALUE;
        float yMin = Float.MAX_VALUE;
        float yMax = Float.MIN_VALUE;

        int width = getWidth() - mPadding[1] - mPadding[3];
        int height = getHeight() - mPadding[0] - mPadding[2];

        // Align points within viewport
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

        // Store aligned points
        synchronized (mPointsAligned) {
            mPointsAligned.clear();
            mPointsAligned.addAll(pixels);
        }

        // Compute spline & interpolate each pixel on screen
        float[] x = new float[pixels.size()];
        float[] y = new float[pixels.size()];
        for (int i = 0; i < pixels.size(); i++) {
            DeltaPoint pixel = pixels.get(i);
            x[i] = pixel.x;
            y[i] = pixel.y;
        }

        mSpline = Spline.createMonotoneCubicSpline(x, y);

        ArrayList<DeltaPoint> curve = new ArrayList<DeltaPoint>(getWidth());
        for (int i = 0; i < getWidth(); i++) {
            int curveX = i;
            int curveY = (int)mSpline.interpolate(i);

            curve.add(i, new DeltaPoint(curveX, curveY));
        }

        synchronized (mPixels) {
            mPixels.clear();
            mPixels.addAll(curve);
        }
    }
}
