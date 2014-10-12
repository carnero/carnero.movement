package carnero.movement.ui;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.FrameLayout.LayoutParams;

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
    private static final String ARGS_DAY = "day";
    //
    @InjectView(R.id.stats_container)
    LinearLayout vStatsContainer;
    @InjectView(R.id.stats_steps)
    TextView vStatsSteps;
    @InjectView(R.id.stats_distance)
    TextView vStatsDistance;
    @InjectView(R.id.graph)
    SplineGraph vGraph;
    @InjectView(R.id.checkins_container)
    FrameLayout vCheckinsContainer;
    @InjectView(R.id.no_data)
    View vNoData;
    @InjectView(R.id.progressbar)
    SmoothProgressBar vProgressBar;
    //
    @InjectView(R.id.detailed_container)
    RelativeLayout vDetailedContainer;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View layout = inflater.inflate(R.layout.fragment_graph, container, false);
        ButterKnife.inject(this, layout);

        initGraph();
        initViews();

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        new DataTask().start();

        App.getTracker().send(new HitBuilders.AppViewBuilder().build());
    }

    private void initGraph() {
        mPaths.clear();
        mPaths.add(mPathSteps);
        mPaths.add(mPathDistance);
    }

    private void initViews() {
        vStatsContainer.setClickable(true);
        vStatsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int finalRadius = vDetailedContainer.getHeight();
                int cx = vStatsContainer.getLeft() + (vStatsContainer.getWidth() / 2);
                int cy = vStatsContainer.getTop() + (vStatsContainer.getHeight() / 2);

                ValueAnimator anim = ViewAnimationUtils
                    .createCircularReveal(vDetailedContainer, cx, cy, 0, finalRadius);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);

                        vDetailedContainer.setVisibility(View.VISIBLE);
                    }
                });
                anim.start();
            }
        });
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

            // Checkins
            ArrayList<Checkin> checkins = mHelper.getCheckinsForDay(getDay());
            if (checkins != null) {
                synchronized (mCheckins) {
                    mCheckins.clear();
                    mCheckins.addAll(checkins);
                }
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
                vCheckinsContainer.setVisibility(View.GONE);

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
            int containerWidth = vCheckinsContainer.getWidth();
            int containerHeight = vCheckinsContainer.getHeight();
            int checkinWidth = getResources().getDimensionPixelSize(R.dimen.swarm_icon_width);
            int checkinHeight = getResources().getDimensionPixelSize(R.dimen.swarm_icon_height);
            int checkinMarginBottom = getResources().getDimensionPixelSize(R.dimen.swarm_icon_margin_bottom);
            double widthMilli = containerWidth / (double)(24 * 60 * 60 * 1000);

            vCheckinsContainer.setVisibility(View.GONE);
            vCheckinsContainer.removeAllViewsInLayout();
            if (containerWidth > 0) {
                for (Checkin checkin : mCheckins) {
                    final View layout = mInflater.inflate(R.layout.item_swarm, vCheckinsContainer, false);
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

                    vCheckinsContainer.addView(layout, params);

                    ImageLoaderSingleton.getInstance()
                        .displayImage(checkin.iconPrefix + Checkin.sizes[3] + checkin.iconSuffix, icon);
                }
            }
            vCheckinsContainer.bringToFront();
            vCheckinsContainer.setVisibility(View.VISIBLE);

            // Progress bar
            if (isAdded()) {
                vProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }
}
