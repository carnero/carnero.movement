package carnero.movement.ui;

import java.util.ArrayList;

import android.os.Bundle;

import carnero.movement.R;
import carnero.movement.common.Utils;
import carnero.movement.common.model.XY;
import carnero.movement.graph.DistancePath;
import carnero.movement.graph.HistoryPath;

public class DistanceActivity extends AbstractGraphActivity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mYestedayPath = new HistoryPath();
        mTodayPath = new DistancePath();

        initGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();

        vLabel.setText(R.string.title_distance);

        ArrayList<XY> values = getValues(mContainer.distanceList);
        if (values.size() > 12) { // We have two days
            mYestedayPath.setData(values.subList(0, 11));
            mTodayPath.setData(values.subList(12, values.size() - 1));
        } else {
            mYestedayPath.setData(null);
            mTodayPath.setData(values);
        }

        vGraph.setData(mPaths);
        vGraph.invalidate();
    }
}
