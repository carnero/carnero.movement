package carnero.movement.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.SmoothLineGraph;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.db.Helper;
import carnero.movement.db.Model;

public class GraphFragment extends Fragment {

    private Helper mHelper;
    private Line mLineSteps;
    private Line mLineDistance;
    //
    @InjectView(R.id.graph)
    SmoothLineGraph vGraph;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View layout = inflater.inflate(R.layout.fragment_graph, container, false);
        ButterKnife.inject(this, layout);

        // Graph
        mLineSteps = new Line();
        mLineSteps.setShowingPoints(false);
        mLineSteps.setColor(getResources().getColor(R.color.graph_steps));
        mLineSteps.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.line_stroke));
        vGraph.addLine(mLineSteps);

        mLineDistance = new Line();
        mLineDistance.setShowingPoints(false);
        mLineDistance.setColor(getResources().getColor(R.color.graph_distance));
        mLineDistance.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.line_stroke));
        vGraph.addLine(mLineDistance);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        mHelper = new Helper(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        new DataLoadTask().start();
    }

    // Classes

    private class DataLoadTask extends BaseAsyncTask {

        private float mMaxStp = Float.MIN_VALUE;
        private float mMinStp = Float.MAX_VALUE;
        private float mMaxDst = Float.MIN_VALUE;
        private float mMinDst = Float.MAX_VALUE;

        @Override
        public void inBackground() {
            ArrayList<Model> data = mHelper.getLastWeek();
            if (data != null) {
                mLineSteps.getPoints().clear();

                float stepsPrev = 0f;
                float distancePrev = 0f;

                for (int i = 0; i < data.size(); i++) {
                    Model model = data.get(i);

                    // Steps
                    float steps = model.steps - stepsPrev;

                    LinePoint pointSteps = new LinePoint();
                    pointSteps.setX(i);
                    pointSteps.setY(steps);
                    mLineSteps.addPoint(pointSteps);

                    if (steps > mMaxStp) {
                        mMaxStp = steps;
                    }
                    if (steps < mMinStp) {
                        mMinStp = steps;
                    }
                    stepsPrev = model.steps;

                    // Distance
                    float distance = model.distance - distancePrev;

                    LinePoint pointDistance = new LinePoint();
                    pointDistance.setX(i);
                    pointDistance.setY(distance);
                    mLineDistance.addPoint(pointDistance);

                    if (distance > mMaxDst) {
                        mMaxDst = distance;
                    }
                    if (distance < mMinDst) {
                        mMinDst = distance;
                    }
                    distancePrev = model.distance;
                }

                // Normalize data
            }
        }

        @Override
        public void postExecute() {
            vGraph.setRangeY(
                    Math.min(mMinStp, mMinDst),
                    Math.max(mMaxStp, mMaxDst)
            );
        }
    }
}
