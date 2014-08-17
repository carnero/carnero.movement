package carnero.movement.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;
import carnero.movement.db.ModelDataContainer;

public class GraphFragment extends Fragment {

    private Preferences mPreferences;
    private Helper mHelper;
    private Line mLineSteps;
    private Line mLineDistance;
    //
    private static final String ARGS_DAY = "day";
    //
    @InjectView(R.id.graph)
    SmoothLineGraph vGraph;
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.stats)
    TextView vStats;

    public static GraphFragment newInstance(int day) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARGS_DAY, day);

        final GraphFragment fragment = new GraphFragment();
        fragment.setArguments(arguments);

        return fragment;
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

    private int getDay() {
        return getArguments().getInt(ARGS_DAY, 0);
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
            ModelDataContainer container = mHelper.getDataForDay(getDay());
            if (container != null) {
                mLineSteps.getPoints().clear();
                mLineDistance.getPoints().clear();

                float stepsPrev = -1f;
                float distancePrev = -1f;

                if (container.start != null) {
                    stepsPrev = container.start.steps;
                    distancePrev = container.start.distance;
                }

                for (int i = 0; i < container.data.length; i++) {
                    ModelData model = container.data[i];

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
                if (mMaxStp > mMaxDst && mMaxDst > 100) {
                    ratio = mMaxStp / mMaxDst;
                    ratioLine = 0;
                } else if (mMaxStp < mMaxDst && mMaxStp > 100) {
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
            float distanceDay = mLabelDistanceMax - mLabelDistanceMin;
            int stepsDay = mLabelStepsMax - mLabelStepsMin;

            if (getDay() == 0) {
                vLabel.setText(R.string.today);
            } else if (getDay() == -1) {
                vLabel.setText(R.string.yesterday);
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, -1);

                DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
                String date = format.format(calendar.getTime());

                vLabel.setText(date);
            }
            vStats.setText(getString(R.string.stats, Utils.formatDistance(distanceDay), stepsDay));

            // Graph
            vGraph.setRangeY(
                    (float) Math.min(mMinStp, mMinDst),
                    (float) Math.max(mMaxStp, mMaxDst)
            );
        }
    }
}
