package com.fourthyearproject.shane.cycled;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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

import static android.content.ContentValues.TAG;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private BluetoothLE ble;
    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    private Marker marker;
    private LatLng destinationLatLng;
    private LatLng currentLatLng;
    private boolean firstUpdate = true;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String TAG = "MapsActivity";
    private boolean bluetoothConnected;
    private boolean bluetoothDisconnected;
    private boolean bluetoothFailedToConnect;
    private MenuItem bluetoothItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect beacons.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        Toolbar myToolbar = (Toolbar) findViewById(R.id.maps_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);

                    PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                            getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
                    autocompleteFragment.setText("Where do you want to go?");
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
                    googleApiClient.connect();
                    Log.d(TAG, "=========================GoogleApiClient has connected=================================");

                    MapsResultReceiver mapsResultReceiver = new MapsResultReceiver(null, this);
                    Intent gpsIntentService = new Intent(this, GPSIntentService.class);
                    gpsIntentService.putExtra("resultReceiver", mapsResultReceiver);
                    startService(gpsIntentService);

                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("App does not have permission to connect via bluetooth");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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
        if(null != marker) marker.remove();
    }

    void updateUI() {

    }

    void onReceiveGPSUpdate(String la, String lo) {

        double latitude = Double.parseDouble(la);
        double longitude = Double.parseDouble(lo);
        currentLatLng = new LatLng(latitude, longitude);
        if(firstUpdate) {
            //MarkerOptions markerOptions = new MarkerOptions().position(currentLatLng);
            //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            //marker = mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12.0f));
            firstUpdate = false;
        }
        else updateUI();
    }

    protected void onStart() {

        super.onStart();
    }
/*
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }
*/

    protected void onDestroy()
    {
        googleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.bluetooth_search) {
            bluetoothItem = item;
            BluetoothResultReceiver bluetoothResultReceiver = new BluetoothResultReceiver(null, this);
            Intent bluetoothLeIntentService = new Intent(this, BluetoothLeIntentService.class);
            bluetoothLeIntentService.putExtra("resultReceiver", bluetoothResultReceiver);
            startService(bluetoothLeIntentService);
            // TODO set up icons for searching and connected
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

    }

    public void onReceiveBluetoothUpdate(Bundle resultData) {
        Log.d(TAG, "========================I am in onReceiveBluetoothUpdate======================================");
        if (null != resultData.getString("Connected")) {
            Log.d(TAG, "========================I am in onReceiveBluetoothUpdate - Connected If Statement========================");
            bluetoothConnected = true;
            bluetoothDisconnected = false;
            bluetoothFailedToConnect = false;
            this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    bluetoothItem.setIcon(R.drawable.ic_bluetooth_black_24dp);
                    Toast.makeText(getApplicationContext(), "Bluetooth Connected", Toast.LENGTH_SHORT).show();
                }
            });


        } else if (null != resultData.getString("Disconnected")) {
            Log.d(TAG, "========================I am in onReceiveBluetoothUpdate - Disconnected If Statement========================");
            bluetoothConnected = false;
            bluetoothDisconnected = true;
            bluetoothFailedToConnect = false;
            this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    bluetoothItem.setIcon(R.mipmap.ic_bluetooth_white_24dp);
                    Toast.makeText(getApplicationContext(), "Bluetooth Disconnected", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (null != resultData.getString("Failed to Connect")) {
            Log.d(TAG, "========================I am in onReceiveBluetoothUpdate - Failed To Connect If Statement====================");
            bluetoothConnected = false;
            bluetoothDisconnected = false;
            bluetoothFailedToConnect = true;
            this.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(getApplicationContext(), "Failed to Connect Bluetooth", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

class MapsResultReceiver extends ResultReceiver
{
    private MapsActivity maps;

    public MapsResultReceiver(Handler handler) {
        super(handler);
    }

    public MapsResultReceiver(Handler handler, MapsActivity m) {
        super(handler);
        maps = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        maps.onReceiveGPSUpdate(resultData.getString("latitude"), resultData.getString("longitude"));
    }
}

class BluetoothResultReceiver extends ResultReceiver
{
    private MapsActivity maps;

    public BluetoothResultReceiver(Handler handler) {
        super(handler);
    }

    public BluetoothResultReceiver(Handler handler, MapsActivity m) {
        super(handler);
        maps = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "========================I am in BluetoothResultReceiver.onReceiveResult====================");
        maps.onReceiveBluetoothUpdate(resultData);
    }
}

