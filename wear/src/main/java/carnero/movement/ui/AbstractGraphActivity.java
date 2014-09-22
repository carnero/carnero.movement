package carnero.movement.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import carnero.movement.common.graph.SplineGraph;
import carnero.movement.common.graph.SplinePath;
import carnero.movement.common.model.XY;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.data.ModelDataContainer;

public abstract class AbstractGraphActivity extends AbstractBaseActivity {

    protected ModelDataContainer mContainer;
    protected SplinePath mYesterdayPath;
    protected SplinePath mTodayPath;
    protected final ArrayList<SplinePath> mPaths = new ArrayList<SplinePath>();
    //
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.graph)
    SplineGraph vGraph;
    
    public void onCreate(Bundle state) {
        super.onCreate(state);

        final Intent intent = getIntent();
        if (intent != null) {
            mContainer = intent.getParcelableExtra("data");
        }

        setContentView(R.layout.notification_activity);
        ButterKnife.inject(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            mContainer = intent.getParcelableExtra("data");
        }
    }

    protected void initGraph() {
        mPaths.clear();
        mPaths.add(mYesterdayPath);
        mPaths.add(mTodayPath);
    }

    protected ArrayList<XY> getValues(ArrayList<Double> values) {
        final ArrayList<XY> xys = new ArrayList<XY>();
        for (int i = 0; i < values.size(); i ++) {
            Double value = values.get(i);

            XY point = new XY(
                i,
                value.floatValue()
            );
            xys.add(point);
        }

        return xys;
    }
}
