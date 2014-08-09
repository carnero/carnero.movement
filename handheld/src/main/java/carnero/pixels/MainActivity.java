package carnero.pixels;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.fivehundredpx.api.FiveHundredException;
import com.fivehundredpx.api.auth.AccessToken;
import com.fivehundredpx.api.tasks.XAuth500pxTask;

public class MainActivity extends Activity implements XAuth500pxTask.Delegate {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    final XAuth500pxTask loginTask = new XAuth500pxTask(this);
    loginTask.execute(
        getString(R.string.auth_consumer_key),
        getString(R.string.auth_consumer_secret),
        "username",
        "pass"
    );
  }

  @Override
  public void onSuccess(AccessToken result) {
    // TODO: logged in
  }

  @Override
  public void onFail(FiveHundredException exception) {
    // TODO: failed
  }
}
