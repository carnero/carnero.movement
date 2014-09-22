package carnero.movement.ui;

import java.util.ArrayList;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.*;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Constants;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.common.graph.OverviewPath;
import carnero.movement.common.graph.SplineGraph;
import carnero.movement.common.graph.SplinePath;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;
import carnero.movement.service.LocationService;
import carnero.movement.common.model.XY;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class MainActivity extends AbstractBaseActivity {

    private PagerAdapter mPagerAdapter;
    private Preferences mPreferences;
    private Helper mHelper;
    private final ArrayList<SplinePath> mPaths = new ArrayList<SplinePath>();
    private final ArrayList<XY> mOverviewPoints = new ArrayList<XY>();
    private final SplinePath mOverviewPath = new OverviewPath();
    //
    private static final int HISTORY_PAGES = 7;
    private static final int GRAPH_DAYS = 14;
    private static final int REQUEST_FSQ_CONNECT = 1001;
    private static final int REQUEST_FSQ_EXCHANGE = 1002;
    //
    @InjectView(R.id.pager)
    ViewPager vPager;
    SplineGraph vGraph;
    TextView vTotalDistance;
    TextView vTotalSteps;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Init
        mPreferences = new Preferences(this);
        mHelper = new Helper(this);

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // ActionBar
        final View customView = LayoutInflater.from(this).inflate(R.layout.item_actionbar_graph, null);
        vGraph = (SplineGraph)customView.findViewById(R.id.action_graph);
        vTotalDistance = (TextView)customView.findViewById(R.id.total_distance);
        vTotalSteps = (TextView)customView.findViewById(R.id.total_steps);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(customView);
            actionBar.setDisplayShowCustomEnabled(true);

            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.primary_dark));

            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            vPager.setPadding(0, config.getPixelInsetTop(true), 0, 0);
        }

        // Graph
        mPaths.clear();
        mPaths.add(mOverviewPath);

        // Set ViewPager
        mPagerAdapter = new PagesAdapter();
        vPager.setOffscreenPageLimit(3);
        vPager.setAdapter(mPagerAdapter);
        vPager.setOnPageChangeListener(new PageChangeListener());
        vPager.setCurrentItem(mPagerAdapter.getCount() - 1);

        // Set statistics
        vTotalDistance.setText(Utils.formatDistance(mPreferences.getDistance()));
        vTotalSteps.setText(getString(R.string.stats_steps, mPreferences.getSteps()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        new GraphDataTask().start();
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

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_foursquare:
                startFsqConnection();

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
                } else {
                    RemoteLog.e("Failed to get Foursquare token: " + responseExchange.getException().getMessage());
                }

                break;
        }
    }

    private void startFsqConnection() {
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

    private class PagesAdapter extends FragmentStatePagerAdapter {

        public PagesAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            return GraphFragment.newInstance(position - HISTORY_PAGES + 1);
        }

        @Override
        public int getCount() {
            return HISTORY_PAGES;
        }
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageSelected(int i) {
            if (vGraph != null) {
                // TODO: highlight one sector
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
    }

    private class GraphDataTask extends BaseAsyncTask {

        private float mMinDst = Float.MAX_VALUE;
        private float mMaxDst = Float.MIN_VALUE;

        @Override
        public void inBackground() {
            mOverviewPoints.clear();

            for (int i = -GRAPH_DAYS; i <= 0; i++) {
                ModelData summary = mHelper.getSummaryForDay(i);
                XY point = new XY();

                if (summary == null) {
                    point.x = GRAPH_DAYS + i;
                    point.y = 0;

                    mMinDst = Math.min(mMinDst, 0f);
                    mMaxDst = Math.max(mMaxDst, 0f);
                } else {
                    point.x = GRAPH_DAYS + i;
                    point.y = summary.distance;

                    mMinDst = Math.min(mMinDst, summary.distance);
                    mMaxDst = Math.max(mMaxDst, summary.distance);
                }

                mOverviewPoints.add(point);
            }

            mOverviewPath.setData(mOverviewPoints);
        }

        @Override
        public void postExecute() {
            // Graph
            vGraph.setData(mPaths);
            vGraph.invalidate();
        }
    }
}
