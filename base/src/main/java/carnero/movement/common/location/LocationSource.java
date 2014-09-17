package carnero.movement.common.location;

import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationSource implements com.google.android.gms.maps.LocationSource, LocationListener {

    private LocationManager mManager;
    private OnLocationChangedListener mListener;

    public LocationSource(Context context) {
        mManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;

        List<String> providers = mManager.getAllProviders();
        Location lastLoc;
        for (String provider : providers) {
            lastLoc = mManager.getLastKnownLocation(provider);
            if (lastLoc != null) {
                onLocationChanged(lastLoc);
            }
        }

        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setCostAllowed(true);

        final String provider = mManager.getBestProvider(criteria, true);
        if (provider != null) {
            mManager.requestLocationUpdates(provider, 500, 0, this);
        }
    }

    @Override
    public void deactivate() {
        mManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mListener != null) {
            mListener.onLocationChanged(location);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // empty
    }

    @Override
    public void onProviderEnabled(String provider) {
        // empty
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // empty
    }
}