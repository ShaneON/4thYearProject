package com.fourthyearproject.shane.cycled;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class GPSIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private LocationRequest locationRequest;
    private Location currentLocation;
    private GoogleApiClient googleApiClient;
    private LatLng currentLatLng;
    ResultReceiver resultReceiver;

    private static final String TAG = "GPSIntentService";

    public GPSIntentService() {
        super("GPSIntentService");
    }

    public GPSIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        resultReceiver = intent.getParcelableExtra("resultReceiver");
        createLocationRequest();
        //Create an instance of GoogleAPIClient.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        googleApiClient.connect();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (currentLocation != null) {

            currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        startLocationUpdates();
    }

    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        String bundleLatitude = Double.toString(currentLocation.getLatitude());
        String bundleLongitude = Double.toString(currentLocation.getLongitude());
        Bundle locationBundle = new Bundle();
        locationBundle.putString("latitude", bundleLatitude);
        locationBundle.putString("longitude", bundleLongitude);
        resultReceiver.send(0, locationBundle);
    }

    @Override
    public void onConnectionSuspended(int result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
    }
/*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "inside onStartCommand");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        resultReceiver = intent.getParcelableExtra("receiver");
        return super.onStartCommand(intent,flags,startId);
    }*/
}
