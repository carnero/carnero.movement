package carnero.movement.common.graph;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import carnero.movement.R;

public class SplineGraph extends View {

    private final ArrayList<SplinePath> mPaths = new ArrayList<SplinePath>();
    private Bitmap mCacheBitmap;
    private Canvas mCacheCanvas;
    private int[] mPadding = new int[]{0, 0, 0, 0}; // top, right, bottom, left
    // TODO: move to XML
    private int[] mPaddingRes = new int[]{R.dimen.graph_padding_top, 0, 0, 0};

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

        mCacheBitmap.recycle();
        mCacheBitmap = null;
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
        // Create canvas if necessary
        if (mCacheBitmap == null || mCacheBitmap.isRecycled()) {
            mCacheBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
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

        // Draw it out
        canvas.save();
        canvas.drawBitmap(mCacheBitmap, 0, 0, null);
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int wOld, int hOld) {
        super.onSizeChanged(w, h, wOld, hOld);

        synchronized (mPaths) {
            for (SplinePath path : mPaths) {
                path.alignToViewPort(getWidth(), getHeight(), mPadding);
            }
        }
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

        invalidate();
    }
}
