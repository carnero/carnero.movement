package carnero.movement.ui;

import java.util.ArrayList;

import carnero.movement.R;

public class StepsActivity extends AbstractGraphActivity {

    protected int getLineColor() {
        return getResources().getColor(R.color.graph_steps);
    }

    protected ArrayList<Double> getValues() {
        return mContainer.stepsList;
    }

    @Override
    protected void onResume() {
        super.onResume();

        vLabel.setText(getString(R.string.today) + ": " + getString(R.string.stats_steps, mContainer.stepsToday));
    }
}
