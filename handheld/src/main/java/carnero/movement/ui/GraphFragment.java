package carnero.movement.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.SmoothLineGraph;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;

public class GraphFragment extends Fragment {

    private Preferences mPreferences;
    private Helper mHelper;
    private Line mLineSteps;
    private Line mLineDistance;
    //
    @InjectView(R.id.graph)
    SmoothLineGraph vGraph;
    @InjectView(R.id.stats_steps)
    TextView vStatsSteps;
    @InjectView(R.id.stats_distance)
    TextView vStatsDistance;

    public static GraphFragment newInstance() {
        return new GraphFragment();
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        mPreferences = new Preferences(getActivity());
        mHelper = new Helper(getActivity());
    }

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
        //
        private int mLabelStepsMin = Integer.MAX_VALUE;
        private int mLabelStepsMax = Integer.MIN_VALUE;
        private float mLabelDistanceMin = Float.MAX_VALUE;
        private float mLabelDistanceMax = Float.MIN_VALUE;

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

                    // Values for labels
                    if (model != null) {
                        if (model.steps > mLabelStepsMax) {
                            mLabelStepsMax = model.steps;
                        }
                        if (model.steps < mLabelStepsMin) {
                            mLabelStepsMin = model.steps;
                        }
                        if (model.distance > mLabelDistanceMax) {
                            mLabelDistanceMax = model.distance;
                        }
                        if (model.distance < mLabelDistanceMin) {
                            mLabelDistanceMin = model.distance;
                        }
                    }

                    // Graph
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

                    // Distance
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
            // Statistics
            StringBuilder stB = new StringBuilder();
            stB.append("• today ");
            stB.append(mLabelStepsMax - mLabelStepsMin);
            stB.append(" steps, total ");
            stB.append(mPreferences.getSteps());
            stB.append(" steps");
            SpannableString stS = new SpannableString(stB.toString());
            stS.setSpan(
                    new ForegroundColorSpan(getResources().getColor(R.color.graph_steps)),
                    0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            vStatsSteps.setText(stS);

            StringBuilder dsB = new StringBuilder();
            dsB.append("• today ");
            dsB.append(Utils.formatDistance(mLabelDistanceMax - mLabelDistanceMin));
            dsB.append(", total ");
            dsB.append(Utils.formatDistance(mPreferences.getDistance()));
            SpannableString dsS = new SpannableString(dsB.toString());
            dsS.setSpan(
                    new ForegroundColorSpan(getResources().getColor(R.color.graph_distance)),
                    0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            vStatsDistance.setText(dsS);

            // Graph
            vGraph.setRangeY(
                    (float) Math.min(mMinStp, mMinDst),
                    (float) Math.max(mMaxStp, mMaxDst)
            );
        }
    }
}
