package carnero.movement.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import carnero.movement.common.graph.ActivitiesGraph;
import carnero.movement.common.model.Movement;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.graph.DistancePath;
import carnero.movement.common.graph.SplineGraph;
import carnero.movement.common.graph.SplinePath;
import carnero.movement.graph.StepsPath;
import carnero.movement.db.Helper;
import carnero.movement.model.Checkin;
import carnero.movement.model.Location;
import carnero.movement.model.MovementContainer;
import carnero.movement.model.MovementData;
import carnero.movement.common.model.XY;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class GraphFragment extends Fragment {

    private Helper mHelper;
    private Preferences mPreferences;
    private boolean mAbsolute = false;
    private final ArrayList<XY> mPointsDistance = new ArrayList<XY>();
    private final ArrayList<XY> mPointsSteps = new ArrayList<XY>();
    private final SplinePath mPathDistance = new DistancePath();
    private final SplinePath mPathSteps = new StepsPath();
    private final ArrayList<SplinePath> mPaths = new ArrayList<SplinePath>();
    private final ArrayList<Checkin> mCheckins = new ArrayList<Checkin>();
    private final ArrayList<Movement> mMovements = new ArrayList<Movement>();
    //
    private float mMapStrokeWidth;
    private int mMapColorStart;
    private int mMapColorEnd;
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
    @InjectView(R.id.activities)
    ActivitiesGraph vActivities;
    @InjectView(R.id.separator)
    View vSeparator;
    @InjectView(R.id.map)
    MapView vMap;
    @InjectView(R.id.visual_helper)
    View vVisualHelper;
    @InjectView(R.id.no_data)
    View vNoData;
    @InjectView(R.id.progressbar)
    SmoothProgressBar vProgressBar;

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

        mHelper = Helper.getInstance();
        mPreferences = new Preferences();

        mAbsolute = mPreferences.getGraphType();

        mMapStrokeWidth = getResources().getDimension(R.dimen.map_line_stroke);
        mMapColorStart = getResources().getColor(R.color.map_history_start);
        mMapColorEnd = getResources().getColor(R.color.map_history_end);
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

        new DataTask().start();
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

        new DataTask().start();
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
        mPaths.clear();
        mPaths.add(mPathSteps); // steps are below
        mPaths.add(mPathDistance);
    }

    private int getDay() {
        return getArguments().getInt(ARGS_DAY, 0);
    }

    // Classes

    private class DataTask extends BaseAsyncTask {

        private MovementContainer mContainer;
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
        //
        final private ArrayList<PolylineOptions> mPolylines = new ArrayList<PolylineOptions>();
        final private ArrayList<MarkerOptions> mMarkers = new ArrayList<MarkerOptions>();
        private LatLngBounds mBounds;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (isAdded()) {
                vProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void inBackground() {
            mContainer = mHelper.getDataForDay(getDay());
            if (mContainer == null || mContainer.locations == null || mContainer.locations.isEmpty()) {
                return;
            }

            mPointsDistance.clear();
            mPointsSteps.clear();

            float distancePrev = 0f;
            float stepsPrev = 0f;

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
                MovementData model = mContainer.movements[i];

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

                XY point;

                // Distance points
                point = new XY();
                point.x = i;
                point.y = distance;
                mPointsDistance.add(point);

                // Steps points
                point = new XY();
                point.x = i;
                point.y = steps;
                mPointsSteps.add(point);
            }

            mPathDistance.setData(mPointsDistance);
            mPathSteps.setData(mPointsSteps);

            // Movements
            ArrayList<Movement> movements = mHelper.getMovementsForDay(getDay());
            if (movements != null) {
                synchronized (mMovements) {
                    mMovements.clear();
                    mMovements.addAll(movements);
                }
            }

            // Pre-generate map polylines
            if (mContainer.locations != null && !mContainer.locations.isEmpty()) {
                final Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, getDay());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                final long midnight = calendar.getTimeInMillis();

                final int colorStartR = Color.red(mMapColorStart);
                final int colorStartG = Color.green(mMapColorStart);
                final int colorStartB = Color.blue(mMapColorStart);

                final double colorRStep = (Color.red(mMapColorEnd) - colorStartR) / (double)DateUtils.DAY_IN_MILLIS;
                final double colorGStep = (Color.green(mMapColorEnd) - colorStartG) / (double)DateUtils.DAY_IN_MILLIS;
                final double colorBStep = (Color.blue(mMapColorEnd) - colorStartB) / (double)DateUtils.DAY_IN_MILLIS;

                double[] latBounds = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
                double[] lonBounds = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};

                LatLng latLngPrev = null;
                for (Location model : mContainer.locations) {
                    latBounds[0] = Math.min(latBounds[0], model.latitude);
                    latBounds[1] = Math.max(latBounds[1], model.latitude);
                    lonBounds[0] = Math.min(lonBounds[0], model.longitude);
                    lonBounds[1] = Math.max(lonBounds[1], model.longitude);

                    LatLng latLng = new LatLng(model.latitude, model.longitude);

                    if (latLngPrev != null) {
                        int color = Color.argb(
                            255,
                            (int)(colorStartB + (colorRStep * (model.time - midnight))),
                            (int)(colorStartG + (colorGStep * (model.time - midnight))),
                            (int)(colorStartB + (colorBStep * (model.time - midnight)))
                        );

                        final PolylineOptions polylineOpts = new PolylineOptions();
                        polylineOpts.zIndex(1010);
                        polylineOpts.width(mMapStrokeWidth);
                        polylineOpts.color(color);
                        polylineOpts.geodesic(true);

                        polylineOpts.add(latLngPrev);
                        polylineOpts.add(latLng);

                        mPolylines.add(polylineOpts);
                    }

                    latLngPrev = latLng;
                }

                LatLng ne = new LatLng(latBounds[0], lonBounds[0]);
                LatLng sw = new LatLng(latBounds[1], lonBounds[1]);
                mBounds = new LatLngBounds(ne, sw);
            }

            // Checkins
            ArrayList<Checkin> checkins = mHelper.getCheckinsForDay(getDay());
            if (checkins != null) {
                synchronized (mCheckins) {
                    mCheckins.clear();
                    mCheckins.addAll(checkins);
                }
            }

            // Checkin markers
            final BitmapDescriptor pin = BitmapDescriptorFactory.fromResource(R.drawable.ic_checkin);

            for (Checkin checkin : mCheckins) {
                String title;
                if (TextUtils.isEmpty(checkin.shout)) {
                    title = checkin.name;
                } else {
                    title = "\"" + checkin.shout + "\" @ " + checkin.name;
                }

                final MarkerOptions markerOpts = new MarkerOptions();
                markerOpts.position(new LatLng(checkin.latitude, checkin.longitude));
                markerOpts.title(title);
                markerOpts.icon(pin);
                markerOpts.anchor(0.5f, 0.5f);

                mMarkers.add(markerOpts);
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
                vStatsSteps.setVisibility(View.GONE);
                vStatsDistance.setVisibility(View.GONE);
                vGraph.setVisibility(View.GONE);
                vVisualHelper.setVisibility(View.GONE);
                vSeparator.setVisibility(View.GONE);
                vMap.setVisibility(View.GONE);

                vNoData.setVisibility(View.VISIBLE);

                if (isAdded()) {
                    vProgressBar.setVisibility(View.GONE);
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
            vGraph.setData(mPaths);

            // Movements
            vActivities.setData(getDay(), mMovements);

            // Locations
            final GoogleMap map = vMap.getMap();
            map.clear();

            if (!mPolylines.isEmpty() && mBounds != null) {
                for (PolylineOptions polylineOptions : mPolylines) {
                    map.addPolyline(polylineOptions);
                }

                map.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        mBounds,
                        getResources().getDimensionPixelSize(R.dimen.margin_map)
                    )
                );

                if (map.getCameraPosition().zoom > 14) {
                    map.moveCamera(CameraUpdateFactory.zoomTo(14));
                }

                vSeparator.setVisibility(View.VISIBLE);
                vMap.setVisibility(View.VISIBLE);
            } else {
                vSeparator.setVisibility(View.GONE);
                vMap.setVisibility(View.GONE);
            }

            // Checkins
            if (!mMarkers.isEmpty()) {
                for (MarkerOptions markerOptions : mMarkers) {
                    map.addMarker(markerOptions);
                }
            }

            // Progress bar
            if (isAdded()) {
                vProgressBar.setVisibility(View.GONE);
            }
        }
    }
}
