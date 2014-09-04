package carnero.movement.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;
import carnero.movement.service.LocationService;
import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.SmoothLineGraph;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class MainActivity extends AbstractBaseActivity {

    private PagerAdapter mPagerAdapter;
    private Helper mHelper;
    private Line mLineDistance;
    //
    private static final int HISTORY_PAGES = 31;
    private static final int GRAPH_DAYS = 21;
    //
    @InjectView(R.id.pager)
    ViewPager vPager;
    SmoothLineGraph vGraph;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Init
        mHelper = new Helper(this);

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // ActionBar
        final View customView = LayoutInflater.from(this).inflate(R.layout.item_actionbar_graph, null);
        vGraph = (SmoothLineGraph)customView.findViewById(R.id.action_graph);

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
        initGraph();

        // Set ViewPager
        mPagerAdapter = new PagesAdapter();
        vPager.setAdapter(mPagerAdapter);
        vPager.setOnPageChangeListener(new PageChangeListener());
        vPager.setCurrentItem(mPagerAdapter.getCount() - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new GraphDataTask().start();
    }

    private void initGraph() {
        mLineDistance = new Line();
        mLineDistance.setFill(true);
        mLineDistance.setShowingPoints(true);
        mLineDistance.setColor(getResources().getColor(R.color.graph_actionbar));
        vGraph.addLine(mLineDistance);
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
                vGraph.setHighlightSegment(GRAPH_DAYS - (HISTORY_PAGES - i - 1));
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
            mLineDistance.getPoints().clear();

            for (int i = -GRAPH_DAYS; i <= 0; i++) {
                ModelData summary = mHelper.getSummaryForDay(i);
                LinePoint pointDistance = new LinePoint();

                if (summary == null) {
                    pointDistance.setX(GRAPH_DAYS + i);
                    pointDistance.setY(0);

                    mMinDst = Math.min(mMinDst, 0f);
                    mMaxDst = Math.max(mMaxDst, 0f);
                } else {
                    pointDistance.setX(GRAPH_DAYS + i);
                    pointDistance.setY(summary.distance);

                    mMinDst = Math.min(mMinDst, summary.distance);
                    mMaxDst = Math.max(mMaxDst, summary.distance);
                }
                mLineDistance.addPoint(pointDistance);
            }
        }

        @Override
        public void postExecute() {
            vGraph.setRangeY(
                0,
                mMaxDst
            );
            vGraph.invalidate();
        }
    }
}
