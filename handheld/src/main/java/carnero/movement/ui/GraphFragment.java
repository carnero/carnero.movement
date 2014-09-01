package carnero.movement.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.common.graph.SplineGraph;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;
import carnero.movement.db.ModelDataContainer;
import carnero.movement.db.ModelLocation;
import carnero.movement.ui.view.XY;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

public class GraphFragment extends Fragment {

    private Helper mHelper;
    private Preferences mPreferences;
    private boolean mAbsolute = false;
    private final ArrayList<XY> mPointsDst = new ArrayList<XY>();
    //
    private static final String ARGS_DAY = "day";
    //
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.stats_steps)
    TextView vStatsSteps;
    @InjectView(R.id.stats_distance)
    TextView vStatsDistance;
    @InjectView(R.id.graph)
    SplineGraph vGraph;
    @InjectView(R.id.separator)
    View vSeparator;
    @InjectView(R.id.map)
    MapView vMap;
    @InjectView(R.id.visual_helper)
    View vVisualHelper;
    @InjectView(R.id.no_data)
    View vNoData;

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

        mHelper = new Helper(getActivity());
        mPreferences = new Preferences(getActivity());

        mAbsolute = mPreferences.getGraphType();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View layout = inflater.inflate(R.layout.fragment_graph, container, false);
        ButterKnife.inject(this, layout);

        MapsInitializer.initialize(getActivity());
        vMap.onCreate(state);

        initMap();
        initGraph();

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        vMap.onResume();

        new GraphDataTask().start();
    }

    @Override
    public void onPause() {
        vMap.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        vMap.onDestroy();
        super.onDestroyView();
    }

    public void changeGraph() {
        mAbsolute = !mAbsolute;

        mPreferences.setGraphType(mAbsolute);

        new GraphDataTask().start();
    }

    private void initMap() {
        final GoogleMap map = vMap.getMap();
        map.setMyLocationEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        final UiSettings ui = map.getUiSettings();
        ui.setMyLocationButtonEnabled(false);
        ui.setCompassEnabled(false);
        ui.setZoomControlsEnabled(false);
    }

    private void initGraph() {
        // Nothing yet
    }

    private int getDay() {
        return getArguments().getInt(ARGS_DAY, 0);
    }

    // Classes

    private class GraphDataTask extends BaseAsyncTask {

        private ModelDataContainer mContainer;
        //
        private double mMinDst = Double.MAX_VALUE;
        private double mMaxDst = Double.MIN_VALUE;
        private double mMinStp = Double.MAX_VALUE;
        private double mMaxStp = Double.MIN_VALUE;
        //
        private float mLabelDistanceMin = Float.MAX_VALUE;
        private float mLabelDistanceMax = Float.MIN_VALUE;
        private int mLabelStepsMin = Integer.MAX_VALUE;
        private int mLabelStepsMax = Integer.MIN_VALUE;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (isAdded()) {
                // TODO: show progress bar
            }
        }

        @Override
        public void inBackground() {
            mContainer = mHelper.getDataForDay(getDay());
            if (mContainer == null || mContainer.locations == null || mContainer.locations.isEmpty()) {
                return;
            }

            mPointsDst.clear();

            float stepsPrev = -1f;
            float distancePrev = -1f;

            if (mContainer.previousEntry != null) {
                stepsPrev = mContainer.previousEntry.steps;
                distancePrev = mContainer.previousEntry.distance;

                // Values for labels
                mLabelStepsMin = Math.min(mLabelStepsMin, mContainer.previousEntry.steps);
                mLabelStepsMax = Math.max(mLabelStepsMax, mContainer.previousEntry.steps);
                mLabelDistanceMin = Math.min(mLabelDistanceMin, mContainer.previousEntry.distance);
                mLabelDistanceMax = Math.max(mLabelDistanceMax, mContainer.previousEntry.distance);
            }

            for (int i = 0; i < mContainer.movements.length; i++) {
                ModelData model = mContainer.movements[i];

                // Values for labels
                if (model != null) {
                    mLabelStepsMin = Math.min(mLabelStepsMin, model.steps);
                    mLabelStepsMax = Math.max(mLabelStepsMax, model.steps);
                    mLabelDistanceMin = Math.min(mLabelDistanceMin, model.distance);
                    mLabelDistanceMax = Math.max(mLabelDistanceMax, model.distance);
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
                    if (!mAbsolute) {
                        stepsPrev = model.steps;
                        distancePrev = model.distance;
                    }
                }

                // Min/max values
                mMinStp = Math.min(mMinStp, steps);
                mMaxStp = Math.max(mMaxStp, steps);
                mMinDst = Math.min(mMinDst, distance);
                mMaxDst = Math.max(mMaxDst, distance);

                // Distance points
                XY point = new XY();
                point.x = i;
                point.y = distance;
                mPointsDst.add(point);

                // TODO: Step points
            }
        }

        @Override
        public void postExecute() {
            if (!isAdded()) {
                return;
            }

            // Date
            if (getDay() == 0) {
                vLabel.setText(R.string.today);
            } else if (getDay() == -1) {
                vLabel.setText(R.string.yesterday);
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, getDay());

                DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
                String date = format.format(calendar.getTime());

                vLabel.setText(date);
            }

            // No data
            if (mContainer == null || mContainer.locations == null || mContainer.locations.size() < 2) {
                vStatsSteps.setText(getString(R.string.stats_steps, 0));
                vStatsDistance.setText(Utils.formatDistance(0));
                vGraph.setVisibility(View.GONE);
                vVisualHelper.setVisibility(View.GONE);
                vSeparator.setVisibility(View.GONE);
                vMap.setVisibility(View.GONE);

                vNoData.setVisibility(View.VISIBLE);

                if (isAdded()) {
                    getActivity().setProgressBarIndeterminateVisibility(false);
                }
                return;
            }

            // Stats
            float distanceDay = mLabelDistanceMax - mLabelDistanceMin;
            int stepsDay = mLabelStepsMax - mLabelStepsMin;

            vNoData.setVisibility(View.GONE);
            vStatsSteps.setText(getString(R.string.stats_steps, stepsDay));
            vStatsDistance.setText(Utils.formatDistance(distanceDay));

            // Graph
            vGraph.setData(mPointsDst);

            // Locations
            final GoogleMap map = vMap.getMap();
            map.clear();

            if (mContainer.locations != null && !mContainer.locations.isEmpty()) {
                final int color = getResources().getColor(R.color.map_history);
                final PolylineOptions polylineOpts = new PolylineOptions();
                polylineOpts.zIndex(1010);
                polylineOpts.width(getResources().getDimension(R.dimen.map_line_stroke));
                polylineOpts.color(color);

                double[] latBounds = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
                double[] lonBounds = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};

                for (ModelLocation model : mContainer.locations) {
                    latBounds[0] = Math.min(latBounds[0], model.latitude);
                    latBounds[1] = Math.max(latBounds[1], model.latitude);
                    lonBounds[0] = Math.min(lonBounds[0], model.longitude);
                    lonBounds[1] = Math.max(lonBounds[1], model.longitude);

                    LatLng latLng = new LatLng(model.latitude, model.longitude);
                    polylineOpts.add(latLng);

                    map.addPolyline(polylineOpts);
                }

                LatLng ne = new LatLng(latBounds[0], lonBounds[0]);
                LatLng sw = new LatLng(latBounds[1], lonBounds[1]);
                LatLngBounds bounds = new LatLngBounds(ne, sw);

                map.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        bounds,
                        getResources().getDimensionPixelSize(R.dimen.margin_map)
                    )
                );
                if (map.getCameraPosition().zoom > 14) {
                    map.animateCamera(CameraUpdateFactory.zoomTo(14));
                }

                vSeparator.setVisibility(View.VISIBLE);
                vMap.setVisibility(View.VISIBLE);
            } else {
                vSeparator.setVisibility(View.GONE);
                vMap.setVisibility(View.GONE);
            }

            // Progress bar
            if (isAdded()) {
                // TODO: hide progressbar
            }
        }
    }
}
