package carnero.movement.ui;

import android.app.Activity;
import android.os.Bundle;

import com.mariux.teleport.lib.TeleportClient;

public abstract class AbstractBaseActivity extends Activity {

    private TeleportClient mTeleport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTeleport = new TeleportClient(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        mTeleport.connect();
    }

    @Override
    protected void onPause() {
        mTeleport.disconnect();

        super.onPause();
    }

    public TeleportClient teleport() {
        return mTeleport;
    }
}
