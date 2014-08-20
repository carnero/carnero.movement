package carnero.movement.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.transition.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LinePoint;
import com.echo.holographlibrary.SmoothLineGraph;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;
import carnero.movement.service.LocationService;

public class MainActivity extends AbstractBaseActivity {

    private PagerAdapter mPagerAdapter;
    private Helper mHelper;
    private Line mLineDistance;
    //
    private static final int HISTORY_PAGES = 7;
    private static final int GRAPH_DAYS = 21;
    //
    @InjectView(R.id.pager)
    ViewPager vPager;
    SmoothLineGraph vGraph;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        if (Build.VERSION.SDK_INT > 20) {
            requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setExitTransition(new Explode());
        }

        // Init
        mHelper = new Helper(this);

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // ActionBar
        final View customView = LayoutInflater.from(this).inflate(R.layout.item_actionbar_graph, null);
        vGraph = (SmoothLineGraph) customView.findViewById(R.id.action_graph);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(customView);
            actionBar.setDisplayShowCustomEnabled(true);
        }
        initGraph();

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

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
        mLineDistance.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.line_stroke_light));
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

            for (int i = -GRAPH_DAYS; i <= 0; i ++) {
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
