package carnero.movement.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.mariux.teleport.lib.TeleportClient;

public abstract class AbstractBaseActivity extends FragmentActivity {

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
