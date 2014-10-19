package carnero.movement.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.*;
import carnero.movement.common.model.Achvmnt;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.db.Helper;
import carnero.movement.model.Checkin;
import carnero.movement.model.Location;
import carnero.movement.model.MovementContainer;
import carnero.movement.service.FoursquareService;
import carnero.movement.service.LocationService;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.achievement.Achievements;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.example.games.basegameutils.BaseGameActivity;

public class MainActivity
    extends BaseGameActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Helper mMovementHelper;
    private Preferences mPreferences;
    private PagesAdapter mPagerAdapter;
    private boolean mHasFsqToken = true;
    private GoogleApiClient mGoogleApiClient;
    private MapDataTask mMapDataTask;
    private final HashMap<Long, Achvmnt> mAchievements = new HashMap<Long, Achvmnt>();
    //
    private float mMapStrokeWidth;
    private int mMapColorStart;
    private int mMapColorEnd;
    //
    private static final int HISTORY_PAGES = 31;
    private static final int REQUEST_FSQ_CONNECT = 1001;
    private static final int REQUEST_FSQ_EXCHANGE = 1002;
    private static final int REQUEST_ACHIEVEMENTS = 1003;
    //
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.sub_label)
    TextView vSubLabel;
    @InjectView(R.id.achievements)
    LinearLayout vAchievements;
    @InjectView(R.id.map)
    MapView vMap;
    @InjectView(R.id.pager)
    ViewPager vPager;

    @Override
    protected void onCreate(Bundle state) {
        // Play Services: Games
        setRequestedClients(BaseGameActivity.CLIENT_GAMES);

        super.onCreate(state);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0); // Otherwise it's raised above content
        }

        // Init
        mMovementHelper = Helper.getInstance();
        mPreferences = new Preferences();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Games.API)
            .addScope(Games.SCOPE_GAMES)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
        mGoogleApiClient.connect();

        // Load resources
        mMapStrokeWidth = getResources().getDimension(R.dimen.map_line_stroke);
        mMapColorStart = getResources().getColor(R.color.map_history_start);
        mMapColorEnd = getResources().getColor(R.color.map_history_end);

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // Map
        MapsInitializer.initialize(this);
        vMap.onCreate(state);
        initMap();

        // Set ViewPager
        mPagerAdapter = new PagesAdapter();
        vPager.setOffscreenPageLimit(3);
        vPager.setAdapter(mPagerAdapter);
        vPager.setCurrentItem(mPagerAdapter.getCount() - 1);
        vPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int i) {
                final GraphFragment fragment = mPagerAdapter.getFragment(i);
                if (fragment != null) {
                    vLabel.setText(fragment.getLabel());
                    vSubLabel.setText(fragment.getSubLabel());

                    setColors();
                    hideAndDisplayAchievements();
                    displayMapData();
                }
            }

            @Override
            public void onPageScrolled(int i, float v, int i2) {
                // empty
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // empty
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        vMap.onResume();

        new MenuTask().start();
    }

    @Override
    public void onPause() {
        vMap.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        vMap.onDestroy();
        super.onDestroy();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_foursquare).setVisible(!mHasFsqToken);
        menu.findItem(R.id.menu_achievements).setVisible(mGoogleApiClient.isConnected());
        menu.findItem(R.id.menu_debug).setVisible(RemoteLog.isEnabled());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_foursquare:
                startFsqConnection();

                return true;
            case R.id.menu_achievements:
                startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), REQUEST_ACHIEVEMENTS);

                return true;
            case R.id.menu_debug:
                RemoteLog.forceSendLogs();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FSQ_CONNECT:
                final AuthCodeResponse responseConnect = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
                final String code = responseConnect.getCode();

                if (!TextUtils.isEmpty(code)) {
                    startFsqExchange(code);
                } else {
                    RemoteLog.e("Failed to connect to Foursquare: " + responseConnect.getException().getMessage());
                }

                break;
            case REQUEST_FSQ_EXCHANGE:
                final AccessTokenResponse responseExchange = FoursquareOAuth.getTokenFromResult(resultCode, data);
                final String token = responseExchange.getAccessToken();

                if (!TextUtils.isEmpty(token)) {
                    mPreferences.saveFoursquareToken(token);
                    invalidateOptionsMenu();

                    Games.Achievements.unlock(getApiClient(), getString(R.string.achievement_make_it_social));
                } else {
                    RemoteLog.e("Failed to get Foursquare token: " + responseExchange.getException().getMessage());
                }

                FoursquareService.setAlarm(true);

                App.getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("foursquare")
                    .setAction("connection_done")
                    .build());

                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        invalidateOptionsMenu();

        new AchievementsTask().start();
    }

    @Override
    public void onConnectionSuspended(int i) {
        invalidateOptionsMenu();

        // TODO
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO
    }

    @Override
    public void onSignInSucceeded() {
        // TODO
    }

    @Override
    public void onSignInFailed() {
        // TODO
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

    public void setLabel(int day, String label, String subLabel) {
        if (day == getDay(vPager.getCurrentItem())) {
            vLabel.setText(label);
            vSubLabel.setText(subLabel);

            setColors();
            hideAndDisplayAchievements();
            displayMapData();
        }
    }

    private int getDay(int position) {
        return (position - HISTORY_PAGES + 1);
    }

    private void setColors() {
        // TODO: set colorPrimary, colorPrimaryDark according to goal reach
    }

    private void hideAndDisplayAchievements() {
        final int count = vAchievements.getChildCount();

        if (count > 0) {
            final PathInterpolator interpolator = new PathInterpolator(0.0f, 0.7f, 0.9f, 1.0f);
            final Path path = new Path();
            path.moveTo(1, 1);
            path.lineTo(0, 0);

            for (int i = 0; i < count; i ++) {
                View view = vAchievements.getChildAt(i);
                View icon = view.findViewById(R.id.icon);

                if (icon == null) {
                    continue;
                }

                ObjectAnimator animator = ObjectAnimator.ofFloat(
                    icon,
                    View.SCALE_X,
                    View.SCALE_Y,
                    path
                );
                animator.addListener(new EndAnimatorListener(icon, count, i));
                animator.setInterpolator(interpolator);
                animator.setStartDelay((i + 1) * 75);
                animator.start();
            }
        } else {
            displayAchievements();
        }
    }

    private void displayAchievements() {
        // Achievements
        final ArrayList<Achvmnt> achievements = new ArrayList<Achvmnt>();
        final long[] dayTimes = Utils.getTimesForDay(getDay(vPager.getCurrentItem()));

        synchronized (mAchievements) {
            final Set<Long> keys = mAchievements.keySet();
            for (Long key : keys) {
                Achvmnt achievement = mAchievements.get(key);

                if (achievement != null && key >= dayTimes[0] && key < dayTimes[1]) {
                    achievements.add(achievement);
                }
            }
        }

        // Animation params
        final PathInterpolator interpolator = new PathInterpolator(0.0f, 0.2f, 0.4f, 1.0f);
        final Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(1, 1);

        // Add views
        vAchievements.removeAllViews();

        int cnt = 0;
        for (Achvmnt achvmnt : achievements) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_achievement, vAchievements, false);

            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setContentDescription(achvmnt.description);

            vAchievements.addView(view);

            ImageLoaderSingleton.getInstance()
                .displayImage(achvmnt.unlockedImageUrl, icon);

            ObjectAnimator animator = ObjectAnimator.ofFloat(
                icon,
                View.SCALE_X,
                View.SCALE_Y,
                path
            );
            animator.addListener(new StartAnimatorListener(icon));
            animator.setInterpolator(interpolator);
            animator.setStartDelay((achievements.size() - cnt) * 75);
            animator.start();

            cnt ++;
        }
        vAchievements.setVisibility(View.VISIBLE);
    }

    private void displayMapData() {
        final int day = getDay(vPager.getCurrentItem());
        if (mMapDataTask != null && mMapDataTask.getDay() != day) {
            mMapDataTask.cancel(true);
        }

        mMapDataTask = new MapDataTask(day);
        mMapDataTask.start();
    }

    private void startFsqConnection() {
        App.getTracker().send(new HitBuilders.EventBuilder()
            .setCategory("foursquare")
            .setAction("connection_init")
            .build());

        final Intent intent = FoursquareOAuth.getConnectIntent(
            App.get(),
            Constants.FSQ_CLIENT_ID
        );
        startActivityForResult(intent, REQUEST_FSQ_CONNECT);
    }

    private void startFsqExchange(String code) {
        final Intent intent = FoursquareOAuth.getTokenExchangeIntent(
            App.get(),
            Constants.FSQ_CLIENT_ID,
            Constants.FSQ_CLIENT_SECRET,
            code
        );
        startActivityForResult(intent, REQUEST_FSQ_EXCHANGE);
    }

    // Classes

    public class PagesAdapter extends FragmentStatePagerAdapter {

        private final HashMap<Integer, GraphFragment> mFragments = new HashMap<Integer, GraphFragment>();

        public PagesAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            final GraphFragment fragment = GraphFragment.newInstance(getDay(position));
            mFragments.put(position, fragment);

            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.remove(position);

            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return HISTORY_PAGES;
        }

        public GraphFragment getFragment(int position) {
            return mFragments.get(position);
        }
    }

    /**
     * Check achievements queue and unlock if necessary
     */
    public class AchievementsTask extends BaseAsyncTask {

        @Override
        public void inBackground() {
            // Load achievements
            PendingResult pending = Games.Achievements.load(mGoogleApiClient, false);
            Achievements.LoadAchievementsResult result = (Achievements.LoadAchievementsResult)pending
                .await(60, TimeUnit.SECONDS);

            int status = result.getStatus().getStatusCode();
            if (status != GamesStatusCodes.STATUS_OK) {
                result.release();
                return;
            }

            AchievementBuffer buffer = result.getAchievements();
            int bufSize = buffer.getCount();

            final HashMap<String, Integer> available = new HashMap<String, Integer>();
            final HashMap<Long, Achvmnt> unlocked = new HashMap<Long, Achvmnt>();
            for (int i = 0; i < bufSize; i++) {
                Achievement achievement = buffer.get(i);

                Achvmnt achvmnt = new Achvmnt();
                achvmnt.id = achievement.getAchievementId();
                achvmnt.state = achievement.getState();
                achvmnt.lastChange = achievement.getLastUpdatedTimestamp();
                achvmnt.unlockedImageUrl = achievement.getUnlockedImageUrl();
                achvmnt.description = achievement.getDescription();

                available.put(achvmnt.id, achvmnt.state);
                if (achvmnt.state == Achievement.STATE_UNLOCKED) {
                    unlocked.put(achvmnt.lastChange, achvmnt);
                }
            }

            buffer.close();
            result.release();

            synchronized (mAchievements) {
                mAchievements.clear();
                mAchievements.putAll(unlocked);
            }

            // Check waiting achievements
            final Set<String> queue = mPreferences.getAchievementsToUnlock();
            if (queue == null || queue.isEmpty()) {
                return; // Nothing to do
            }

            // Unlock waiting achievements
            for (String item : queue) {
                int state = available.get(item);
                if (state == Achievement.STATE_UNLOCKED) {
                    mPreferences.removeAchievementFromQueue(item);
                    continue;
                }

                PendingResult updatePending = Games.Achievements.unlockImmediate(mGoogleApiClient, item);
                Achievements.UpdateAchievementResult updateResult = (Achievements.UpdateAchievementResult)updatePending
                    .await(60, TimeUnit.SECONDS);

                if (updateResult != null) {
                    int updateStatus = updateResult.getStatus().getStatusCode();
                    if (updateStatus == GamesStatusCodes.STATUS_OK) {
                        mPreferences.removeAchievementFromQueue(item);

                        if (item.equalsIgnoreCase(getString(R.string.achievement_100000_km))) {
                            Games.Achievements.reveal(mGoogleApiClient, getString(R.string.achievement_384400_km));
                            Games.Achievements.reveal(mGoogleApiClient, getString(R.string.achievement_1_au));
                        }
                    }
                }
            }
        }

        @Override
        public void postExecute() {
            // Nothing
        }
    }

    /**
     * Load data for options menu off UI
     */
    private class MenuTask extends BaseAsyncTask {

        private boolean mHasToken = false;

        @Override
        public void inBackground() {
            mHasToken = mPreferences.hasFoursquareToken();
        }

        @Override
        public void postExecute() {
            if (mHasFsqToken != mHasToken) {
                mHasFsqToken = mHasToken;
                invalidateOptionsMenu();
            }
        }
    }

    private class MapDataTask extends BaseAsyncTask {

        private MovementContainer mContainer;
        private final ArrayList<Checkin> mCheckins = new ArrayList<Checkin>();
        private int mDay;
        //
        final private ArrayList<PolylineOptions> mPolylines = new ArrayList<PolylineOptions>();
        final private ArrayList<MarkerOptions> mMarkers = new ArrayList<MarkerOptions>();
        private LatLngBounds mBounds;

        public MapDataTask(int day) {
            mDay = day;
        }

        @Override
        public void inBackground() {
            mContainer = mMovementHelper.getDataForDay(mDay);
            if (mContainer == null || mContainer.locations == null || mContainer.locations.isEmpty()) {
                return;
            }

            // Midnight
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, mDay);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long midnight = calendar.getTimeInMillis();

            // Pre-generate map polylines
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

                latBounds[0] = Math.min(latBounds[0], model.latitude);
                latBounds[1] = Math.max(latBounds[1], model.latitude);
                lonBounds[0] = Math.min(lonBounds[0], model.longitude);
                lonBounds[1] = Math.max(lonBounds[1], model.longitude);
            }

            // Checkins
            ArrayList<Checkin> checkins = mMovementHelper.getCheckinsForDay(mDay);
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

                latBounds[0] = Math.min(latBounds[0], checkin.latitude);
                latBounds[1] = Math.max(latBounds[1], checkin.latitude);
                lonBounds[0] = Math.min(lonBounds[0], checkin.longitude);
                lonBounds[1] = Math.max(lonBounds[1], checkin.longitude);
            }

            // Map bounds
            if (!mContainer.locations.isEmpty() || !mCheckins.isEmpty()) {
                LatLng ne = new LatLng(latBounds[0], lonBounds[0]);
                LatLng sw = new LatLng(latBounds[1], lonBounds[1]);
                mBounds = new LatLngBounds(ne, sw);
            }
        }

        @Override
        public void postExecute() {
            // Locations
            final GoogleMap map = vMap.getMap();
            map.clear();

            // Polyline
            for (PolylineOptions polylineOptions : mPolylines) {
                map.addPolyline(polylineOptions);
            }

            // Checkins
            if (!mMarkers.isEmpty()) {
                for (MarkerOptions markerOptions : mMarkers) {
                    map.addMarker(markerOptions);
                }
            }

            // Center map
            int mapMargin = getResources().getDimensionPixelSize(R.dimen.margin_map);
            map.setPadding(
                mapMargin,
                vMap.getHeight() - getResources().getDimensionPixelSize(R.dimen.map_size), // top
                mapMargin,
                mapMargin
            );

            if (mBounds != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        mBounds,
                        mapMargin
                    )
                );
            } else {
                // TODO: center to my location, zoom 14
            }
        }

        public int getDay() {
            return mDay;
        }
    }

    private class StartAnimatorListener implements Animator.AnimatorListener {

        private View mView;

        public StartAnimatorListener(View view) {
            mView = view;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            mView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            // empty
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            // empty
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // empty
        }
    }

    private class EndAnimatorListener implements Animator.AnimatorListener {

        private View mView;
        private int mTotal;
        private int mCurrent;

        public EndAnimatorListener(View view, int total, int current) {
            mView = view;
            mTotal = total;
            mCurrent = current;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            // empty
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mView.setVisibility(View.INVISIBLE);

            if (mCurrent == (mTotal - 1)) {
                displayAchievements();
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            // empty
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // empty
        }
    }
}
