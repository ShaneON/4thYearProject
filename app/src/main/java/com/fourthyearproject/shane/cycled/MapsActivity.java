package com.fourthyearproject.shane.cycled;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private BluetoothLE ble;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    private Marker marker;
    private LatLng destinationLatLng;
    private LatLng currentLatLng;
    private boolean firstUpdate;

    private static final String TAG = "MapsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.maps_toolbar);
        setSupportActionBar(myToolbar);

        MapsResultReceiver resultReceiver = new MapsResultReceiver(null, this);
        Log.d(TAG, "--------------------------inside oncreate--------------------------");
        Intent gpsIntentService = new Intent(this, GPSIntentService.class);
        gpsIntentService.putExtra("resultReceiver", resultReceiver);
        startService(gpsIntentService);

        firstUpdate = true;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destinationLatLng = place.getLatLng();
                //Log.i(TAG, "Place: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                //Log.i(TAG, "An error occurred: " + status);
            }
        });

        //Create an instance of GoogleAPIClient.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    @Override
    public void onConnectionSuspended(int result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    @Override
    public void onConnected(Bundle connectionHint) {}

    @Override
    protected void onPause() {
        super.onPause();
        marker.remove();
    }

    void updateUI() {

        marker.setPosition(currentLatLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
    }

    void onReceiveGPSUpdate(String la, String lo) {

        double latitude = Double.parseDouble(la);
        double longitude = Double.parseDouble(lo);
        currentLatLng = new LatLng(latitude, longitude);

        if(firstUpdate) {
            MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng);
            marker = mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
            mMap.moveCamera(CameraUpdateFactory.zoomIn()); // or zoomBy(float)?
            firstUpdate = false;
        }
        else updateUI();
    }

    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

    }
}

class MapsResultReceiver extends ResultReceiver
{
    private MapsActivity maps;

    public MapsResultReceiver(Handler handler, MapsActivity m) {
        super(handler);
        maps = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        maps.onReceiveGPSUpdate(resultData.getString("latitude"), resultData.getString("longitude"));
    }
}

