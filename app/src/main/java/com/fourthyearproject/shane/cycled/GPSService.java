package com.fourthyearproject.shane.cycled;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GPSService extends Service implements GoogleApiClient.ConnectionCallbacks,
        LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GPSService";

    private LocationRequest locationRequest;
    private Location currentLocation;
    private GoogleApiClient googleApiClient;
    ResultReceiver resultReceiver;
    private String urlString;
    private JSONParser parser;
    private int step;
    private boolean routeDownloaded = false;
    private String direction = null;
    private boolean aboutToTurn;
    private float distance = 0.0f;
    private int turnNum = 0;

    @Override // Function called when service is started from  activity
    public int onStartCommand(Intent intent, int flags, int startId) {
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

        resultReceiver = intent.getParcelableExtra("resultReceiver");
        if(null != intent.getExtras().getString("URL"))
        {
            urlString = intent.getExtras().getString("URL");
            Log.d(TAG, urlString + "in onStartCommand");
            getDirections();
        }
        step = 0;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Creates a new location request
    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(250);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    // Calls the inner class download directions
    private void getDirections() {
        try{
            new DownloadDirections().execute(
                    new URL(urlString));
        }catch(MalformedURLException m) {}

    }

    // Makes a web request for directions from the google API, this is an asynchronous class
    // so doesnt run on the main thread
    private class DownloadDirections extends AsyncTask<URL, Integer, Long> {

        @Override
        protected Long doInBackground(URL... params) {
            Log.d(TAG, "in doInBackground");
            BufferedReader buffer;
            InputStream stream = null;
            HttpsURLConnection connection = null;
            String result = "";
            URL url = params[0];
            Log.d(TAG, url.toString() + "in doInBackground");
            try {
                connection = (HttpsURLConnection) url.openConnection();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                connection.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                connection.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                connection.setRequestMethod("GET");
                // Already true by default but setting just in case; needs to be true since this request
                // is carrying an input (response) body.
                connection.setDoInput(true);
                // Open communications link (network traffic occurs here).
                connection.connect();
                int responseCode = connection.getResponseCode();
                if(responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }
                // Retrieve the response body as an InputStream.
                stream = connection.getInputStream();
                buffer = new BufferedReader(new InputStreamReader(stream));
                String nextLine;
                StringBuilder sb = new StringBuilder();
                while((nextLine = buffer.readLine()) != null) {
                    sb.append(nextLine + "\n");
                    Log.d(TAG, nextLine);
                }

                String directions = sb.toString();
                routeDownloaded = true;
                parser = new JSONParser(directions);
            }
            catch(MalformedURLException m) {}
            catch(IOException e) {}
            finally {
                // Close Stream and disconnect HTTPS connection.
                try {
                    if (stream != null) {
                        stream.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                catch(IOException e) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Long aLong) {
            stopSelf();
            super.onPostExecute(aLong);
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
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
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
        if(!userOnRoute()) {
            updateRouteURL();
            getDirections();
        }
        if(routeDownloaded && parser.getListFull()){
            distance = getDistanceToTurn();
            String distanceString = "" + distance;
            Log.d(TAG, distanceString);
            if(getDistanceToTurn() < 30.0f && !aboutToTurn) {
                lightUpTurnSignal();
                aboutToTurn = true;
            }
            if(getDistanceToTurn() < 15.0f && aboutToTurn) {
                aboutToTurn = false;
                step++;
            }
        }
        sendBundle();
    }

    // Checks if the user is still on the correct route
    private boolean userOnRoute() {

        return true;
    }

    // Light up the appropriate turn signal
    private void lightUpTurnSignal() {
        turnNum++;
        Intent intent = new Intent("directions message");
        direction = parser.getStepsList().get(step + 1).getManeuver() + " " + turnNum;
        //intent.putExtra("direction", direction);
        //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Find the distance between the user and the next turn
    private float getDistanceToTurn() {
        String endOfStepLat = parser.getStepsList().get(step).getEndLat();
        String endOfStepLng = parser.getStepsList().get(step).getEndLng();
        Location turn = new Location("turn");
        turn.setLatitude(Double.parseDouble(endOfStepLat));
        turn.setLongitude(Double.parseDouble(endOfStepLng));
        return currentLocation.distanceTo(turn);
    }

    // Send the up-to-date GPS co ordinates to the maps activity's result receiver
    private void sendBundle() {
        String polyline = null;
        Bundle locationBundle = new Bundle();
        if(routeDownloaded && null != parser.getPolyline())
            polyline = parser.getPolyline();
        String distanceFloat = "" + distance;
        locationBundle.putString("latitude", Double.toString(currentLocation.getLatitude()));
        locationBundle.putString("longitude", Double.toString(currentLocation.getLongitude()));
        locationBundle.putString("polyline", polyline);
        locationBundle.putString("direction", direction);
        locationBundle.putString("distance", distanceFloat);
        resultReceiver.send(0, locationBundle);
        direction = "";
    }

    // Makes a new URL for an updated route to be requested
    private void updateRouteURL() {
    }

    @Override
    public void onConnectionSuspended(int result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

