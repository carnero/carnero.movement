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
    private ArrayList<Movement> mActivities = new ArrayList<Movement>();
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
        mColors[MovementEnum.STILL.ordinal()] = getResources().getColor(R.color.movement_still);
        mColors[MovementEnum.WALK.ordinal()] = getResources().getColor(R.color.movement_walk);
        mColors[MovementEnum.RUN.ordinal()] = getResources().getColor(R.color.movement_run);
        mColors[MovementEnum.RIDE.ordinal()] = getResources().getColor(R.color.movement_ride);
        mColors[MovementEnum.UNKNOWN.ordinal()] = getResources().getColor(R.color.none);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
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
        double milliWidth = getWidth() / (double) (mDayEnd - mDayStart);
        float pxStart;
        float pxEnd;

        for (Movement activity : mActivities) {
            pxStart = (float) (((activity.start / 1e6) - mDayStart) * milliWidth);
            pxEnd = (float) (((activity.end / 1e6) - mDayStart) * milliWidth);

            RemoteLog.d("Activity " + activity.type + ": " + pxStart + " .. " + pxEnd + " (from " + getWidth() + ")");

            mPaint.setColor(mColors[activity.type.ordinal()]);
            mCacheCanvas.drawRect(
                pxStart,
                0,
                pxEnd,
                getHeight(),
                mPaint
            );
        }

        // Draw it out
        canvas.save();
        canvas.drawBitmap(mCacheBitmap, 0, 0, null);
        canvas.restore();
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

        invalidate();
    }
}
