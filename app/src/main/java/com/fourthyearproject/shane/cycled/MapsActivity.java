package com.fourthyearproject.shane.cycled;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.util.LinkedList;
import java.util.List;

import static com.fourthyearproject.shane.cycled.R.id.map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private static final String TAG = "MapsActivity";

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    private LatLng destinationLatLng = null;
    private LatLng currentLatLng = null;
    private boolean firstUpdate = true;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private boolean bluetoothConnected;
    private boolean bluetoothDisconnected;
    private boolean bluetoothFailedToConnect;
    private boolean bluetoothScanning;
    private MenuItem bluetoothItem;
    private boolean firstSelection = true;
    private Polyline polyline = null;
    private LinkedList<LatLng> waypointList = new LinkedList<>();
    //private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //tv = (TextView) findViewById(R.id.distance_text_view);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.maps_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onStart() {
        super.onStart();
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            dialogBuilder("Location access", "Please grant location access", true, false);
        }
        else onStartSetup();
    }

    private void onStartSetup() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        directionsSetup();
        googleApiClientSetup();
        gpsServiceStartup();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void onStop() {
        super.onStop();
        if(null != googleApiClient) googleApiClient.disconnect();
    }

    protected void onDestroy() {
        super.onDestroy();
        this.stopService(new Intent(this, GPSService.class));
        this.stopService(new Intent(this, BluetoothLeService.class));
    }

    void dialogBuilder(String title, String message, boolean p, boolean s) {
        final boolean permission = p;
        final boolean scanning = s;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        if(permission) builder.setPositiveButton(android.R.string.ok, null);
        else builder.setPositiveButton(android.R.string.cancel, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(permission)
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
                if(scanning) {
                    stopService(new Intent(getApplicationContext(), BluetoothLeService.class));
                    Toast.makeText(getApplicationContext(), "Bluetooth service has stopped",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.bluetooth_search) {
            bluetoothItem = item;
            bluetoothLeServiceStartup();
        }
        else if (id == R.id.bicycle_icon)
            gpsServiceStartup();
        else if (id == R.id.refresh_icon)
            refreshPage();
        return super.onOptionsItemSelected(item);
    }

    private void refreshPage() {
        if(null != polyline)
            polyline.remove();
        mMap.clear();
        waypointList.clear();
        destinationLatLng = null;
        firstSelection = true;
        polyline = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) onStartSetup();
                else dialogBuilder("Functionality limited", "No bluetooth permission", false, false);
            }
        }
    }

    private void googleApiClientSetup() {
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
    }

    private void gpsServiceStartup() {
        MapsResultReceiver mapsResultReceiver = new MapsResultReceiver(null, this);
        Intent gpsService = new Intent(this, GPSService.class);
        gpsService.putExtra("resultReceiver", mapsResultReceiver);
        if(destinationLatLng != null){
            String origin = currentLatLng.latitude + "," + currentLatLng.longitude;
            String destination = destinationLatLng.latitude + "," + destinationLatLng.longitude;
            String url = new DirectionsURL().makeDirectionsURL(origin, destination, waypointList);
            gpsService.putExtra("URL", url);
            gpsService.putExtra("Destination", destination);
        }
        startService(gpsService);
    }


    private void directionsSetup() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                processPlaceSelection(place.getLatLng());
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.d(TAG, "Error with place selection: " + status);
            }
        });
    }

    private void processPlaceSelection(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
        if(firstSelection) {
            firstSelection = false;
            destinationLatLng = latLng;
        }
        else waypointList.add(latLng);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

    }

    @Override
    public void onConnected(Bundle connectionHint) {}

    @Override
    public void onConnectionSuspended(int i) {

    }

    void onReceiveGPSUpdate(String la, String lo, String pol, String dir, String dis) {
        double latitude = Double.parseDouble(la);
        double longitude = Double.parseDouble(lo);
        currentLatLng = new LatLng(latitude, longitude);

        if(firstUpdate) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12.0f));
            firstUpdate = false;
        }
        if(null != pol) {
            updateUiPolyline(pol);
        }
        /*
        if(null != dir && !"".equals(dir)) {
            updateUiDirection(dir);
        }
        if(null != dis && !"".equals(dis)) {
            updateUiDirection(dis);
        }
        */
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
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                processPlaceSelection(latLng);
            }
        });
    }

    void updateUiPolyline(String p) {
            List<LatLng> polylineList = PolyUtil.decode(p);
            polyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(polylineList)
                    .width(6)
                    .color(ContextCompat.getColor(getApplicationContext(), R.color.sky))
                    .geodesic(true));

    }
/*
    void updateUiDirection(String s) {
        final String message = s;
        this.runOnUiThread(new Runnable() {
            public void run() {
                tv.setText(message);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
*/
    private void bluetoothLeServiceStartup() {
        BluetoothResultReceiver bluetoothResultReceiver = new BluetoothResultReceiver(null, this);
        Intent bluetoothLeService = new Intent(this, BluetoothLeService.class);
        bluetoothLeService.putExtra("resultReceiver", bluetoothResultReceiver);
        startService(bluetoothLeService);
    }

    public void onReceiveBluetoothUpdate(Bundle resultData) {
        if (null != resultData.getString("Connected")) {
            connectionStatus(true, false, false, false);
            updateUiBluetooth("Bluetooth Connected", null);
        } else if (null != resultData.getString("Disconnected")) {
            connectionStatus(false, true, false, false);
            //updateUiBluetooth("Bluetooth Disconnected", null);
        } else if (null != resultData.getString("Failed to Connect")) {
            connectionStatus(false, true, true, true);
            //updateUiBluetooth("Failed to Connect Bluetooth", null);
        } else if (null != resultData.getString("No device")) {
            connectionStatus(false, true, true, true);
            //updateUiBluetooth("No Bluetooth Device Present", null);
        } else if (null != resultData.getString("Scanning")) {
            connectionStatus(false, true, false, true);
            //updateUiBluetooth("Scanning for Bluetooth Device...", null);
        }
    }

    void updateUiBluetooth(String s, String p) {
        final String message = s;
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(bluetoothConnected)
                    bluetoothItem.setIcon(R.drawable.ic_bluetooth_black_24dp);
                else
                    bluetoothItem.setIcon(R.mipmap.ic_bluetooth_white_24dp);
                if(bluetoothScanning)
                    //dialogBuilder("Scanning", "Scanning for device...", false, true);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectionStatus(boolean connected, boolean disconnected, boolean failed, boolean scanning) {
        bluetoothConnected = connected;
        bluetoothDisconnected = disconnected;
        bluetoothFailedToConnect = failed;
        bluetoothScanning = scanning;
    }
}