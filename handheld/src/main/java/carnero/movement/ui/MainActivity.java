package carnero.movement.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
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
import carnero.movement.common.Constants;
import carnero.movement.common.Preferences;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.service.FoursquareService;
import carnero.movement.service.LocationService;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;
import com.google.android.gms.analytics.HitBuilders;

public class MainActivity extends AbstractBaseActivity {

    private PagesAdapter mPagerAdapter;
    private Preferences mPreferences;
    //
    private static final int HISTORY_PAGES = 7;
    private static final int REQUEST_FSQ_CONNECT = 1001;
    private static final int REQUEST_FSQ_EXCHANGE = 1002;
    //
    @InjectView(R.id.label)
    TextView vLabel;
    @InjectView(R.id.pager)
    ViewPager vPager;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Init
        mPreferences = new Preferences();

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
        vPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.margin_page));
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

                FoursquareService.setAlarm(true);

                App.getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("foursquare")
                    .setAction("connection_done")
                    .build());

                break;
        }
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
}
