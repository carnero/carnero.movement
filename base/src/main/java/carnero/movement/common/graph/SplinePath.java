package carnero.movement.common.graph;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.*;

import carnero.movement.common.R;
import carnero.movement.common.model.XY;
import carnero.movement.common.remotelog.RemoteLog;

@SuppressWarnings("unused")
public abstract class SplinePath {

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
    //
    private int mFillColor1;
    private int mFillColor2;
    private int mStrokeColor1;
    private int mStrokeColor2;
    //
    protected boolean mFillPath = false;
    protected int mFillColor1Res = R.color.none;
    protected int mFillColor2Res = R.color.none;
    protected int mFillGradient = GRADIENT_NONE;
    protected int mStrokeWidthRes = R.dimen.graph_stroke;
    protected int mStrokeColor1Res = R.color.none;
    protected int mStrokeColor2Res = R.color.none;
    protected boolean mStrokeGradient = false;
    protected int mPathWidthRes = R.dimen.graph_path;
    protected boolean mShowPoints = false;
    protected int mPointColorRes = R.color.none;
    protected int mPointSizeRes = R.dimen.graph_point;
    protected int mPointPaddingRes = R.dimen.graph_point_padding;
    //
    protected static final int GRADIENT_NONE = 0;
    protected static final int GRADIENT_HORIZONTAL = 1;
    protected static final int GRADIENT_VERTICAL = 1;

    public SplinePath() {
        // empty
    }

    public SplinePath(ArrayList<XY> XYs) {
        setData(XYs);
    }

    public void init(Resources resources) {
        // Load resources
        mFillColor1 = resources.getColor(mFillColor1Res);
        mFillColor2 = resources.getColor(mFillColor2Res);
        mStrokeColor1 = resources.getColor(mStrokeColor1Res);
        mStrokeColor2 = resources.getColor(mStrokeColor2Res);

        final int pointColor = resources.getColor(mPointColorRes);
        final int strokeWidth = resources.getDimensionPixelSize(mStrokeWidthRes);
        final int pathWidth = resources.getDimensionPixelSize(mPathWidthRes);
        final int pointSize;
        if (mPointSizeRes == 0) {
            pointSize = strokeWidth;
        } else {
            pointSize = resources.getDimensionPixelSize(mPointSizeRes);
        }
        final int pointPadding;
        if (mPointPaddingRes == 0) {
            pointPadding = 0;
        } else {
            pointPadding = resources.getDimensionPixelSize(mPointPaddingRes);
        }

        // Initialize paints
        mPaintFill = new Paint();
        mPaintFill.setColor(mFillColor1);
        mPaintFill.setStrokeWidth(1);
        mPaintFill.setStyle(Paint.Style.STROKE);

        mPaintPathBase = new Paint(); // For the transparent "corridor" for line
        mPaintPathBase.setAntiAlias(true);
        mPaintPathBase.setColor(Color.TRANSPARENT);
        mPaintPathBase.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
        mPaintPathBase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaintPathBase.setStrokeWidth(pathWidth);
        mPaintPathBase.setStyle(Paint.Style.STROKE);
        mPaintPathBase.setStrokeJoin(Paint.Join.ROUND);
        mPaintPathBase.setStrokeCap(Paint.Cap.ROUND);

        mPaintPathStroke = new Paint(); // For the line itself
        mPaintPathStroke.setAntiAlias(true);
        mPaintPathStroke.setColor(mStrokeColor1);
        mPaintPathStroke.setStrokeWidth(strokeWidth);
        mPaintPathStroke.setStyle(Paint.Style.STROKE);
        mPaintPathStroke.setStrokeJoin(Paint.Join.ROUND);
        mPaintPathStroke.setStrokeCap(Paint.Cap.ROUND);

        mPaintPointBase = new Paint();
        mPaintPointBase.setAntiAlias(true);
        mPaintPointBase.setColor(Color.TRANSPARENT);
        mPaintPointBase.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
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

    public void setData(List<XY> XYs) {
        synchronized (mPoints) {
            mPoints.clear();
            if (XYs != null) {
                mPoints.addAll(XYs);
            }
        }
    }

    public void alignToViewPort(int width, int height, int[] padding) {
        if (width == 0 || height == 0) {
            return;
        }

        // Set gradients
        if (mFillGradient == GRADIENT_HORIZONTAL) {
            final Shader shader = new LinearGradient(
                padding[3],
                0,
                width - padding[1],
                0,
                mFillColor1,
                mFillColor2,
                Shader.TileMode.CLAMP
            );

            mPaintFill.setShader(shader);
        } else if (mFillGradient == GRADIENT_VERTICAL) {
            final Shader shader = new LinearGradient(
                0,
                padding[2],
                0,
                height - padding[0],
                mFillColor1,
                mFillColor2,
                Shader.TileMode.CLAMP
            );

            mPaintFill.setShader(shader);
        }
        if (mStrokeGradient) {
            final Shader shader = new LinearGradient(
                padding[3],
                0,
                width - padding[1],
                0,
                mStrokeColor1,
                mStrokeColor2,
                Shader.TileMode.CLAMP
            );

            mPaintPathStroke.setShader(shader);
        }

        // Recalculate points
        if (mPoints.isEmpty()) {
            return;
        }

        final ArrayList<DeltaPoint> pixels = new ArrayList<DeltaPoint>();

        float xMin = Float.MAX_VALUE;
        float xMax = Float.MIN_VALUE;
        float yMin = Float.MAX_VALUE;
        float yMax = Float.MIN_VALUE;

        int graphW = width - padding[1] - padding[3];
        int graphH = height - padding[0] - padding[2];

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

                int x = (int)(xPercent * graphW);
                int y = (int)(graphH - (graphH * yPercent));

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

        ArrayList<DeltaPoint> curve = new ArrayList<DeltaPoint>(width);
        for (int i = 0; i < width; i++) {
            int curveX = i;
            int curveY = (int)mSpline.interpolate(i);

            curve.add(i, new DeltaPoint(curveX, curveY));
        }

        synchronized (mPixels) {
            mPixels.clear();
            mPixels.addAll(curve);
        }
    }

    public void draw(Canvas canvas, int[] padding) {
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
                        point.x + padding[3],
                        point.y + padding[0]
                    );

                    first = false;
                } else {
                    DeltaPoint prev = mPixels.get(i - 1);
                    mPath.cubicTo(
                        prev.x + prev.dx + padding[3],
                        prev.y + prev.dy + padding[0],
                        point.x - point.dx + padding[3],
                        point.y - point.dy + padding[0],
                        point.x + padding[3],
                        point.y + padding[0]
                    );
                }
            }
        }

        // Draw curve "corridor"
        canvas.drawPath(mPath, mPaintPathBase);

        // Fill path
        if (mFillPath) {
            synchronized (mPixels) {
                for (DeltaPoint pixel : mPixels) {
                    canvas.drawLine(
                        pixel.x + padding[3],
                        pixel.y + padding[0],
                        pixel.x + padding[3],
                        canvas.getHeight() - padding[2],
                        mPaintFill
                    );
                }
            }
        }

        // Draw curve
        canvas.drawPath(mPath, mPaintPathStroke);

        // Draw points
        if (mShowPoints) {
            synchronized (mPointsAligned) {
                for (DeltaPoint point : mPointsAligned) {
                    if (point.y + padding[0] >= canvas.getHeight() - padding[2]) {
                        continue;
                    }

                    canvas.drawCircle(
                        point.x + padding[3],
                        point.y + padding[0],
                        mPaintPointBase.getStrokeWidth() / 2,
                        mPaintPointBase
                    );
                    canvas.drawCircle(
                        point.x + padding[3],
                        point.y + padding[0],
                        mPaintPointStroke.getStrokeWidth() / 2,
                        mPaintPointStroke
                    );
                }
            }
        }
    }
}
