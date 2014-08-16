package carnero.movement.ui;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import carnero.movement.R;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.LocationSource;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelLocation;

public class MapFragment extends com.google.android.gms.maps.MapFragment {

    private Helper mHelper;
    private LocationSource mLocationSource;
    private boolean mFollowMe = true;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        mHelper = new Helper(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        mLocationSource = new LocationSource(getActivity());

        final GoogleMap map = getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);

        map.setMyLocationEnabled(true);
        map.setLocationSource(mLocationSource);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                centerMap(location, false);
            }
        });

        centerMap(map.getMyLocation(), true);

        new DataLoadTask().start();
    }

    @Override
    public void onPause() {
        mLocationSource.deactivate();

        super.onPause();
    }

    private void centerMap(Location location, boolean fast) {
        if (mFollowMe && location != null) {
            if (fast) {
                getMap().moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()),
                                14
                        )
                );
            } else {
                getMap().animateCamera(
                        CameraUpdateFactory.newLatLng(
                                new LatLng(location.getLatitude(), location.getLongitude())
                        )
                );
            }
            mFollowMe = false;
        }
    }

    // Classes

    private class DataLoadTask extends BaseAsyncTask {

        private ArrayList<ModelLocation> mData;

        @Override
        public void inBackground() {
            mData = mHelper.getLocationsToday();
        }

        @Override
        public void postExecute() {
            final GoogleMap map = getMap();

            map.clear();
            if (mData == null) {
                return;
            }

            final PolylineOptions polylineOpts = new PolylineOptions();
            polylineOpts.zIndex(1010);
            polylineOpts.width(getResources().getDimension(R.dimen.line_stroke));
            polylineOpts.color(getResources().getColor(R.color.map_history));

            for (ModelLocation model : mData) {
                LatLng latLng = new LatLng(model.latitude, model.longitude);
                polylineOpts.add(latLng);
            }

            map.addPolyline(polylineOpts);
        }
    }
}