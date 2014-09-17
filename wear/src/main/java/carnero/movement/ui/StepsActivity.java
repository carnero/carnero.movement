package carnero.movement.ui;

import java.util.ArrayList;

import android.os.Bundle;

import carnero.movement.R;
import carnero.movement.graph.DistancePath;
import carnero.movement.graph.StepsPath;

public class StepsActivity extends AbstractGraphActivity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mTodayPath = new StepsPath();

        initGraph();
    }

    @Override
    protected void onResume() {
        super.onResume();

        vLabel.setText(R.string.title_steps);

        mTodayPath.setData(getValues(mContainer.stepsList));

        vGraph.setData(mPaths);
        vGraph.invalidate();
    }
}
