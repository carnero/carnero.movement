package carnero.movement.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class StepsGraphFragment extends Fragment {

    private Helper mHelper;
    private Line mLineSteps;
    //
    @InjectView(R.id.graph)
    LineGraph vGraph;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View layout = inflater.inflate(R.layout.fragment_graph, container, false);
        ButterKnife.inject(this, layout);

        // Graph
        mLineSteps = new Line();
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
            vGraph.setLineToFill(0);
        }
    }
}
