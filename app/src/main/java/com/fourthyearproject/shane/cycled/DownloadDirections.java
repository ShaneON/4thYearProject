package com.fourthyearproject.shane.cycled;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

class DownloadDirections extends AsyncTask<URL, Integer, String> {

    private static final String TAG = "Download Directions";

    private String directions;

    public String getDirections()
    {
        return directions;
    }

    @Override
    protected String doInBackground(URL... params) {
        Log.d(TAG, "in doInBackground");
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
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
            String nextLine;
            StringBuilder sb = new StringBuilder();
            while((nextLine = buffer.readLine()) != null) {
                sb.append(nextLine + "\n");
                Log.d(TAG, nextLine);
            }

            directions = sb.toString();
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
        return directions;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }
}
