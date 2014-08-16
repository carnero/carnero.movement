package carnero.movement.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.SmoothLineGraph;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;

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
        mLineDistance = new Line();
        mLineDistance.setShowingPoints(false);
        mLineDistance.setColor(getResources().getColor(R.color.graph_distance));
        mLineDistance.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.line_stroke));
        vGraph.addLine(mLineDistance);

        mLineSteps = new Line();
        mLineSteps.setShowingPoints(false);
        mLineSteps.setColor(getResources().getColor(R.color.graph_steps));
        mLineSteps.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.line_stroke));
        vGraph.addLine(mLineSteps);

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

        private double mMaxStp = Double.MIN_VALUE;
        private double mMinStp = Double.MAX_VALUE;
        private double mMaxDst = Double.MIN_VALUE;
        private double mMinDst = Double.MAX_VALUE;

        @Override
        public void inBackground() {
            ModelData[] data = mHelper.getDataToday();
            if (data != null) {
                mLineSteps.getPoints().clear();
                mLineDistance.getPoints().clear();

                float stepsPrev = -1f;
                float distancePrev = -1f;

                for (int i = 0; i < data.length; i++) {
                    ModelData model = data[i];

                    float steps;
                    float distance;
                    if (model == null) {
                        steps = 0;
                        distance = 0;
                    } else if (stepsPrev == -1f || distancePrev == -1f) {
                        stepsPrev = model.steps;
                        distancePrev = model.distance;

                        continue;
                    } else {
                        steps = model.steps - stepsPrev;
                        distance = model.distance - distancePrev;
                        stepsPrev = model.steps;
                        distancePrev = model.distance;
                    }

                    // Steps
                    double stepsLog = 0;
                    if (steps > 0) {
                        stepsLog = Math.log10(steps);
                    }

                    LinePoint pointSteps = new LinePoint();
                    pointSteps.setX(i);
                    pointSteps.setY(stepsLog);
                    mLineSteps.addPoint(pointSteps);

                    if (stepsLog > mMaxStp) {
                        mMaxStp = stepsLog;
                    }
                    if (stepsLog < mMinStp) {
                        mMinStp = stepsLog;
                    }

                    // Distance
                    double distanceLog = 0;
                    if (distance > 0) {
                        distanceLog = Math.log10(distance);
                    }

                    LinePoint pointDistance = new LinePoint();
                    pointDistance.setX(i);
                    pointDistance.setY(distanceLog);
                    mLineDistance.addPoint(pointDistance);

                    if (distanceLog > mMaxDst) {
                        mMaxDst = distanceLog;
                    }
                    if (distanceLog < mMinDst) {
                        mMinDst = distanceLog;
                    }
                }

                // Normalize data
                double ratio = 1.0f;
                int ratioLine = -1; // steps:0, distance:1
                if (mMaxStp > mMaxDst) {
                    ratio = mMaxStp / mMaxDst;
                    ratioLine = 0;
                } else if (mMaxStp < mMaxDst) {
                    ratio = mMaxDst / mMaxStp;
                    ratioLine = 1;
                }

                if (ratioLine == 0) {
                    for (LinePoint point : mLineSteps.getPoints()) {
                        point.setY(point.getY() / ratio);
                    }
                    mMinStp = mMinStp / ratio;
                    mMaxStp = mMaxStp / ratio;
                }
                if (ratioLine == 1) {
                    for (LinePoint point : mLineDistance.getPoints()) {
                        point.setY(point.getY() / ratio);
                    }
                    mMinDst = mMinDst / ratio;
                    mMaxDst = mMaxDst / ratio;
                }
            }
        }

        @Override
        public void postExecute() {
            // Graph
            vGraph.setRangeY(
                    (float) Math.min(mMinStp, mMinDst),
                    (float) Math.max(mMaxStp, mMaxDst)
            );
        }
    }
}
