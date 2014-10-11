package carnero.movement.common.graph;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import carnero.movement.common.R;

public class SplineGraph extends View {

    private final ArrayList<SplinePath> mPaths = new ArrayList<SplinePath>();
    private Bitmap mCacheBitmap;
    private Canvas mCacheCanvas;
    private int[] mPadding = new int[]{0, 0, 0, 0}; // top, right, bottom, left
    // TODO: move to XML
    private int[] mPaddingRes = new int[]{
        R.dimen.graph_padding_top,
        0,
        0,
        0
    };

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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
            mCacheBitmap = null;
        }
    }

    /**
     * Initialize graph (paint, paths...)
     */
    private void init() {
        for (int i = 0; i < 4; i++) {
            if (mPaddingRes[i] == 0) {
                mPadding[i] = 0;
                continue;
            }

            mPadding[i] = getResources().getDimensionPixelSize(mPaddingRes[i]);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mCacheBitmap == null) {
            return;
        }

        // Draw it out
        canvas.save();
        canvas.drawBitmap(mCacheBitmap, 0, 0, null);
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int wOld, int hOld) {
        super.onSizeChanged(w, h, wOld, hOld);

        if (w <= 0 || h <= 0) {
            return;
        }

        // Align paths to viewport
        synchronized (mPaths) {
            for (SplinePath path : mPaths) {
                path.alignToViewPort(w, h, mPadding);
            }
        }

        drawIt();
    }

    public void setData(ArrayList<SplinePath> paths) {
        synchronized (mPaths) {
            mPaths.clear();
            mPaths.addAll(paths);

            for (SplinePath path : mPaths) {
                path.init(getResources());
                path.alignToViewPort(getWidth(), getHeight(), mPadding);
            }
        }

        drawIt();
        invalidate();
    }

    private void drawIt() {
        final int w = getWidth();
        final int h = getHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        // Create canvas if necessary
        if (mCacheBitmap == null || mCacheBitmap.isRecycled()
            || mCacheBitmap.getWidth() != w || mCacheBitmap.getHeight() != h) {

            mCacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCacheCanvas = new Canvas(mCacheBitmap);
        }

        // Clear canvas
        mCacheCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Draw paths
        synchronized (mPaths) {
            for (SplinePath path : mPaths) {
                path.draw(mCacheCanvas, mPadding);
            }
        }
    }
}
