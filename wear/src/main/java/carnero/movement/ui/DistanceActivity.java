package carnero.movement.ui;

import java.util.ArrayList;

import android.os.Bundle;

import carnero.movement.R;
import carnero.movement.common.Utils;
import carnero.movement.graph.DistancePath;

public class DistanceActivity extends AbstractGraphActivity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mTodayPath = new DistancePath();

        initGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();

        vLabel.setText(R.string.title_distance);

        mTodayPath.setData(getValues(mContainer.distanceList));

        vGraph.setData(mPaths);
        vGraph.invalidate();
    }
}
