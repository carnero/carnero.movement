package carnero.movement.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
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
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;

public class GraphFragment extends Fragment {

    private Helper mHelper;
    private Line mLineSteps;
    private Line mLineDistance;
    //
    @InjectView(R.id.graph)
    SmoothLineGraph vGraph;
    @InjectView(R.id.label_top)
    TextView vTop;

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

        private float mMaxStp = Float.MIN_VALUE;
        private float mMinStp = Float.MAX_VALUE;
        private float mMaxDst = Float.MIN_VALUE;
        private float mMinDst = Float.MAX_VALUE;
        private float mMaxStpLabel;
        private float mMaxDstLabel;

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

                // Save for labels
                mMaxStpLabel = mMaxStp;
                mMaxDstLabel = mMaxDst;

                // Normalize data
                float ratio = 1.0f;
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
                    Math.min(mMinStp, mMinDst),
                    Math.max(mMaxStp, mMaxDst)
            );

            // Format top label
            StringBuilder topBuilder = new StringBuilder();
            topBuilder.append(Integer.toString((int) mMaxStpLabel));
            topBuilder.append(" st");
            int topDstLen = topBuilder.length();
            topBuilder.append("\n");
            if (mMaxDstLabel > 1600) {
                topBuilder.append(String.format("%.1f", mMaxDstLabel / 1000));
                topBuilder.append(" km");
            } else {
                topBuilder.append(String.format("%.1f", mMaxDstLabel));
                topBuilder.append(" m");
            }

            SpannableString top = new SpannableString(topBuilder.toString());
            top.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.graph_steps)), 0, topDstLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            top.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.graph_distance)), topDstLen, topBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            vTop.setText(top);
        }
    }
}
