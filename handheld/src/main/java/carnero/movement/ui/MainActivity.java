package carnero.movement.ui;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Constants;
import carnero.movement.common.Preferences;
import carnero.movement.common.remotelog.RemoteLog;
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
import com.google.example.games.basegameutils.BaseGameActivity;

public class MainActivity
    extends BaseGameActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private PagesAdapter mPagerAdapter;
    private Preferences mPreferences;
    private GoogleApiClient mGoogleApiClient;
    //
    private static final int HISTORY_PAGES = 31;
    private static final int REQUEST_FSQ_CONNECT = 1001;
    private static final int REQUEST_FSQ_EXCHANGE = 1002;
    private static final int REQUEST_ACHIEVEMENTS = 1003;
    //
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.pager)
    ViewPager vPager;

    @Override
    protected void onCreate(Bundle state) {
        // Play Services: Games
        setRequestedClients(BaseGameActivity.CLIENT_GAMES);

        super.onCreate(state);

        // Init
        mPreferences = new Preferences();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Games.API)
            .addScope(Games.SCOPE_GAMES)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
        mGoogleApiClient.connect();

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

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
    protected void onDestroy() {
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
        menu.findItem(R.id.menu_foursquare).setVisible(!mPreferences.hasFoursquareToken());
        menu.findItem(R.id.menu_achievements).setVisible(mGoogleApiClient.isConnected());

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

    public void setLabel(int day, String label) {
        if (day == getDay(vPager.getCurrentItem())) {
            vLabel.setText(label);
        }
    }

    private int getDay(int position) {
        return (position - HISTORY_PAGES + 1);
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

    public class AchievementsTask extends BaseAsyncTask {

        @Override
        public void inBackground() {
            final HashMap<String, Boolean> available = new HashMap<String, Boolean>();
            final Set<String> queue = mPreferences.getAchievementsToUnlock();
            if (queue == null || queue.isEmpty()) {
                return; // Nothing to do
            }

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
            for (int i = 0; i < bufSize; i++) {
                Achievement achievement = buffer.get(i);

                available.put(
                    achievement.getAchievementId(),
                    achievement.getState() == Achievement.STATE_UNLOCKED
                );
            }

            buffer.close();
            result.release();

            // Unlock waiting achievements
            for (String item : queue) {
                Boolean unlocked = available.get(item);
                if (unlocked != null && unlocked) {
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
}
