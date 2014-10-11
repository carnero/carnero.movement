package carnero.movement.ui;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.ImageLoaderSingleton;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.common.graph.SplineGraph;
import carnero.movement.common.graph.SplinePath;
import carnero.movement.common.model.Movement;
import carnero.movement.common.model.MovementEnum;
import carnero.movement.common.model.XY;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.db.Helper;
import carnero.movement.graph.DistancePath;
import carnero.movement.graph.StepsPath;
import carnero.movement.model.Checkin;
import carnero.movement.model.Location;
import carnero.movement.model.MovementContainer;
import carnero.movement.model.MovementData;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class GraphFragment extends Fragment {

    private Helper mHelper;
    private Preferences mPreferences;
    private LayoutInflater mInflater;
    private long mMidnight = 0;
    private final ArrayList<XY> mPointsDistance = new ArrayList<XY>();
    private final ArrayList<XY> mPointsSteps = new ArrayList<XY>();
    private final SplinePath mPathDistance = new DistancePath();
    private final SplinePath mPathSteps = new StepsPath();
    private final ArrayList<SplinePath> mPaths = new ArrayList<SplinePath>();
    private final ArrayList<Checkin> mCheckins = new ArrayList<Checkin>();
    private String mLabel;
    private String mSubLabel;
    //
    private float mMapStrokeWidth;
    private int mMapColorStart;
    private int mMapColorEnd;
    //
    private static final String ARGS_DAY = "day";
    //
    @InjectView(R.id.stats_steps)
    TextView vStatsSteps;
    @InjectView(R.id.stats_distance)
    TextView vStatsDistance;
    @InjectView(R.id.graph)
    SplineGraph vGraph;
    @InjectView(R.id.checkins_container)
    FrameLayout vCheckins;
    @InjectView(R.id.separator)
    View vSeparator;
    @InjectView(R.id.map)
    MapView vMap;
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
        mInflater = LayoutInflater.from(getActivity());

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, getDay());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        mMidnight = calendar.getTimeInMillis();

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

        App.getTracker().send(new HitBuilders.AppViewBuilder().build());
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
        mPaths.add(mPathSteps);
        mPaths.add(mPathDistance);
    }

    private int getDay() {
        return getArguments().getInt(ARGS_DAY, 0);
    }

    public String getLabel() {
        return mLabel;
    }

    public String getSubLabel() {
        return mSubLabel;
    }

    // Classes

    private class DataTask extends BaseAsyncTask {

        private MovementContainer mContainer;
        private int mWalk = 0; // seconds
        private int mRun = 0; // seconds
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

                    stepsPrev = model.steps;
                    distancePrev = model.distance;
                }

                if (steps < 0) {
                    steps = 0;
                }
                if (distance < 0) {
                    distance = 0;
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
                long walk = 0;
                long run = 0;
                for (Movement movement : movements) {
                    if (movement.type == MovementEnum.WALK) {
                        walk += movement.end - movement.start;
                    } else if (movement.type == MovementEnum.RUN) {
                        run += movement.end - movement.start;
                    }
                }

                mWalk = (int) (walk / 1e9);
                mRun = (int) (run / 1e9);
            }

            // Pre-generate map polylines
            if (mContainer.locations != null && !mContainer.locations.isEmpty()) {
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
                            (int)(colorStartB + (colorRStep * (model.time - mMidnight))),
                            (int)(colorStartG + (colorGStep * (model.time - mMidnight))),
                            (int)(colorStartB + (colorBStep * (model.time - mMidnight)))
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
                mLabel = getString(R.string.today);
                mSubLabel = null;
            } else if (getDay() == -1) {
                mLabel = getString(R.string.yesterday);
                mSubLabel = null;
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, getDay());

                DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
                DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
                String[] days = symbols.getWeekdays();

                mLabel = days[calendar.get(Calendar.DAY_OF_WEEK)];
                mSubLabel = format.format(calendar.getTime());
            }

            MainActivity activity = (MainActivity) getActivity();
            activity.setLabel(getDay(), mLabel, mSubLabel);

            // No data
            if (mContainer == null || mContainer.locations == null || mContainer.locations.size() < 2) {
                vStatsSteps.setVisibility(View.GONE);
                vStatsDistance.setVisibility(View.GONE);
                vGraph.setVisibility(View.GONE);
                vCheckins.setVisibility(View.GONE);
                vSeparator.setVisibility(View.GONE);
                vMap.setVisibility(View.GONE);

                vNoData.setVisibility(View.VISIBLE);

                if (isAdded()) {
                    vProgressBar.setVisibility(View.INVISIBLE);
                }
                return;
            }

            float distanceDay = mLabelDistanceMax - mLabelDistanceMin;
            int stepsDay = mLabelStepsMax - mLabelStepsMin;

            vNoData.setVisibility(View.GONE);
            vStatsSteps.setText(getString(R.string.stats_steps, stepsDay));
            vStatsDistance.setText(Utils.formatDistance(distanceDay));

            // Graph
            vGraph.setData(mPaths);

            // Checkins
            int containerWidth = vCheckins.getWidth();
            int containerHeight = vCheckins.getHeight();
            int checkinWidth = getResources().getDimensionPixelSize(R.dimen.swarm_icon_width);
            int checkinHeight = getResources().getDimensionPixelSize(R.dimen.swarm_icon_height);
            int checkinMarginBottom = getResources().getDimensionPixelSize(R.dimen.swarm_icon_margin_bottom);
            double widthMilli = containerWidth / (double)(24 * 60 * 60 * 1000);

            vCheckins.setVisibility(View.GONE);
            vCheckins.removeAllViewsInLayout();
            if (containerWidth > 0) {
                for (Checkin checkin : mCheckins) {
                    final View layout = mInflater.inflate(R.layout.item_swarm, vCheckins, false);
                    ImageView icon = (ImageView) layout.findViewById(R.id.icon);

                    int marginLeft = (int)Math.round((checkin.createdAt - mMidnight) * widthMilli);
                    int marginBottom = containerHeight - mPathDistance.getPixelValue(marginLeft);

                    if (marginLeft < 0) {
                        marginLeft = 0;
                    } else if (marginLeft > containerWidth) {
                        marginLeft = containerWidth;
                    }
                    if (marginBottom < 0) {
                        marginBottom = 0;
                    } else if (marginBottom > containerHeight - checkinHeight) {
                        marginBottom = containerHeight - checkinHeight;
                    }

                    if (mPathDistance.isIncreasing(marginLeft)) {
                        marginLeft = marginLeft - checkinWidth;
                        layout.setBackgroundResource(R.drawable.bg_swarm_left);
                    } else {
                        layout.setBackgroundResource(R.drawable.bg_swarm_right);
                    }

                    LayoutParams params = new LayoutParams(
                        checkinWidth,
                        checkinHeight
                    );
                    params.leftMargin = marginLeft;
                    params.topMargin = containerHeight - (marginBottom + checkinMarginBottom) - checkinHeight;

                    vCheckins.addView(layout, params);

                    ImageLoaderSingleton.getInstance()
                        .displayImage(checkin.iconPrefix + Checkin.sizes[3] + checkin.iconSuffix, icon);
                }
            }
            vCheckins.bringToFront();
            vCheckins.setVisibility(View.VISIBLE);

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
                vProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }
}
