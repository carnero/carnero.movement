package carnero.movement.common.graph;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import carnero.movement.common.R;
import carnero.movement.common.model.Movement;
import carnero.movement.common.model.MovementEnum;
import carnero.movement.common.remotelog.RemoteLog;

public class ActivitiesGraph extends View {

    private long mDayStart; // ms
    private long mDayEnd; // ms
    private final ArrayList<Movement> mActivities = new ArrayList<Movement>();
    private MovementEnum[] mPixels;
    private Bitmap mCacheBitmap;
    private Canvas mCacheCanvas;
    private Paint mPaint;
    private int[] mColors = new int[MovementEnum.values().length];

    @SuppressWarnings("unused")
    public ActivitiesGraph(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("unused")
    public ActivitiesGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused")
    public ActivitiesGraph(Context context, AttributeSet attrs, int defStyleAttr) {
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
        mColors[MovementEnum.UNKNOWN.ordinal()] = getResources().getColor(R.color.none);
        mColors[MovementEnum.STILL.ordinal()] = getResources().getColor(R.color.movement_still);
        mColors[MovementEnum.WALK.ordinal()] = getResources().getColor(R.color.movement_walk);
        mColors[MovementEnum.RUN.ordinal()] = getResources().getColor(R.color.movement_run);
        mColors[MovementEnum.RIDE.ordinal()] = getResources().getColor(R.color.movement_ride);

        mPaint = new Paint();
        mPaint.setAntiAlias(false);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
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

        // Draw blocks
        if (mPixels == null) {
            return;
        }

        for (int i = 0; i < mPixels.length; i ++) {
            MovementEnum pixel = mPixels[i];
            if (pixel == null) {
                continue;
            }

            mPaint.setColor(mColors[pixel.ordinal()]);
            mCacheCanvas.drawRect(
                i,
                0,
                i + 1,
                getHeight(),
                mPaint
            );
        }

        // Draw it out
        canvas.save();
        canvas.drawBitmap(mCacheBitmap, 0, 0, null);
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int wOld, int hOld) {
        super.onSizeChanged(w, h, wOld, hOld);

        alignToPixels();
    }

    public void setData(int day, ArrayList<Movement> movements) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, day);
        mDayStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        mDayEnd = calendar.getTimeInMillis();

        synchronized (mActivities) {
            mActivities.clear();
            mActivities.addAll(movements);
        }

        alignToPixels();

        invalidate();
    }

    private void alignToPixels() {
        final int width = getWidth();
        if (width == 0) {
            return;
        }

        final MovementEnum[] pixels = new MovementEnum[getWidth()];

        synchronized (mActivities) {
            double milliWidth = width / (double) (mDayEnd - mDayStart);
            int pxStart;
            int pxEnd;

            for (Movement activity : mActivities) {
                pxStart = (int) Math.floor(((activity.start / 1e6) - mDayStart) * milliWidth);
                pxEnd = (int) Math.ceil(((activity.end / 1e6) - mDayStart) * milliWidth);

                if (pxStart < 0) {
                    pxStart = 0;
                }
                if (pxEnd >= width) {
                    pxEnd = width - 1;
                }
                if (pxEnd < pxStart) {
                    pxEnd = pxStart;
                }

                for (int i = pxStart; i < pxEnd; i ++) {
                    if (pixels[i] == null || activity.type.ordinal() > pixels[i].ordinal()) {
                        pixels[i] = activity.type;
                    }
                }
            }
        }

        mPixels = pixels;
    }
}
