package carnero.movement.ui;

import java.util.ArrayList;

import carnero.movement.R;
import carnero.movement.common.Utils;

public class DistanceActivity extends AbstractGraphActivity {

    protected int getLineColor() {
        return getResources().getColor(R.color.graph_distance);
    }

    protected ArrayList<Double> getValues() {
        return mContainer.distanceList;
    }

    @Override
    protected void onResume() {
        super.onResume();

        vLabel.setText(getString(R.string.today) + ": " + Utils.formatDistance(mContainer.distanceToday));
    }
}
