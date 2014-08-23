package carnero.movement.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.SmoothLineGraph;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Utils;
import carnero.movement.data.ModelDataContainer;

public abstract class AbstractGraphActivity extends AbstractBaseActivity {

    protected ModelDataContainer mContainer;
    protected Line mLine;
    //
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.graph)
    SmoothLineGraph vGraph;
    
    protected abstract int getLineColor();

    protected abstract ArrayList<Double> getValues();

    public void onCreate(Bundle state) {
        super.onCreate(state);

        final Intent intent = getIntent();
        if (intent != null) {
            mContainer = intent.getParcelableExtra("data");
        }

        setContentView(R.layout.notification_activity);
        ButterKnife.inject(this);

        mLine = new Line();
        mLine.setFill(true);
        mLine.setShowingPoints(false);
        mLine.setColor(getLineColor());

        vGraph.addLine(mLine);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Label
        if (this instanceof DistanceActivity) {
            vLabel.setText(getString(R.string.today) + ": " + Utils.formatDistance(mContainer.distance));
        } else if (this instanceof StepsActivity) {
            vLabel.setText(getString(R.string.today) + ": " + getString(R.string.stats_steps, mContainer.steps));
        }

        // Graph
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        mLine.getPoints().clear();

        ArrayList<Double> values = getValues();
        for (int i = 0; i < values.size(); i ++) {
            Double distance = values.get(i);

            LinePoint pointDistance = new LinePoint();
            pointDistance.setX(i);
            pointDistance.setY(distance);
            mLine.addPoint(pointDistance);

            min = Math.min(min, distance);
            max = Math.max(max, distance);
        }

        vGraph.setRangeY((float) min, (float) max);
        vGraph.invalidate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            mContainer = intent.getParcelableExtra("data");
        }
    }
}
