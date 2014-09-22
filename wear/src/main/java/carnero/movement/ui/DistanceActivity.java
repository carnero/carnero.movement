package carnero.movement.ui;

import java.util.ArrayList;

import android.os.Bundle;

import carnero.movement.R;
import carnero.movement.common.model.XY;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.graph.DistancePath;
import carnero.movement.graph.HistoryPath;

public class DistanceActivity extends AbstractGraphActivity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mYesterdayPath = new HistoryPath();
        mTodayPath = new DistancePath();

        initGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();

        vLabel.setText(R.string.title_distance);

        ArrayList<XY> values = getValues(mContainer.distanceList);

        // Set common maximum
        float yMax = Float.MIN_VALUE;
        for (XY xy : values) {
            yMax = Math.max(yMax, xy.y);
        }
        mYesterdayPath.setMaximumY(yMax);
        mTodayPath.setMaximumY(yMax);

        RemoteLog.i("Distance values: " + values.size());

        // Set data
        if (values.size() > 12) { // We have two days
            mYesterdayPath.setData(values.subList(0, 12));
            mTodayPath.setData(values.subList(12, values.size()));
        } else {
            mYesterdayPath.setData(null);
            mTodayPath.setData(values);
        }

        vGraph.setData(mPaths);
        vGraph.invalidate();
    }
}
