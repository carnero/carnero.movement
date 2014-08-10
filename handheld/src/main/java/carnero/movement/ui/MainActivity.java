package carnero.movement.ui;

import android.content.Intent;
import android.os.Bundle;

import butterknife.ButterKnife;
import carnero.movement.R;
import carnero.movement.service.LocationService;

public class MainActivity extends AbstractBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        final Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);
    }
}
