package carnero.movement.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.transition.*;
import android.view.Window;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.service.LocationService;

public class MainActivity extends AbstractBaseActivity {

    private PagerAdapter mPagerAdapter;
    //
    @InjectView(R.id.pager)
    ViewPager vPager;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        if (Build.VERSION.SDK_INT > 20) {
            requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setExitTransition(new Explode());
        }

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mPagerAdapter = new PagesAdapter();
        vPager.setAdapter(mPagerAdapter);
        vPager.setCurrentItem(2);
    }

    // Classes

    private class PagesAdapter extends FragmentStatePagerAdapter {
        public PagesAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GraphFragment.newInstance(-2); // Day before yesterday
                case 1:
                    return GraphFragment.newInstance(-1); // Yesterday
                case 2:
                    return GraphFragment.newInstance(0); // Today
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
