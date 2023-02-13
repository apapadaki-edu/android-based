package com.example.myapplication;


import android.Manifest;
import android.annotation.SuppressLint;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Bundle>{

    private MapView mMapView;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // Google's API for location services.
    private FusedLocationProviderClient mFusedLocationClient;

    Location mCurLocation;
    private GoogleMap mMap;


    LocationRequest locationRequest;
    private LocationCallback locationCallBack;
    TextView locationInfo;
    private Marker mCurLocationMarker;

    @SuppressLint({"ResourceAsColor", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.main_activity);


        Button save = (Button) findViewById(R.id.add_place);
        save.bringToFront();
        locationInfo = (TextView) findViewById(R.id.location_info);
        locationInfo.bringToFront();
        SwitchCompat changeMode = (SwitchCompat) findViewById(R.id.sw_gps);
        SwitchCompat track = (SwitchCompat) findViewById(R.id.track);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        changeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(changeMode.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                }else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                }
            }
        });

        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(track.isChecked()){
                    startLocationUpdates();
                }else {
                    stopLocationUpdates();
                }
            }
        });

        // this is triggered when the interval update is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                updateUILocationInfo(location);
                updateMarker(location);
            }

        };

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Location l = mCurLocation;
                Log.i("CurLoc", l.toString());
                Bundle args = new Bundle();
                args.putDouble("lat", l.getLatitude());
                args.putDouble("long", l.getLongitude());
                LoaderManager.getInstance(MapActivity.this)
                        .initLoader(0, args, MapActivity.this);
                Log.i("InSave", "inSave");
            }
        });
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

        updateLocation();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
    }



    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.clear();
        mMap = googleMap;
        LatLng lastPlaced = retrieveCheckpoints(googleMap);
        //TODO: GET ALL LOCATIONS FROM THE DATABASE AND DISPLAY THEM ON THE MAP
        if (mCurLocation != null) {
            googleMap.addMarker(new MarkerOptions().position(new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude())).title("curLocation"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude()), 12f));
        }else {
            if (lastPlaced!=null){
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPlaced, 12f));
            }
        }

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                marker.showInfoWindow();
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            //TODO: get current location and zoom on the map
            // if the gps can get location (means that the it is active
            // then get its current location
            //else get the last location from the database
            //maybe use a function
            updateLocation();

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }


    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    private boolean checkPermissions() {
        int permissionStateFineLocation = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        return permissionStateFineLocation == PackageManager.PERMISSION_GRANTED ;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("MapActivity", "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MapActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    1);
                        }
                    });

        } else {
            Log.i("MapActivity", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("SecondMapsActivity", "onRequestPermissionResult");
        if (requestCode == 1) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("SecondMapsActivity", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.

                //TODO: CHECK IF I WILL KEEP THIS
                updateLocation();


            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    public void updateUILocationInfo(Location location){
        String text = "Lat: " + location.getLatitude() + "\n" +
                "Long: " + location.getLongitude() + "\n" +
                "Speed: " + location.getSpeed();
        locationInfo.setText(text);
    }

    @SuppressLint("MissingPermission")
    public void updateLocation(){
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                mCurLocation = location;
                mMap.addMarker(new MarkerOptions().position(new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude())).title("curLocation"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude()), 12f));
                updateUILocationInfo(mCurLocation);

            }
        });
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates(){
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallBack, null);
    }

    public void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(locationCallBack);
    }

    public void updateMarker(Location location){
        if (mCurLocationMarker != null){
            mCurLocationMarker.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mCurLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng));

        if(location.getSpeed()!= 0){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15.f));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.f),1000,null);
        }else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11.f));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.f));
        }
    }

    @NonNull
    @Override
    public Loader<Bundle> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id){
            case 0:
                return new FetchLocationLoader(this, args);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Bundle> loader, Bundle data) {
        if(data!=null) {
            addCheckpoint(data);
            Log.i("outOfThread", "");
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Bundle> loader) {

    }

    @SuppressLint("Range")
    public LatLng retrieveCheckpoints(GoogleMap map){
        LatLng position = null;
        String title;
        String snippet;

        Log.i("InRETRIEVE", "");
        String URL = "content://com.example.myapplication.CheckPointContentProvider";
        Uri checkpoints = Uri.parse(URL);
        Cursor c = managedQuery(checkpoints, null, null, null, "_ID DESC");
        if(c.moveToFirst()){
            do {
                position = new LatLng(c.getDouble(c.getColumnIndex(CheckPointContentProvider.lat)),
                        c.getDouble(c.getColumnIndex(CheckPointContentProvider.lon)));
                title = c.getString(c.getColumnIndex(CheckPointContentProvider.city));
                snippet = c.getString(c.getColumnIndex(CheckPointContentProvider.address)) + "\n" +
                        c.getString(c.getColumnIndex(CheckPointContentProvider.postalCode)) + "\n" +
                        c.getString(c.getColumnIndex(CheckPointContentProvider.knownName))  + "\n" +
                        c.getString(c.getColumnIndex(CheckPointContentProvider.city));
                map.addMarker(new MarkerOptions()
                        .position(position)
                        .title(title)
                        .snippet(snippet));

            } while (c.moveToNext());

        }
        // last marker placed
        return position;
    }

    public void addCheckpoint(Bundle checkpoint){
        ContentValues values = new ContentValues();

        Log.i("inAdd", "inAdd");
        values.put(CheckPointContentProvider.lat, checkpoint.getDouble("lat"));
        values.put(CheckPointContentProvider.lon, checkpoint.getDouble("long"));
        values.put(CheckPointContentProvider.address, checkpoint.getString("address"));
        values.put(CheckPointContentProvider.city, checkpoint.getString("city"));
        values.put(CheckPointContentProvider.country, checkpoint.getString("country"));
        values.put(CheckPointContentProvider.postalCode, checkpoint.getString("postalCode"));
        values.put(CheckPointContentProvider.knownName, checkpoint.getString("knownName"));

        Uri uri = getContentResolver().insert(
                CheckPointContentProvider.CONTENT_URI, values);
        Toast.makeText(getBaseContext(), uri.toString(), Toast.LENGTH_LONG).show();

    }
}
