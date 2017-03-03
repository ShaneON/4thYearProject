package com.fourthyearproject.shane.cycled;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by hp on 22/02/2017.
 */

class MapsResultReceiver extends ResultReceiver
{
    private static final String TAG = "MapsResultReceiver";

    private MapsActivity maps;

    public MapsResultReceiver(Handler handler, MapsActivity m) {
        super(handler);
        maps = m;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        maps.onReceiveGPSUpdate(resultData.getString("latitude"), resultData.getString("longitude"),
                                resultData.getString("polyline"), resultData.getString("direction"),
                                resultData.getString("distance"));
    }
}
