package com.fourthyearproject.shane.cycled;

import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DirectionsIntentService extends IntentService {

    private static final String TAG = "DirectionsIntentService";
    ResultReceiver resultReceiver;
    private BufferedReader buffer;


    public DirectionsIntentService()
    {
        super("DirectionsIntentService");
    }

    public DirectionsIntentService(String name) {
        super(name);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        resultReceiver = intent.getParcelableExtra("resultReceiver");
        InputStream stream = null;
        HttpsURLConnection connection = null;
        String result = "";

        try {
            URL url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=Toronto&destination=Montreal");
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
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            buffer = new BufferedReader(new InputStreamReader(stream));
            String nextLine;
            while((nextLine = buffer.readLine()) != null) {
                result = result + nextLine;
                Log.d(TAG, nextLine);
            }
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
    }



}
