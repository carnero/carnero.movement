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
    @InjectView(R.id.label_top)
    TextView vTop;
    @InjectView(R.id.label_bottom)
    TextView vBottom;

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
        private float mMaxStpLabel;
        private float mMinStpLabel;
        private float mMaxDstLabel;
        private float mMinDstLabel;

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

                // Save for labels
                mMinStpLabel = mMinStp;
                mMaxStpLabel = mMaxStp;
                mMinDstLabel = mMinDst;
                mMaxDstLabel = mMaxDst;

                // Normalize data
                float shift = 0.0f;
                int shiftLine = -1; // steps:0, distance: 1
                if (mMinStp > mMinDst) {
                    shift = mMinStp - mMinDst;
                    shiftLine = 0;
                } else if (mMinStp < mMinDst) {
                    shift = mMinDst - mMinStp;
                    shiftLine = 1;
                }

                float ratio = 1.0f;
                int ratioLine = -1; // steps:0, distance:1
                if (mMaxStp > mMaxDst) {
                    ratio = mMaxStp / mMaxDst;
                    ratioLine = 0;
                } else if (mMaxStp < mMaxDst) {
                    ratio = mMaxDst / mMaxStp;
                    ratioLine = 1;
                }

                if (shiftLine == 0 || ratioLine == 0) {
                    for (LinePoint point : mLineSteps.getPoints()) {
                        point.setY((point.getY() / ratio) - shift);
                    }
                    mMinStp = (mMinStp / ratio) - shift;
                    mMaxStp = (mMaxStp / ratio) - shift;
                }
                if (shiftLine == 1 || ratioLine == 1) {
                    for (LinePoint point : mLineDistance.getPoints()) {
                        point.setY((point.getY() / ratio) - shift);
                    }
                    mMinDst = (mMinDst / ratio) - shift;
                    mMaxDst = (mMaxDst / ratio) - shift;
                }
            }
        }

        @Override
        public void postExecute() {
            vGraph.setRangeY(
                    Math.min(mMinStp, mMinDst),
                    Math.max(mMaxStp, mMaxDst)
            );

            // Format top label
            StringBuilder topBuilder = new StringBuilder();
            topBuilder.append(Integer.toString((int) mMaxStp));
            topBuilder.append(" st");
            int topDstLen = topBuilder.length();
            topBuilder.append("\n");
            if (mMaxDst > 1600) {
                topBuilder.append(String.format("%.1f", mMaxDst / 1000));
                topBuilder.append(" km");
            } else {
                topBuilder.append(String.format("%.1f", mMaxDst));
                topBuilder.append(" m");
            }

            SpannableString top = new SpannableString(topBuilder.toString());
            top.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.graph_steps)), 0, topDstLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            top.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.graph_distance)), topDstLen, topBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Format top label
            StringBuilder bottomBuilder = new StringBuilder();
            bottomBuilder.append(Integer.toString((int) mMinStp));
            bottomBuilder.append(" st");
            int bottomDstLen = bottomBuilder.length();
            bottomBuilder.append("\n");
            if (mMinDst > 1600) {
                bottomBuilder.append(String.format("%.1f", mMinDst / 1000));
                bottomBuilder.append(" km");
            } else {
                bottomBuilder.append(String.format("%.1f", mMinDst));
                bottomBuilder.append(" m");
            }

            SpannableString bottom = new SpannableString(bottomBuilder.toString());
            bottom.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.graph_steps)), 0, bottomDstLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            bottom.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.graph_distance)), bottomDstLen, bottomBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            vTop.setText(top);
            vBottom.setText(bottom);
        }
    }
}
