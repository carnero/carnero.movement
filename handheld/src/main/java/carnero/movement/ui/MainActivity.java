package carnero.movement.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.db.Helper;
import carnero.movement.db.Model;
import carnero.movement.service.LocationService;

public class MainActivity extends AbstractBaseActivity {

    private Helper mHelper;
    private Line mLineSteps;
    //
    @InjectView(R.id.graph)
    LineGraph vGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHelper = new Helper(this);

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // Graph
        mLineSteps = new Line();
        mLineSteps.setColor(getResources().getColor(R.color.graph_steps));
        mLineSteps.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.line_stroke));

        vGraph.addLine(mLineSteps);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new DataLoadTask().start();
    }

    // Classes

    private class DataLoadTask extends BaseAsyncTask {

        private int mMaxSteps = Integer.MIN_VALUE;
        private int mMinSteps = Integer.MAX_VALUE;

        @Override
        public void inBackground() {
            ArrayList<Model> data = mHelper.getData();
            if (data != null) {
                mLineSteps.getPoints().clear();

                for (int i = 0; i < data.size(); i++) {
                    Model model = data.get(i);

                    // Steps
                    LinePoint pointSteps = new LinePoint();
                    pointSteps.setX(i);
                    pointSteps.setY(model.steps);

                    if (model.steps > mMaxSteps) {
                        mMaxSteps = model.steps;
                    }
                    if (model.steps < mMinSteps) {
                        mMinSteps = model.steps;
                    }

                    mLineSteps.addPoint(pointSteps);
                }
            }
        }

        @Override
        public void postExecute() {
            vGraph.setRangeY(mMinSteps, mMaxSteps);
            vGraph.setOnPointClickedListener(new LineGraph.OnPointClickedListener() {
                @Override
                public void onClick(int lineIndex, int pointIndex) {
                    final LinePoint point = vGraph.getLine(lineIndex).getPoint(pointIndex);

                    Toast.makeText(MainActivity.this, "Steps: " + (int) point.getY(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
