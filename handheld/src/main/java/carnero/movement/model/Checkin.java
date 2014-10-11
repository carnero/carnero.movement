package carnero.movement.model;

import android.view.View;
import android.widget.RelativeLayout.LayoutParams;

@SuppressWarnings("unused")
public class Checkin {

    public long id;
    public String checkinId;
    public long createdAt;
    public double latitude;
    public double longitude;
    public String name;
    public String shout;
    public String iconPrefix;
    public String iconSuffix;
    //
    public static final int[] sizes = new int[] {32, 44, 64, 88};

    /*
    iconUrl = iconPrefix + size[x] + iconSuffix
     */
}
