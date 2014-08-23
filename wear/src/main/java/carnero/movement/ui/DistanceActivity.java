package carnero.movement.ui;

import android.content.Intent;
import android.os.Bundle;

import carnero.movement.R;
import carnero.movement.data.ModelDataContainer;

public class DistanceActivity extends AbstractBaseActivity {

    private ModelDataContainer mContainer;

    public void onCreate(Bundle state) {
        super.onCreate(state);

        final Intent intent = getIntent();
        if (intent != null) {
            mContainer = intent.getParcelableExtra("data");
        }

        setContentView(R.layout.notification_activity);
    }
}
