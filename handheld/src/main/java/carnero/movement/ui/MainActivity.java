package carnero.movement.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.InjectView;
import carnero.movement.R;
import carnero.movement.service.LocationService;

public class MainActivity extends AbstractBaseActivity {

    @InjectView(R.id.container)
    View vContainer;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        // getWindow().setExitTransition(new Explode());

        // Start service
        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        // Init layout
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Fragment fragment = getFragmentManager().findFragmentById(R.id.container);
        if (fragment == null) {
            fragment = GraphFragment.newInstance();
            // fragment = MapFragment.newInstance();
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }
    }
}
